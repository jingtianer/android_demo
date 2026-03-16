package com.jingtian.composedemo.multiplatform

import androidx.compose.ui.graphics.ImageBitmap
import com.jingtian.composedemo.dao.model.FileType
import java.io.File
import java.io.InputStream

interface MultiplatformFile {
    val fileName: String?

    val isHidden: Boolean

    val mediaType: FileType

    val extension: String

    val inputStream: InputStream?

    val fileStoreInputStream: InputStream?

    val videoThumbnail: ImageBitmap?

    val audioThumbnail: ImageBitmap?

    val imageRatio: Pair<Int, Int>

    val file: File?

    fun onDelete() {}
}

interface IMultiplatformFileFactory {
    fun fromFile(file: File): MultiplatformFile
    fun fromFile(file: File, extension: String?): MultiplatformFile {
        return fromFile(file)
    }

    fun shareFile(file: File): MultiplatformFile
}

expect fun getMultiplatformFileFactory() : IMultiplatformFileFactory