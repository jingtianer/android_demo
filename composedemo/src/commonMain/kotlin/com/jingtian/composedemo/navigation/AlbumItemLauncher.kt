package com.jingtian.composedemo.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import com.jingtian.composedemo.dao.model.FileInfo

interface IAlbumItemLauncher {
    fun launch(fileInfo: FileInfo)
}
@Composable
expect fun rememberAlbumLauncher(onResult: (Long)->Unit): MutableState<IAlbumItemLauncher>