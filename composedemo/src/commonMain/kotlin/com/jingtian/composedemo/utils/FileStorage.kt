package com.jingtian.composedemo.utils

import androidx.compose.ui.graphics.ImageBitmap
import com.jingtian.composedemo.dao.model.FileInfo
import com.jingtian.composedemo.dao.model.FileType
import com.jingtian.composedemo.multiplatform.MultiplatformFile
import com.jingtian.composedemo.multiplatform.getLongStorage
import com.jingtian.composedemo.multiplatform.getMultiplatformFileFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.lang.ref.SoftReference
import java.util.concurrent.ConcurrentHashMap

expect fun getFileStorageRootDir(): File
expect fun getFileCacheStorageRootDir(): File

object FileStorageUtils {
    private const val RANK_IMAGE_STORE_ROOT_DIR = "fileStorage"
    private const val RANK_IMAGE_STORE_DIR = "${RANK_IMAGE_STORE_ROOT_DIR}/file_"
    private const val RANK_IMAGE_STORE_PREFIX = "file_"

    private val storage = ConcurrentHashMap<FileType, SoftReference<FileStorage>>()

    fun checkRootDir() {
        val storeDir = File(getFileStorageRootDir(), RANK_IMAGE_STORE_ROOT_DIR)
        if (storeDir.exists()) {
            if (storeDir.isFile) {
                storeDir.delete()
                storeDir.mkdir()
            }
        } else {
            storeDir.mkdir()
        }
    }

    fun getStorage(fileType: FileType): FileStorage? {
        return storage.compute(fileType) { fileType, present->
            if (present?.get() == null) {
                SoftReference(FileStorage(fileType))
            } else {
                present
            }
        }?.get()
    }

    fun MultiplatformFile.extension(): String {
        return this.extension
    }

    class FileStorage(val fileType: FileType) {

        private var id by SharedPreferenceUtils.SynchronizedProperty(
            SharedPreferenceUtils.StorageLong(
                getLongStorage("file-store_id"),
                "file_id_${fileType.value}",
                0L
            )
        )

        private val rankImageStoreDir: String = RANK_IMAGE_STORE_DIR + fileType.value

        private val uriCache = ConcurrentHashMap<Long, MultiplatformFile>()

        private fun rankImagePrefix(id: Long): String {
            return "${RANK_IMAGE_STORE_PREFIX}_${id}"
        }

        fun get(id: Long): MultiplatformFile? {
            if (id == -1L) {
                return null
            }
            val cachedUri = uriCache.get(id)
            if (cachedUri != null) {
                return cachedUri
            }
            val storeDir = File(getFileStorageRootDir(), rankImageStoreDir)
            val storageFile = File(storeDir, rankImagePrefix(id))
            return if (storageFile.exists()) {
                getMultiplatformFileFactory().fromFile(storageFile)
            } else {
                null
            }
        }

        fun delete(id: Long) {
            val storeDir = File(getFileStorageRootDir(), rankImageStoreDir)
            val storageFile = File(storeDir, rankImagePrefix(id))
            if (storageFile.exists()) {
                storageFile.delete()
            }
        }

        fun asyncStore(oldId: Long, uri: MultiplatformFile): Long {
            val id = synchronized(this) {
                if (oldId >= this.id || oldId < 0) {
                    this.id++
                } else {
                    oldId
                }
            }
            uriCache.remove(oldId)
            uriCache[id] = uri
            CoroutineUtils.runIOTask({
                val storageFile = getStoreFile(id)
                if (uri.file?.absolutePath?.equals(storageFile.absolutePath) != true) {
                    if (storageFile.exists()) {
                        storageFile.delete()
                    }
                    uri.inputStream?.use { input ->
                        innerStoreImage(id, input, storageFile)
                    }
                }
            }) {}
            return id
        }

        fun asyncStore(uri: MultiplatformFile): Long {
            val id = synchronized(this) {
                this.id++
            }
            uriCache[id] = uri
            CoroutineUtils.runIOTask({
                val storageFile = getStoreFile(id)
                storageFile.delete()
                uri.inputStream?.use { input ->
                    innerStoreImage(id, input, storageFile)
                }
            }, {})
            return id
        }

        private fun getStoreFile(id: Long): File {
            val storeDir = File(getFileStorageRootDir(), rankImageStoreDir)
            if (!storeDir.exists()) {
                storeDir.mkdirs()
            } else if (storeDir.isFile) {
                storeDir.delete()
                storeDir.mkdirs()
            }
            return File(storeDir, rankImagePrefix(id))
        }

        private fun innerStoreImage(
            id: Long,
            input: InputStream,
            storageFile: File
        ): FileInfo {
            storageFile.outputStream().use { output ->
                input.copyTo(output)
            }
            val uri = getMultiplatformFileFactory().fromFile(storageFile)
            uriCache[id] = uri
            return FileInfo(storageId = id, fileType = fileType)
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