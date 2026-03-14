package com.jingtian.composedemo.multiplatform

import androidx.core.net.toUri
import com.jingtian.composedemo.BuildKonfig
import com.jingtian.composedemo.utils.SerializationUtils.readAsUri
import java.io.File

private object MultiplatformFileFactory : IMultiplatformFileFactory {
    override fun fromFile(file: File): MultiplatformFile {
        if (BuildKonfig.isRemote) {
            return MultiplatformFileImpl(file.inputStream().readAsUri())
        }
        return MultiplatformFileImpl(file.toUri())
    }
}

actual fun getMultiplatformFileFactory() : IMultiplatformFileFactory = MultiplatformFileFactory
