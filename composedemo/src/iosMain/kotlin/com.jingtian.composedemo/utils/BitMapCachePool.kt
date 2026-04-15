package com.jingtian.composedemo.utils

import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.jingtian.composedemo.multiplatform.readAllBytesOrNull
import kotlinx.io.RawSink
import kotlinx.io.RawSource
import kotlinx.io.buffered
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect
import androidx.compose.ui.graphics.ImageBitmap

actual fun uriToImageBitmap(`is`: RawSource, scaleFactor: Int): ImageBitmap? {
    val bytes = `is`.buffered().readAllBytesOrNull() ?: return null
    val image = Image.makeFromEncoded(bytes)
    if (scaleFactor == 1 || scaleFactor <= 0) {
        return image.toComposeImageBitmap()
    }
    val width = image.width
    val height = image.height
    val targetWidth = (width.toFloat() / scaleFactor).toInt()
    val targetHeight = (height.toFloat() / scaleFactor).toInt()
    val scaledBitmap = Bitmap().apply {
        allocPixels(
            ImageInfo.makeS32(
                targetWidth,
                targetHeight,
                ColorAlphaType.PREMUL
            )
        )
    }

    // 创建 Canvas 用于绘制缩放后的图片
    val canvas = Canvas(scaledBitmap)

    // 定义源矩形（原始图片尺寸）和目标矩形（缩放后尺寸）
    val sourceRect = Rect.makeWH(width.toFloat(), height.toFloat())
    val destRect = Rect.makeWH(targetWidth.toFloat(), targetHeight.toFloat())

    // 绘制并缩放图片
    canvas.drawImageRect(
        image = image,
        src = sourceRect,
        dst = destRect,
        paint = Paint().apply {
            // 设置缩放过滤，保证缩放质量
//            isFilterQuality = filterQuality
            isAntiAlias = true // 抗锯齿
        }
    )
    return Image.makeFromBitmap(scaledBitmap).also {
        scaledBitmap.close()
    }.toComposeImageBitmap()
}
actual fun uriToImageBitmap(`is`: RawSource): ImageBitmap? = uriToImageBitmap(`is`, 1)
actual fun uriToImageSize(`is`: RawSource): Pair<Int, Int> {
    val bytes = `is`.buffered().readAllBytesOrNull() ?: return 1 to 1
    val image = Image.makeFromEncoded(bytes)
    return image.width to image.height
}
actual fun writeImage(bitmap: ImageBitmap, os: RawSink) {
    // 1. Convert ImageBitmap to Skia Image and encode
    val skiaBitmap = bitmap.asSkiaBitmap()
    val data = Image.makeFromBitmap(skiaBitmap).encodeToData(EncodedImageFormat.PNG)
        ?: throw Exception("Failed to encode bitmap")

    // 2. Convert Data to ByteArray (skia bytes)
    val byteArray = data.bytes

    // 3. Write to RawSink
    os.buffered().use { bufferedSink ->
        bufferedSink.write(byteArray)
        bufferedSink.flush()
    }
}

