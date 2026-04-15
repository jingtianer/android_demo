package com.jingtian.composedemo.multiplatform

import com.jingtian.composedemo.utils.extension
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

actual fun getMultiplatformFileFactory() : IMultiplatformFileFactory {
    return object : IMultiplatformFileFactory {
        override fun fromFile(file: Path): MultiplatformFile {
            val realFile = SystemFileSystem.source(file).buffered().use { fis->
                val bytes = fis.readAllBytesOrNull()
                Path(bytes?.toString() ?: throw RuntimeException("file not found $file, bytes=$bytes"))
            }
            return MultiplatformFileImpl(realFile, realFile.extension)
        }
        override fun fromFile(file: Path, extension: String?): MultiplatformFile {
            val realFile = SystemFileSystem.source(file).buffered().use { fis->
                val bytes = fis.readAllBytesOrNull()
                Path(bytes?.toString() ?: throw RuntimeException("file not found $file, bytes=$bytes"))
            }
            return MultiplatformFileImpl(realFile, extension ?: file.extension)
        }

        override fun shareFile(file: Path): MultiplatformFileImpl {
            return MultiplatformFileImpl(Path(file.toString()), file.extension)
        }
    }
}