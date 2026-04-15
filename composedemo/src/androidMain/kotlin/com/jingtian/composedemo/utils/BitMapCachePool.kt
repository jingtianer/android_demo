package com.jingtian.composedemo.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.io.RawSink
import kotlinx.io.RawSource
import kotlinx.io.asInputStream
import kotlinx.io.asOutputStream
import kotlinx.io.buffered


actual fun uriToImageBitmap(`is`: RawSource, scaleFactor: Int): ImageBitmap? {
    val options = BitmapFactory.Options()
    // 第三步：按缩放比例解码图片
    options.apply {
        inJustDecodeBounds = false // 实际加载像素
        inSampleSize = scaleFactor // 缩放比例（2的倍数，如 2=1/2 大小，4=1/4 大小）
        inPreferredConfig = Bitmap.Config.RGB_565 // 可选：使用 RGB_565 节省内存（比 ARGB_8888 节省一半）
        inMutable = false // 不可变
    }
    return `is`.buffered().asInputStream().use {
        BitmapFactory.decodeStream(it, null, options)?.asImageBitmap()
    }
}

actual fun uriToImageSize(`is`: RawSource): Pair<Int, Int> {
    val options = BitmapFactory.Options().apply {
        inJustDecodeBounds = true // 只获取尺寸，不加载像素
    }
    return `is`.buffered().asInputStream().use {
        BitmapFactory.decodeStream(it, null, options)?.let {
            it.width to it.height
        } ?: (-1 to -1)
    }
}

actual fun uriToImageBitmap(`is`: RawSource): ImageBitmap? {
    return `is`.buffered().asInputStream().use {
        BitmapFactory.decodeStream(it)?.asImageBitmap()
    }
}

actual fun writeImage(bitmap: ImageBitmap, os: RawSink) {
    os.buffered().asOutputStream().use {
        bitmap.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
    }

}