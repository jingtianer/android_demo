package com.jingtian.composedemo.viewmodels

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.jingtian.composedemo.dao.DataBase
import com.jingtian.composedemo.dao.model.Album
import com.jingtian.composedemo.dao.model.relation.AlbumItemRelation
import com.jingtian.composedemo.utils.CoroutineUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class AlbumViewModel : ViewModel() {
    companion object {
        private const val TAG = "AlbumViewModel"
    }
    val albumDap = DataBase.dbImpl.getAlbumDao()
    val albumItemDap = DataBase.dbImpl.getAlbumItemDao()
    val menuItemsFlow: Flow<List<Album>>
        get() = albumDap.getAllAlbum()

    fun getAllAlbumItem(album: Album): Flow<List<AlbumItemRelation>> {
        return albumItemDap.getAllAlbumItemWithExtra(albumId = album.albumId ?: return flow {  })
    }

    val albumListChange: MutableLiveData<Int> = MutableLiveData(0)

    fun addAlbum(album: Album) {
        CoroutineUtils.runIOTask({
            albumDap.insertAlbum(album)
            albumDap.getAllAlbum().collect { Log.d(TAG, "addAlbum: ${it.map { "${it.albumId}, ${it.albumName}, ${it.createTime}" }.joinToString { "," }}") }
            Log.d(TAG, "addAlbum: ${album.albumName}")
        }) {
            albumListChange.value = (albumListChange.value ?: 0) + 1
        }
    }
    fun deleteAlbum(album: Album) {
        CoroutineUtils.runIOTask({
            albumDap.deleteAlbum(album)
            albumDap.getAllAlbum().collect { Log.d(TAG, "addAlbum: ${it.map { "${it.albumId}, ${it.albumName}, ${it.createTime}" }.joinToString { "," }}") }
            Log.d(TAG, "addAlbum: ${album.albumName}")
        }) {
            albumListChange.value = (albumListChange.value ?: 0) + 1
        }
    }
}