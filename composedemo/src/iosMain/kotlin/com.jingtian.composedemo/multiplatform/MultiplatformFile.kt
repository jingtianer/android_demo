package com.jingtian.composedemo.multiplatform

import com.jingtian.composedemo.utils.extension
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString

actual fun getMultiplatformFileFactory() : IMultiplatformFileFactory {
    return object : IMultiplatformFileFactory {
        override fun fromFile(file: Path): MultiplatformFile {
            val realFile = Path(
                SystemFileSystem.source(file).buffered().use {
                    it.readString()
                }
            )
            return MultiplatformFileImpl(realFile, realFile.extension)
        }
        override fun fromFile(file: Path, extension: String?): MultiplatformFile {
            val realFile = Path(
                SystemFileSystem.source(file).buffered().use {
                    it.readString()
                }
            )
            return MultiplatformFileImpl(realFile, extension ?: realFile.extension)
        }

        override fun shareFile(file: Path): MultiplatformFileImpl {
            return MultiplatformFileImpl(Path(file.toString()), file.extension)
        }
    }
}