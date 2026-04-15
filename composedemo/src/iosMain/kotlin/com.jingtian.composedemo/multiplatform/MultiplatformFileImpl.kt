package com.jingtian.composedemo.multiplatform

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.jingtian.composedemo.dao.model.FileType
import com.jingtian.composedemo.utils.extension
import com.jingtian.composedemo.utils.uriToImageSize
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.io.Buffer
import kotlinx.io.RawSource
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.writeString
import org.jetbrains.skia.Image
import platform.AVFoundation.AVAsset
import platform.AVFoundation.AVAssetImageGenerator
import platform.AVFoundation.AVMetadataCommonKeyArtwork
import platform.AVFoundation.AVMetadataItem
import platform.AVFoundation.AVURLAsset
import platform.AVFoundation.commonKey
import platform.AVFoundation.commonMetadata
import platform.CoreMedia.CMTimeMake
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.UIKit.UIImage
import platform.UIKit.UIImagePNGRepresentation
import platform.posix.memcpy

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
class MultiplatformFileImpl(val realFile: Path, val realExtension: String) : MultiplatformFile {
    companion object {
        val imageExtensions = arrayOf("jpg", "jpeg", "png", "gif", "bmp", "webp", "tiff", "ico", "heic", "heif", "jfif")
        val videoExtensions = arrayOf("mp4", "mov", "mkv", "avi", "webm", "flv", "wmv")
        val audioExtensions = arrayOf("mp3", "wav", "flac", "aac", "m4a", "ogg", "wma", "amr", "mid")
        val htmlExtensions = arrayOf("html", "svg")
    }

    @OptIn(ExperimentalForeignApi::class)
    fun getVideoThumbnail(filePath: Path): ImageBitmap?  {
        return try {
            val url = NSURL.fileURLWithPath(filePath.toString())
            val asset = AVAsset.assetWithURL(url)
            val generator = AVAssetImageGenerator(asset)
            generator.appliesPreferredTrackTransform = true

            val time = CMTimeMake(1, 60)
            val cgImage = generator.copyCGImageAtTime(time, null, null)
            val uiImage = UIImage.imageWithCGImage(cgImage)
            uiImage.toImageBitmap()
        } catch (e: Exception) {
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

    override val fileName: String?
        get() = realFile.name
    override val isHidden: Boolean
        get() = realFile.isHidden
    override val mediaType: FileType
        get() = when(realFile.extension.lowercase()) {
            in imageExtensions -> FileType.IMAGE
            in videoExtensions -> FileType.VIDEO
            in audioExtensions -> FileType.AUDIO
            in htmlExtensions -> FileType.HTML
            else -> FileType.RegularFile
        }
    override val inputStream: RawSource?
        get() = SystemFileSystem.source(realFile)

    override val videoThumbnail: ImageBitmap?
        get() = getVideoThumbnail(realFile)

    override val audioThumbnail: ImageBitmap?
        get() = getAudioCoverImage(realFile) // iOS 端暂未实现音频封面提取

    override val imageRatio: Pair<Int, Int>
        get() = runCatching {
            SystemFileSystem.source(realFile).use { src ->
                uriToImageSize(src)
            }
        }.getOrElse { 1 to 1 }

    override val fileStoreInputStream: RawSource?
        get() {
            return SystemFileSystem.source(realFile)
        }
    override val file: Path
        get() = Path(realFile)
    override val extension: String = realExtension
    override val path: String
        get() = realFile.toString()
}
