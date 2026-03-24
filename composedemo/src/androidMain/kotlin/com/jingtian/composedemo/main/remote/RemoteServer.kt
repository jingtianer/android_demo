package com.jingtian.composedemo.main.remote

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.ChannelSftp.LsEntry
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import com.jcraft.jsch.UserInfo
import com.jingtian.composedemo.dao.DataBase
import com.jingtian.composedemo.dao.model.Album
import com.jingtian.composedemo.dao.model.AlbumItem
import com.jingtian.composedemo.dao.model.FileInfo
import com.jingtian.composedemo.utils.Base64Utils
import com.jingtian.composedemo.utils.FileStorageUtils
import com.jingtian.composedemo.viewmodels.AlbumViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.charset.Charset
import java.security.KeyStore
import java.util.Properties
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec
import kotlin.random.Random
import kotlin.random.nextULong


enum class ServerType(val type: Int) {
    SFTP(0)
}

abstract class RemoteServer(
    var serverName: String = "新建: ${Random.nextULong()}",
    var serverId: String = "",
)

fun <T> ChannelSftp.use(block: (ChannelSftp)->T): T? {
    return try {
        block(this)
    } finally {
        this.disconnect()
    }
}

class SftpServer(
    serverName: String = "新建: ${Random.nextULong()}",
    serverId: String = "",
    var userName: String = "",
    var port: Int = 22,
    var ip: String = "localhost",
    var password: String = "",
    var passwordIv: String? = null,
    var path: String = "/",
) : RemoteServer(serverName, serverId) {
    private var session: Session? = null
    private fun getSession(logger: (String, String)->Unit): Session? {
        val session = session
        if (session != null && session.isConnected) {
            return session
        } else {
            return newSession(logger).also {
                this.session = it
            }
        }
    }
    fun connect(logger: (String, String) -> Unit): ChannelSftp? {
        val session = getSession(logger) ?: return null
        return runCatching {
            val channel: com.jcraft.jsch.Channel = session.openChannel("sftp")
            channel.connect()
            logger("jingtian", "session连接成功")
            val sftpChannel = channel as ChannelSftp
            sftpChannel
        }.getOrElse {
            session.disconnect()
            logger("jingtian", "session连接失败 $it")
            null
        }
    }
    private fun newSession(logger: (String, String)->Unit): Session? {
        val jsch: JSch = runCatching {
            JSch()
        }.getOrElse {
            logger("jingtian", "newSession: JSch创建失败 $it")
            null
        } ?: return null
        val session: Session = runCatching {
            val session = jsch.getSession(
                userName,
                ip,
                port
            )
            val decryptCipher = getDecryptCipher(this)
            val password = decryptCipher?.doFinal(Base64Utils.decryptAsByteArray(password))?.toString(Charset.defaultCharset()) ?: password
            session.setPassword(password)
            val config = Properties()

            config["StrictHostKeyChecking"] = "ask"
            config["PreferredAuthentications"] = "password"
            session.setConfig(config)

            session.userInfo = object : UserInfo {
                override fun getPassphrase(): String? {
                    return null
                }

                override fun getPassword(): String {
                    return this@SftpServer.password
                }

                override fun promptPassword(message: String): Boolean {
                    return true
                }

                override fun promptPassphrase(message: String): Boolean {
                    return false
                }

                override fun promptYesNo(message: String): Boolean {
                    logger("jingtian", "服务器密钥信息：$message")
                    return true
                }

                override fun showMessage(message: String) {
                    logger("jingtian", "完整密钥信息：$message")
                }
            }

            session.connect()
            session
        }.getOrElse {
            logger("jingtian", "newSession: session创建失败 $it")
            null
        } ?: return null
        return session
    }
    suspend fun importFiles(viewModel: AlbumViewModel, album: Album) {
        withContext(Dispatchers.IO) {
            val sftpChannel = connect { tag, msg ->
                viewModel.sendMessage("$tag: $msg")
                // Log.d(tag, msg)
            } ?: return@withContext
            val pathSet = DataBase.dbImpl.getAlbumItemDao().getAllAlbumItemWithExtra(album.albumId ?: return@withContext).map { it.fileInfo.filePath }.toSet()
            runCatching {
                val fileInfoList: MutableList<Pair<FileInfo, AlbumItem>> = mutableListOf()
                traverseEntry(sftpChannel, viewModel, pathSet, path, album, fileInfoList)
                viewModel.sendMessage("正在写入数据库")
                viewModel.importFiles(album, fileInfoList)
            }.getOrElse {
                viewModel.sendMessage("导入失败 $it")
            }
            sftpChannel.disconnect()
        }
    }

    private fun traverseEntry(sftpChannel: ChannelSftp, viewModel: AlbumViewModel, pathSet: Set<String>, root: String, album: Album, fileInfoList: MutableList<Pair<FileInfo, AlbumItem>>) {
        val ls = sftpChannel.ls(root)
        for (entry in ls) {
            val entry = (entry as? ChannelSftp.LsEntry) ?: continue
            viewModel.sendMessage("导入: ${entry.filename}: $root")
            if (entry.attrs.isDir) {
                traverseEntry(sftpChannel, viewModel, pathSet, "${root}/${entry.filename}", album, fileInfoList)
            } else {
                readFile(sftpChannel, viewModel, pathSet, root, entry, album, fileInfoList)
            }
            // Log.d("jingtian", "导入: ${entry.filename}: $root")
        }
    }

    private fun readFile(sftpChannel: ChannelSftp, viewModel: AlbumViewModel, pathSet: Set<String>, root: String, entry: LsEntry, album: Album, fileInfoList: MutableList<Pair<FileInfo, AlbumItem>>) {
        val uri = RemoteUriUtils.fromSftpEntry(this, root, entry)
        if (uri.isHidden || pathSet.contains(uri.path)) {
            return
        }
        val type = uri.mediaType
        val fileName = uri.fileName
        val fileStorageId = FileStorageUtils.getStorage(type)?.asyncStore(uri) ?: DataBase.INVALID_ID
        val (width, height) = (-1 to -1)//getFileIntrinsicSize(uri, type)
        val fileInfo = FileInfo(
            storageId = fileStorageId,
            fileType = type,
            filePath = uri.path,
            intrinsicWidth = width,
            intrinsicHeight = height,
            extension = uri.extension
        )
        val albumItem = AlbumItem(itemName = fileName, albumId = album.albumId ?: DataBase.INVALID_ID)
        fileInfoList.add(fileInfo to albumItem)
    }

    fun getSecretKey(server: RemoteServer, encrypt: Boolean = true): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        if (encrypt) {
            keyStore.deleteEntry(server.serverId)
        }
        if (!keyStore.containsAlias(server.serverId)) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            val keySpec = KeyGenParameterSpec.Builder(
                server.serverId,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
//                .setUserAuthenticationRequired(true)
//                .setInvalidatedByBiometricEnrollment(true)
//                .let {
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//                        it.setUserAuthenticationParameters(0 /* duration */,
//                        KeyProperties.AUTH_BIOMETRIC_STRONG or
//                                KeyProperties.AUTH_DEVICE_CREDENTIAL
//                        )
//                    } else {
//                        it
//                    }
//                }
                .build()
            keyGenerator.init(keySpec)
            keyGenerator.generateKey()
        }
        return keyStore.getKey(server.serverId, null) as SecretKey
    }

    fun getCipher(): Cipher {
        return Cipher.getInstance(
            KeyProperties.KEY_ALGORITHM_AES + "/"
                    + KeyProperties.BLOCK_MODE_CBC + "/"
                    + KeyProperties.ENCRYPTION_PADDING_PKCS7)
    }

    fun getDecryptCipher(server: SftpServer): Cipher? {
        val cipher = getCipher()
        val sk = getSecretKey(server, false)
        val iv = Base64Utils.decryptAsByteArray(server.passwordIv ?: return null)

        // 兼容 AndroidKeyStore2：优先 GCMParameterSpec，失败则用 IvParameterSpec
        try {
            val gcmSpec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, sk, gcmSpec)
        } catch (e: Exception) {
            val ivSpec = IvParameterSpec(iv)
            cipher.init(Cipher.DECRYPT_MODE, sk, ivSpec)
        }
        return cipher
    }
}


