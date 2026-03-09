package com.jingtian.composedemo.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.InputStream
import java.io.OutputStream


actual fun uriToImageBitmap(`is`: InputStream, scaleFactor: Int): ImageBitmap? {
    val options = BitmapFactory.Options()
    // 第三步：按缩放比例解码图片
    options.apply {
        inJustDecodeBounds = false // 实际加载像素
        inSampleSize = scaleFactor // 缩放比例（2的倍数，如 2=1/2 大小，4=1/4 大小）
        inPreferredConfig = Bitmap.Config.RGB_565 // 可选：使用 RGB_565 节省内存（比 ARGB_8888 节省一半）
        inMutable = false // 不可变
    }
    return BitmapFactory.decodeStream(`is`, null, options)?.asImageBitmap()
}

actual fun uriToImageSize(`is`: InputStream): Pair<Int, Int> {
    val options = BitmapFactory.Options().apply {
        inJustDecodeBounds = true // 只获取尺寸，不加载像素
    }
    return BitmapFactory.decodeStream(`is`, null, options)?.let { 
        it.width to it.height
    } ?: (-1 to -1)
}

actual fun uriToImageBitmap(`is`: InputStream): ImageBitmap? {
    return BitmapFactory.decodeStream(`is`)?.asImageBitmap()
}

actual fun writeImage(bitmap: ImageBitmap, os: OutputStream) {
    bitmap.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, os)
    
}