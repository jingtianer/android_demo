package com.jingtian.composedemo.main.remote


import androidx.compose.ui.graphics.ImageBitmap
import com.jingtian.composedemo.dao.model.Album
import com.jingtian.composedemo.dao.model.AlbumItem
import com.jingtian.composedemo.dao.model.FileInfo
import com.jingtian.composedemo.dao.model.LabelInfo
import com.jingtian.composedemo.main.remote.ByteStringFormatter.toString
import com.jingtian.composedemo.multiplatform.MultiplatformFile

import com.jingtian.composedemo.viewmodels.AlbumViewModel
import com.jingtian.composedemo.viewmodels.AuthParams
import com.jingtian.composedemo.viewmodels.AuthSuccess
import kotlinx.io.Buffer
import kotlinx.io.RawSink
import kotlinx.io.RawSource
import kotlin.random.Random
import kotlin.random.nextULong


enum class ServerType(val type: Int) {
    SFTP(0)
}

interface RemoteSftpFile: MultiplatformFile

@kotlinx.serialization.Serializable
abstract class RemoteServer(
    var serverName: String = "新建: ${Random.nextLong(0, Long.MAX_VALUE)}",
    var serverId: String = "",
) {
    abstract suspend fun deleteFiles(files: List<RemoteSftpFile>)
}

expect fun createSftpServer(
        serverName: String = "新建: ${Random.nextLong(0, Long.MAX_VALUE)}",
        serverId: String = "",
        userName: String = "",
        port: Int = 22,
        ip: String = "localhost",
        password: String = "",
      passwordIv: String? = null,
        path: String = "/",
) : SftpServer

@kotlinx.serialization.Serializable
abstract class SftpServer : RemoteServer {
    var userName: String
    var port: Int
    var ip: String
    var password: String
    var passwordIv: String?
    var path: String
    constructor(serverName: String = "新建: ${Random.nextLong(0, Long.MAX_VALUE)}",
                serverId: String = "",
                userName: String = "",
                port: Int = 22,
                ip: String = "localhost",
                password: String = "",
                passwordIv: String? = null,
                path: String = "/"
    ) : super(serverName, serverId) {
        this.userName = userName
        this.port = port
        this.ip = ip
        this.password = password
        this.passwordIv = passwordIv
        this.path = path
    }
    abstract suspend fun importFiles(viewModel: AlbumViewModel, album: Album)

    abstract fun getInitializedCipher(): AuthParams

    abstract fun onUpdateAuthSuccess(authResult: AuthSuccess)

    abstract fun onAddSftpServerAuthSuccess(authResult: AuthSuccess)

    abstract fun getDecryptCipher(server: SftpServer): AuthParams

    abstract suspend fun uploadFileAndStore(album: Album, albumViewModel: AlbumViewModel, albumItem: AlbumItem, labelInfoList: List<LabelInfo>, webSnapshot: ImageBitmap?, uri: MultiplatformFile, onResult: suspend (success: Boolean, errMsg: String?) -> Unit)

    abstract suspend fun uploadFileListAndStore(album: Album, albumViewModel: AlbumViewModel, albumItem: MutableList<Triple<MultiplatformFile, FileInfo, AlbumItem>>, onResult: suspend (success: Boolean, errMsg: String?) -> Unit)

    abstract override suspend fun deleteFiles(files: List<RemoteSftpFile>)
}

fun joinRemotePath(root: String, filename: String): String {
    return when {
        root.isEmpty() || root == "/" -> "/$filename"
        root.endsWith("/") -> "$root$filename"
        else -> "$root/$filename"
    }
}

fun splitFileSuffix(fileName: String) : Pair<String, String> {
    val dotIndex = fileName.indexOf(".", fileName.lastIndexOf("/"))
    if (dotIndex <= 0 || dotIndex == fileName.lastIndex) { // ’.‘ 不存在 或 以 ‘.’ 开头的隐藏文件
        return fileName to ""
    }
    return fileName.substring(0, dotIndex) to fileName.substring(dotIndex + 1)
}

fun fileNameAddSuffix(fileName: String, suffix: String): String {
    val (namePart, ext) = splitFileSuffix(fileName)
    return if (ext.isEmpty()) {
        "$namePart$suffix"
    } else {
        "$namePart$suffix.$ext"
    }
}

fun <T> T.wire(that: T, block: T.(that: T)->Unit) {
    this.block(that)
}

object ByteStringFormatter {
    private val unitList = listOf("B", "KB", "MB", "GB", "TB")
    private fun format(result:Long, remain:Long, unitSuffix:String, unit: Long):String {
        val remainInt = (remain.toDouble() / unit * 100).toInt()
        val remainStr = if (remainInt < 10) {
            "0${remainInt}"
        } else if (remainInt <= 99) {
            "$remainInt"
        } else {
            "99"
        }
        return "${result}.${remainStr} $unitSuffix"
    }

    fun Long.format(): String {
        var result: Long = this
        var remain: Long = 0
        var unit: Long = 1
        for (unitSuffix in unitList.subList(0, unitList.size - 1)) {
            result = this / unit
            remain = this % unit
            if (result < 1024) {
                return format(result, remain, unitSuffix, unit)
            }
            unit = unit shl 10
        }
        return format(result, remain, unitList.last(), unit)
    }
}

inline fun copyWithProgress(
    rawSource: RawSource,
    sink: RawSink,
    crossinline onProgress: (progressText: String) -> Unit
) {
    val buffer = Buffer()
    val bufSize = 8 * 1024L
    var totalWritten = 0L
    var lastReport = 0L
    val reportStep = 256 * 1024L

    while (true) {
        val read = rawSource.readAtMostTo(buffer, bufSize)
        if (read == -1L) break

        sink.write(buffer, read)
        totalWritten += read

        if (totalWritten - lastReport >= reportStep) {
            lastReport = totalWritten
            with(ByteStringFormatter) {
                onProgress(totalWritten.format())
            }
        }
    }

    sink.flush()
    with(ByteStringFormatter) {
        onProgress(totalWritten.format())
    }
}
