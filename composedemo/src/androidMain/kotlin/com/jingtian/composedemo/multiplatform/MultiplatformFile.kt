package com.jingtian.composedemo.multiplatform

import androidx.core.net.toUri
import com.jingtian.composedemo.BuildKonfig
import com.jingtian.composedemo.main.remote.RemoteUriUtils
import com.jingtian.composedemo.utils.SerializationUtils.readAsUri
import java.io.File

private object MultiplatformFileFactory : IMultiplatformFileFactory {
    override fun fromFile(file: File): MultiplatformFile {
        if (BuildKonfig.isRemote) {
            val uri = file.inputStream().readAsUri()
            if (uri.scheme?.startsWith("jingtian") == true) {
                RemoteUriUtils.parse(uri)?.let {
                    return it
                }
            }
            return MultiplatformFileImpl(file.inputStream().readAsUri())
        }
        return MultiplatformFileImpl(file.toUri())
    }

    override fun shareFile(file: File): MultiplatformFile {
        return MultiplatformFileImpl(file.toUri())
    }
}

actual fun getMultiplatformFileFactory() : IMultiplatformFileFactory = MultiplatformFileFactory
