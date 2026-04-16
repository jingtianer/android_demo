package com.jingtian.composedemo.utils

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.io.files.Path
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSHomeDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

val globalWorkDir = NSSearchPathForDirectoriesInDomains(
    NSDocumentDirectory, NSUserDomainMask, true
).first() as String

actual fun getFileStorageRootDir(): Path = Path(globalWorkDir, "filestore")

actual fun getFileCacheStorageRootDir(): Path = Path(NSSearchPathForDirectoriesInDomains(
    NSCachesDirectory, NSUserDomainMask, true
).first() as String, "cache")

actual fun compressImageBitmap(bitmap: ImageBitmap, width: Int, height: Int, scale: Float): ImageBitmap {
    val image = Image.makeFromBitmap(bitmap.asSkiaBitmap())
    val originWidth = image.width
    val originHeight = image.height
    val targetWidth = width
    val targetHeight = height
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
    val sourceRect = Rect.makeWH(originWidth.toFloat(), originHeight.toFloat())
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
