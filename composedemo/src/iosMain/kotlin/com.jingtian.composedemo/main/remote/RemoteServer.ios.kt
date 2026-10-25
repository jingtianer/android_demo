package com.jingtian.composedemo.main.remote

import androidx.compose.ui.graphics.ImageBitmap
import cocoapods.NMSSHT7.NMSFTPFile
import cocoapods.NMSSHT7.NMSSHSession
import com.jingtian.composedemo.dao.DataBase
import com.jingtian.composedemo.dao.model.Album
import com.jingtian.composedemo.dao.model.AlbumItem
import com.jingtian.composedemo.dao.model.FileInfo
import com.jingtian.composedemo.dao.model.LabelInfo
import com.jingtian.composedemo.main.remote.ByteStringFormatter.format
import com.jingtian.composedemo.multiplatform.MultiplatformFile
import com.jingtian.composedemo.utils.Base64Utils
import com.jingtian.composedemo.utils.BitMapCachePool
import com.jingtian.composedemo.utils.FileStorageUtils
import com.jingtian.composedemo.viewmodels.AlbumViewModel
import com.jingtian.composedemo.viewmodels.AuthParams
import com.jingtian.composedemo.viewmodels.AuthSuccess
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.io.asNSInputStream
import kotlinx.io.asSink
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.serialization.KSerializer
import platform.Foundation.NSInputStream
import platform.Foundation.NSString
import platform.Foundation.writeToFile
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

actual fun createSftpServer(
    serverName: String,
    serverId: String,
    userName: String,
    port: Int,
    ip: String,
    password: String,
    passwordIv: String?,
    path: String,
): SftpServer = IosSftpServer(
    serverName = serverName,
    serverId = serverId,
    userName = userName,
    port = port,
    ip = ip,
    password = password,
    passwordIv = passwordIv,
    path = path,
)

actual fun sftpServerImplSerializer(): KSerializer<out SftpServer> = IosSftpServer.serializer()

@kotlinx.serialization.Serializable
class IosSftpServer : SftpServer {
    constructor(
        serverName: String = "新建: ${Random.nextLong(0, Long.MAX_VALUE)}",
        serverId: String = "",
        userName: String = "",
        port: Int = 22,
        ip: String = "localhost",
        password: String = "",
        passwordIv: String? = null,
        path: String = "/",
    ) : super(serverName, serverId, userName, port, ip, password, passwordIv, path)

    @OptIn(ExperimentalForeignApi::class)
    private fun getSession(logger: (String, String) -> Unit): NMSSHSession? {
        val newSession = runCatching {
            logger("jingtian", "开始连接 SFTP $userName@$ip:$port")
            val created = NMSSHSession.connectToHost(ip, port = port.toLong(), withUsername = userName)
            if (!created.connected) {
                logger("jingtian", "session连接失败 ${created.lastError?.localizedDescription ?: ""}")
                return null
            }
            val decryptedPassword = runCatching {
                Base64Utils.decrypt(password)
            }.getOrElse {
                password
            }
            if (!created.authenticateByPassword(decryptedPassword)) {
                val error = created.lastError?.localizedDescription ?: ""
                created.disconnect()
                logger("jingtian", "密码认证失败 $error")
                return null
            }
            created
        }.getOrElse {
            logger("jingtian", "session创建失败 $it")
            null
        }
        return newSession
    }

    @OptIn(ExperimentalForeignApi::class)
    fun connect(logger: (String, String) -> Unit): NMSSHSession? {
        val activeSession = getSession(logger) ?: return null
        val sftp = activeSession.sftp
        if (sftp.connected || sftp.connect()) {
            logger("jingtian", "session连接成功")
            return activeSession
        }
        activeSession.disconnect()
        logger("jingtian", "sftp连接失败 ${activeSession.lastError?.localizedDescription ?: ""}")
        return null
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun disconnect() {
    }

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun importFiles(viewModel: AlbumViewModel, album: Album) {
        withContext(Dispatchers.IO) {
            val activeSession = connect { tag, msg ->
                viewModel.sendMessage("$tag: $msg")
            } ?: return@withContext

            val albumId = album.albumId ?: return@withContext
            val pathSet = DataBase.dbImpl.getAlbumItemDao()
                .getAllAlbumItemWithExtra(albumId)
                .map { it.fileInfo.filePath }
                .toSet()

            runCatching {
                val fileInfoList = mutableListOf<Pair<FileInfo, AlbumItem>>()
                traverseEntry(activeSession, viewModel, pathSet, path, album, fileInfoList)
                viewModel.sendMessage("正在写入数据库")
                viewModel.importFiles(album, fileInfoList)
            }.getOrElse {
                viewModel.sendMessage("导入失败 $it")
            }
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private suspend fun traverseEntry(
        activeSession: NMSSHSession,
        viewModel: AlbumViewModel,
        pathSet: Set<String>,
        root: String,
        album: Album,
        fileInfoList: MutableList<Pair<FileInfo, AlbumItem>>,
    ) {
        val entries = activeSession.sftp.contentsOfDirectoryAtPath(root) ?: return
        for (item in entries) {
            val entry = item as? NMSFTPFile ?: continue
            val filename = entry.filename
            if (filename == "." || filename == "..") {
                continue
            }
            viewModel.sendMessage("导入: $filename: $root")
            val entryPath = joinRemotePath(root, filename)
            if (entry.isDirectory) {
                traverseEntry(activeSession, viewModel, pathSet, entryPath, album, fileInfoList)
            } else {
                readFile(viewModel, pathSet, root, entry.filename, album, fileInfoList)
            }
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private suspend fun readFile(
        viewModel: AlbumViewModel,
        pathSet: Set<String>,
        root: String,
        entry: String,
        album: Album,
        fileInfoList: MutableList<Pair<FileInfo, AlbumItem>>?,
    ): Pair<FileInfo, AlbumItem>? {
        val uri = RemoteUriUtils.fromSftpEntry(this, root, entry)
        if (uri.isHidden() || pathSet.contains(uri.path())) {
            return null
        }

        val type = uri.mediaType()
        val fileName = uri.fileName() ?: ""
        val (fileStorageId, _) = FileStorageUtils.getStorage(type).asyncStore(uri)
        val (width, height) = -1 to -1 //FileStorageUtils.getFileIntrinsicSize(uri, type)
        val fileInfo = FileInfo(
            storageId = fileStorageId,
            fileType = type,
            filePath = uri.path(),
            intrinsicWidth = width,
            intrinsicHeight = height,
            extension = uri.extension(),
        )
        val albumItem = AlbumItem(itemName = fileName, albumId = album.albumId ?: DataBase.INVALID_ID)
        viewModel.sendMessage("正在导入: $fileName")
        return (fileInfo to albumItem).also {
            fileInfoList?.add(it)
        }
    }

    override fun getInitializedCipher(): AuthParams = AuthParams()

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


    override fun getDecryptCipher(server: SftpServer): AuthParams = AuthParams()

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun deleteFiles(files: List<RemoteSftpFile>) {
        withContext(Dispatchers.IO) {
            val activeSession = connect { _, _ -> } ?: return@withContext
            files.forEach { file ->
                runCatching {
                    activeSession.sftp.removeFileAtPath(file.path())
                }
            }
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    suspend fun downloadFile(path: String, localPath: String): Boolean = withContext(Dispatchers.IO) {
        val activeSession = connect { _, _ -> } ?: return@withContext false
        activeSession.sftp.contentsAtPath(path)
            ?.writeToFile(localPath, atomically = true)
            ?: false
    }

    @OptIn(ExperimentalForeignApi::class, ExperimentalTime::class)
    override suspend fun uploadFileAndStore(
        album: Album,
        albumViewModel: AlbumViewModel,
        albumItem: AlbumItem,
        labelInfoList: List<LabelInfo>,
        webSnapshot: ImageBitmap?,
        uri: MultiplatformFile,
        onResult: suspend (success: Boolean, errMsg: String?) -> Unit
    ) {
        val activeSession = connect { _, _ -> }
        if (activeSession == null) {
            onResult(false, "connection 创建失败")
            return
        }
        try {
            val uploadedFileList = uploadFileInner(
                activeSession = activeSession,
                album = album,
                albumViewModel = albumViewModel,
                fileUploadInfoList = listOf(FileUploadInfo(labelInfoList, webSnapshot, albumItem, uri)),
                onResult = onResult,
            )
            val uploadedFile = uploadedFileList.firstOrNull() ?: return
            try {
                albumViewModel.importFile(album, uploadedFile.first, uploadedFile.second, uploadedFile.third)
                onResult(true, null)
            } catch (e: Exception) {
                onResult(false, "写入数据库失败: $e")
            }
        } finally {
            activeSession.disconnect()
        }
    }

    @OptIn(ExperimentalForeignApi::class, ExperimentalTime::class)
    override suspend fun uploadFileListAndStore(
        album: Album,
        albumViewModel: AlbumViewModel,
        albumItem: MutableList<Triple<MultiplatformFile, FileInfo, AlbumItem>>,
        onResult: suspend (success: Boolean, errMsg: String?) -> Unit
    ) {
        val activeSession = connect { tag, msg ->
            albumViewModel.sendMessage("uploadFileAndStore: $tag, $msg")
        }
        if (activeSession == null) {
            onResult(false, "connection 创建失败")
            return
        }
        try {
            val uploadedFileList = uploadFileInner(
                activeSession = activeSession,
                album = album,
                albumViewModel = albumViewModel,
                fileUploadInfoList = albumItem.map { (uri, _, albumItem) ->
                    FileUploadInfo(listOf(), null, albumItem, uri)
                },
                onResult = { _, _ -> },
            )
            albumViewModel.importFileList(album, uploadedFileList)
            if (uploadedFileList.size == albumItem.size) {
                onResult(true, null)
            } else {
                onResult(false, "部分文件上传失败")
            }
        } catch (e: Exception) {
            onResult(false, "写入数据库失败: $e")
        } finally {
            activeSession.disconnect()
        }
    }

    data class FileUploadInfo(
        val labelInfoList: List<LabelInfo>,
        val webSnapshot: ImageBitmap?,
        val albumItem: AlbumItem,
        val uri: MultiplatformFile,
    )

    @OptIn(ExperimentalForeignApi::class, ExperimentalTime::class)
    private suspend fun uploadFileInner(
        activeSession: NMSSHSession,
        album: Album,
        albumViewModel: AlbumViewModel,
        fileUploadInfoList: List<FileUploadInfo>,
        onResult: suspend (success: Boolean, errMsg: String?) -> Unit,
    ): List<Triple<FileInfo, AlbumItem, List<LabelInfo>>> {
        ensureSftpDir(activeSession, path)
        return fileUploadInfoList.mapIndexedNotNull { index, fileUploadInfo ->
            val (labelInfoList, webSnapshot, albumItem, uri) = fileUploadInfo
            if (uri is RemoteSftpFile) {
                onResult(false, "文件: ${uri.path()}已经是RemoteFile了")
                return@mapIndexedNotNull null
            }

            val fileName = fileNameAddSuffix(albumItem.itemName, "_${Clock.System.now().toEpochMilliseconds()}_${albumItem.hashCode()}")
            val targetFile = joinRemotePath(this.path, fileName)
            val uploadSuccess = runCatching {
                if (activeSession.sftp.fileExistsAtPath(targetFile)) {
                    onResult(false, "文件名冲突")
                    return@runCatching false
                }
                var lastReportProgress = -1L
                val reportGap = 256 * 1024L
                val writeSuccess = activeSession.sftp.writeFileAtPath(uri.path(), targetFile) { progressNS ->
                    val progress = progressNS.toLong()
                    if (progress - lastReportProgress >= reportGap || lastReportProgress == -1L) {
                        lastReportProgress = progress
                        val progressStr = with(ByteStringFormatter) {
                            progress.format()
                        }
                        albumViewModel.sendMessage("上传中: $progressStr (${index + 1} / ${fileUploadInfoList.size})")
                    }
                    true
                }
                if (!writeSuccess) {
                    onResult(false, "文件上传失败")
                    return@runCatching false
                }
                true
            }.getOrElse { e ->
                onResult(false, "文件拷贝发生错误: ${e.message}")
                false
            }

            withContext(Dispatchers.Main) {
                uri.onStoreFinish()
            }
            if (!uploadSuccess) {
                return@mapIndexedNotNull null
            }

            val fileInfo = readFile(albumViewModel, setOf(), path, fileName, album, null)
            if (fileInfo == null) {
                onResult(false, "文件创建失败: readFile")
                return@mapIndexedNotNull null
            }
            albumItem.wire(fileInfo.second) { that ->
                this.itemName = that.itemName
            }
            BitMapCachePool.loadImage(fileInfo.first) { webSnapshot }
            Triple(fileInfo.first, albumItem, labelInfoList)
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
fun ensureSftpDir(sftp: NMSSHSession, remoteDir: String): Boolean {
    if (remoteDir.isEmpty()) return true
    if (sftp.sftp.fileExistsAtPath(remoteDir)) {
        return true
    }
    val lastSlashIndex = remoteDir.lastIndexOf('/')
    if (lastSlashIndex <= 0) {
        return sftp.sftp.createDirectoryAtPath(remoteDir)
    }
    val parentDir = remoteDir.substring(0, lastSlashIndex)
    if (!ensureSftpDir(sftp, parentDir)) {
        return false
    }
    return sftp.sftp.createDirectoryAtPath(remoteDir)
}
