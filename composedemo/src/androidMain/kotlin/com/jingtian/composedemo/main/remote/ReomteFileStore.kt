package com.jingtian.composedemo.main.remote

import android.net.Uri
import android.util.Log
import com.jcraft.jsch.SftpProgressMonitor
import com.jingtian.composedemo.utils.ensureDirExist
import com.jingtian.composedemo.utils.ensureFile
import com.jingtian.composedemo.utils.ensureFileExist
import com.jingtian.composedemo.utils.getFileCacheStorageRootDir
import com.jingtian.composedemo.utils.getFileStorageRootDir
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


class RemoteFileStore(serverType: ServerType) {
    private val storageRoot = File(getFileCacheStorageRootDir(), "remote/filestore/server_${serverType.type}")
    init {
        storageRoot.ensureDirExist()
    }

    private val locks = ConcurrentHashMap<String, ReentrantLock>()
    private val executor = ThreadPoolExecutor(16, 16,
        0L, TimeUnit.MILLISECONDS,
        LinkedBlockingQueue(), ThreadPoolExecutor.DiscardOldestPolicy()
    )

    fun loadFile(originUri: Uri, server: SftpServer): File {
        val realFile = get(originUri)
        val task = executor.submit<File> task@{
//            Log.d("jingtian", "读取文件 等待锁 ${originUri.path}")
            val tmpFile = getTmp(originUri)
            val lock = locks.computeIfAbsent(originUri.toString()) { ReentrantLock() }
            try {
                lock.lockInterruptibly()
                val path = originUri.path ?: "/"
//                Log.d("jingtian", "读取文件 获得锁 ${originUri.path}")
                if (realFile.exists() && realFile.length() > 0 && !tmpFile.exists()) {
                    if (realFile.isFile) {
                        return@task realFile
                    }
                }
                if (realFile.exists()) {
                    if (realFile.isFile) {
                        realFile.delete()
                    } else {
                        realFile.deleteRecursively()
                    }
                }
                runCatching {
                    tmpFile.ensureFileExist { file->
                        server.connect({ _, msg ->
                            // Log.d("jingtian", "读取文件 $path $msg")
                        })?.use { channel ->
//                            Log.d("jingtian", "读取文件 $path 开始")
                            val bos = FileOutputStream(file)
                            channel.get(path, bos)
                        } ?: throw RuntimeException("连接失败")
                    }
                }.fold(onSuccess = { file->
                    file.renameTo(realFile)
//                    Log.d("jingtian", "读取文件 $path 完成")
                }, onFailure = {
//                    Log.d("jingtian", "读取文件 ${originUri.path} 失败 $it")
                    return@task tmpFile
                })
            } catch (e : Exception) {
                return@task tmpFile.ensureFileExist {  }
            } finally {
                lock.unlock()
            }
            return@task realFile
        }
        return task.get()

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