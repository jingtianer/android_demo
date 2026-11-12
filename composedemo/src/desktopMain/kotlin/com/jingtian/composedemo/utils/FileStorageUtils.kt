package com.jingtian.composedemo.utils

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.io.files.Path
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect
import org.jetbrains.skiko.toImage
import java.io.File

val globalWorkDir = if (File(".").canWrite()) {
    File("./ComposeDemo/workdir")
} else {
    File(System.getProperty("user.home"), "ComposeDemo/workdir")
}

actual fun getFileStorageRootDir(): Path = Path(File(globalWorkDir, "filestore").absolutePath)
actual fun getFileCacheStorageRootDir(): Path = Path(File(globalWorkDir, "cache").absolutePath)
actual fun getDBStorageRootDir(): Path = Path(File(globalWorkDir, "database").absolutePath)
