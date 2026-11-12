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
import com.jingtian.composedemo.utils.getAudioCoverImage
import com.jingtian.composedemo.utils.getVideoThumbnail
import com.jingtian.composedemo.utils.readImageBoundsOnly
import com.jingtian.composedemo.utils.toCFURLRef
import com.jingtian.composedemo.utils.uriToImageBitmap
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.autoreleasepool
import kotlinx.cinterop.convert
import kotlinx.cinterop.interpretObjCPointerOrNull
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
import platform.Foundation.CFBridgingRelease

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
    protected suspend fun imageBoundsOnly(): Pair<Int, Int>? = memScoped {
        val filePath = file()
        val fileUrl = filePath.toCFURLRef() ?: return null
        val securityScopedUrl = interpretObjCPointerOrNull<NSURL>(fileUrl.rawValue)
        val needStopAccess = securityScopedUrl?.startAccessingSecurityScopedResource() ?: false
        try {
            return@memScoped readImageBoundsOnly(fileUrl)
        } finally {
            if (needStopAccess) {
                securityScopedUrl?.stopAccessingSecurityScopedResource()
            }
            CFRelease(fileUrl)
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
