package com.jingtian.composedemo.multiplatform

import androidx.core.net.toUri
import java.io.File

private object MultiplatformFileFactory : IMultiplatformFileFactory {
    override fun fromFile(file: File): MultiplatformFile {
        return MultiplatformFileImpl(file.toUri())
    }
}

actual fun getMultiplatformFileFactory() : IMultiplatformFileFactory = MultiplatformFileFactory
