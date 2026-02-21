package com.jingtian.composedemo.utils

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.core.net.toUri
import com.jingtian.composedemo.base.app
import com.jingtian.composedemo.dao.model.FileInfo
import com.jingtian.composedemo.dao.model.FileType
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
                Uri.EMPTY
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
            // 方式2：兜底方案（通过文件扩展名判断，防止ContentResolver获取失败）
            else -> {
                val extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
                val fallbackMimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
                when {
                    fallbackMimeType?.startsWith("image/") == true -> FileType.IMAGE
                    fallbackMimeType?.startsWith("video/") == true -> FileType.VIDEO
                    fallbackMimeType?.startsWith("audio/") == true -> FileType.AUDIO
                    else -> FileType.RegularFile
                }
            }
        }
    }


    fun getVideoThumbnail(
        coroutineScope: CoroutineScope,
        videoUri: Uri,
        onLoadBitmap: suspend (Bitmap?)->Unit
    ): Job = coroutineScope.launch(Dispatchers.IO) {
        val bitmap = getVideoThumbnail(videoUri)
        withContext(Dispatchers.Main) {
            onLoadBitmap(bitmap)
        }
    }

    fun getVideoThumbnail(
        fileInfo: FileInfo,
        coroutineScope: CoroutineScope,
        videoUri: Uri,
        maxWidth: Int = -1,
        maxHeight: Int = -1,
        onLoadBitmap: suspend (Bitmap?)->Unit
    ): Job = coroutineScope.launch(Dispatchers.IO) {
        BitMapCachePool.loadImage(fileInfo, maxWidth, maxHeight) { getVideoThumbnail(videoUri) }.second?.let {
            withContext(Dispatchers.Main) {
                onLoadBitmap(it)
            }
            return@launch
        }
        val bitmap = getVideoThumbnail(videoUri)
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

    suspend fun getVideoThumbnailIntrinsicSize(videoUri: Uri): Pair<Int, Int> {
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
        else -> -1 to -1
    }
}