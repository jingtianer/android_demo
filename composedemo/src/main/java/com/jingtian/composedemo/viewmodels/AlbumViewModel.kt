package com.jingtian.composedemo.viewmodels

import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jingtian.composedemo.base.app
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
import com.jingtian.composedemo.utils.FileStorageUtils.getFileIntrinsicSize
import com.jingtian.composedemo.utils.FileStorageUtils.getFileNameFromUri
import com.jingtian.composedemo.utils.FileStorageUtils.getMediaType
import com.jingtian.composedemo.utils.FileStorageUtils.getVideoThumbnail
import com.jingtian.composedemo.utils.FileStorageUtils.getVideoThumbnailIntrinsicSize
import com.jingtian.composedemo.utils.FileStorageUtils.safeToFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class AlbumViewModel : ViewModel() {
    companion object {
        private const val TAG = "AlbumViewModel"
    }
    val albumDao = DataBase.dbImpl.getAlbumDao()
    val albumItemDao = DataBase.dbImpl.getAlbumItemDao()
    val menuItemsFlow: Flow<List<Album>>
        get() = albumDao.getAllAlbum()

    val editDialogAlbum = MutableLiveData<Album?>(null)
    val editDialogAlbumItem = MutableLiveData<AlbumItemRelation?>(null)

    fun getLabelList(album: Album): Flow<List<String>> {
        return DataBase.dbImpl.getAlbumItemDao().getLabelList(album.albumId ?: return flow {  })
    }

    fun getAllAlbumItem(album: Album): Flow<List<AlbumItemRelation>> {
        return albumItemDao.getAllAlbumItemWithExtra(albumId = album.albumId ?: return flow {  })
    }

    val albumListChange: MutableLiveData<Int> = MutableLiveData(0)
    val albumNameChange: MutableLiveData<Int> = MutableLiveData(0)
    val albumItemListChange: MutableLiveData<Int> = MutableLiveData(0)

    fun MutableLiveData<Int>.notifyChange() {
        this.value = (this.value ?: 0) + 1
    }

    fun getAlbumName(albumId: Long): Album {
        return albumDao.getAlbum(albumId)
    }

    fun addAlbum(album: Album) {
        CoroutineUtils.runIOTask({
            albumDao.insertAlbum(album)
//            albumDao.getAllAlbum().collect { Log.d(TAG, "addAlbum: ${it.map { "${it.albumId}, ${it.albumName}, ${it.createTime}" }.joinToString { "," }}") }
//            Log.d(TAG, "addAlbum: ${album.albumName}")
        }) {
            albumListChange.notifyChange()
            sendMessage("添加相册: ${album.albumName} 成功!")
        }
    }

    fun editAlbum(album: Album) {
        CoroutineUtils.runIOTask({
            albumDao.updateAlbum(album)
//            albumDao.getAllAlbum().collect { Log.d(TAG, "addAlbum: ${it.map { "${it.albumId}, ${it.albumName}, ${it.createTime}" }.joinToString { "," }}") }
//            Log.d(TAG, "addAlbum: ${album.albumName}")
        }) {
            albumNameChange.notifyChange()
            sendMessage("添加相册: ${album.albumName} 成功!")
        }
    }

    private var currentMessageDelayJob: Job? = null
    val currentBackgroundTask = MutableLiveData<String?>(null)

    fun sendMessage(message: String) {
        currentMessageDelayJob?.cancel()
        currentMessageDelayJob = viewModelScope.launch(Dispatchers.Main) {
            currentBackgroundTask.value = message
            withContext(Dispatchers.IO) {
                delay(2000)
            }
            currentBackgroundTask.value = null
            currentMessageDelayJob = null
        }
    }

    fun deleteAlbum(album: Album) {
        val albumId = album.albumId ?: return
        CoroutineUtils.runIOTask({
            val itemInfoList = albumItemDao.getAllAlbumItemListWithExtra(albumId)
            val files = itemInfoList.mapNotNull { it.albumItem.itemName to (it.fileInfo ?: return@mapNotNull null) }
            files.forEach {
                FileStorageUtils.getStorage(it.second.fileType)?.delete(it.second.storageId)
                sendMessage("正在删除文件: ${it.first}")
            }
            sendMessage("正在删除数据库记录")
            DataBase.dbImpl.getFileInfoDao().deleteAllFileInfo(files.map { it.second })
            val albumItemIds = itemInfoList.mapNotNull { it.albumItem.itemId }
            DataBase.dbImpl.getLabelInfoDao().deleteAllLabelOfAlbumItemIdList(albumItemIds)
            albumDao.deleteAlbum(album)
        }) {
            albumListChange.notifyChange()
            sendMessage("删除相册: ${album.albumName} 成功!")
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
            albumItemListChange.notifyChange()
            sendMessage("删除文件: ${album.itemName} 成功!")
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
            val (width, height) = getFileIntrinsicSize(uri, mediaType)
            val file = FileInfo(
                uri = uri,
                storageId = nextId,
                fileType = mediaType,
                intrinsicWidth = width,
                intrinsicHeight = height,
            )
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
            albumItemListChange.notifyChange()
            sendMessage("添加文件 ${album.albumName} 成功!")
        }
    }

    fun importFiles(album: Album, uri: Uri) {
        val documentFile = DocumentFile.fromTreeUri(app, uri)
        CoroutineUtils.runIOTask({
            documentFile ?: return@runIOTask
            val fileInfoList: MutableList<Pair<FileInfo, AlbumItem>> = mutableListOf()
            traverseUri(documentFile, album, fileInfoList)
            sendMessage("批量导入: 正在写入数据库")
            val idList = DataBase.dbImpl.getFileInfoDao().insertAllFileInfo(fileInfoList.map { it.first })
            val albumItemList = fileInfoList.mapIndexed { index: Int, pair: Pair<FileInfo, AlbumItem> ->
                pair.second.also {
                    it.fileId = idList[index]
                }
            }
            DataBase.dbImpl.getAlbumItemDao().insertAllAlbumItem(albumItemList)
        }) {
            albumItemListChange.notifyChange()
            sendMessage("批量导入 ${documentFile?.name} 成功!")
        }
    }

    private suspend fun traverseUri(documentFile: DocumentFile, album: Album, fileInfoList: MutableList<Pair<FileInfo, AlbumItem>>) {
        if (documentFile.isDirectory) {
            sendMessage("导入目录: ${documentFile.name}")
            documentFile.listFiles().forEach {file->
                traverseUri(file, album, fileInfoList)
            }
        } else {
            val uri = documentFile.uri
            val type = getMediaType(uri)
            val fileName = getFileNameFromUri(uri) ?: ""
            val fileStorageId = FileStorageUtils.getStorage(type)?.asyncStore(uri) ?: DataBase.INVALID_ID
            val (width, height) = getFileIntrinsicSize(uri, type)
            val fileInfo = FileInfo(uri = uri, storageId = fileStorageId, fileType = type, intrinsicWidth = width, intrinsicHeight = height)
            val albumItem = AlbumItem(itemName = fileName, albumId = album.albumId ?: DataBase.INVALID_ID)
            sendMessage("正在导入: ${documentFile.name}")
            fileInfoList.add(fileInfo to albumItem)
        }
    }

    fun updateItem(
        albumItemRelation: AlbumItemRelation,
        selectedUri: Uri,
        selectedFileType: FileType,
        itemName: String,
        itemRank: ItemRank,
        itemDesc: String,
        itemScore: Float,
        itemLabel: List<LabelInfo>,
        targetAlbumId: Long?,
    ) {
        val album = albumItemRelation.albumItem
        val albumId = targetAlbumId ?: album.albumId
        val uri = selectedUri
        val mediaType = selectedFileType
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
            val (width, height) = getFileIntrinsicSize(uri, mediaType)
            val file = FileInfo(id = albumItemRelation.fileInfo?.id, uri = uri, storageId = nextId, fileType = mediaType, intrinsicWidth = width, intrinsicHeight = height)
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
            albumItemListChange.notifyChange()
            sendMessage("更新文件 ${album.itemName} 成功!")
        }
    }
}