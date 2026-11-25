package com.jingtian.composedemo.multiplatform

import com.jingtian.composedemo.BuildKonfig
import com.jingtian.composedemo.main.remote.RemoteUriUtils
import com.jingtian.composedemo.main.remote.Uri
import com.jingtian.composedemo.utils.extension
import io.ktor.utils.io.charsets.Charsets
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import java.io.File
import java.io.InputStream
import java.nio.charset.StandardCharsets

actual fun getMultiplatformFileFactory() : IMultiplatformFileFactory {
    val regex = Regex("^([a-zA-Z0-9]+)://([^/]+)(/.*)$")
    fun String.readAsUri(): Uri? {
        val url = this
        val matchResult = regex.matchEntire(url)
        return matchResult?.let {
            val scheme = it.groups[1]?.value ?: return@let null
            val authority = it.groups[2]?.value ?: return@let null
            val path = it.groups[3]?.value ?: return@let null
            Uri(scheme = scheme, authority = authority, path = path)
        }
    }
    return object : IMultiplatformFileFactory {
        override fun fromFile(contentRaw: ByteArray, extension: String?): MultiplatformFile? {
            val content = contentRaw.toString(Charsets.UTF_8)
            if (BuildKonfig.isRemote) {
                val uri = content.readAsUri()
                if (uri?.scheme?.startsWith("jingtian") == true) {
                    RemoteUriUtils.parse(uri)?.let {
                        return it
                    }
                }
            }
            val realFile = File(content)
            return MultiplatformFileImpl(realFile, extension ?: file.extension)
        }

        override fun shareFile(file: Path): MultiplatformFileImpl {
            return MultiplatformFileImpl(File(file.toString()), file.extension)
        }
    }
}