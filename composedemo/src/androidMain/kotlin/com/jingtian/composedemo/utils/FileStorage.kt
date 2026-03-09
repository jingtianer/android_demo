package com.jingtian.composedemo.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.webkit.MimeTypeMap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.core.net.toUri
import com.jingtian.composedemo.base.app
import com.jingtian.composedemo.dao.model.FileInfo
import com.jingtian.composedemo.dao.model.FileType
import com.jingtian.composedemo.multiplatform.MultiplatformFile
import com.jingtian.composedemo.multiplatform.MultiplatformFileFactory
import com.jingtian.composedemo.multiplatform.MultiplatformFileImpl
import com.jingtian.composedemo.utils.BitMapCachePool.toImmutable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.lang.ref.SoftReference
import java.util.concurrent.ConcurrentHashMap


object FileStorageUtils {
    private const val RANK_IMAGE_STORE_ROOT_DIR = "fileStorage"
    private const val RANK_IMAGE_STORE_DIR = "${RANK_IMAGE_STORE_ROOT_DIR}/file_"
    private const val RANK_IMAGE_STORE_PREFIX = "file_"

    private val storage = ConcurrentHashMap<FileType, SoftReference<FileStorage>>()

    fun checkRootDir() {
        val storeDir = File(app.filesDir, RANK_IMAGE_STORE_ROOT_DIR)
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
        return MimeTypeMap.getFileExtensionFromUrl(this.toString())
    }

    class FileStorage(val fileType: FileType) {

        private val sp = app.getSharedPreferences("file-store_id", Context.MODE_PRIVATE)
        private var id by SharedPreferenceUtils.SynchronizedProperty(
            SharedPreferenceUtils.StorageLong(sp, "file_id_${fileType.value}", 0L)
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
            val storeDir = File(app.filesDir, rankImageStoreDir)
            val storageFile = File(storeDir, rankImagePrefix(id))
            return if (storageFile.exists()) {
                MultiplatformFileFactory.fromFile(storageFile)
            } else {
                null
            }
        }

        fun delete(id: Long) {
            val storeDir = File(app.filesDir, rankImageStoreDir)
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
            val storeDir = File(app.filesDir, rankImageStoreDir)
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
            val uri = MultiplatformFileFactory.fromFile(storageFile)
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
        onLoadBitmap: suspend (Bitmap?)->Unit
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

        // 2. 创建缩放矩阵
        val matrix = Matrix().apply {
            postScale(scale, scale)
        }

        // 3. 压缩并返回新Bitmap（注意回收原始Bitmap避免内存泄漏）
        val androidBitmap = bitmap.asAndroidBitmap()
        val compressedBitmap = Bitmap.createBitmap(
            androidBitmap, 0, 0, width, height, matrix, true
        )
        if (compressedBitmap != androidBitmap) {
            androidBitmap.recycle() // 回收原始Bitmap
        }
        withContext(Dispatchers.Main) {
            onLoadBitmap(compressedBitmap.toImmutable())
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