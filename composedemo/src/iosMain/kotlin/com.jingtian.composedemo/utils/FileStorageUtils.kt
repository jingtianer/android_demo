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
actual fun getDBStorageRootDir():Path = Path(globalWorkDir, "db.db")
actual fun getFileCacheStorageRootDir(): Path = Path(NSSearchPathForDirectoriesInDomains(
    NSCachesDirectory, NSUserDomainMask, true
).first() as String, "cache")
