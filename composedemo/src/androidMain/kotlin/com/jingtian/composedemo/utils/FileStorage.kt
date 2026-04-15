package com.jingtian.composedemo.utils

import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.jingtian.composedemo.base.app
import kotlinx.io.files.Path
import java.io.File
import java.nio.file.Files
import kotlin.io.path.inputStream
import kotlin.io.path.isDirectory

actual fun getFileStorageRootDir(): Path = Path(app.getExternalFilesDir(null)!!.absolutePath)
actual fun getFileCacheStorageRootDir(): Path = Path(app.externalCacheDir!!.absolutePath)

actual fun compressImageBitmap(bitmap: ImageBitmap, width: Int, height: Int, scale: Float): ImageBitmap {
    // 2. 创建缩放矩阵
    val matrix = Matrix().apply {
        postScale(scale, scale)
    }

    // 3. 压缩并返回新Bitmap（注意回收原始Bitmap避免内存泄漏）
    val androidBitmap = bitmap.asAndroidBitmap()
    val compressedBitmap = Bitmap.createBitmap(
        androidBitmap, 0, 0, width, height, matrix, true
    )
    if (compressedBitmap != androidBitmap) {
        androidBitmap.recycle() // 回收原始Bitmap
    }
    return compressedBitmap.asImageBitmap()
}

@RequiresApi(Build.VERSION_CODES.O)
fun traverseDirNio(root: Path, execute: (String, Path)->Unit) {
    _traverseDirNio(root, "", execute)
}

@RequiresApi(Build.VERSION_CODES.O)
private fun _traverseDirNio(root: Path, rootDir: String, execute: (String, Path)->Unit) {
    Files.list(root)?.use { stream->
        stream.forEach { file->
            if (file.isDirectory()) {
                _traverseDirNio(file,  "${rootDir}/${file.fileName}", execute)
            } else {
                execute(rootDir, file)
            }
        }
    }
}

fun traverseDirFiles(root: File, execute: (String, File)->Unit) {
    _traverseDirFiles(root, "", execute)
}

fun _traverseDirFiles(root: File, rootDir: String, execute: (String, File)->Unit) {
    root.list()?.forEach { child->
        val file = File(root, child)
        if (file.isDirectory) {
            _traverseDirFiles(file, "${rootDir}/${file.name}", execute)
        } else {
            execute(rootDir, file)
        }
    }
}

fun File.ensureFile():File {
    if (exists()) {
        if (isDirectory) {
            deleteRecursively()
        } else {
            delete()
        }
    }
    this.parentFile?.takeIf { !it.exists() }?.mkdirs()
    this.createNewFile()
    return this
}

fun File.ensureFileExist(onAbsent: (File)->Unit):File {
    if (exists()) {
        if (isDirectory) {
            deleteRecursively()
        } else {
            return this
        }
    }
    this.parentFile?.takeIf { !it.exists() }?.mkdirs()
    this.createNewFile()
    onAbsent(this)
    return this
}

fun File.ensureDirExist():File {
    if (exists()) {
        if (isDirectory) {
            return this
        } else {
            delete()
        }
    }
    this.mkdirs()
    return this
}

fun copyDir(from : File, to: File) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        traverseDirNio(from.toPath()) { dir, file->
            file.inputStream().use { `is`->
                File(to, "${dir}/${file.fileName}").ensureFile().outputStream().use { os->
                    `is`.copyTo(os)
                    os.flush()
                }
            }
        }
    } else {
        traverseDirFiles(from) { dir, file->
            file.inputStream().use { `is`->
                File(to, "${dir}/${file.name}").ensureFile().outputStream().use { os->
                    `is`.copyTo(os)
                    os.flush()
                }
            }
        }
    }
}