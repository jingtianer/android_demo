package com.jingtian.composedemo.multiplatform

import com.jingtian.composedemo.utils.extension
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import java.io.File
import java.io.FileInputStream
import java.nio.charset.StandardCharsets

actual fun getMultiplatformFileFactory() : IMultiplatformFileFactory {
    return object : IMultiplatformFileFactory {
        override fun fromFile(file: Path): MultiplatformFile {
            val realFile = SystemFileSystem.source(file).buffered().use { fis->
                val bytes = fis.readAllBytesOrNull()
                File(bytes?.toString(StandardCharsets.UTF_8) ?: throw RuntimeException("file not found $file, bytes=$bytes"))
            }
            return MultiplatformFileImpl(realFile, realFile.extension)
        }
        override fun fromFile(file: Path, extension: String?): MultiplatformFile {
            val realFile = SystemFileSystem.source(file).buffered().use { fis->
                val bytes = fis.readAllBytesOrNull()
                File(bytes?.toString(StandardCharsets.UTF_8) ?: throw RuntimeException("file not found $file, bytes=$bytes"))
            }
            return MultiplatformFileImpl(realFile, extension ?: file.extension)
        }

        override fun shareFile(file: Path): MultiplatformFileImpl {
            return MultiplatformFileImpl(File(file.toString()), file.extension)
        }
    }
}