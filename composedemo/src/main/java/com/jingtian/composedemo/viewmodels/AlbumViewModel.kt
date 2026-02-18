package com.jingtian.composedemo.viewmodels

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.jingtian.composedemo.dao.DataBase
import com.jingtian.composedemo.dao.model.Album
import com.jingtian.composedemo.utils.CoroutineUtils
import kotlinx.coroutines.flow.Flow

class AlbumViewModel : ViewModel() {
    companion object {
        private const val TAG = "AlbumViewModel"
    }
    val dao = DataBase.dbImpl.getAlbumDao()
    val menuItemsFlow: Flow<List<Album>>
        get() = dao.getAllAlbum()

    val dataChange: MutableLiveData<Int> = MutableLiveData(0)

    fun addAlbum(album: Album) {
        CoroutineUtils.runIOTask({
            dao.insertAlbum(album)
            dao.getAllAlbum().collect { Log.d(TAG, "addAlbum: ${it.map { "${it.albumId}, ${it.albumName}, ${it.createTime}" }.joinToString { "," }}") }
            Log.d(TAG, "addAlbum: ${album.albumName}")
        }) {
            dataChange.value = (dataChange.value ?: 0) + 1
        }
    }
    fun deleteAlbum(album: Album) {
        CoroutineUtils.runIOTask({
            dao.deleteAlbum(album)
            dao.getAllAlbum().collect { Log.d(TAG, "addAlbum: ${it.map { "${it.albumId}, ${it.albumName}, ${it.createTime}" }.joinToString { "," }}") }
            Log.d(TAG, "addAlbum: ${album.albumName}")
        }) {
            dataChange.value = (dataChange.value ?: 0) + 1
        }
    }
}