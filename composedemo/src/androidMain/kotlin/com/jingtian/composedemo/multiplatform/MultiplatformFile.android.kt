package com.jingtian.composedemo.multiplatform

import android.net.Uri
import androidx.core.net.toUri
import com.jingtian.composedemo.BuildKonfig
import com.jingtian.composedemo.main.remote.RemoteUriUtils
import com.jingtian.composedemo.utils.SerializationUtils.readAsUri
import com.jingtian.composedemo.utils.getFileStorageRootDir
import kotlinx.io.files.Path
import java.io.File
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.buffered
import kotlinx.io.readString

private object MultiplatformFileFactory : IMultiplatformFileFactory {
    override fun fromFile(file: Path): MultiplatformFile {
        if (BuildKonfig.isRemote) {
            val uri = File(file.toString()).inputStream().readAsUri()
            if (uri?.scheme?.startsWith("jingtian") == true) {
                RemoteUriUtils.parse(uri)?.let {
                    return it
                }
            }
        }
        val realFile = Path(
            getFileStorageRootDir(),
            SystemFileSystem.source(file).buffered().use {
                it.readString()
            }
        )
        return MultiplatformFileImpl(File(realFile.toString()).toUri(), canDelete = true)
    }

    override fun shareFile(file: Path): MultiplatformFile {
        return MultiplatformFileImpl(Uri.fromFile(File(file.toString())), canDelete = true)
    }
}

actual fun getMultiplatformFileFactory() : IMultiplatformFileFactory = MultiplatformFileFactory
