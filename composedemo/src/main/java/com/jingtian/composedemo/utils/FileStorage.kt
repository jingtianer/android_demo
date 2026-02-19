package com.jingtian.composedemo.utils

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
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
import java.lang.ref.WeakReference
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

        private fun rankImagePrefix(id: Long): String {
            return "${RANK_IMAGE_STORE_PREFIX}_${id}"
        }

        fun get(id: Long): Uri? {
            if (id == -1L) {
                Uri.EMPTY
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
        onLoadBitmap: (Bitmap?)->Unit
    ): Job = coroutineScope.launch(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        val bitmap =  try {
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
        withContext(Dispatchers.Main) {
            onLoadBitmap(bitmap)
        }
    }
}