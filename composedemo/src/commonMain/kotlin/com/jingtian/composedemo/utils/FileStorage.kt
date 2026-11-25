package com.jingtian.composedemo.utils

import androidx.compose.ui.graphics.ImageBitmap
import com.jingtian.composedemo.BuildKonfig
import com.jingtian.composedemo.dao.DataBase
import com.jingtian.composedemo.dao.model.FileInfo
import com.jingtian.composedemo.dao.model.FileStorageInfo
import com.jingtian.composedemo.dao.model.FileType
import com.jingtian.composedemo.main.http.file.HttpMultiplatformFile
import com.jingtian.composedemo.main.remote.RemoteSftpFile
import com.jingtian.composedemo.main.remote.ServerStorage
import com.jingtian.composedemo.multiplatform.MultiplatformFile
import com.jingtian.composedemo.multiplatform.WeakRef
import com.jingtian.composedemo.multiplatform.getMultiplatformFileFactory
import com.jingtian.composedemo.multiplatform.newReentrantLock
import com.jingtian.composedemo.multiplatform.readAllBytesOrNull
import com.jingtian.composedemo.multiplatform.use
import com.jingtian.composedemo.utils.BitMapCachePool.calculateScaleFactor
import com.jingtian.composedemo.utils.BitMapCachePool.scale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.io.Buffer
import kotlinx.io.files.Path

expect fun getFileStorageRootDir(): Path
expect fun getDBStorageRootDir(): Path
expect fun getFileCacheStorageRootDir(): Path

object FileStorageUtils {
    private const val RANK_IMAGE_STORE_ROOT_DIR = "fileStorage"
    private const val RANK_IMAGE_STORE_DIR = "${RANK_IMAGE_STORE_ROOT_DIR}/filetype_"
    private const val RANK_IMAGE_STORE_PREFIX = "file_"

    fun checkRootDir() {
        val storeDir = Path(getFileStorageRootDir(), RANK_IMAGE_STORE_ROOT_DIR)
        storeDir.ensureDir()
    }

    private val imageStore by lazy {
        FileStorage(FileType.IMAGE)
    }

    private val videoStore by lazy {
        FileStorage(FileType.VIDEO)
    }

    private val audioStore by lazy {
        FileStorage(FileType.AUDIO)
    }

    private val htmlStore by lazy {
        FileStorage(FileType.HTML)
    }

    private val regularStore by lazy {
        FileStorage(FileType.RegularFile)
    }

    fun getStorage(fileType: FileType): FileStorage {
        return when(fileType) {
            FileType.IMAGE -> imageStore
            FileType.VIDEO -> videoStore
            FileType.AUDIO -> audioStore
            FileType.HTML -> htmlStore
            FileType.RegularFile -> regularStore
        }
    }

    class FileStorage(val fileType: FileType) {

        private val rankImageStoreDir: String = RANK_IMAGE_STORE_DIR + fileType.value

        private val uriCache = mutableMapOf<Long, WeakRef<MultiplatformFile>>()
        private val storeJobs = mutableMapOf<Long, WeakRef<Job>>()

        private val lock = newReentrantLock()

        private fun rankImagePrefix(id: Long): String {
            return "${RANK_IMAGE_STORE_PREFIX}${id}"
        }

        private val dao by lazy {
            DataBase.dbImpl.getFileStorageDao()
        }

        suspend fun get(id: Long, fileInfo: FileInfo): MultiplatformFile? {
//            println("FileStorage: fileType:${fileType.name}, id=$id, fileInfo=${fileInfo.extension}")
            withContext(Dispatchers.IO) {
                if (id == -1L) {
                    return@withContext null
                }
                storeJobs[id]?.get()?.join()
                val cachedUri = uriCache[id]?.get()
                if (cachedUri != null) {
                    return@withContext cachedUri
                }
                val content = dao.queryFileStorageInfo(id)?.content
//            println("FileStorage: fileType:${fileType.name}, id=$id, storageFile=$storageFile")
                return@withContext if (content != null) {
//                println("FileStorage: exist fileType:${fileType.name}, id=$id, storageFile=$storageFile")
                    getMultiplatformFileFactory().fromFile(content, fileInfo.extension)
                } else {
//                println("FileStorage: not exist fileType:${fileType.name}, id=$id, storageFile=$storageFile")
                    null
                }
            }
        }

        suspend fun delete(id: Long) {
            withContext(Dispatchers.IO) {
                val storeDir = Path(getFileStorageRootDir(), rankImageStoreDir)
                val storageFile = Path(storeDir, rankImagePrefix(id))
                dao.deleteFileStoreInfo(id)
                storageFile.ensureNotExist()
            }
        }

        suspend fun asyncStore(oldId: Long, uri: MultiplatformFile): Pair<Long, Job> {
            val id = allocateId(uri, oldId)
            val job = CoroutineUtils.runIOTask({
                doStore(uri, id)
            })
            storeJobs[id] = WeakRef(job)
            return id to job
        }

        private suspend fun doStore(uri: MultiplatformFile, id: Long) {
            storeByType(uri, id)
            uri.onStoreFinish()
        }

        private fun isRemoteFile(uri: MultiplatformFile): Boolean {
            return BuildKonfig.isDesktop || uri is RemoteSftpFile || uri is HttpMultiplatformFile
        }

        private suspend fun allocateId(uri: MultiplatformFile, oldId: Long = -1): Long {
            return lock.use {
                val id = if (oldId != -1L) {
                    oldId
                } else {
                    dao.insertFileStoreInfo(FileStorageInfo(content = ByteArray(0)))
                }
                val rawContent = if (isRemoteFile(uri)) {
                    uri.fileStoreInputStream().use { it.readAllBytesOrNull() }
                } else {
                    getStoreFile(id, uri.fileName()).toString().encodeToByteArray()
                }
                dao.updateFileStorageInfo(FileStorageInfo(
                    id = id,
                    content = rawContent
                ))
                uriCache.remove(oldId)
                uriCache[id] = WeakRef(uri)
                id
            }
        }

        suspend fun asyncStore(uri: MultiplatformFile): Pair<Long, Job> {
            val id = allocateId(uri)
            val job = CoroutineUtils.runIOTask({
                doStore(uri, id)
            })
            storeJobs[id] = WeakRef(job)
            return id to job
        }

        private suspend fun storeByType(uri: MultiplatformFile, id: Long) {
            if (isRemoteFile(uri)) {
                return
            }
            val realFile = getStoreFile(id, uri.fileName())
            val bytes = realFile.relativeTo(getFileStorageRootDir()).toString().encodeToByteArray()
            val buffer = Buffer()
            buffer.write(bytes, 0, bytes.size)
            uri.inputStream()?.use { `is`->
                `is`.copyTo(realFile)
            }
        }

        private suspend fun getStoreFile(id: Long, fileName: String?): Path {
            val storeDir = Path(getFileStorageRootDir(), rankImageStoreDir, rankImagePrefix(id))
            lock.use {
                storeDir.ensureDir()
            }
            return Path(storeDir, fileName ?: "real_$id")
        }
    }

    private suspend fun getThumbnailByType(fileType: FileType, uri: MultiplatformFile): ImageBitmap? {
        return when (fileType) {
            FileType.VIDEO -> uri.videoThumbnail()
            FileType.AUDIO -> uri.audioThumbnail()
            FileType.HTML -> null
            FileType.RegularFile -> null
            FileType.IMAGE -> null
        }
    }


    fun getThumbnail(
        fileType: FileType,
        coroutineScope: CoroutineScope,
        videoUri: MultiplatformFile,
        maxWidth: Int = -1,
        maxHeight: Int = -1,
        onLoadBitmap: suspend (ImageBitmap?)->Unit
    ): Job = coroutineScope.launch(Dispatchers.IO) {
        val bitmap = getThumbnailByType(fileType, videoUri)
        if (bitmap == null) {
            withContext(Dispatchers.Main) {
                onLoadBitmap(null)
            }
            return@launch
        }
        val compressedBitmap = bitmap.scale(calculateScaleFactor(bitmap.width, bitmap.height, maxWidth, maxHeight))
        withContext(Dispatchers.Main) {
            onLoadBitmap(compressedBitmap)
        }
    }

    suspend fun getThumbnail(
        fileInfo: FileInfo,
        coroutineScope: CoroutineScope,
        videoUri: MultiplatformFile,
        maxWidth: Int = -1,
        maxHeight: Int = -1,
        onLoadBitmap: suspend (ImageBitmap?)->Unit
    ): Job = coroutineScope.launch(Dispatchers.IO) {
        BitMapCachePool.loadThumbnail(fileInfo, maxWidth, maxHeight) {
            getThumbnailByType(fileInfo.fileType, videoUri)
        }.second?.let {
            withContext(Dispatchers.Main) {
                onLoadBitmap(it)
            }
            return@launch
        }
        val bitmap = getThumbnailByType(fileInfo.fileType, videoUri)
        withContext(Dispatchers.Main) {
            onLoadBitmap(bitmap)
        }
    }

    suspend fun getFileIntrinsicSize(uri: MultiplatformFile, mediaType: FileType) = when(mediaType) {
        FileType.IMAGE -> {
            uri.imageRatio()
        }
        FileType.VIDEO -> {
            uri.videoThumbnail()?.let {
                it.width to it.height
            } ?: (-1 to -1)
        }
        FileType.AUDIO -> {
            uri.audioThumbnail()?.let {
                it.width to it.height
            } ?: (-1 to -1)
        }
        FileType.HTML -> {
            1 to 1
        }
        else -> -1 to -1
    }
}