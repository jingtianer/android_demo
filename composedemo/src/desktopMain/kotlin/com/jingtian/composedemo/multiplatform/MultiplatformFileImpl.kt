package com.jingtian.composedemo.multiplatform

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.jingtian.composedemo.dao.model.FileType
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.Java2DFrameConverter
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.images.Artwork
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets
import javax.imageio.ImageIO
import javax.imageio.ImageReader


class MultiplatformFileImpl(val realFile: File, val realExtension: String) : MultiplatformFile {
    companion object {
        val imageExtensions = arrayOf("jpg", "jpeg", "png", "gif", "bmp", "webp", "tiff", "ico", "heic", "heif", "jfif")
        val videoExtensions = arrayOf("mp4", "mov", "mkv", "avi", "webm", "flv", "wmv")
        val audioExtensions = arrayOf("mp3", "wav", "flac", "aac", "m4a", "ogg", "wma", "amr", "mid")
        val htmlExtensions = arrayOf("html", "svg")
    }

    // 视频第一帧提取函数
    private fun extractVideoFirstFrame(videoFile: File): ImageBitmap? {
        val grabber = FFmpegFrameGrabber(videoFile)

        return try {
            grabber.start()

            val converter = Java2DFrameConverter()

            val bufferedImage = converter.convert(grabber.grabImage())

            bufferedImage?.toComposeImageBitmap()
        } catch (e: Exception) {
            null
        } finally {
            grabber.stop()
            grabber.release()
        }
    }

    // 音频封面提取函数
    private fun extractAudioCover(audioFile: File): ImageBitmap? {
        return try {
            val audioFileObj = AudioFileIO.readAs(audioFile, realExtension)
            val tag = audioFileObj.tag
            // 获取第一个封面图片
            val artworkList = tag?.artworkList ?: return null
            if (artworkList.isEmpty()) return null

            val artwork: Artwork = artworkList[0]
            val imageBytes = artwork.binaryData
            val inputStream = imageBytes.inputStream()
            val bufferedImage = ImageIO.read(inputStream)

            // 转换为Compose的ImageBitmap
            bufferedImage?.toComposeImageBitmap()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getImageSize(imageFile: File): Pair<Int, Int>? {
        if (!imageFile.exists() || !imageFile.isFile) {
            return null
        }

        ImageIO.createImageInputStream(imageFile).use { iis ->
            // 获取该图片格式对应的ImageReader
            val readers: Iterator<ImageReader> = ImageIO.getImageReaders(iis)
            if (!readers.hasNext()) {
                return null
            }

            val reader: ImageReader = readers.next()
            try {
                reader.setInput(iis, true) // true = 只读取元数据，不读取像素

                // 读取宽高（索引0表示第一张图片，单张图片文件默认索引0）
                val width: Int = reader.getWidth(0)
                val height: Int = reader.getHeight(0)

                return width to height
            } catch (e: Exception) {
                return null
            } finally {
                reader.dispose() // 释放资源
            }
        }
    }


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
    override val inputStream: InputStream
        get() = FileInputStream(realFile)
    override val videoThumbnail: ImageBitmap?
        get() = extractVideoFirstFrame(realFile)
    override val audioThumbnail: ImageBitmap?
        get() = extractAudioCover(realFile)
    override val imageRatio: Pair<Int, Int>
        get() = getImageSize(realFile) ?: (1 to 1)
    override val fileStoreInputStream: InputStream
        get() = ByteArrayInputStream(realFile.absolutePath.toByteArray(StandardCharsets.UTF_8))
    override val file: File
        get() = realFile
    override val extension: String = realFile.extension
    override val path: String
        get() = realFile.path
}