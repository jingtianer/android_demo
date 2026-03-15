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
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
class RemoteSftpFileImpl(
    private val originUri: Uri,
    private val server: SftpServer,
) : MultiplatformFileImpl(fileStore.get(originUri).toUri()) {

    companion object {
        private val fileStore by lazy {
            RemoteFileStore(ServerType.SFTP)
        }
    }

    private val contentFile: File
        get() {
            return fileStore.loadFile(originUri, server)
        }


    private fun getOrignExtension(): String {
        return originUri.lastPathSegment?.split(".")?.lastOrNull() ?: ""
    }

    private fun getOrignFileName(): String {
        return originUri.lastPathSegment ?: ""
    }

    override val uri: Uri
        get() = contentFile.toUri()

    override val fileName: String
        get() = getOrignFileName()

    override val isHidden: Boolean
        get() = fileName.startsWith(".")
                && fileName != "."
                && fileName != ".."

    override val mediaType: FileType
        get() = getMediaTypeByExtension(getOrignExtension())

    override val extension: String
        get() = getOrignExtension()

    override val inputStream: InputStream
        get() = FileInputStream(contentFile)

    override val fileStoreInputStream: InputStream
        get() = originUri.toInputStream()

    override val videoThumbnail: ImageBitmap?
        get() {
            return getVideoThumbnail(contentFile.toUri())?.asImageBitmap()
        }

    override val audioThumbnail: ImageBitmap?
        get() {
            return getAudioThumbnail(contentFile.toUri())?.asImageBitmap()
        }
    override val imageRatio: Pair<Int, Int>
        get() {
            return getImageRatio(contentFile.toUri())
        }
    override val file: File
        get() = contentFile
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
}