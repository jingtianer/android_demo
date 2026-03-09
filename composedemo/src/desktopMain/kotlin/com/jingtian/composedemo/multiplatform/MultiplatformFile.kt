package com.jingtian.composedemo.multiplatform

import java.io.File

actual fun getMultiplatformFileFactory() : IMultiplatformFileFactory {
    return object : IMultiplatformFileFactory {
        override fun fromFile(file: File): MultiplatformFile {
            return MultiplatformFileImpl(file)
        }
    }
}