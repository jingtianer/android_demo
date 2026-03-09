package com.jingtian.composedemo.utils

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.jingtian.composedemo.base.app
import java.io.File

actual fun getFileStorageRootDir(): File = app.filesDir
actual fun getFileCacheStorageRootDir(): File = app.cacheDir

actual fun compressImageBitmap(bitmap: ImageBitmap, width: Int, height: Int, scale: Float): ImageBitmap {
    // 2. 创建缩放矩阵
    val matrix = Matrix().apply {
        postScale(scale, scale)
    }

    // 3. 压缩并返回新Bitmap（注意回收原始Bitmap避免内存泄漏）
    val androidBitmap = bitmap.asAndroidBitmap()
    val compressedBitmap = Bitmap.createBitmap(
        androidBitmap, 0, 0, width, height, matrix, true
    )
    if (compressedBitmap != androidBitmap) {
        androidBitmap.recycle() // 回收原始Bitmap
    }
    return compressedBitmap.asImageBitmap()
}