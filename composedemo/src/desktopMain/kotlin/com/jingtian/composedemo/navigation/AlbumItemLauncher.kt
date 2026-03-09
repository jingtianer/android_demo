package com.jingtian.composedemo.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.jingtian.composedemo.dao.model.FileInfo

class AlbumItemLauncher : IAlbumItemLauncher {
    override fun launch(fileInfo: FileInfo) {

    }
}

@Composable
actual fun rememberAlbumLauncher(onResult: (Long)->Unit): MutableState<IAlbumItemLauncher> {
    return remember {
        mutableStateOf(AlbumItemLauncher())
    }
}