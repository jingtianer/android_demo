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
actual fun getDBStorageRootDir(): Path = Path(getFileStorageRootDir().toString(), "database")

@RequiresApi(Build.VERSION_CODES.O)
fun traverseDirNio(root: java.nio.file.Path, execute: (String, java.nio.file.Path)->Unit) {
    _traverseDirNio(root, "", execute)
}

@RequiresApi(Build.VERSION_CODES.O)
private fun _traverseDirNio(root: java.nio.file.Path, rootDir: String, execute: (String, java.nio.file.Path)->Unit) {
    Files.list(File(root.toString()).toPath())?.use { stream->
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

fun File.ensureIsFile(onAbsent: (File)->Unit):File {
    if (exists()) {
        if (isDirectory) {
            deleteRecursively()
        } else {
            onAbsent(this)
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