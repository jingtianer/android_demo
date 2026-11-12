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
import cnames.structs.__CFNumber
import com.jingtian.composedemo.multiplatform.logD
import com.jingtian.composedemo.multiplatform.toImageBitmap
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import kotlinx.io.files.Path
import kotlinx.io.readByteArray
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionaryGetValue
import platform.CoreFoundation.CFDictionarySetValue
import platform.CoreFoundation.CFNumberCreate
import platform.CoreFoundation.CFNumberGetValue
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFURLCreateFromFileSystemRepresentation
import platform.CoreFoundation.CFURLRef
import platform.CoreFoundation.kCFAllocatorDefault
import platform.CoreFoundation.kCFBooleanFalse
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFNumberIntType
import platform.CoreFoundation.kCFNumberSInt32Type
import platform.CoreGraphics.CGImageRef
import platform.Foundation.NSURL
import platform.ImageIO.CGImageSourceCopyPropertiesAtIndex
import platform.ImageIO.CGImageSourceCreateThumbnailAtIndex
import platform.ImageIO.CGImageSourceCreateWithURL
import platform.ImageIO.kCGImagePropertyPixelHeight
import platform.ImageIO.kCGImagePropertyPixelWidth
import platform.ImageIO.kCGImageSourceCreateThumbnailFromImageAlways
import platform.ImageIO.kCGImageSourceCreateThumbnailWithTransform
import platform.ImageIO.kCGImageSourceGenerateImageSpecificLumaScaling
import platform.ImageIO.kCGImageSourceShouldCache
import platform.ImageIO.kCGImageSourceShouldCacheImmediately
import platform.ImageIO.kCGImageSourceThumbnailMaxPixelSize
import platform.UIKit.UIImage

import kotlinx.cinterop.*
import org.jetbrains.skia.ColorType
import platform.CoreGraphics.*
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
private fun Path.toCFURLRef(isDirectory: Boolean = false): CFURLRef? = memScoped {
    val pathBytes = this@toCFURLRef.toString().encodeToByteArray()
    pathBytes.usePinned { pinned ->
        CFURLCreateFromFileSystemRepresentation(
            kCFAllocatorDefault,
            pinned.addressOf(0).reinterpret(),
            pathBytes.size.convert(),
            isDirectory
        )
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun readImageBoundsOnly(fileUrl: CFURLRef): Pair<Int, Int>? = memScoped {
    val imageSource = CGImageSourceCreateWithURL(fileUrl, null) ?: return@memScoped null
    try {
        val properties = CGImageSourceCopyPropertiesAtIndex(imageSource, 0u, null) ?: return@memScoped null
        try {
            val widthRef = CFDictionaryGetValue(properties, kCGImagePropertyPixelWidth) ?: return@memScoped null
            val heightRef = CFDictionaryGetValue(properties, kCGImagePropertyPixelHeight) ?: return@memScoped null
            val width = alloc<IntVar>()
            val height = alloc<IntVar>()
            CFNumberGetValue(widthRef.reinterpret<__CFNumber>(), kCFNumberSInt32Type, width.ptr)
            CFNumberGetValue(heightRef.reinterpret<__CFNumber>(), kCFNumberSInt32Type, height.ptr)
            if (width.value <= 0 || height.value <= 0) {
                null
            } else {
                width.value to height.value
            }
        } finally {
            CFRelease(properties)
        }
    } finally {
        CFRelease(imageSource)
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun decodeSampledImage(fileUrl: CFURLRef, maxPixelSize: Int): CGImageRef? = memScoped {
    val imageSource = CGImageSourceCreateWithURL(fileUrl, null) ?: return@memScoped null
    val maxPixelSizeValue = alloc<IntVar>().apply { value = maxPixelSize }
    val maxPixelSizeNumber = CFNumberCreate(kCFAllocatorDefault, kCFNumberIntType, maxPixelSizeValue.ptr)
    return@memScoped try {
        val options = CFDictionaryCreateMutable(kCFAllocatorDefault, 0, null, null)
        try {
            CFDictionarySetValue(options, kCGImageSourceShouldCache, kCFBooleanFalse)
            CFDictionarySetValue(options, kCGImageSourceShouldCacheImmediately, kCFBooleanTrue)
            CFDictionarySetValue(options, kCGImageSourceCreateThumbnailFromImageAlways, kCFBooleanTrue)
            CFDictionarySetValue(options, kCGImageSourceCreateThumbnailWithTransform, kCFBooleanTrue)
            CFDictionarySetValue(options, kCGImageSourceThumbnailMaxPixelSize, maxPixelSizeNumber)
            CFDictionarySetValue(options, kCGImageSourceGenerateImageSpecificLumaScaling, kCFBooleanFalse)
            CGImageSourceCreateThumbnailAtIndex(imageSource, 0u, options)
        } finally {
            if (options != null) {
                CFRelease(options)
            }
        }
    } finally {
        if (maxPixelSizeNumber != null) {
            CFRelease(maxPixelSizeNumber)
        }
        CFRelease(imageSource)
    }
}

@OptIn(ExperimentalForeignApi::class)
actual fun uriToImageBitmap(path: Path, scaleFactor: Int): ImageBitmap? {
    val fileUrl = path.toCFURLRef() ?: return null
    val securityScopedUrl = NSURL.fileURLWithPath(path.toString(), false)

    val needStopAccess = securityScopedUrl.startAccessingSecurityScopedResource()
    return try {
        val (intrinsicWidth, intrinsicHeight) = readImageBoundsOnly(fileUrl) ?: return null
        val safeScaleFactor = maxOf(1, scaleFactor)
        val targetLongSide = maxOf(intrinsicWidth, intrinsicHeight) / safeScaleFactor
        val cgImage = decodeSampledImage(fileUrl, targetLongSide) ?: return null
        try {
            cgImage.toSkiaImageBitmap()?.also {
                logD("uriToImageBitmap") {
                    "path=$path\nsize=${it.width},${it.height}\noriginalSize=${intrinsicWidth},${intrinsicHeight}"
                }
            }
        } finally {
            CFRelease(cgImage)
        }
    } finally {
        if (needStopAccess) {
            securityScopedUrl.stopAccessingSecurityScopedResource()
        }
        CFRelease(fileUrl)
    }
}

@OptIn(ExperimentalForeignApi::class)
fun CGImageRef.toSkiaImageBitmap(): ImageBitmap? {
    val width = CGImageGetWidth(this).toInt()
    val height = CGImageGetHeight(this).toInt()
    if (width <= 0 || height <= 0) return null

    val colorSpace = CGColorSpaceCreateWithName(kCGColorSpaceSRGB)
        ?: CGColorSpaceCreateDeviceRGB()
        ?: return null

    return try {
        val bytesPerRow = width * 4
        val totalBytes = height * bytesPerRow
        val bitmapInfo = CGImageAlphaInfo.kCGImageAlphaPremultipliedLast.value or
                kCGBitmapByteOrder32Big

        val context = CGBitmapContextCreate(
            data = null,
            width = width.toULong(),
            height = height.toULong(),
            bitsPerComponent = 8u,
            bytesPerRow = bytesPerRow.toULong(),
            space = colorSpace,
            bitmapInfo = bitmapInfo
        ) ?: return null

        try {
            CGContextDrawImage(context, CGRectMake(0.0, 0.0, width.toDouble(), height.toDouble()), this)
            val rawData = CGBitmapContextGetData(context) ?: return null

            val imageInfo = ImageInfo(
                width = width,
                height = height,
                colorType = ColorType.RGBA_8888,
                alphaType = ColorAlphaType.PREMUL
            )

            val skiaBitmap = Bitmap()
            if (!skiaBitmap.allocPixels(imageInfo)) return null

            // 关键修改：直接向 Skia Bitmap 内部底层内存地址拷贝，不在 Kotlin 堆上创建大 ByteArray，减少 GC 卡顿
            val dstNativePtr = skiaBitmap.peekPixels()?.addr ?: return null
            val dstPointer = interpretCPointer<ByteVar>(dstNativePtr) ?: return null
            memcpy(dstPointer, rawData, totalBytes.toULong())

            Image.makeFromBitmap(skiaBitmap).toComposeImageBitmap()
        } finally {
            CGContextRelease(context)
        }
    } finally {
        CGColorSpaceRelease(colorSpace)
    }
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

