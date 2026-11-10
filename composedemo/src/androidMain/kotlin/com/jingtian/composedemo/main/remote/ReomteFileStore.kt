package com.jingtian.composedemo.main.remote

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.room.concurrent.AtomicBoolean
import com.jcraft.jsch.SftpProgressMonitor
import com.jingtian.composedemo.base.app
import com.jingtian.composedemo.utils.CoroutineUtils
import com.jingtian.composedemo.utils.ensureDirExist
import com.jingtian.composedemo.utils.ensureFile
import com.jingtian.composedemo.utils.ensureFileExist
import com.jingtian.composedemo.utils.ensureIsFile
import com.jingtian.composedemo.utils.getFileCacheStorageRootDir
import com.jingtian.composedemo.utils.getFileStorageRootDir
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Objects
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.RejectedExecutionHandler
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.coroutines.resume


class RemoteFileStore(serverType: ServerType) {
    private val storageRoot = File(File(getFileCacheStorageRootDir().toString()), "remote/filestore/server_${serverType.type}")
    init {
        storageRoot.ensureDirExist()
    }

    private val locks = ConcurrentHashMap<String, Semaphore>()

    suspend fun loadFile(originUri: Uri, server: SftpServer): File {
        val server = server as SftpServerImpl
        return withContext(Dispatchers.IO) {
            val realFile = get(originUri)
            val tmpFile = getTmp(originUri)
            val lock = locks.computeIfAbsent(originUri.toString()) { Semaphore(1) }
            return@withContext lock.withPermit {
                suspendCancellableCoroutine { cancelable->
//                    Log.d("jingtian", "start: ${originUri.path}")
                    try {
                        val path = originUri.path ?: "/"
//                Log.d("jingtian", "读取文件 获得锁 ${originUri.path}")
                        if (realFile.exists() && realFile.length() > 0 && !tmpFile.exists()) {
                            if (realFile.isFile) {
//                                Log.d("jingtian", "end5: ${originUri.path}")
                                cancelable.resume(realFile)
                                return@suspendCancellableCoroutine
                            }
                        }
                        if (realFile.exists()) {
                            if (realFile.isFile) {
                                realFile.delete()
                            } else {
                                realFile.deleteRecursively()
                            }
                        }
                        if (cancelable.isCancelled) {
//                            Log.d("jingtian", "canceled 2: ${originUri.path}")
                            throw InterruptedException()
                        } else {
//                            Log.d("jingtian", "start download: ${originUri.path}")
                        }
                        runCatching {
                            val isSuccess = arrayOf(false)
                            tmpFile.ensureIsFile { file->
                                server.connect({ _, msg ->
//                                    Log.d("jingtian", "读取文件 $path $msg")
                                })?.use { channel ->
                                    cancelable.invokeOnCancellation {
                                        runCatching { channel.disconnect() }
                                    }
//                                    Log.d("jingtian", "读取文件 $path 开始")
                                    if (cancelable.isCancelled) {
//                                        Log.d("jingtian", "canceled 1: ${originUri.path}")
                                        throw InterruptedException()
                                    } else {
//                                        Log.d("jingtian", "start download: ${originUri.path}")
                                    }
                                    val bos = FileOutputStream(file)
                                    bos.use {
                                        channel.get(path, bos, object : SftpProgressMonitor {
                                            override fun init(op: Int, src: String?, dest: String?, max: Long) {
                                            }

                                            override fun count(count: Long): Boolean {
                                                if (cancelable.isCancelled) {
//                                                    Log.d("jingtian", "canceled 0: ${originUri.path}")
                                                    return false
                                                }
                                                return true
                                            }

                                            override fun end() {
                                                isSuccess[0] = true
                                            }
                                        })
                                        bos.flush()
                                    }
                                } ?: throw RuntimeException("连接失败")
                                if (cancelable.isCancelled) {
                                    throw InterruptedException()
                                }
                            }.takeIf { isSuccess[0] }
                        }.fold(onSuccess = { file->
                            if (file == null) {
                                cancelable.resume(tmpFile)
                                return@suspendCancellableCoroutine
                            }
                            file.renameTo(realFile)
                        }, onFailure = {
                            cancelable.resume(tmpFile)
                            return@suspendCancellableCoroutine
                        })
                    } catch (e : Exception) {
                        cancelable.resume(tmpFile.ensureFileExist {  })
                        return@suspendCancellableCoroutine
                    }
                    cancelable.resume(realFile)
                    return@suspendCancellableCoroutine
                }
            }
        }
    }

    fun delete(originUri: Uri) {
        CoroutineUtils.runIOTask({
            val lock = locks.computeIfAbsent(originUri.toString()) { Semaphore(1) }
            try {
                lock.acquire()
                get(originUri).delete()
                getTmp(originUri).delete()
            } finally {
                lock.release()
            }
        })
    }

    fun get(originUri: Uri): File {
        val fileName = originUri.lastPathSegment ?: ""
        val path = originUri.path ?: "/"
        return File(storageRoot, "${filePathHashCode(fileName, path)}_$fileName")
    }

    private fun getTmp(originUri: Uri): File {
        val fileName = originUri.lastPathSegment ?: ""
        val path = originUri.path ?: "/"
        return File(storageRoot, "tmp_${filePathHashCode(fileName, path)}_$fileName")
    }

    private fun filePathHashCode(fileName: String, path: String): Long {
        var result = 17L
        result = 31L * result + Objects.hashCode(path)
        result = 31L * result + Objects.hashCode(fileName)
        return result
    }
}