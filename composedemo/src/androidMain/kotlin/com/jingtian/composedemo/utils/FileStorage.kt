package com.jingtian.composedemo.utils

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.core.net.toUri
import com.jingtian.composedemo.base.app
import com.jingtian.composedemo.dao.model.FileInfo
import com.jingtian.composedemo.dao.model.FileType
import com.jingtian.composedemo.utils.BitMapCachePool.toImmutable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.lang.ref.SoftReference
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap


object FileStorageUtils {
    private const val RANK_IMAGE_STORE_ROOT_DIR = "fileStorage"
    private const val RANK_IMAGE_STORE_DIR = "${RANK_IMAGE_STORE_ROOT_DIR}/file_"
    private const val RANK_IMAGE_STORE_PREFIX = "file_"

    private val storage = ConcurrentHashMap<FileType, SoftReference<FileStorage>>()

    fun getFileNameFromUri(uri: Uri): String? {
        // 方案1：直接通过ContentResolver查询DISPLAY_NAME
        val projection = arrayOf(MediaStore.MediaColumns.DISPLAY_NAME)
        val cursor: Cursor? = app.contentResolver.query(uri, projection, null, null, null)

        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    return it.getString(nameIndex)
                }
            }
        }

        // 方案2：如果上面的方式失败，尝试从Uri路径中提取
        return runCatching {
            when {
                // 处理DocumentProvider
                DocumentsContract.isDocumentUri(app, uri) -> {
                    val docId = DocumentsContract.getDocumentId(uri)
                    val split = docId.split(":")
                    if (split.size >= 2) split[1] else null
                }
                // 处理普通Uri
                else -> uri.lastPathSegment
            }
        }.getOrNull()
    }

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


    fun Uri.safeToFile(): File? {
        return if ("file".equals(scheme, ignoreCase = true)) {
            path?.let { File(it) }
        } else {
            null
        }
    }

    fun Uri.isHidden(): Boolean {
        val file = safeToFile()
        if (file != null) {
            return file.let { it.exists() && it.isHidden }
        }
        val filePath = getRealPathFromContentUri(app, this)
        if (filePath != null) {
            return File(filePath).let { it.exists() && it.isHidden }
        }

        val fileName = getFileNameFromContentUri(app, this)
        if (fileName != null && fileName.startsWith(".")) {
            return true
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                app.contentResolver.openInputStream(this)?.use { `is` ->
                    val path: Path = Paths.get(this.path)
                    return Files.isHidden(path)
                }
            } catch (e: FileNotFoundException) {
            } catch (e: IOException) {
            }
        }
        return false
    }

    /**
     * 从Content URI获取文件真实路径（适配媒体文件）
     */
    private fun getRealPathFromContentUri(context: Context, uri: Uri): String? {
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(uri, projection, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                return cursor.getString(columnIndex)
            }
        } catch (e: java.lang.Exception) {
        } finally {
            cursor?.close()
        }
        return null
    }

    /**
     * 从Content URI获取文件名
     */
    private fun getFileNameFromContentUri(context: Context, uri: Uri): String? {
        val projection = arrayOf(MediaStore.MediaColumns.DISPLAY_NAME)
        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(uri, projection, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                return cursor.getString(columnIndex)
            }
        } catch (e: Exception) {
        } finally {
            cursor?.close()
        }
        return null
    }

    fun Uri.extension(): String {
        return MimeTypeMap.getFileExtensionFromUrl(this.toString())
    }

    class FileStorage(val fileType: FileType) {

        private val sp = app.getSharedPreferences("file-store_id", Context.MODE_PRIVATE)
        private var id by SharedPreferenceUtils.SynchronizedProperty(
            SharedPreferenceUtils.StorageLong(sp, "file_id_${fileType.value}", 0L)
        )

        private val rankImageStoreDir: String = RANK_IMAGE_STORE_DIR + fileType.value

        private val uriCache = ConcurrentHashMap<Long, Uri>()

        private fun rankImagePrefix(id: Long): String {
            return "${RANK_IMAGE_STORE_PREFIX}_${id}"
        }

        fun get(id: Long): Uri? {
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
                storageFile.toUri()
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

        fun asyncStore(oldId: Long, uri: Uri): Long {
            if (uri == Uri.EMPTY) {
                return -1
            }
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
                if (uri.safeToFile()?.absolutePath?.equals(storageFile.absolutePath) != true) {
                    if (storageFile.exists()) {
                        storageFile.delete()
                    }
                    app.contentResolver.openInputStream(uri)?.use { input ->
                        innerStoreImage(id, input, storageFile)
                    }
                }
            }) {}
            return id
        }

        fun asyncStore(uri: Uri): Long {
            if (uri == Uri.EMPTY) {
                return -1
            }
            val id = synchronized(this) {
                this.id++
            }
            uriCache[id] = uri
            CoroutineUtils.runIOTask({
                val storageFile = getStoreFile(id)
                storageFile.delete()
                app.contentResolver.openInputStream(uri)?.use { input ->
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
            val uri = storageFile.toUri()
            uriCache[id] = uri
            return FileInfo(storageId = id, uri = uri, fileType = fileType)
        }
    }

    fun getMediaType(uri: Uri): FileType {
        // 方式1：通过ContentResolver获取MIME类型（推荐，最可靠）
        val contentResolver: ContentResolver = app.contentResolver
        val mimeType = contentResolver.getType(uri)
        return when {
            // 判断是否为图片
            mimeType?.startsWith("image/") == true -> FileType.IMAGE
            // 判断是否为视频
            mimeType?.startsWith("video/") == true -> FileType.VIDEO
            mimeType?.startsWith("audio/") == true -> FileType.AUDIO
            mimeType?.startsWith("text/html") == true -> FileType.HTML
            // 方式2：兜底方案（通过文件扩展名判断，防止ContentResolver获取失败）
            else -> {
                val extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
                val fallbackMimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
                when {
                    fallbackMimeType?.startsWith("image/") == true -> FileType.IMAGE
                    fallbackMimeType?.startsWith("video/") == true -> FileType.VIDEO
                    fallbackMimeType?.startsWith("audio/") == true -> FileType.AUDIO
                    fallbackMimeType?.startsWith("text/html") == true -> FileType.HTML
                    else -> when {
                        extension.equals("jfif") -> FileType.IMAGE // 二进制先按照图片处理
                        else -> FileType.RegularFile
                    }
                }
            }
        }
    }

    private fun getThumbnailByType(fileType: FileType, uri: Uri): Bitmap? {
        return when (fileType) {
            FileType.VIDEO -> getVideoThumbnail(uri)
            FileType.AUDIO -> getAudioThumbnail(uri)
            FileType.HTML -> getWebThumbnail(uri)
            FileType.RegularFile -> null
            FileType.IMAGE -> null
        }
    }


    fun getThumbnail(
        fileType: FileType,
        coroutineScope: CoroutineScope,
        videoUri: Uri,
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
        val compressedBitmap = Bitmap.createBitmap(
            bitmap, 0, 0, width, height, matrix, true
        )
        if (compressedBitmap != bitmap) {
            bitmap.recycle() // 回收原始Bitmap
        }
        withContext(Dispatchers.Main) {
            onLoadBitmap(compressedBitmap.toImmutable())
        }
    }

    fun getThumbnail(
        fileInfo: FileInfo,
        coroutineScope: CoroutineScope,
        videoUri: Uri,
        maxWidth: Int = -1,
        maxHeight: Int = -1,
        onLoadBitmap: suspend (Bitmap?)->Unit
    ): Job = coroutineScope.launch(Dispatchers.IO) {
        BitMapCachePool.loadImage(fileInfo, maxWidth, maxHeight) {
            getThumbnailByType(fileInfo.fileType, videoUri)?.apply {
                this.toImmutable()
            }
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

    private fun getVideoThumbnail(videoUri: Uri): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(app, videoUri)
            // 获取第一帧作为封面
            retriever.frameAtTime
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
            }
        }
    }

    private fun getAudioThumbnail(uri: Uri): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(app, uri)
            val coverBytes = retriever.embeddedPicture
            coverBytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            retriever.release()
        }
    }

    private fun getWebThumbnail(uri: Uri): Bitmap? {
        return null
    }

    private suspend fun getVideoThumbnailIntrinsicSize(videoUri: Uri): Pair<Int, Int> {
        return withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            return@withContext try {
                retriever.setDataSource(app, videoUri)
                // 获取第一帧作为封面
                retriever.frameAtTime
            } catch (e: Exception) {
                e.printStackTrace()
                null
            } finally {
                try {
                    retriever.release()
                } catch (e: Exception) {
                }
            }
        }?.let {
            it.width to it.height
        } ?: (-1 to -1)
    }

    suspend fun getFileIntrinsicSize(uri: Uri, mediaType: FileType) = when(mediaType) {
        FileType.IMAGE -> {
            BitMapCachePool.getImageRatio(uri)
        }
        FileType.VIDEO -> {
            getVideoThumbnailIntrinsicSize(uri)
        }
        FileType.AUDIO -> {
            getVideoThumbnailIntrinsicSize(uri)
        }
        FileType.HTML -> {
            1 to 1
        }
        else -> -1 to -1
    }
}