package com.jingtian.composedemo.main.remote

import android.net.Uri
import android.util.Log
import com.jingtian.composedemo.utils.ensureDirExist
import com.jingtian.composedemo.utils.ensureFile
import com.jingtian.composedemo.utils.ensureFileExist
import com.jingtian.composedemo.utils.getFileCacheStorageRootDir
import com.jingtian.composedemo.utils.getFileStorageRootDir
import java.io.File
import java.io.FileOutputStream
import java.util.Objects
import java.util.concurrent.ConcurrentHashMap


class RemoteFileStore(serverType: ServerType) {
    private val storageRoot = File(getFileCacheStorageRootDir(), "remote/filestore/server_${serverType.type}")
    init {
        storageRoot.ensureDirExist()
    }

    private val locks = ConcurrentHashMap<String, Any>()

    fun loadFile(originUri: Uri, server: SftpServer): File {
        val realFile = get(originUri)
        // Log.d("jingtian", "读取文件 等待锁 ${originUri.path}")
        synchronized(locks.computeIfAbsent(originUri.toString()) { Any() }) {
            // Log.d("jingtian", "读取文件 获得锁 ${originUri.path}")
            val path = originUri.path ?: "/"
            if (realFile.exists() && realFile.length() > 0) {
                if (realFile.isFile) {
                    return realFile
                }
                realFile.deleteRecursively()
            }
            runCatching<Any, File> {
                getTmp(originUri).ensureFileExist { file->
                    server.connect({ _, msg ->
                        // Log.d("jingtian", "读取文件 $path $msg")
                    })?.use { channel ->
                        // Log.d("jingtian", "读取文件 $path 开始")
                        val bos = FileOutputStream(file)
                        channel.get(path, bos)
                    } ?: throw RuntimeException("连接失败")
                }
            }.fold(onSuccess = { file->
                // Log.d("jingtian", "读取文件 $path 完成")
                file.renameTo(realFile)
            }, onFailure = {
                // Log.d("jingtian", "读取文件 ${originUri.path} 失败 $it")
                realFile.ensureFile()
            })
        }
        return realFile
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