package com.jingtian.composedemo.multiplatform

import androidx.compose.ui.graphics.ImageBitmap
import com.jingtian.composedemo.dao.model.FileType
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

class MultiplatformFileImpl(val realFile: File) : MultiplatformFile {
    companion object {
        val imageExtensions = arrayOf("png", "jpg", "jpeg", "bmp", "gif")
        val videoExtensions = arrayOf("mp4")
        val audioExtensions = arrayOf("mp3")
        val htmlExtensions = arrayOf("html")
    }
    override val fileName: String?
        get() = realFile.name
    override val isHidden: Boolean
        get() = realFile.isHidden
    override val mediaType: FileType
        get() = when(realFile.extension) {
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