package com.jingtian.composedemo.viewmodels

import android.net.Uri
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.jingtian.composedemo.dao.DataBase
import com.jingtian.composedemo.dao.model.Album
import com.jingtian.composedemo.dao.model.AlbumItem
import com.jingtian.composedemo.dao.model.FileInfo
import com.jingtian.composedemo.dao.model.FileType
import com.jingtian.composedemo.dao.model.ItemRank
import com.jingtian.composedemo.dao.model.LabelInfo
import com.jingtian.composedemo.dao.model.relation.AlbumItemRelation
import com.jingtian.composedemo.utils.BitMapCachePool
import com.jingtian.composedemo.utils.CoroutineUtils
import com.jingtian.composedemo.utils.FileStorageUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class AlbumViewModel : ViewModel() {
    companion object {
        private const val TAG = "AlbumViewModel"
    }
    val albumDao = DataBase.dbImpl.getAlbumDao()
    val albumItemDao = DataBase.dbImpl.getAlbumItemDao()
    val menuItemsFlow: Flow<List<Album>>
        get() = albumDao.getAllAlbum()

    fun getLabelList(album: Album): Flow<List<String>> {
        return DataBase.dbImpl.getAlbumItemDao().getLabelList(album.albumId ?: return flow {  })
    }

    fun getAllAlbumItem(album: Album): Flow<List<AlbumItemRelation>> {
        return albumItemDao.getAllAlbumItemWithExtra(albumId = album.albumId ?: return flow {  })
    }

    val albumListChange: MutableLiveData<Int> = MutableLiveData(0)
    val albumItemListChange: MutableLiveData<Int> = MutableLiveData(0)

    fun addAlbum(album: Album) {
        CoroutineUtils.runIOTask({
            albumDao.insertAlbum(album)
            albumDao.getAllAlbum().collect { Log.d(TAG, "addAlbum: ${it.map { "${it.albumId}, ${it.albumName}, ${it.createTime}" }.joinToString { "," }}") }
            Log.d(TAG, "addAlbum: ${album.albumName}")
        }) {
            albumListChange.value = (albumListChange.value ?: 0) + 1
        }
    }
    fun deleteAlbum(album: Album) {
        CoroutineUtils.runIOTask({
            albumDao.deleteAlbum(album)
            albumDao.getAllAlbum().collect { Log.d(TAG, "addAlbum: ${it.map { "${it.albumId}, ${it.albumName}, ${it.createTime}" }.joinToString { "," }}") }
            Log.d(TAG, "addAlbum: ${album.albumName}")
        }) {
            albumListChange.value = (albumListChange.value ?: 0) + 1
        }
    }

    fun deleteItem(albumItemRelation: AlbumItemRelation) {
        val album = albumItemRelation.albumItem
        CoroutineUtils.runIOTask({
            DataBase.dbImpl.getAlbumItemDao().deleteAllAlbumItem(album)
            albumItemRelation.fileInfo?.let {
                DataBase.dbImpl.getFileInfoDao().deleteFileInfo(it)
                FileStorageUtils.getStorage(it.fileType)?.delete(it.storageId)
            }
            album.itemId?.let {
                DataBase.dbImpl.getLabelInfoDao().deleteAllLabel(it)
            }
        }) {
            albumItemListChange.value = 1 + (albumItemListChange.value ?: 0)
        }
    }

    fun addItem(
        album: Album,
        selectedUri: Uri?,
        itemName: String,
        itemRank: ItemRank,
        itemDesc: String,
        itemScore: Float,
        itemLabel: List<LabelInfo>,
    ) {
        val albumId = album.albumId ?: return
        val uri = selectedUri ?: return
        CoroutineUtils.runIOTask({
            val mediaType = FileStorageUtils.getMediaType(uri)
            val imageStorage =
                FileStorageUtils.getStorage(mediaType) ?: return@runIOTask
            val nextId = imageStorage.asyncStore(uri)
            val file = FileInfo(uri = uri, storageId = nextId, fileType = mediaType)
            val fileId = DataBase.dbImpl.getFileInfoDao().insertFileInfo(file)
            val albumItem = AlbumItem(
                itemName = itemName,
                rank = itemRank,
                desc = itemDesc,
                score = itemScore,
                albumId = albumId,
                fileId = fileId
            )
            val albumItemId = DataBase.dbImpl.getAlbumItemDao().insertAlbumItem(albumItem)
            itemLabel.forEach { it.albumItemId = albumItemId }
            DataBase.dbImpl.getLabelInfoDao().insertAllLabel(itemLabel)
        }) {
            albumItemListChange.value = 1 + (albumItemListChange.value ?: 0)
        }
    }

    fun updateItem(
        albumItemRelation: AlbumItemRelation,
        selectedUri: Uri?,
        selectedFileType: FileType?,
        itemName: String?,
        itemRank: ItemRank,
        itemDesc: String,
        itemScore: Float,
        itemLabel: List<LabelInfo>,
    ) {
        val album = albumItemRelation.albumItem
        val albumId = album.albumId
        val uri = selectedUri ?: return
        val mediaType = selectedFileType ?: return
        val itemName = itemName.takeIf { !it.isNullOrBlank() } ?: return
        val oldUri = albumItemRelation.fileInfo?.getFileUri()

        CoroutineUtils.runIOTask({
            val nextId = if (uri != oldUri) {
                val storage = FileStorageUtils.getStorage(mediaType)
                if (oldUri != null) {
                    val oldMediaType = albumItemRelation.fileInfo.fileType
                    if (oldMediaType == FileType.IMAGE) {
                        BitMapCachePool.invalid(albumItemRelation.fileInfo.storageId)
                    }
                    if (mediaType == oldMediaType) {
                        if (albumItemRelation.fileInfo.storageId != DataBase.INVALID_ID) {
                            storage?.asyncStore(albumItemRelation.fileInfo.storageId, uri)
                        } else {
                            storage?.asyncStore(uri)
                        }
                    } else {
                        val oldStorage = FileStorageUtils.getStorage(oldMediaType)
                        oldStorage?.delete(albumItemRelation.fileInfo.storageId)
                        storage?.asyncStore(uri)
                    }
                } else {
                    storage?.asyncStore(uri)
                }
            } else {
                albumItemRelation.fileInfo.storageId
            } ?: DataBase.INVALID_ID
            val file = FileInfo(id = albumItemRelation.fileInfo?.id, uri = uri, storageId = nextId, fileType = mediaType)
            DataBase.dbImpl.getFileInfoDao().updateFileInfo(file)
            val albumItemId = albumItemRelation.albumItem.itemId ?: DataBase.INVALID_ID
            val fileId = albumItemRelation.fileInfo?.id ?: DataBase.INVALID_ID
            val albumItem = AlbumItem(
                itemName = itemName,
                rank = itemRank,
                desc = itemDesc,
                score = itemScore,
                albumId = albumId,
                fileId = fileId,
                itemId = albumItemId,
            )
            DataBase.dbImpl.getAlbumItemDao().updateAlbumItem(albumItem)
            itemLabel.forEach { it.albumItemId = albumItemId }
            DataBase.dbImpl.getLabelInfoDao().deleteAllLabel(albumItemId)
            DataBase.dbImpl.getLabelInfoDao().insertAllLabel(itemLabel)
        }) {
            albumItemListChange.value = 1 + (albumItemListChange.value ?: 0)
        }
    }
}