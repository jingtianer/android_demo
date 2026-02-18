package com.jingtian.composedemo.utils

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.net.toUri
import com.jingtian.composedemo.base.app
import com.jingtian.composedemo.dao.model.FileInfo
import com.jingtian.composedemo.dao.model.FileType
import java.io.File
import java.io.InputStream
import java.lang.ref.SoftReference
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap

object FileStorageUtils {

    private const val RANK_IMAGE_PREFIX = "file_"
    private const val RANK_IMAGE_STORE_DIR = "file"

    private val storage = ConcurrentHashMap<FileType, SoftReference<FileStorage>>()

    fun getStorage(fileType: FileType): FileStorage? {
        return storage.compute(fileType) { fileType, present->
            if (present?.get() == null) {
                SoftReference(FileStorage(fileType))
            } else {
                present
            }
        }?.get()
    }

    class FileStorage(val fileType: FileType) {

        private val sp = app.getSharedPreferences("file-store_${fileType.value}", Context.MODE_PRIVATE)
        private var id by SharedPreferenceUtils.SynchronizedProperty(
            SharedPreferenceUtils.StorageLong(sp, "file_id_${fileType.value}", 0L)
        )

        private fun rankImageStoreDir(): String {
            return RANK_IMAGE_STORE_DIR + fileType.value
        }

        private fun rankImagePrefix(id: Long): String {
            return "${RANK_IMAGE_PREFIX}_${fileType.value}_${id}"
        }

        fun get(id: Long): Uri? {
            if (id == -1L) {
                Uri.EMPTY
            }
            val storeDir = File(app.filesDir, rankImageStoreDir())
            val storageFile = File(storeDir, rankImagePrefix(id))
            return if (storageFile.exists()) {
                storageFile.toUri()
            } else {
                null
            }
        }

        fun delete(id: Long) {
            val storeDir = File(app.filesDir, RANK_IMAGE_STORE_DIR)
            val storageFile = File(storeDir, RANK_IMAGE_PREFIX + id)
            if (storageFile.exists()) {
                storageFile.delete()
            }
        }

        fun Uri.safeToFile(): File? {
            return if ("file".equals(scheme, ignoreCase = true)) {
                path?.let { File(it) }
            } else {
                null
            }
        }

        fun store(oldId: Long, uri: Uri): Long {
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
            val storageFile = getStoreFile(id)
            if (uri.safeToFile()?.absolutePath?.equals(storageFile.absolutePath) != true) {
                if (storageFile.exists()) {
                    storageFile.delete()
                }
                app.contentResolver.openInputStream(uri)?.use { input ->
                    innerStoreImage(id, input, storageFile)
                }
            }
            return id
        }

        fun store(uri: Uri): Long {
            if (uri == Uri.EMPTY) {
                return -1
            }
            val id = synchronized(this) {
                this.id++
            }
            val storageFile = getStoreFile(id)
            storageFile.delete()
            app.contentResolver.openInputStream(uri)?.use { input ->
                innerStoreImage(id, input, storageFile)
            }
            return id
        }

        fun store(): Pair<File, Long> {
            val id = synchronized(this) {
                this.id++
            }
            val storageFile = getStoreFile(id)
            storageFile.delete()
            return storageFile to id
        }

        private fun getStoreFile(id: Long): File {
            val storeDir = File(app.filesDir, RANK_IMAGE_STORE_DIR)
            if (!storeDir.exists()) {
                storeDir.mkdirs()
            } else if (storeDir.isFile) {
                storeDir.delete()
                storeDir.mkdirs()
            }
            return File(storeDir, RANK_IMAGE_PREFIX + id)
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
            return FileInfo(id, uri)
        }
    }
}