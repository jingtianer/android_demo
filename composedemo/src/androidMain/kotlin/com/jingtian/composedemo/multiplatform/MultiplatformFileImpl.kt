package com.jingtian.composedemo.multiplatform

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.jingtian.composedemo.base.app
import com.jingtian.composedemo.dao.model.FileType
import com.jingtian.composedemo.utils.BitMapCachePool.toImmutable
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class MultiplatformFileImpl(val uri: Uri) : MultiplatformFile {
    private fun getFileNameFromUri(uri: Uri): String? {
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

    private fun Uri.isHidden(): Boolean {
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

    private fun getMediaType(uri: Uri): FileType {
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

    private fun getVideoThumbnail(videoUri: Uri): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(app, videoUri)
            // 获取第一帧作为封面
            retriever.frameAtTime.toImmutable()
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
            coverBytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }.toImmutable()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            retriever.release()
        }
    }

    private fun getImageRatio(uri: Uri): Pair<Int, Int> {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true // 只获取尺寸，不加载像素
        }
        val (width, height) =  app.contentResolver.openInputStream(uri)?.use { `is`->
            // 第一步：仅解码边界，获取图片原始宽高
            BitmapFactory.decodeStream(`is`, null, options)
            options.outWidth to options.outHeight
        } ?: (-1 to -1)
        return width to height
    }

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

    private fun Uri.safeToFile(): File? {
        return if ("file".equals(scheme, ignoreCase = true)) {
            path?.let { File(it) }
        } else {
            null
        }
    }

    override val fileName: String?
        get() = getFileNameFromUri(uri)

    override val isHidden: Boolean
        get() = uri.isHidden()

    override val mediaType: FileType
        get() = getMediaType(uri)

    override val inputStream: InputStream?
        get() = app.contentResolver.openInputStream(uri)

    override val videoThumbnail: ImageBitmap?
        get() = getVideoThumbnail(uri)?.asImageBitmap()

    override val audioThumbnail: ImageBitmap?
        get() = getAudioThumbnail(uri)?.asImageBitmap()

    override val imageRatio: Pair<Int, Int>
        get() = getImageRatio(uri)

    override val file: File?
        get() = uri.safeToFile()
}