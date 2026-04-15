package com.jingtian.composedemo.multiplatform

import android.net.Uri
import androidx.core.net.toUri
import com.jingtian.composedemo.BuildKonfig
import com.jingtian.composedemo.main.remote.RemoteUriUtils
import com.jingtian.composedemo.utils.SerializationUtils.readAsUri
import kotlinx.io.files.Path
import java.io.File

private object MultiplatformFileFactory : IMultiplatformFileFactory {
    override fun fromFile(file: Path): MultiplatformFile {
        if (BuildKonfig.isRemote) {
            val uri = File(file.toString()).inputStream().readAsUri()
            if (uri.scheme?.startsWith("jingtian") == true) {
                RemoteUriUtils.parse(uri)?.let {
                    return it
                }
            }
            return MultiplatformFileImpl(File(file.toString()).inputStream().readAsUri())
        }
        return MultiplatformFileImpl(File(file.toString()).toUri())
    }

    override fun shareFile(file: Path): MultiplatformFile {
        return MultiplatformFileImpl(Uri.fromFile(File(file.toString())))
    }
}

actual fun getMultiplatformFileFactory() : IMultiplatformFileFactory = MultiplatformFileFactory
