package com.jingtian.composedemo.utils

import android.net.Uri
import android.os.Build
import androidx.core.net.toUri
import com.jingtian.composedemo.dao.model.FileInfo
import com.jingtian.composedemo.multiplatform.MultiplatformFileImpl
import java.io.File
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.random.Random
import kotlin.random.nextUInt
import kotlin.random.nextULong

object FileLinkProvider {
//    private val fileLinkDir = File(getFileCacheStorageRootDir(), "tmplink")
    fun init() {
//        if (fileLinkDir.exists()) {
//            if (fileLinkDir.isDirectory) {
//                fileLinkDir.deleteRecursively()
//            } else if (fileLinkDir.isFile) {
//                fileLinkDir.delete()
//            }
//        }
//        fileLinkDir.mkdirs()
    }



    // rom 限制硬链接 不玩了
    fun get(fileName: String, fileInfo: FileInfo, defaultExtension: String = ""): Uri? {
        return null
//        val originalFile = (fileInfo.getFileUri() as? MultiplatformFileImpl)?.uri ?: return null
//        val tmpLinkFile = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            Files.createLink(
//                Path(fileLinkDir.path, "${fileName}_tmplink_${Random.nextULong()}.${fileInfo.extension ?: defaultExtension}"),
//                Path(originalFile.path ?: return null)
//            ).toFile()
//        } else {
//            return originalFile
//        }
//        return tmpLinkFile?.let {
//            it.deleteOnExit()
//            it.toUri()
//        } ?: originalFile
    }
}