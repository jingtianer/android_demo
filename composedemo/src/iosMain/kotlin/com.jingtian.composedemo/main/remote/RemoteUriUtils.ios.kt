package com.jingtian.composedemo.main.remote

import androidx.compose.ui.graphics.ImageBitmap
import cocoapods.NMSSHT7.NMSFTPFile
import com.jingtian.composedemo.dao.model.FileType
import com.jingtian.composedemo.multiplatform.MultiplatformFileImpl
import com.jingtian.composedemo.multiplatform.logD
import com.jingtian.composedemo.multiplatform.logE
import com.jingtian.composedemo.multiplatform.use
import com.jingtian.composedemo.utils.extension
import com.jingtian.composedemo.utils.uriToImageSize
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.usePinned
import kotlinx.io.Buffer
import kotlinx.io.RawSource
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

class Uri(
    val lastPathSegment: String,
    val path: String,
    val authority: String,
    val scheme: String,
) {
    constructor(path: String, authority: String, scheme: String) : this(
        lastPathSegment = path.substringAfterLast("/", missingDelimiterValue = path),
        path = path,
        authority = authority,
        scheme = scheme,
    )

    val raw: String = "$scheme://$authority$path"

    fun getExtension(): String = lastPathSegment.substringAfterLast(".", missingDelimiterValue = "")
}

class RemoteSftpFileImpl(
    val originUri: Uri,
    private val server: SftpServer,
) : MultiplatformFileImpl(fileStore.get(originUri), originUri.getExtension()), RemoteSftpFile {

    companion object {
        private val fileStore by lazy {
            RemoteFileStore(ServerType.SFTP)
        }
    }

    override fun getServerInstance(): SftpServer {
        return server
    }

    private suspend fun getContentFile(): Path {
        return fileStore.loadFile(originUri, server)
    }

    private fun getOrignExtension(): String {
        return originUri.lastPathSegment?.split(".")?.lastOrNull() ?: ""
    }

    private fun getOrignFileName(): String {
        logD("getOrignFileName") {
            Exception().stackTraceToString()
        }
        return originUri.lastPathSegment ?: ""
    }

    override suspend fun fileName(): String = getOrignFileName()

    override suspend fun isHidden(): Boolean {
        val fileName = fileName()
        return fileName.startsWith(".") && fileName != "." && fileName != ".."
    }

    override suspend fun mediaType(): FileType = when(getOrignExtension().lowercase()) {
        in imageExtensions -> FileType.IMAGE
        in videoExtensions -> FileType.VIDEO
        in audioExtensions -> FileType.AUDIO
        in htmlExtensions -> FileType.HTML
        else -> FileType.RegularFile
    }

    override suspend fun extension(): String = getOrignExtension()

    override suspend fun inputStream(): RawSource? = SystemFileSystem.source(getContentFile())

    override suspend fun fileStoreInputStream(): RawSource? {
        logD("jingtian", "fileStoreInputStream: ${originUri.raw}")
        return StringRawSource(originUri.raw)
    }

    override suspend fun videoThumbnail(): ImageBitmap? {
        return getVideoThumbnail(getContentFile())
    }

    override suspend fun audioThumbnail(): ImageBitmap? {
        return getAudioCoverImage(getContentFile())
    }

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun imageRatio(): Pair<Int, Int> {
        return runCatching {
            SystemFileSystem.source(getContentFile()).usePinned {
                uriToImageSize(it.get())
            }
        }.getOrElse { 1 to 1 }
    }

    override suspend fun file(): Path = getContentFile()

    override fun onDelete() {
        fileStore.delete(originUri)
    }

    override suspend fun path(): String = originUri.path
}

private class StringRawSource(
    value: String,
) : RawSource {
    private val bytes = value.encodeToByteArray()
    private var offset = 0

    override fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
        require(byteCount >= 0) { "byteCount < 0: $byteCount" }
        if (byteCount == 0L) return 0L
        if (offset >= bytes.size) return -1L

        val read = minOf(byteCount.toInt(), bytes.size - offset)
        sink.write(bytes, offset, read)
        offset += read
        return read.toLong()
    }

    override fun close() = Unit
}

object RemoteUriUtils {
    @OptIn(ExperimentalForeignApi::class)
    fun fromSftpEntry(server: SftpServer, root: String, filename: String): RemoteSftpFileImpl {
        return RemoteSftpFileImpl(
            Uri(
                path = joinRemotePath(root, filename),
                authority = server.serverId,
                scheme = SFTP_SCHEME,
            ),
            server,
        )
    }

    fun parse(uri: Uri): RemoteSftpFileImpl? {
        val serverId = uri.authority
        return when (uri.scheme) {
            SFTP_SCHEME -> {
                val server = ServerStorage.getStorage<SftpServer>(ServerType.SFTP).getServer(serverId)
                    ?: return null
                RemoteSftpFileImpl(uri, server)
            }
            else -> null
        }
    }

    fun serverTypeAndId(file: RemoteSftpFileImpl): Pair<ServerType, String>? {
        return when (file.originUri.scheme) {
            SFTP_SCHEME -> ServerType.SFTP to file.originUri.authority
            else -> null
        }
    }

    private const val SFTP_SCHEME = "jingtianSftp"
}

actual fun serverTypeAndId(file: RemoteSftpFile): Pair<ServerType, String>? {
    return RemoteUriUtils.serverTypeAndId(file as RemoteSftpFileImpl)
}
