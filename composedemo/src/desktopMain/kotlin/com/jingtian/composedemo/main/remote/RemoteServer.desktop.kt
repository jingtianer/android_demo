package com.jingtian.composedemo.main.remote

import com.jingtian.composedemo.multiplatform.*
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
import com.jingtian.composedemo.viewmodels.AuthParams
import com.jingtian.composedemo.viewmodels.AuthSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import java.util.Properties
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.random.Random
import kotlin.random.nextULong

@OptIn(ExperimentalContracts::class)
inline fun <T> ChannelSftp.use(block: (ChannelSftp)->T): T? {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    return try {
        block(this)
    } finally {
        this.disconnect()
    }
}

actual fun createSftpServer(serverName: String, serverId: String, userName: String, port: Int, ip: String, password: String, passwordIv: String?, path: String,
) : SftpServer {
    return SftpServerImpl(serverName, serverId, userName, port, ip, password, passwordIv, path)
}

actual fun sftpServerImplSerializer(): KSerializer<out SftpServer> = SftpServerImpl.serializer()

@kotlinx.serialization.Serializable
class SftpServerImpl : SftpServer {
    constructor(    serverName: String = "新建: ${Random.nextLong(0, Long.MAX_VALUE)}",
                    serverId: String = "",
                    userName: String = "",
                    port: Int = 22,
                    ip: String = "localhost",
                    password: String = "",
                    passwordIv: String? = null,
                    path: String = "/"
    ): super(serverName, serverId, userName, port, ip, password, passwordIv, path)
    @kotlinx.serialization.Transient
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
            val password = Base64Utils.decrypt(password)
            logger("newSession", password)
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
                    return this@SftpServerImpl.password
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
    override suspend fun importFiles(viewModel: AlbumViewModel, album: Album) {
        withContext(Dispatchers.IO) {
            val sftpChannel = connect { tag, msg ->
                viewModel.sendMessage("$tag: $msg")
                logD(tag, msg)
//                 Log.d(tag, msg)
            }
            if (sftpChannel == null) {
                viewModel.sendMessage("失败 未知错误")
                logD("importFiles", "失败 未知错误")
                return@withContext
            }
            val pathSet = DataBase.dbImpl.getAlbumItemDao().getAllAlbumItemWithExtra(album.albumId ?: return@withContext).map { it.fileInfo.filePath }.toSet()
            runCatching {
                val fileInfoList: MutableList<Pair<FileInfo, AlbumItem>> = mutableListOf()
                viewModel.sendMessage("开始")
                logD("importFiles", "开始")
                traverseEntry(sftpChannel, viewModel, pathSet, path, album, fileInfoList)
                viewModel.sendMessage("正在写入数据库")
                logD("importFiles", "正在写入数据库")
                viewModel.importFiles(album, fileInfoList)
            }.getOrElse {
                viewModel.sendMessage("导入失败 $it")
                logE("importFiles", "导入失败", it)
            }
            sftpChannel.disconnect()
        }
    }

    private suspend fun traverseEntry(sftpChannel: ChannelSftp, viewModel: AlbumViewModel, pathSet: Set<String>, root: String, album: Album, fileInfoList: MutableList<Pair<FileInfo, AlbumItem>>) {
        val ls = sftpChannel.ls(root)
        for (entry in ls) {
            val entry = (entry as? ChannelSftp.LsEntry) ?: continue
            viewModel.sendMessage("导入: ${entry.filename}: $root")
            logD("traverseEntry") { "导入: ${entry.filename}: $root" }
            if (entry.attrs.isDir) {
                if (entry.filename == "." || entry.filename == "..") {
                    continue
                }
                traverseEntry(sftpChannel, viewModel, pathSet, "${root}/${entry.filename}", album, fileInfoList)
            } else {
                logD("traverseEntry") { "readFile: ${entry.filename}: $root" }
                readFile(sftpChannel, viewModel, pathSet, root, entry, album, fileInfoList)
            }
            // Log.d("jingtian", "导入: ${entry.filename}: $root")
        }
    }

    private suspend fun readFile(sftpChannel: ChannelSftp, viewModel: AlbumViewModel, pathSet: Set<String>, root: String, entry: LsEntry, album: Album, fileInfoList: MutableList<Pair<FileInfo, AlbumItem>>) {
        val uri = RemoteUriUtils.fromSftpEntry(this, root, entry)
        if (uri.isHidden() || pathSet.contains(uri.path())) {
            return
        }
        val type = uri.mediaType()
        val fileName = uri.fileName()
        val (fileStorageId, _) = FileStorageUtils.getStorage(type).asyncStore(uri)
        val (width, height) = (-1 to -1)//getFileIntrinsicSize(uri, type)
        val fileInfo = FileInfo(
            storageId = fileStorageId,
            fileType = type,
            filePath = uri.path(),
            intrinsicWidth = width,
            intrinsicHeight = height,
            extension = uri.extension()
        )
        val albumItem = AlbumItem(itemName = fileName, albumId = album.albumId ?: DataBase.INVALID_ID)
        fileInfoList.add(fileInfo to albumItem)
    }

    override fun getInitializedCipher(): AuthParams {
        return AuthParams()
    }

    override fun onUpdateAuthSuccess(authResult: AuthSuccess){
        val encodedPassword = Base64Utils.encrypt(this.password)
        this.password = encodedPassword
        this.passwordIv = ""
    }

    override fun onAddSftpServerAuthSuccess(authResult: AuthSuccess) {
        val encodedPassword = Base64Utils.encrypt(this.password)
        this.password = encodedPassword
        this.passwordIv = ""
    }

    override fun getDecryptCipher(server: SftpServer): AuthParams {
        return AuthParams()
    }

    override suspend fun deleteFiles(files: List<RemoteSftpFile>) {
        connect { tag, msg ->
//            Log.d(tag, "deleteFiles: $msg")
        }?.use { channel->
            files.forEach {
                runCatching {
                    channel.rm(it.path())
//                    Log.d("jingtian", "deleteFiles: rm: ${it.path()}")
                }.onFailure { t->
//                    Log.e("jingtian", "deleteFiles: rm: ${it.path()}, fail:\n $t")
                }
            }
        }
    }
}

