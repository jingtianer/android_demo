package com.jingtian.composedemo.viewmodels

import androidx.lifecycle.ViewModel
import com.jingtian.composedemo.dao.DataBase
import com.jingtian.composedemo.dao.model.Album
import kotlinx.coroutines.flow.Flow

class AlbumViewModel : ViewModel() {
    val menuItemsFlow: Flow<List<Album>> = DataBase.dbImpl.getAlbumDao().getAllAlbum()
}