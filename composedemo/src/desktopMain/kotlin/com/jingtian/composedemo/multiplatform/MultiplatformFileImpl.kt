package com.jingtian.composedemo.multiplatform

import androidx.compose.ui.graphics.ImageBitmap
import com.jingtian.composedemo.dao.model.FileType
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

class MultiplatformFileImpl(val realFile: File) : MultiplatformFile {
    companion object {
        val imageExtensions = arrayOf("jpg", "jpeg", "png", "gif", "bmp", "webp", "tiff", "ico", "heic", "heif", "jfif")
        val videoExtensions = arrayOf("mp4", "mov", "mkv", "avi", "webm", "flv", "wmv")
        val audioExtensions = arrayOf("mp3", "wav", "flac", "aac", "m4a", "ogg", "wma", "amr", "mid")
        val htmlExtensions = arrayOf("html", "svg")
    }
    override val fileName: String?
        get() = realFile.name
    override val isHidden: Boolean
        get() = realFile.isHidden
    override val mediaType: FileType
        get() = when(realFile.extension.lowercase()) {
            in imageExtensions -> FileType.IMAGE
            in videoExtensions -> FileType.VIDEO
            in audioExtensions -> FileType.AUDIO
            in htmlExtensions -> FileType.HTML
            else -> FileType.RegularFile
        }
    override val inputStream: InputStream
        get() = FileInputStream(realFile)
    override val videoThumbnail: ImageBitmap?
        get() = null
    override val audioThumbnail: ImageBitmap?
        get() = null
    override val imageRatio: Pair<Int, Int>
        get() = 1 to 1
    override val file: File
        get() = realFile
    override val extension: String = realFile.extension
}