package com.jingtian.composedemo.multiplatform

import java.io.File
import java.io.FileInputStream
import java.nio.charset.StandardCharsets

actual fun getMultiplatformFileFactory() : IMultiplatformFileFactory {
    return object : IMultiplatformFileFactory {
        override fun fromFile(file: File): MultiplatformFile {
            val realFile = FileInputStream(file).use { fis->
                File(fis.readBytes().toString(StandardCharsets.UTF_8))
            }
            return MultiplatformFileImpl(realFile, realFile.extension)
        }
        override fun fromFile(file: File, extension: String?): MultiplatformFile {
            val realFile = FileInputStream(file).use { fis->
                File(fis.readBytes().toString(StandardCharsets.UTF_8))
            }
            return MultiplatformFileImpl(realFile, extension ?: file.extension)
        }
    }
}