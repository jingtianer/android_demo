package com.jingtian.composedemo.utils

import androidx.annotation.IntRange
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import com.jingtian.composedemo.dao.DataBase
import com.jingtian.composedemo.dao.model.FileInfo
import com.jingtian.composedemo.dao.model.FileType
import com.jingtian.composedemo.multiplatform.MultiplatformFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.lang.ref.SoftReference
import java.util.concurrent.ConcurrentHashMap
import javax.imageio.ImageIO
import kotlin.math.max

actual fun uriToImageBitmap(`is`: InputStream, scaleFactor: Int): ImageBitmap? {
    val image = Image.makeFromEncoded(`is`.readBytes())
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
actual fun uriToImageBitmap(`is`: InputStream): ImageBitmap? = uriToImageBitmap(`is`, 1)
actual fun uriToImageSize(`is`: InputStream): Pair<Int, Int> {
    val image = Image.makeFromEncoded(`is`.readBytes())
    return image.width to image.height
}
actual fun writeImage(bitmap: ImageBitmap, os: OutputStream) {
    val image = bitmap.toAwtImage()
    ImageIO.write(image, "png", os)
}