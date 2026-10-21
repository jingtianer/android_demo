package com.jingtian.composedemo.main.remote

import com.jingtian.composedemo.utils.CoroutineUtils
import com.jingtian.composedemo.utils.createNewFile
import com.jingtian.composedemo.utils.delete
import com.jingtian.composedemo.utils.deleteRecursively
import com.jingtian.composedemo.utils.exists
import com.jingtian.composedemo.utils.getFileCacheStorageRootDir
import com.jingtian.composedemo.utils.isDirectory
import com.jingtian.composedemo.utils.isFile
import com.jingtian.composedemo.utils.mkdirs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

class RemoteFileStore(serverType: ServerType) {
    private val storageRoot = Path(getFileCacheStorageRootDir(), "remote/filestore/server_${serverType.type}")
    private val locks = mutableMapOf<String, Semaphore>()
    private val locksMutex = Mutex()

    init {
        storageRoot.ensureDirExist()
    }

    suspend fun loadFile(originUri: Uri, server: SftpServer): Path {
        val iosServer = server as? IosSftpServer ?: return get(originUri).ensureFileExist()
        return withContext(Dispatchers.IO) {
            val realFile = get(originUri)
            val tmpFile = getTmp(originUri)
            val lock = lockFor(originUri.raw)
            lock.withPermit {
                if (realFile.exists() && realFile.isFile && fileSize(realFile) > 0L && !tmpFile.exists()) {
                    return@withPermit realFile
                }

                if (realFile.exists()) {
                    if (realFile.isDirectory) {
                        realFile.deleteRecursively()
                    } else {
                        realFile.delete()
                    }
                }

                tmpFile.ensureFileExist()
                val downloadSuccess = iosServer.downloadFile(originUri.path, tmpFile.toString())
                if (!downloadSuccess) {
                    return@withPermit tmpFile
                }

                if (realFile.exists()) {
                    realFile.delete()
                }
                SystemFileSystem.atomicMove(tmpFile, realFile)
                realFile
            }
        }
    }

    fun delete(originUri: Uri) {
        CoroutineUtils.runIOTask({
            val lock = lockFor(originUri.raw)
            lock.withPermit {
                get(originUri).deleteIfExists()
                getTmp(originUri).deleteIfExists()
            }
        })
    }

    fun get(originUri: Uri): Path {
        val fileName = originUri.lastPathSegment
        val path = originUri.path
        return Path(storageRoot, "${filePathHashCode(fileName, path)}_$fileName")
    }

    private suspend fun lockFor(key: String): Semaphore {
        return locksMutex.withLock {
            locks.getOrPut(key) { Semaphore(1) }
        }
    }

    private fun getTmp(originUri: Uri): Path {
        val fileName = originUri.lastPathSegment
        val path = originUri.path
        return Path(storageRoot, "tmp_${filePathHashCode(fileName, path)}_$fileName")
    }

    private fun fileSize(path: Path): Long {
        return SystemFileSystem.metadataOrNull(path)?.size ?: 0L
    }

    private fun filePathHashCode(fileName: String, path: String): Long {
        var result = 17L
        result = 31L * result + path.hashCode()
        result = 31L * result + fileName.hashCode()
        return result
    }
}

private fun Path.ensureDirExist(): Path {
    if (exists()) {
        if (isDirectory) {
            return this
        }
        delete()
    }
    mkdirs()
    return this
}

private fun Path.ensureFileExist(): Path {
    val parent = parent
    if (parent != null && !parent.exists()) {
        parent.mkdirs()
    }
    if (exists()) {
        if (isDirectory) {
            deleteRecursively()
        } else {
            return this
        }
    }
    createNewFile()
    return this
}

private fun Path.deleteIfExists() {
    if (!exists()) {
        return
    }
    if (isDirectory) {
        deleteRecursively()
    } else {
        delete()
    }
}
