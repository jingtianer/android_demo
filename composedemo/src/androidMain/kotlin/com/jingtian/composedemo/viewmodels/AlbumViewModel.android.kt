package com.jingtian.composedemo.viewmodels

import androidx.documentfile.provider.DocumentFile
import com.jingtian.composedemo.base.app
import com.jingtian.composedemo.dao.DataBase
import com.jingtian.composedemo.dao.model.Album
import com.jingtian.composedemo.dao.model.AlbumItem
import com.jingtian.composedemo.dao.model.FileInfo
import com.jingtian.composedemo.multiplatform.MultiplatformFile
import com.jingtian.composedemo.multiplatform.MultiplatformFileImpl
import com.jingtian.composedemo.utils.FileStorageUtils
import com.jingtian.composedemo.utils.FileStorageUtils.getFileIntrinsicSize


actual suspend fun traverseUri(album: Album, uri: MultiplatformFile, fileInfoList: MutableList<Pair<FileInfo, AlbumItem>>, sendMessage: (String)->Unit) {
    val documentFile = DocumentFile.fromTreeUri(app, (uri as MultiplatformFileImpl).uri) ?: return
    realTraverseUri(documentFile, album, fileInfoList, sendMessage)
}


private suspend fun realTraverseUri(documentFile: DocumentFile, album: Album, fileInfoList: MutableList<Pair<FileInfo, AlbumItem>>, sendMessage: (String)->Unit) {
    if (documentFile.isDirectory) {
        sendMessage("导入目录: ${documentFile.name}")
        documentFile.listFiles().forEach {file->
            realTraverseUri(file, album, fileInfoList, sendMessage)
        }
    } else {
        val uri = MultiplatformFileImpl(documentFile.uri)
        if (uri.isHidden) {
            return
        }
        val type = uri.mediaType
        val fileName = uri.fileName ?: ""
        val (fileStorageId, _) = FileStorageUtils.getStorage(type).asyncStore(uri)
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
        sendMessage("正在导入: ${documentFile.name}")
        fileInfoList.add(fileInfo to albumItem)
    }
}