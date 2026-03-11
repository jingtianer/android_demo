package com.jingtian.composedemo.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import com.jingtian.composedemo.dao.model.FileInfo
import com.jingtian.composedemo.dao.model.relation.AlbumItemRelation

interface IAlbumItemLauncher {
    fun launch(fileName: String, fileInfo: FileInfo)
    fun launch(albumItemRelation: AlbumItemRelation)
}
@Composable
expect fun rememberAlbumLauncher(onResult: (Long)->Unit): MutableState<IAlbumItemLauncher>