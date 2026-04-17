package com.jingtian.composedemo.multiplatform

import androidx.compose.ui.graphics.ImageBitmap
import com.jingtian.composedemo.dao.model.FileType
import kotlinx.io.RawSource
import kotlinx.io.Source
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.readByteArray

interface MultiplatformFile {
    val fileName: String?

    val isHidden: Boolean

    val mediaType: FileType

    val extension: String

    val inputStream: RawSource?

    val fileStoreInputStream: RawSource?

    val videoThumbnail: ImageBitmap?

    val audioThumbnail: ImageBitmap?

    val imageRatio: Pair<Int, Int>

    val file: Path?

    val path: String

    fun onDelete() {}

    fun onStoreFinish() {}
}

/**
 * 将 `RawSource?` 转成一次性使用的 `ByteArray?`，方便和只接受 `InputStream` 的平台 API 交互。
 */
fun RawSource?.readAllBytesOrNull(): ByteArray? {
    val raw = this ?: return null
    raw.use {
        val source: Source = raw.buffered()
        return source.readByteArray()
    }
}

interface IMultiplatformFileFactory {
    fun fromFile(file: Path): MultiplatformFile
    fun fromFile(file: Path, extension: String?): MultiplatformFile {

        return fromFile(file)
    }

    fun shareFile(file: Path): MultiplatformFile
}

expect fun getMultiplatformFileFactory() : IMultiplatformFileFactory
