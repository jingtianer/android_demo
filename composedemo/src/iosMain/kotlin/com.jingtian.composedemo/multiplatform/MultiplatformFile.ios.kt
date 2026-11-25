package com.jingtian.composedemo.multiplatform

import com.jingtian.composedemo.BuildKonfig
import com.jingtian.composedemo.main.remote.RemoteUriUtils
import com.jingtian.composedemo.main.remote.Uri
import com.jingtian.composedemo.utils.extension
import com.jingtian.composedemo.utils.getFileStorageRootDir
import com.jingtian.composedemo.utils.relativeTo
import kotlinx.io.Buffer
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString

actual fun getMultiplatformFileFactory() : IMultiplatformFileFactory {
    val regex = Regex("^([a-zA-Z0-9]+)://([^/]+)(/.*)$")
    fun readAsRemoteUri(url: String): Uri? {
//        logD("readAsRemoteUri", "url: $url")
        val matchResult = regex.matchEntire(url) ?: return null
        val scheme = matchResult.groups[1]?.value ?: return null
        val authority = matchResult.groups[2]?.value ?: return null
        val path = matchResult.groups[3]?.value ?: return null
//        logD("readAsRemoteUri", "url: $url, scheme=$scheme, authority=$authority, path=$path")
        return Uri(scheme = scheme, authority = authority, path = path)
    }

    return object : IMultiplatformFileFactory {
        override fun fromFile(rawContent: ByteArray, extension: String?): MultiplatformFile? {
            val buffer = Buffer()
            buffer.write(rawContent)
            val content = buffer.use {
                it.readString()
            }
            if (BuildKonfig.isRemote) {
//                logD("fromFile", "$file, extension=$extension")
                val uri = readAsRemoteUri(content)
                if (uri?.scheme?.startsWith("jingtian") == true) {
                    RemoteUriUtils.parse(uri)?.let {
                        return it
                    }
                }
            }
            val realFile = Path(
                getFileStorageRootDir(),
                content
            )
            return MultiplatformFileImpl(realFile, extension ?: realFile.extension, relativePath = file.relativeTo(getFileStorageRootDir()), canDelete = true)
        }

        override fun shareFile(file: Path): MultiplatformFileImpl {
            return MultiplatformFileImpl(Path(file.toString()), file.extension, canDelete = true)
        }
    }
}
