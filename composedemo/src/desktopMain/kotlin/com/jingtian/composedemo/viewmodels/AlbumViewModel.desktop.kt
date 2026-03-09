package com.jingtian.composedemo.viewmodels

import com.jingtian.composedemo.dao.model.Album
import com.jingtian.composedemo.dao.model.AlbumItem
import com.jingtian.composedemo.dao.model.FileInfo
import com.jingtian.composedemo.multiplatform.MultiplatformFile

actual suspend fun traverseUri(
    album: Album,
    uri: MultiplatformFile,
    fileInfoList: MutableList<Pair<FileInfo, AlbumItem>>,
    sendMessage: (String) -> Unit
) {
}