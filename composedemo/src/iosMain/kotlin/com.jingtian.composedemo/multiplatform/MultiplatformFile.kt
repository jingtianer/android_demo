package com.jingtian.composedemo.multiplatform

import com.jingtian.composedemo.utils.extension
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString

actual fun getMultiplatformFileFactory() : IMultiplatformFileFactory {
    return object : IMultiplatformFileFactory {
        override fun fromFile(file: Path): MultiplatformFile {
            return MultiplatformFileImpl(file, file.extension)
        }
        override fun fromFile(file: Path, extension: String?): MultiplatformFile {
            return MultiplatformFileImpl(file, extension ?: file.extension)
        }

        override fun shareFile(file: Path): MultiplatformFileImpl {
            return MultiplatformFileImpl(Path(file.toString()), file.extension)
        }
    }
}