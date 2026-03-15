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
import com.jingtian.composedemo.utils.ensureFileExist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream

class RemoteSftpFileImpl(
    private val originUri: Uri,
    val server: SftpServer,
) : MultiplatformFileImpl(fileStore.get(originUri).toUri()) {
    companion object {
        private val fileStore by lazy {
            RemoteFileStore(ServerType.SFTP)
        }
    }
    val path = originUri.path ?: "/"

    private val realUri = fileStore.get(originUri).toUri()

    private val contentFile by lazy {
        runBlocking {
            val originFile = realUri.toFile()
            withContext(Dispatchers.IO) {
                originFile.ensureFileExist { file->
                    val path = originUri.path ?: "/"
                    server.connect({ _, msg ->
                        Log.d("jingtian", "读取文件 $path $msg")
                    })?.use { channel ->
                        val bos = FileOutputStream(file)
                        channel.get(path, bos)
                        Log.d("jingtian", "读取文件 $path 完成")
                    }
                }
            }
            originFile
        }
    }

    override val uri: Uri
        get() = contentFile.toUri()

    override val fileName: String
        get() = contentFile.toUri().lastPathSegment ?: ""

    override val isHidden: Boolean
        get() = fileName.startsWith(".")
                && fileName != "."
                && fileName != ".."

    override val mediaType: FileType
        get() = getMediaTypeByExtension(contentFile.extension)

    override val extension: String
        get() = contentFile.extension

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
        Log.d("jingtian", "fromSftpEntry: $uri")
        return RemoteSftpFileImpl(uri, server).apply {
            Log.d("jingtian", "fromSftpEntry: $this")
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