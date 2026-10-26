package com.jingtian.composedemo.main.remote

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.compose.ui.graphics.ImageBitmap
import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.ChannelSftp.LsEntry
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import com.jcraft.jsch.SftpProgressMonitor
import com.jcraft.jsch.UserInfo
import com.jingtian.composedemo.dao.DataBase
import com.jingtian.composedemo.dao.model.Album
import com.jingtian.composedemo.dao.model.AlbumItem
import com.jingtian.composedemo.dao.model.FileInfo
import com.jingtian.composedemo.dao.model.LabelInfo
import com.jingtian.composedemo.multiplatform.MultiplatformFile
import com.jingtian.composedemo.utils.Base64Utils
import com.jingtian.composedemo.utils.BitMapCachePool
import com.jingtian.composedemo.utils.FileStorageUtils
import com.jingtian.composedemo.viewmodels.AlbumViewModel
import com.jingtian.composedemo.viewmodels.AuthParams
import com.jingtian.composedemo.viewmodels.AuthSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.Buffer
import kotlinx.io.RawSink
import kotlinx.io.RawSource
import kotlinx.io.asSink
import kotlinx.io.buffered
import kotlinx.serialization.KSerializer
import java.io.FileOutputStream
import java.nio.charset.Charset
import java.security.KeyStore
import java.util.Properties
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.io.path.Path
import kotlin.random.Random
import kotlin.random.nextULong
import java.io.ByteArrayOutputStream
import java.nio.file.Path

@OptIn(ExperimentalContracts::class)
inline fun <T> ChannelSftp.use(block: (ChannelSftp)->T): T {
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
            val decryptCipher = getDecryptCipher(this).cipher
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
//                 Log.d(tag, msg)
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

    private suspend fun traverseEntry(sftpChannel: ChannelSftp, viewModel: AlbumViewModel, pathSet: Set<String>, root: String, album: Album, fileInfoList: MutableList<Pair<FileInfo, AlbumItem>>) {
        val ls = sftpChannel.ls(root)
        for (entry in ls) {
            val entry = (entry as? ChannelSftp.LsEntry) ?: continue
            viewModel.sendMessage("导入: ${entry.filename}: $root")
            if (entry.attrs.isDir) {
                if (entry.filename == "." || entry.filename == "..") {
                    continue
                }
                traverseEntry(sftpChannel, viewModel, pathSet, "${root}/${entry.filename}", album, fileInfoList)
            } else {
                readFile(sftpChannel, viewModel, pathSet, root, entry.filename, album, fileInfoList)
            }
            // Log.d("jingtian", "导入: ${entry.filename}: $root")
        }
    }

    private suspend fun readFile(sftpChannel: ChannelSftp, viewModel: AlbumViewModel, pathSet: Set<String>, root: String, entry: String, album: Album, fileInfoList: MutableList<Pair<FileInfo, AlbumItem>>?): Pair<FileInfo, AlbumItem>? {
        val uri = RemoteUriUtils.fromSftpEntry(this, root, entry)
        if (uri.isHidden() || pathSet.contains(uri.path())) {
            return null
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
        return (fileInfo to albumItem).also {
            fileInfoList?.add(it)
        }
    }

    private fun getSecretKey(encrypt: Boolean = true): SecretKey {
        val server: RemoteServer = this
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

    private fun getCipher(): Cipher {
        return Cipher.getInstance(
            KeyProperties.KEY_ALGORITHM_AES + "/"
                    + KeyProperties.BLOCK_MODE_CBC + "/"
                    + KeyProperties.ENCRYPTION_PADDING_PKCS7)
    }

    override fun getInitializedCipher(): AuthParams {
        val cipher = getCipher()
        val sk = this.getSecretKey()
        cipher.init(Cipher.ENCRYPT_MODE, sk)
        return AuthParams(cipher)
    }

    override fun onUpdateAuthSuccess(authResult: AuthSuccess){
        authResult.result.cryptoObject?.cipher?.let { cipher->
            cipher.doFinal(this.password.toByteArray(Charset.defaultCharset()))?.let { cipherResult->
                val encodedPassword = Base64Utils.encrypt(cipherResult)
                this.password = encodedPassword
                this.passwordIv = Base64Utils.encrypt(cipher.iv)
            }
        }
    }

    override fun onAddSftpServerAuthSuccess(authResult: AuthSuccess) {
        authResult.result.cryptoObject?.cipher?.let { cipher ->
            cipher.doFinal(this.password.toByteArray(Charset.defaultCharset()))?.let { cipherResult ->
                val encodedPassword = Base64Utils.encrypt(cipherResult)
                this.password = encodedPassword
                this.passwordIv = Base64Utils.encrypt(cipher.iv)
            }
        }
    }

    override fun getDecryptCipher(server: SftpServer): AuthParams {
        val cipher = getCipher()
        val sk = getSecretKey(false)
        val iv = Base64Utils.decryptAsByteArray(server.passwordIv ?: return AuthParams(null))

        // 兼容 AndroidKeyStore2：优先 GCMParameterSpec，失败则用 IvParameterSpec
        try {
            val gcmSpec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, sk, gcmSpec)
        } catch (e: Exception) {
            val ivSpec = IvParameterSpec(iv)
            cipher.init(Cipher.DECRYPT_MODE, sk, ivSpec)
        }
        return AuthParams(cipher)
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

    override suspend fun uploadFileAndStore(
        album: Album,
        albumViewModel: AlbumViewModel,
        albumItem: AlbumItem,
        labelInfoList: List<LabelInfo>,
        webSnapshot: ImageBitmap?,
        uri: MultiplatformFile,
        onResult: suspend (success: Boolean, errMsg: String?) -> Unit
    ) {
        val connection = connect { tag, msg ->
            albumViewModel.sendMessage("uploadFileAndStore: $tag, $msg")
        }
        if (connection == null) {
            onResult(false, "connection 创建失败")
            return
        }
        connection.use { channel ->
            uploadFileInner(channel, album, albumViewModel, listOf(FileUploadInfo(labelInfoList, webSnapshot, albumItem, uri)), onResult)
        }
    }

    override suspend fun uploadFileListAndStore(
        album: Album,
        albumViewModel: AlbumViewModel,
        albumItem: MutableList<Triple<MultiplatformFile, FileInfo, AlbumItem>>,
        onResult: suspend (success: Boolean, errMsg: String?) -> Unit
    ) {
        val connection = connect { tag, msg ->
            albumViewModel.sendMessage("uploadFileAndStore: $tag, $msg")
        }
        if (connection == null) {
            onResult(false, "connection 创建失败")
            return
        }
        connection.use { channel ->
            uploadFileInner(channel, album, albumViewModel, albumItem.map {
                FileUploadInfo(listOf(), null, it.third, it.first)
            }) { _, _ ->  }
        }
        onResult(true, null)
    }

    data class FileUploadInfo(
        val labelInfoList: List<LabelInfo>,
        val webSnapshot: ImageBitmap?,
        val albumItem: AlbumItem,
        val uri: MultiplatformFile,
    )

    private suspend fun uploadFileInner(
        channel: ChannelSftp,
        album: Album,
        albumViewModel: AlbumViewModel,
        fileUploadInfoList: List<FileUploadInfo>,
        onResult: suspend (success: Boolean, errMsg: String?) -> Unit
    ) {
        val insertSuccessFileInfo = fileUploadInfoList.mapIndexedNotNull { index, fileUploadInfo ->
            val (labelInfoList, webSnapshot, albumItem, uri) = fileUploadInfo
            when(uri) {
                is RemoteSftpFile -> {
                    onResult(false, "文件: ${uri.path()}已经是RemoteFile了")
                    return@mapIndexedNotNull null
                }
                else -> {
                    val fileInfo = runCatching {
                        val fileName = fileNameAddSuffix(albumItem.itemName, "_${System.currentTimeMillis()}_${albumItem.hashCode()}")
                        val fileNameConflict = channel.ls(this.path).mapNotNull { (it as? ChannelSftp.LsEntry)?.filename }.contains(fileName)
                        if (fileNameConflict) {
                            onResult(false, "文件名冲突")
                            return@runCatching null
                        }
                        val targetFile = joinRemotePath(this.path, fileName)
                        val progressUpdater = ProgressUpdater.createProgressUpdater(albumViewModel, fileUploadInfoList.size) { progressStr-> "正在上传: $progressStr" }
                        uri.inputStream()?.use { `is` ->
                            channel.put(targetFile).asSink().use { os->
                                copyWithProgress(`is`, os) { progress->
                                    progressUpdater(progress, index)
                                }
                            }
                        } ?: run {
                            onResult(false, "源文件inputStream打开失败")
                            return@runCatching null
                        }
                        val fileEntry = channel.ls(targetFile).firstNotNullOfOrNull { (it as? LsEntry) }
                        if (fileEntry == null) {
                            onResult(false, "文件上传失败")
                            return@runCatching null
                        }
                        val fileInfo = readFile(channel, albumViewModel, setOf(), path, fileName, album, null)
                        if (fileInfo == null) {
                            onResult(false, "文件创建失败: readFile")
                            return@runCatching null
                        }
                        albumItem.wire(fileInfo.second) { that->
                            this.itemName = that.itemName
                        }
                        onResult(true,  null)
                        BitMapCachePool.loadImage(fileInfo.first) { webSnapshot }
                        return@runCatching fileInfo
                    }.getOrElse {  e->
                        onResult(false, "文件拷贝发生错误: ${e.message}")
                        return@getOrElse null
                    }
                    withContext(Dispatchers.Main) {
                        uri.onStoreFinish()
                    }
                    return@mapIndexedNotNull fileInfo?.let {
                        Triple(it.first, it.second, labelInfoList)
                    }
                }
            }
        }
        albumViewModel.importFileList(album, insertSuccessFileInfo)
    }

    override suspend fun readMetadata(viewModel: AlbumViewModel): String {
        val activeSession = connect { tag, msg ->
            viewModel.sendMessage("readMetadata: $tag, $msg")
        } ?: return ""
        val metadataPath = metadataStorePath()
        val metadataFileName = metadataFilename()
        val targetFile = joinRemotePath(metadataPath, metadataFileName)
        val metadataExist = activeSession.ls(this.path).mapNotNull { (it as? ChannelSftp.LsEntry)?.filename }.contains(metadataFileName)
        if (metadataExist) {
            val metadataFileContent = runCatching {
                val progressUpdater = ProgressUpdater.createProgressUpdater(viewModel, 1) { progressStr-> "正在下载: $metadataFileName $progressStr" }
                val bos = ByteArrayOutputStream()
                bos.use {
                    activeSession.get(path, bos, object : SftpProgressMonitor {
                        override fun init(op: Int, src: String?, dest: String?, max: Long) {
                        }

                        override fun count(count: Long): Boolean {
                            progressUpdater(count, 0)
                            return true
                        }

                        override fun end() {
                        }
                    })
                    bos.flush()
                }
                bos.toByteArray().decodeToString()
            }.onFailure {
                viewModel.sendMessage("下载$metadataFileName 失败: $it")
            }.getOrDefault(null)
            return metadataFileContent ?: ""
        }
        return ""
    }

    override suspend fun writeMetadata(viewModel: AlbumViewModel, metadataJson: Path): Boolean {
        val connection = connect { tag, msg ->
            viewModel.sendMessage("uploadFileAndStore: $tag, $msg")
        } ?: return false
        val metadataPath = metadataStorePath()
        val metadataFileName = metadataFilename()
        val targetFile = joinRemotePath(metadataPath, metadataFileName)
//        if (!ensureSftpDir(activeSession, metadataPath)) {
//            viewModel.sendMessage("上传$metadataFileName 失败，远程目录创建失败")
//            return false
//        }
        val progressUpdater = ProgressUpdater.createProgressUpdater(viewModel, 1) { progressStr-> "正在上传: $metadataFileName $progressStr" }
        val success = connection.use {
            runCatching {
                Buffer().apply {
                    write(metadataJson.encodeToByteArray())
                }.use { `is`->
                    connection.put(targetFile).asSink().use { os->
                        copyWithProgress(`is`, os) { progress->
                            progressUpdater(progress, 0)
                        }
                    }
                }
                true
            }.onFailure {
                viewModel.sendMessage("上传$metadataFileName 失败: $it")
            }.getOrDefault(false)
        }
        return success ?: false
    }
}
