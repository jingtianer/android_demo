package com.jingtian.composedemo.main.remote


import androidx.compose.ui.graphics.ImageBitmap
import com.jingtian.composedemo.dao.model.Album
import com.jingtian.composedemo.dao.model.AlbumItem
import com.jingtian.composedemo.dao.model.FileInfo
import com.jingtian.composedemo.dao.model.LabelInfo
import com.jingtian.composedemo.multiplatform.MultiplatformFile
import com.jingtian.composedemo.multiplatform.logE
import com.jingtian.composedemo.utils.CoroutineUtils
import com.jingtian.composedemo.utils.share.ShareUtils

import com.jingtian.composedemo.viewmodels.AlbumViewModel
import com.jingtian.composedemo.viewmodels.AuthParams
import com.jingtian.composedemo.viewmodels.AuthSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import kotlinx.io.Buffer
import kotlinx.io.RawSink
import kotlinx.io.RawSource
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.ExperimentalTime


enum class ServerType(val type: Int) {
    SFTP(0)
}

interface RemoteSftpFile: MultiplatformFile {
    fun getServerInstance(): SftpServer
}

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

    @kotlinx.serialization.Transient
    private val metadataSyncManager = MetadataSyncManager()

    abstract suspend fun importFiles(viewModel: AlbumViewModel, album: Album)

    abstract fun getInitializedCipher(): AuthParams

    abstract fun onUpdateAuthSuccess(authResult: AuthSuccess)

    abstract fun onAddSftpServerAuthSuccess(authResult: AuthSuccess)

    abstract fun getDecryptCipher(server: SftpServer): AuthParams

    abstract suspend fun uploadFileAndStore(album: Album, albumViewModel: AlbumViewModel, albumItem: AlbumItem, labelInfoList: List<LabelInfo>, webSnapshot: ImageBitmap?, uri: MultiplatformFile, onResult: suspend (success: Boolean, errMsg: String?) -> Unit)

    abstract suspend fun uploadFileListAndStore(album: Album, albumViewModel: AlbumViewModel, albumItem: MutableList<Triple<MultiplatformFile, FileInfo, AlbumItem>>, onResult: suspend (success: Boolean, errMsg: String?) -> Unit)

    protected abstract suspend fun readMetadata(viewModel: AlbumViewModel): String
    protected abstract suspend fun writeMetadata(viewModel: AlbumViewModel, metadataStr: String): Boolean

    suspend fun pullMetadata(viewModel: AlbumViewModel, onFinish: suspend (success: Boolean, errMsg: String?)->Unit = { _, _ -> }) {
        metadataSyncManager.enqueue(MetadataSyncManager.SyncType.PULL, onFinish) {
            val remoteMetadata = readMetadata(viewModel)
            viewModel.sendMessage("正在将metadata写入数据库")
            ShareUtils.importSharedDb(viewModel, remoteMetadata)
        }
    }

    suspend fun pushMetadata(viewModel: AlbumViewModel, onFinish: suspend (success: Boolean, errMsg: String?)->Unit = { _, _ -> }) {
        metadataSyncManager.enqueue(MetadataSyncManager.SyncType.PUSH, onFinish) {
            viewModel.sendMessage("正在导出本地数据")
            val localMetadata = ShareUtils.getMetadata()
            writeMetadata(viewModel, localMetadata)
        }
    }

    abstract override suspend fun deleteFiles(files: List<RemoteSftpFile>)

    fun metadataStorePath(): String {
        return joinRemotePath(path, ".composeDemoMetadata")
    }

    fun metadataFilename(): String {
        return ".metadata.json"
    }

    fun isComposeDemoMetadata(path: String, fileName: String): Boolean {
        if (path.isEmpty()) {
            return false
        }
        return path.substring(0, if (path.last() == '/') path.length - 1 else path.length) == metadataStorePath() && fileName == metadataFilename()
    }

    class MetadataSyncManager {
        enum class SyncType {
            PULL, PUSH;
        }

        private class StatefulJob(val job: suspend ()->Unit, val callback: ArrayDeque<suspend (success: Boolean, errMsg: String?)->Unit>)

        private val syncJobQueue = ArrayDeque<Pair<SyncType, StatefulJob>>()

        private var runningQueue: Job? = null

        suspend fun enqueue(syncType: SyncType, onFinish: suspend (success: Boolean, errMsg: String?) -> Unit, job: suspend ()->Unit) {
            withContext(Dispatchers.Main) {
                if (syncJobQueue.isEmpty()) {
                    syncJobQueue.addLast(Pair(syncType, StatefulJob(job, ArrayDeque(listOf(onFinish)))))
                } else {
                    val callbackQueue = ArrayDeque<suspend (success: Boolean, errMsg: String?) -> Unit>()
                    callbackQueue.addLast(onFinish)
                    while (syncJobQueue.isNotEmpty() && syncJobQueue.last().first == syncType) {
                        val removedJob = syncJobQueue.removeLast()
                        val removedCallbackQueue = removedJob.second.callback
                        while (removedCallbackQueue.isNotEmpty()) {
                            callbackQueue.addFirst(removedCallbackQueue.removeLast())
                        }
                    }
                    syncJobQueue.addLast(Pair(syncType, StatefulJob(job, callbackQueue)))
                }
                awakeRunningQueue()
            }
        }

        fun awakeRunningQueue() {
            CoroutineUtils.runIOTask({
                withContext(Dispatchers.Main) {
                    if (runningQueue == null) {
                        runningQueue = CoroutineUtils.runIOTask({
                            while (true) {
                                val job = withContext(Dispatchers.Main) {
                                    if (syncJobQueue.isNotEmpty()) {
                                        val (_, job) = syncJobQueue.removeFirst()
                                        job
                                    } else {
                                        null
                                    }
                                }

                                job ?: break

                                runCatching {
                                    job.job()
                                }.onFailure { e->
                                    withContext(Dispatchers.Main) {
                                        job.callback.forEach {
                                            it.invoke(false, e.message)
                                        }
                                    }
                                }.onSuccess {
                                    withContext(Dispatchers.Main) {
                                        job.callback.forEach {
                                            it.invoke(true, null)
                                        }
                                    }
                                }
                            }
                        }, onFailure = {
                            runningQueue = null
                            logE("awakeRunningQueue", "onFailure", it)
                        }) {
                            runningQueue = null
                            awakeRunningQueue()
                        }
                    }
                }
            })
        }
    }
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
    crossinline onProgress: (progress: Long) -> Unit
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
            onProgress(totalWritten)

        }
    }

    sink.flush()
    onProgress(totalWritten)
}

object ProgressUpdater {
    private const val REPORT_GAP = 256 * 1024
    private const val REPORT_TIME_GAP = 200
    @OptIn(ExperimentalTime::class)
    fun createProgressUpdater(viewModel: AlbumViewModel, totalTaskCount: Int, outputFormater: (String)->String): (progress: Long, taskIndex: Int) -> Unit {
        var lastReportProgress = -1L
        var lastReportTime = 0L
        var lastReportTaskIndex = -1
        val progressUpdater: (progress: Long, taskIndex: Int) -> Unit = { progress, taskIndex->
            if (progress - lastReportProgress >= REPORT_GAP || taskIndex != lastReportTaskIndex || lastReportProgress == -1L || Clock.System.now().toEpochMilliseconds() - lastReportTime >= REPORT_TIME_GAP) {
                lastReportProgress = progress
                lastReportTaskIndex = taskIndex
                lastReportTime = Clock.System.now().toEpochMilliseconds()
                val progressStr = with(ByteStringFormatter) {
                    progress.format()
                }
                viewModel.sendMessage("${outputFormater(progressStr)} (${taskIndex + 1} / $totalTaskCount)")
            }
        }
        return progressUpdater
    }
}