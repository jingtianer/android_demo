package com.jingtian.composedemo.main.remote

import cocoapods.NMSSHT7.NMSFTPFile
import cocoapods.NMSSHT7.NMSSHSession
import com.jingtian.composedemo.dao.DataBase
import com.jingtian.composedemo.dao.model.Album
import com.jingtian.composedemo.dao.model.AlbumItem
import com.jingtian.composedemo.dao.model.FileInfo
import com.jingtian.composedemo.utils.Base64Utils
import com.jingtian.composedemo.utils.FileStorageUtils
import com.jingtian.composedemo.viewmodels.AlbumViewModel
import com.jingtian.composedemo.viewmodels.AuthParams
import com.jingtian.composedemo.viewmodels.AuthSuccess
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import platform.Foundation.writeToFile
import kotlin.random.Random

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
                readFile(viewModel, pathSet, root, entry, album, fileInfoList)
            }
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private suspend fun readFile(
        viewModel: AlbumViewModel,
        pathSet: Set<String>,
        root: String,
        entry: NMSFTPFile,
        album: Album,
        fileInfoList: MutableList<Pair<FileInfo, AlbumItem>>,
    ) {
        val uri = RemoteUriUtils.fromSftpEntry(this, root, entry)
        if (uri.isHidden() || pathSet.contains(uri.path())) {
            return
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
        fileInfoList.add(fileInfo to albumItem)
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
}

internal fun joinRemotePath(root: String, filename: String): String {
    return when {
        root.isEmpty() || root == "/" -> "/$filename"
        root.endsWith("/") -> "$root$filename"
        else -> "$root/$filename"
    }
}
