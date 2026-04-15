package com.jingtian.composedemo.viewmodels

import com.jingtian.composedemo.dao.DataBase
import com.jingtian.composedemo.dao.model.Album
import com.jingtian.composedemo.dao.model.AlbumItem
import com.jingtian.composedemo.dao.model.FileInfo
import com.jingtian.composedemo.multiplatform.MultiplatformFile
import com.jingtian.composedemo.multiplatform.MultiplatformFileImpl
import com.jingtian.composedemo.utils.FileStorageUtils
import com.jingtian.composedemo.utils.FileStorageUtils.getFileIntrinsicSize
import com.jingtian.composedemo.utils.extension
import com.jingtian.composedemo.utils.isDirectory
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

actual suspend fun traverseUri(
    album: Album,
    uri: MultiplatformFile,
    fileInfoList: MutableList<Pair<FileInfo, AlbumItem>>,
    sendMessage: (String) -> Unit
) {
    realTraverseUri(album, uri.file ?: return, fileInfoList, sendMessage)
}

private fun realTraverseUri(
    album: Album,
    file: Path,
    fileInfoList: MutableList<Pair<FileInfo, AlbumItem>>,
    sendMessage: (String) -> Unit
) {
    if (file.isDirectory) {
        sendMessage("导入目录: ${file.name}")
        SystemFileSystem.list(file).forEach { file->
            realTraverseUri(album, file, fileInfoList, sendMessage)
        }
    } else {
        val uri = MultiplatformFileImpl(file, file.extension)
        if (uri.isHidden) {
            return
        }
        val type = uri.mediaType
        val fileName = uri.fileName ?: ""
        val fileStorageId = FileStorageUtils.getStorage(type)?.asyncStore(uri) ?: DataBase.INVALID_ID
        val (width, height) = getFileIntrinsicSize(uri, type)
        val fileInfo = FileInfo(
            storageId = fileStorageId,
            fileType = type,
            filePath = uri.path,
            intrinsicWidth = width,
            intrinsicHeight = height,
            extension = uri.extension
        )
        val albumItem = AlbumItem(itemName = fileName, albumId = album.albumId ?: DataBase.INVALID_ID)
        sendMessage("正在导入: ${file.name}")
        fileInfoList.add(fileInfo to albumItem)
    }
}