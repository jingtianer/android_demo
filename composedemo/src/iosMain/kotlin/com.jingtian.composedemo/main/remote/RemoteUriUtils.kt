package com.jingtian.composedemo.main.remote

import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.net.toFile
import androidx.core.net.toUri
import com.jcraft.jsch.ChannelSftp.LsEntry
import com.jingtian.composedemo.dao.model.FileType
import com.jingtian.composedemo.multiplatform.MultiplatformFileImpl
import com.jingtian.composedemo.utils.SerializationUtils.toInputStream
import kotlinx.io.Buffer
import kotlinx.io.RawSource
import kotlinx.io.files.Path
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
class RemoteSftpFileImpl(
    val originUri: Uri,
    private val server: SftpServer,
) : MultiplatformFileImpl(fileStore.get(originUri).toUri()) {

    companion object {
        private val fileStore by lazy {
            RemoteFileStore(ServerType.SFTP)
        }
    }

    private suspend fun getContentFile(): File {
        return fileStore.loadFile(originUri, server)
    }


    private fun getOrignExtension(): String {
        return originUri.lastPathSegment?.split(".")?.lastOrNull() ?: ""
    }

    private fun getOrignFileName(): String {
        return originUri.lastPathSegment ?: ""
    }

    override suspend fun getUri(): Uri = getContentFile().toUri()

    override suspend fun fileName(): String = getOrignFileName()

    override suspend fun isHidden(): Boolean = fileName().startsWith(".")
                && fileName() != "."
                && fileName() != ".."

    override suspend fun mediaType(): FileType = getMediaTypeByExtension(getOrignExtension())

    override suspend fun extension(): String = getOrignExtension()

    override suspend fun inputStream(): RawSource = InputStreamRawSource(FileInputStream(getContentFile()))

    override suspend fun fileStoreInputStream(): RawSource = InputStreamRawSource(originUri.toInputStream())

    override suspend fun videoThumbnail(): ImageBitmap? {
            return getVideoThumbnail(getContentFile().toUri())?.asImageBitmap()
        }

    override suspend fun audioThumbnail(): ImageBitmap? {
            return getAudioThumbnail(getContentFile().toUri())?.asImageBitmap()
        }
    override suspend fun imageRatio(): Pair<Int, Int> {
            return getImageRatio(getContentFile().toUri())
        }
    override suspend fun file(): Path = Path(getContentFile().absolutePath)

    override fun onDelete() {
        fileStore.delete(originUri)
    }

    override suspend fun path(): String = originUri.path ?: "/"
}

private class InputStreamRawSource(
    private val input: InputStream
) : RawSource {

    override fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
        require(byteCount >= 0) { "byteCount < 0: $byteCount" }
        if (byteCount == 0L) return 0L

        val toRead = byteCount.coerceAtMost(DEFAULT_BUFFER_SIZE.toLong()).toInt()
        val buffer = ByteArray(toRead)
        val read = input.read(buffer)
        if (read == -1) return -1L

        sink.write(buffer, 0, read)
        return read.toLong()
    }

    override fun close() {
        input.close()
    }

    companion object {
        private const val DEFAULT_BUFFER_SIZE = 8 * 1024
    }
}

object RemoteUriUtils {
    fun fromSftpEntry(server: SftpServer, root: String, entry: LsEntry): RemoteSftpFileImpl {
        val uri = Uri.Builder()
            .scheme("jingtianSftp")
            .authority(server.serverId)
            .path(root)
            .appendPath(entry.filename)
            .build()
        // Log.d("jingtian", "fromSftpEntry: $uri")
        return RemoteSftpFileImpl(uri, server).apply {
            // Log.d("jingtian", "fromSftpEntry: $this")
        }
    }

    fun parse(uri: Uri): RemoteSftpFileImpl? {
        val schema = uri.scheme ?: return null
        val serverId = uri.authority ?: return null
        return when(schema) {
            "jingtianSftp" -> {
                val server = ServerStorage.getStorage<SftpServer>(ServerType.SFTP).getServer(serverId) ?: return null
                RemoteSftpFileImpl(uri, server)
            }
            else -> null
        }
    }

    fun serverTypeAndId(file: RemoteSftpFileImpl): Pair<ServerType, String>? {
        val uri = file.originUri
//        Log.d("jingtian", "serverTypeAndId: uri=$uri")
        val schema = uri.scheme ?: return null
        val serverId = uri.authority ?: return null
        return when(schema) {
            "jingtianSftp" -> {
                ServerType.SFTP to serverId
            }
            else -> null
        }
    }
}
