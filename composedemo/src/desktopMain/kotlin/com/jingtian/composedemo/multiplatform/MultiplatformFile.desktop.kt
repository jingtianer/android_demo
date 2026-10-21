package com.jingtian.composedemo.multiplatform

import com.jingtian.composedemo.BuildKonfig
import com.jingtian.composedemo.main.remote.RemoteUriUtils
import com.jingtian.composedemo.main.remote.Uri
import com.jingtian.composedemo.utils.extension
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import java.io.File
import java.io.InputStream
import java.nio.charset.StandardCharsets

actual fun getMultiplatformFileFactory() : IMultiplatformFileFactory {
    val regex = Regex("^([a-zA-Z0-9]+)://([^/]+)(/.*)$")
    fun InputStream.readAsUri(): Uri? {
        val url = this.readAllBytes().toString(Charsets.UTF_8)
        val matchResult = regex.matchEntire(url)
        return matchResult?.let {
            val scheme = it.groups[1]?.value ?: return@let null
            val authority = it.groups[2]?.value ?: return@let null
            val path = it.groups[3]?.value ?: return@let null
            Uri(scheme = scheme, authority = authority, path = path)
        }
    }
    return object : IMultiplatformFileFactory {
        override fun fromFile(file: Path): MultiplatformFile {
            if (BuildKonfig.isRemote) {
                val uri = File(file.toString()).inputStream().readAsUri()
                if (uri?.scheme?.startsWith("jingtian") == true) {
                    RemoteUriUtils.parse(uri)?.let {
                        return it
                    }
                }
            }
            val realFile = SystemFileSystem.source(file).buffered().use { fis->
                val bytes = fis.readAllBytesOrNull()
                File(bytes?.toString(StandardCharsets.UTF_8) ?: throw RuntimeException("file not found $file, bytes=$bytes"))
            }
            return MultiplatformFileImpl(realFile, realFile.extension)
        }
        override fun fromFile(file: Path, extension: String?): MultiplatformFile {
            val realFile = SystemFileSystem.source(file).buffered().use { fis->
                val bytes = fis.readAllBytesOrNull()
                File(bytes?.toString(StandardCharsets.UTF_8) ?: throw RuntimeException("file not found $file, bytes=$bytes"))
            }
            return MultiplatformFileImpl(realFile, extension ?: file.extension)
        }

        override fun shareFile(file: Path): MultiplatformFileImpl {
            return MultiplatformFileImpl(File(file.toString()), file.extension)
        }
    }
}