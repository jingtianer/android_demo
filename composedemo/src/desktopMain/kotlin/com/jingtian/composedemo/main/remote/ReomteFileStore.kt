package com.jingtian.composedemo.main.remote

import com.jcraft.jsch.SftpProgressMonitor
import com.jingtian.composedemo.utils.CoroutineUtils
import com.jingtian.composedemo.utils.getFileCacheStorageRootDir
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.charset.Charset
import java.util.Objects
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume

class Uri(val lastPathSegment: String, val path: String, val authority: String, val scheme: String) {
    constructor(path: String, authority: String, scheme: String): this(path.split("/").lastOrNull() ?: "", path, authority, scheme)
    val raw = "${scheme}://${authority}/${path}"
    fun toInputStream() : InputStream {
        return ByteArrayInputStream(raw.toByteArray(Charsets.UTF_8))
    }
    fun getExtension(): String {
        return this.lastPathSegment.split(".").lastOrNull() ?: ""
    }
}

fun File.ensureFileExist(onAbsent: (File)->Unit):File {
    if (exists()) {
        if (isDirectory) {
            deleteRecursively()
        } else {
            return this
        }
    }
    this.parentFile?.takeIf { !it.exists() }?.mkdirs()
    this.createNewFile()
    onAbsent(this)
    return this
}

fun File.ensureDirExist():File {
    if (exists()) {
        if (isDirectory) {
            return this
        } else {
            delete()
        }
    }
    this.mkdirs()
    return this
}

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
                            tmpFile.ensureFileExist { file->
                                server.connect({ _, msg ->
//                                    Log.d("jingtian", "读取文件 $path $msg")
                                })?.use { channel ->
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
                                            }
                                        })
                                        bos.flush()
                                    }
                                } ?: throw RuntimeException("连接失败")
                                if (cancelable.isCancelled) {
                                    throw InterruptedException()
                                }
                            }
                        }.fold(onSuccess = { file->
                            file.renameTo(realFile)
//                    Log.d("jingtian", "读取文件 $path 完成")
                        }, onFailure = {
//                    Log.d("jingtian", "读取文件 ${originUri.path} 失败 $it")
//                            Log.d("jingtian", "end3: ${originUri.path}, ${it.message}")
                            cancelable.resume(tmpFile)
                            return@suspendCancellableCoroutine
                        })
                    } catch (e : Exception) {
//                        Log.d("jingtian", "end2: ${originUri.path}")
                        cancelable.resume(tmpFile.ensureFileExist {  })
                        return@suspendCancellableCoroutine
                    }
//                    Log.d("jingtian", "end1: ${originUri.path}")
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