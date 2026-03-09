package com.jingtian.composedemo.utils

import androidx.compose.ui.graphics.ImageBitmap
import java.io.File

actual fun getFileStorageRootDir(): File = File("./filestore")
actual fun getFileCacheStorageRootDir(): File = File("./cache")
actual fun compressImageBitmap(bitmap: ImageBitmap, width: Int, height: Int, scale: Float): ImageBitmap {
    return bitmap
}
