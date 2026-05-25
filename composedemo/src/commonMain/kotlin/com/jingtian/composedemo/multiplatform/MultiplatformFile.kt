package com.jingtian.composedemo.multiplatform

import androidx.compose.ui.graphics.ImageBitmap
import com.jingtian.composedemo.dao.model.FileType
import kotlinx.io.RawSource
import kotlinx.io.Source
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.readByteArray

interface MultiplatformFile {
    suspend fun fileName(): String?

    suspend fun isHidden(): Boolean

    suspend fun mediaType(): FileType

    suspend fun extension(): String

    suspend fun inputStream(): RawSource?

    suspend fun fileStoreInputStream(): RawSource?

    suspend fun videoThumbnail(): ImageBitmap?

    suspend fun audioThumbnail(): ImageBitmap?

    suspend fun imageRatio(): Pair<Int, Int>

    suspend fun file(): Path?

    suspend fun path(): String

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
