package com.jingtian.composedemo.multiplatform

import com.jingtian.composedemo.utils.extension
import com.jingtian.composedemo.utils.getFileStorageRootDir
import com.jingtian.composedemo.utils.relativeTo
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString

actual fun getMultiplatformFileFactory() : IMultiplatformFileFactory {
    return object : IMultiplatformFileFactory {
        override fun fromFile(file: Path): MultiplatformFile {
            val realFile = Path(
                getFileStorageRootDir(),
                SystemFileSystem.source(file).buffered().use {
                    it.readString()
                }
            )
            return MultiplatformFileImpl(realFile, realFile.extension, relativePath = file.relativeTo(getFileStorageRootDir()))
        }
        override fun fromFile(file: Path, extension: String?): MultiplatformFile {
            val realFile = Path(
                getFileStorageRootDir(),
                SystemFileSystem.source(file).buffered().use {
                    it.readString()
                }
            )
            return MultiplatformFileImpl(realFile, extension ?: realFile.extension, relativePath = file.relativeTo(getFileStorageRootDir()))
        }

        override fun shareFile(file: Path): MultiplatformFileImpl {
            return MultiplatformFileImpl(Path(file.toString()), file.extension)
        }
    }
}