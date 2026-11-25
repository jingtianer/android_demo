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
import java.io.ByteArrayInputStream

private object MultiplatformFileFactory : IMultiplatformFileFactory {
    override fun fromFile(rawContent: ByteArray, extension: String?): MultiplatformFile? {
        if (BuildKonfig.isRemote) {
            val uri = ByteArrayInputStream(rawContent).readAsUri() ?: return null
            if (uri?.scheme?.startsWith("jingtian") == true) {
                RemoteUriUtils.parse(uri)?.let {
                    return it
                }
            }
        }
        return MultiplatformFileImpl(File(rawContent.toString(Charsets.UTF_8)).toUri(), canDelete = true)
    }

    override fun shareFile(file: Path): MultiplatformFile {
        return MultiplatformFileImpl(Uri.fromFile(File(file.toString())), canDelete = true)
    }
}

actual fun getMultiplatformFileFactory() : IMultiplatformFileFactory = MultiplatformFileFactory
