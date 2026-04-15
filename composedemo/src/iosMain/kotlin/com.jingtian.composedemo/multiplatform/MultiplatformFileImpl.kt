package com.jingtian.composedemo.multiplatform

import androidx.compose.ui.graphics.ImageBitmap
import com.jingtian.composedemo.dao.model.FileType
import com.jingtian.composedemo.utils.extension
import kotlinx.io.Buffer
import kotlinx.io.RawSource
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem


class MultiplatformFileImpl(val realFile: Path, val realExtension: String) : MultiplatformFile {
    companion object {
        val imageExtensions = arrayOf("jpg", "jpeg", "png", "gif", "bmp", "webp", "tiff", "ico", "heic", "heif", "jfif")
        val videoExtensions = arrayOf("mp4", "mov", "mkv", "avi", "webm", "flv", "wmv")
        val audioExtensions = arrayOf("mp3", "wav", "flac", "aac", "m4a", "ogg", "wma", "amr", "mid")
        val htmlExtensions = arrayOf("html", "svg")
    }

    private val Path.isHidden: Boolean get() {
        return false
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
    override val inputStream: RawSource?
        get() = SystemFileSystem.source(realFile)
    override val videoThumbnail: ImageBitmap?
        get() = null
    override val audioThumbnail: ImageBitmap?
        get() = null
    override val imageRatio: Pair<Int, Int>
        get() = (1 to 1)
    override val fileStoreInputStream: RawSource?
        get() {
            val buffer = Buffer()
            val ba = realFile.toString().encodeToByteArray()
            buffer.write(ba)
            return buffer
        }
    override val file: Path
        get() = Path(realFile)
    override val extension: String = realFile.extension
    override val path: String
        get() = realFile.toString()
}