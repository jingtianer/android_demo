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
import platform.ImageIO.kCGImageSourceShouldCache
import platform.ImageIO.kCGImageSourceShouldCacheImmediately
import platform.ImageIO.kCGImageSourceThumbnailMaxPixelSize
import platform.UIKit.UIImage

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
            CFDictionarySetValue(options, kCGImageSourceShouldCacheImmediately, kCFBooleanFalse)
            CFDictionarySetValue(options, kCGImageSourceCreateThumbnailFromImageAlways, kCFBooleanTrue)
            CFDictionarySetValue(options, kCGImageSourceCreateThumbnailWithTransform, kCFBooleanTrue)
            CFDictionarySetValue(options, kCGImageSourceThumbnailMaxPixelSize, maxPixelSizeNumber)
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
            UIImage.imageWithCGImage(cgImage).toImageBitmap()
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

