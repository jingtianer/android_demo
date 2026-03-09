package com.jingtian.composedemo.multiplatform

import androidx.compose.ui.graphics.ImageBitmap
import com.jingtian.composedemo.dao.model.FileType
import java.io.File
import java.io.InputStream

interface MultiplatformFile {
    val fileName: String?

    val isHidden: Boolean

    val mediaType: FileType

    val inputStream: InputStream?

    val videoThumbnail: ImageBitmap?

    val audioThumbnail: ImageBitmap?

    val imageRatio: Pair<Int, Int>

    val file: File?
}