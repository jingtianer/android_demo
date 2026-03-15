package com.jingtian.composedemo.main.remote

import android.R.id
import android.net.Uri
import com.jingtian.composedemo.utils.ensureDirExist
import com.jingtian.composedemo.utils.ensureFileExist
import com.jingtian.composedemo.utils.getFileCacheStorageRootDir
import java.io.File
import java.util.Objects


class RemoteFileStore(serverType: ServerType) {
    private val storageRoot = File(getFileCacheStorageRootDir(), "remote/filestore/server_${serverType.type}")
    init {
        storageRoot.ensureDirExist()
    }

    fun get(originUri: Uri): File {
        val fileName = originUri.lastPathSegment ?: ""
        val path = originUri.path ?: "/"
        return File(storageRoot, "${filePathHashCode(fileName, path)}_$fileName")
    }

    private fun filePathHashCode(fileName: String, path: String): Int {
        var result = 17
        result = 31 * result + Objects.hashCode(path)
        result = 31 * result + Objects.hashCode(fileName)
        return result
    }
}