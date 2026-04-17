package com.jingtian.composedemo.utils

import androidx.compose.ui.graphics.ImageBitmap
import com.jingtian.composedemo.BuildKonfig
import com.jingtian.composedemo.dao.model.FileInfo
import com.jingtian.composedemo.dao.model.FileType
import com.jingtian.composedemo.multiplatform.MultiplatformFile
import com.jingtian.composedemo.multiplatform.WeakRef
import com.jingtian.composedemo.multiplatform.getLongStorage
import com.jingtian.composedemo.multiplatform.getMultiplatformFileFactory
import com.jingtian.composedemo.multiplatform.newReentrantLock
import com.jingtian.composedemo.utils.FileStorageUtils.extension
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.io.Buffer
import kotlinx.io.RawSource
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

expect fun getFileStorageRootDir(): Path
expect fun getFileCacheStorageRootDir(): Path

object FileStorageUtils {
    private const val RANK_IMAGE_STORE_ROOT_DIR = "fileStorage"
    private const val RANK_IMAGE_STORE_DIR = "${RANK_IMAGE_STORE_ROOT_DIR}/file_"
    private const val RANK_IMAGE_STORE_PREFIX = "file_"

    private val storage = HashMap<FileType, FileStorage>()

    fun checkRootDir() {
        val storeDir = Path(getFileStorageRootDir(), RANK_IMAGE_STORE_ROOT_DIR)
        if (storeDir.exists()) {
            if (storeDir.isFile) {
                storeDir.delete()
                storeDir.mkdir()
            }
        } else {
            storeDir.mkdir()
        }
    }

    private val lock = newReentrantLock()

    fun getStorage(fileType: FileType): FileStorage {
        return lock.use {
            storage.getOrPut(fileType) {
                FileStorage(fileType)
            }
        }
    }

    fun MultiplatformFile.extension(): String {
        return this.extension
    }

    class FileStorage(val fileType: FileType) {

        private var id by SharedPreferenceUtils.StorageLong(
            getLongStorage("file-store_id"),
            "file_id_${fileType.value}",
            0L
        )

        private val rankImageStoreDir: String = RANK_IMAGE_STORE_DIR + fileType.value

        private val uriCache = mutableMapOf<Long, WeakRef<MultiplatformFile>>()

        private val lock = newReentrantLock()

        private fun rankImagePrefix(id: Long): String {
            return "${RANK_IMAGE_STORE_PREFIX}_${id}"
        }

        fun get(id: Long, fileInfo: FileInfo): MultiplatformFile? {
//            println("FileStorage: fileType:${fileType.name}, id=$id, fileInfo=${fileInfo.extension}")
            if (id == -1L) {
                return null
            }
            val cachedUri = uriCache[id]?.get()
            if (cachedUri != null) {
                return cachedUri
            }
            val storeDir = Path(getFileStorageRootDir(), rankImageStoreDir)
            val storageFile = Path(storeDir, rankImagePrefix(id))
//            println("FileStorage: fileType:${fileType.name}, id=$id, storageFile=$storageFile")
            return if (storageFile.exists()) {
//                println("FileStorage: exist fileType:${fileType.name}, id=$id, storageFile=$storageFile")
                getMultiplatformFileFactory().fromFile(storageFile, fileInfo.extension)
            } else {
//                println("FileStorage: not exist fileType:${fileType.name}, id=$id, storageFile=$storageFile")
                null
            }
        }

        fun delete(id: Long) {
            val storeDir = Path(getFileStorageRootDir(), rankImageStoreDir)
            val storageFile = Path(storeDir, rankImagePrefix(id))
            if (storageFile.exists()) {
                storageFile.delete()
            }
        }

        fun asyncStore(oldId: Long, uri: MultiplatformFile): Long {
            val id = lock.use {
                val id = if (oldId >= this@FileStorage.id || oldId < 0) {
                    this@FileStorage.id++
                } else {
                    oldId
                }
                uriCache.remove(oldId)
                uriCache[id] = WeakRef(uri)
                val storageFile = getStoreFile(id)
                ensureFile(storageFile)
                id
            }
            CoroutineUtils.runIOTask({
                val storageFile = getStoreFile(id)
                if (uri.file?.equals(storageFile) != true) {
                    ensureFile(storageFile)
                    if (BuildKonfig.isIOS) {
                        storeIos(uri, storageFile, id)
                    } else {
                        val bytes = uri.fileStoreInputStream!!
                        innerStoreImage(id, bytes, storageFile, uri.extension())
                    }
                }
            }, onFailure = { e->
//                println("asyncStore $e")
                throw e
            }) {
//                println("asyncStore: success: $fileType $id")
            }
            return id
        }

        @OptIn(InternalCoroutinesApi::class)
        fun asyncStore(uri: MultiplatformFile): Long {
            val id = lock.use {
                val id = this.id++
                uriCache[id] = WeakRef(uri)
                val storageFile = getStoreFile(id)
                ensureFile(storageFile)
                id
            }
            CoroutineUtils.runIOTask({
                val storageFile = getStoreFile(id)
                ensureFile(storageFile)
                if (BuildKonfig.isIOS) {
                    storeIos(uri, storageFile, id)
                } else {
                    val bytes = uri.fileStoreInputStream!!
                    innerStoreImage(id, bytes, storageFile, uri.extension())
                }
                uri.onStoreFinish()
            }, onFailure = { e->
//                println("asyncStore: $e")
            }) {
//                println("asyncStore: success: $fileType $id")
            }
            return id
        }

        private fun storeIos(uri: MultiplatformFile, storageFile: Path, id: Long) {
            val realFile = getIosRealFileStoreFile(id, uri.fileName ?: "real")
            val bytes = realFile.relativeTo(getFileStorageRootDir()).toString().encodeToByteArray()
            val buffer = Buffer()
            buffer.write(bytes, 0, bytes.size)
            innerStoreImage(id, buffer, storageFile, uri.extension())
            uri.inputStream?.use { `is`->
                `is`.copyTo(realFile)
            }
        }

        private fun ensureFile(storageFile: Path) {
            if (storageFile.exists()) {
                if (storageFile.isDirectory) {
                    storageFile.deleteRecursively()
                } else {
                    storageFile.delete()
                }
            }
            storageFile.createNewFile()
        }

        private fun getStoreFile(id: Long): Path {
            val storeDir = Path(getFileStorageRootDir(), rankImageStoreDir)
            lock.use {
                if (!storeDir.exists()) {
                    storeDir.mkdirs()
                } else if (storeDir.isFile) {
                    storeDir.delete()
                    storeDir.mkdir()
                }
            }
            return Path(storeDir, rankImagePrefix(id))
        }

        private fun getIosRealFileStoreFile(id: Long, fileName: String): Path {
            val storeDir = Path(getFileStorageRootDir(), rankImageStoreDir)
            lock.use {
                if (!storeDir.exists()) {
                    storeDir.mkdirs()
                } else if (storeDir.isFile) {
                    storeDir.delete()
                    storeDir.mkdir()
                }
            }
            return Path(storeDir, rankImagePrefix(id) + "_${fileName}")
        }

        private fun innerStoreImage(
            id: Long,
            input: RawSource,
            storageFile: Path,
            extension: String,
        ) {
            input.copyTo(storageFile)
            val uri = getMultiplatformFileFactory().fromFile(storageFile, extension)
            lock.use {
                uriCache[id] = WeakRef(uri)
            }
        }
    }

    private fun getThumbnailByType(fileType: FileType, uri: MultiplatformFile): ImageBitmap? {
        return when (fileType) {
            FileType.VIDEO -> uri.videoThumbnail
            FileType.AUDIO -> uri.audioThumbnail
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
        // 1. 计算缩放比例（保持宽高比）
        val width = bitmap.width
        val height = bitmap.height
        val scaleWidth = (maxWidth.toFloat() / width).takeIf { maxWidth > 0 }
        val scaleHeight = (maxHeight.toFloat() / height).takeIf { maxHeight > 0 }
        val scale = if (scaleWidth != null && scaleHeight != null) {
            minOf(scaleWidth, scaleHeight)
        } else scaleWidth ?: scaleHeight ?: 1f
        val compressedBitmap = compressImageBitmap(bitmap, width, height, scale)
        withContext(Dispatchers.Main) {
            onLoadBitmap(compressedBitmap)
        }
    }

    fun getThumbnail(
        fileInfo: FileInfo,
        coroutineScope: CoroutineScope,
        videoUri: MultiplatformFile,
        maxWidth: Int = -1,
        maxHeight: Int = -1,
        onLoadBitmap: suspend (ImageBitmap?)->Unit
    ): Job = coroutineScope.launch(Dispatchers.IO) {
        BitMapCachePool.loadImage(fileInfo, maxWidth, maxHeight) {
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

    fun getFileIntrinsicSize(uri: MultiplatformFile, mediaType: FileType) = when(mediaType) {
        FileType.IMAGE -> {
            uri.imageRatio
        }
        FileType.VIDEO -> {
            uri.videoThumbnail?.let {
                it.width to it.height
            } ?: (-1 to -1)
        }
        FileType.AUDIO -> {
            uri.audioThumbnail?.let {
                it.width to it.height
            } ?: (-1 to -1)
        }
        FileType.HTML -> {
            1 to 1
        }
        else -> -1 to -1
    }
}

expect fun compressImageBitmap(bitmap: ImageBitmap, width: Int, height: Int, scale: Float): ImageBitmap
