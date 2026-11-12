package com.jingtian.composedemo.multiplatform

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import cnames.structs.__CFNumber
import com.jingtian.composedemo.dao.model.FileType
import com.jingtian.composedemo.utils.BitMapCachePool
import com.jingtian.composedemo.utils.delete
import com.jingtian.composedemo.utils.ensureNotExist
import com.jingtian.composedemo.utils.exists
import com.jingtian.composedemo.utils.extension
import com.jingtian.composedemo.utils.toSkiaImageBitmap
import com.jingtian.composedemo.utils.uriToImageBitmap
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.autoreleasepool
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import kotlinx.io.RawSource
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import org.jetbrains.skia.Image
import platform.AVFoundation.AVAsset
import platform.AVFoundation.AVAssetImageGenerator
import platform.AVFoundation.AVMetadataCommonKeyArtwork
import platform.AVFoundation.AVMetadataItem
import platform.AVFoundation.AVURLAsset
import platform.AVFoundation.commonKey
import platform.AVFoundation.commonMetadata
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionaryGetValue
import platform.CoreFoundation.CFDictionarySetValue
import platform.CoreFoundation.CFNumberCreate
import platform.CoreFoundation.CFNumberGetValue
import platform.CoreFoundation.CFNumberRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFURLCreateFromFileSystemRepresentation
import platform.CoreFoundation.CFURLRef
import platform.CoreFoundation.kCFAllocatorDefault
import platform.CoreFoundation.kCFBooleanFalse
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFNumberIntType
import platform.CoreFoundation.kCFNumberSInt32Type
import platform.CoreGraphics.CGImageRef
import platform.CoreMedia.CMTimeGetSeconds
import platform.CoreMedia.CMTimeMake
import platform.CoreMedia.CMTimeMakeWithSeconds
import platform.Foundation.NSData
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
import platform.UIKit.UIImagePNGRepresentation
import platform.posix.memcpy
import platform.CoreFoundation.CFRelease
import platform.CoreGraphics.CGSizeMake
import platform.CoreMedia.CMTimeGetSeconds
import platform.CoreMedia.CMTimeMakeWithSeconds

@OptIn(ExperimentalForeignApi::class)
fun UIImage.toImageBitmap(): ImageBitmap {
    // 1. Convert UIImage to NSData (PNG representation)
    val pngData = UIImagePNGRepresentation(this)
        ?: throw IllegalArgumentException("Could not convert UIImage to PNG data")

    // 2. Convert NSData to Kotlin ByteArray
    val byteArray = ByteArray(pngData.length.toInt()).apply {
        usePinned { pinned ->
            memcpy(pinned.addressOf(0), pngData.bytes, pngData.length)
        }
    }

    // 3. Decode ByteArray into Skia Image and then to Compose ImageBitmap
    return Image.makeFromEncoded(byteArray).toComposeImageBitmap()
}
open class MultiplatformFileImpl(val realFile: Path, val realExtension: String, val relativePath: Path = realFile, val nsurl: NSURL? = null) : MultiplatformFile {
    companion object {
        val imageExtensions = arrayOf("jpg", "jpeg", "png", "gif", "bmp", "webp", "tiff", "ico", "heic", "heif", "jfif")
        val videoExtensions = arrayOf("mp4", "mov", "mkv", "avi", "webm", "flv", "wmv")
        val audioExtensions = arrayOf("mp3", "wav", "flac", "aac", "m4a", "ogg", "wma", "amr", "mid")
        val htmlExtensions = arrayOf("html", "svg")
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    fun getVideoThumbnail(filePath: Path): ImageBitmap? {
        return try {
            autoreleasepool {
                val url = NSURL.fileURLWithPath(filePath.toString())
                val asset = AVAsset.assetWithURL(url)
                val generator = AVAssetImageGenerator(asset).apply {
                    // 1. 自动处理视频旋转方向（竖屏/横屏矫正）
                    appliesPreferredTrackTransform = true
                    // 2. 关键优化：限制最大输出尺寸（保持宽高比），避免 4K 视频吐出巨型位图
//                    maximumSize = CGSizeMake(maxWidth.toCGFloat(), maxHeight.toCGFloat())
                }

                // 注意：如果只是作为列表缩略图且不要求精确到某一帧，
                // 可开启容差以大幅提升关键帧（I-Frame）提取速度（默认是精确解码，速度较慢）：
                // generator.requestedTimeToleranceBefore = CMTimeMakeWithSeconds(0.5, 600)
                // generator.requestedTimeToleranceAfter = CMTimeMakeWithSeconds(0.5, 600)

                val durationSeconds = CMTimeGetSeconds(asset.duration)
                val targetSeconds = when {
                    !durationSeconds.isFinite() || durationSeconds <= 0.0 -> 0.0
                    durationSeconds < 1.0 -> durationSeconds / 2.0
                    else -> 1.0
                }
                val time = CMTimeMakeWithSeconds(targetSeconds, 600)

                // 3. 修复：在 autoreleasepool 内部必须使用带标签的 return@autoreleasepool
                val cgImage = generator.copyCGImageAtTime(time, null, null)
                    ?: return@autoreleasepool null

                try {
                    cgImage.toSkiaImageBitmap()
                } finally {
                    CFRelease(cgImage)
                }
            }
        } catch (e: Exception) {
            logE("getVideoThumbnail", "error", e)
            null
        }
    }


    fun getAudioCoverImage(filePath: Path): ImageBitmap? {
        val asset = AVURLAsset.URLAssetWithURL(NSURL.fileURLWithPath(filePath.toString()), null)

        // Look for common metadata (artwork)
        val metadata = asset.commonMetadata
        for (item in metadata) {
            if (item is AVMetadataItem && item.commonKey == AVMetadataCommonKeyArtwork) {
                val data = item.value as? NSData ?: continue
                return UIImage(data).toImageBitmap() // Returns image NSData
            }
        }
        return null
    }

    private val Path.isHidden: Boolean
        get() = name.startsWith(".")

    override suspend fun fileName(): String = realFile.name
    override suspend fun isHidden(): Boolean = realFile.isHidden
    override suspend fun mediaType(): FileType = when(realFile.extension.lowercase()) {
            in imageExtensions -> FileType.IMAGE
            in videoExtensions -> FileType.VIDEO
            in audioExtensions -> FileType.AUDIO
            in htmlExtensions -> FileType.HTML
            else -> FileType.RegularFile
        }
    override suspend fun inputStream(): RawSource? = SystemFileSystem.source(realFile)

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
    protected suspend fun imageBoundsOnly(): Pair<Int, Int>? = memScoped {
        val filePath = file()
        val fileUrl = filePath.toCFURLRef() ?: return null
        val securityScopedUrl = NSURL.fileURLWithPath(filePath.toString(), false)
        val needStopAccess = securityScopedUrl.startAccessingSecurityScopedResource()
        val imageSource = CGImageSourceCreateWithURL(fileUrl, null)
        if (imageSource == null) {
            if (needStopAccess) {
                securityScopedUrl.stopAccessingSecurityScopedResource()
            }
            CFRelease(fileUrl)
            return@memScoped null
        }
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
            if (needStopAccess) {
                securityScopedUrl.stopAccessingSecurityScopedResource()
            }
            CFRelease(fileUrl)
            CFRelease(imageSource)
        }
    }

    override suspend fun imageThumbnail(scaleFactor: Int): ImageBitmap? {
        return uriToImageBitmap(file(), scaleFactor)
    }

    override suspend fun videoThumbnail(): ImageBitmap? = getVideoThumbnail(realFile)

    override suspend fun audioThumbnail(): ImageBitmap? = getAudioCoverImage(realFile)

    override suspend fun imageRatio(): Pair<Int, Int> = runCatching {
        imageBoundsOnly() ?: (1 to 1)
    }.getOrElse { 1 to 1 }

    override suspend fun fileStoreInputStream(): RawSource? = SystemFileSystem.source(realFile)
    override suspend fun file(): Path = Path(realFile)
    override suspend fun extension(): String = realExtension
    override suspend fun path(): String = relativePath.toString()

    override fun onStoreFinish() {
        nsurl?.stopAccessingSecurityScopedResource()
    }

    override fun onDelete() {
        if (nsurl != null) {
            realFile.ensureNotExist()
        }
    }
}
