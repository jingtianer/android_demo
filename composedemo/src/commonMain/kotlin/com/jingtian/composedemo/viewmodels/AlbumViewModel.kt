package com.jingtian.composedemo.viewmodels

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jingtian.composedemo.dao.DataBase
import com.jingtian.composedemo.dao.model.Album
import com.jingtian.composedemo.dao.model.AlbumItem
import com.jingtian.composedemo.dao.model.FileInfo
import com.jingtian.composedemo.dao.model.FileType
import com.jingtian.composedemo.dao.model.ItemRank
import com.jingtian.composedemo.dao.model.LabelInfo
import com.jingtian.composedemo.dao.model.relation.AlbumItemRelation
import com.jingtian.composedemo.multiplatform.MultiplatformFile
import com.jingtian.composedemo.utils.BitMapCachePool
import com.jingtian.composedemo.utils.CoroutineUtils
import com.jingtian.composedemo.utils.FileStorageUtils
import com.jingtian.composedemo.utils.FileStorageUtils.getFileIntrinsicSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

expect suspend fun traverseUri(album: Album, uri: MultiplatformFile, fileInfoList: MutableList<Pair<FileInfo, AlbumItem>>, sendMessage: (String)->Unit)

class AlbumViewModel : ViewModel() {
    companion object {
        private const val TAG = "AlbumViewModel"

        fun MutableLongState.notifyChange() {
            this.value += 1
        }

        @Composable
        fun MutableLongState.observeAsState() = remember { this }

    }
    val albumDao = DataBase.dbImpl.getAlbumDao()
    val albumItemDao = DataBase.dbImpl.getAlbumItemDao()
    suspend fun menuItems(): List<Album> = albumDao.getAllAlbum()

    suspend fun getLabelList(album: Album): List<String> {
        return DataBase.dbImpl.getAlbumItemDao().getLabelList(album.albumId ?: return listOf())
    }

    suspend fun getAllAlbumItem(album: Album): List<AlbumItemRelation> {
        return albumItemDao.getAllAlbumItemWithExtra(albumId = album.albumId ?: return listOf())
    }

    val albumListChange = mutableLongStateOf(0L)
    val albumNameChange = mutableLongStateOf(0L)
    val albumItemListChange = mutableLongStateOf(0L)
    val filterCheckChanged = mutableLongStateOf(0L)

    suspend fun getAlbumName(albumId: Long): Album {
        return albumDao.getAlbum(albumId)
    }

    fun addAlbum(album: Album) {
        CoroutineUtils.runIOTask({
            albumDao.insertAlbum(album)
//            albumDao.getAllAlbum().collect { Log.d(TAG, "addAlbum: ${it.map { "${it.albumId}, ${it.albumName}, ${it.createTime}" }.joinToString { "," }}") }
//            Log.d(TAG, "addAlbum: ${album.albumName}")
        }) {
            albumListChange.notifyChange()
            sendMessage("添加合集: ${album.albumName} 成功!")
        }
    }

    fun editAlbum(album: Album) {
        CoroutineUtils.runIOTask({
            albumDao.updateAlbum(album)
//            albumDao.getAllAlbum().collect { Log.d(TAG, "addAlbum: ${it.map { "${it.albumId}, ${it.albumName}, ${it.createTime}" }.joinToString { "," }}") }
//            Log.d(TAG, "addAlbum: ${album.albumName}")
        }) {
            albumNameChange.notifyChange()
            sendMessage("添加合集: ${album.albumName} 成功!")
        }
    }

    private var currentMessageDelayJob: Job? = null
    val currentBackgroundTask = mutableStateOf<String?>(null)

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
                BitMapCachePool.invalid(it.second.storageId, it.second.fileType)
                sendMessage("正在删除文件: ${it.first}")
            }
            sendMessage("正在删除数据库记录")
            DataBase.dbImpl.getFileInfoDao().deleteAllFileInfo(files.map { it.second })
            val albumItemIds = itemInfoList.mapNotNull { it.albumItem.itemId }
            DataBase.dbImpl.getLabelInfoDao().deleteAllLabelOfAlbumItemIdList(albumItemIds)
            albumDao.deleteAlbum(album)
        }) {
            albumListChange.notifyChange()
            sendMessage("删除合集: ${album.albumName} 成功!")
        }
    }

    fun deleteItems(albumList: Collection<AlbumItemRelation>) {
        CoroutineUtils.runIOTask({
            for (albumItemRelation in albumList) {
                val album = albumItemRelation.albumItem
                DataBase.dbImpl.getAlbumItemDao().deleteAllAlbumItem(album)
                albumItemRelation.fileInfo.let {
                    DataBase.dbImpl.getFileInfoDao().deleteFileInfo(it)
                    FileStorageUtils.getStorage(it.fileType)?.delete(it.storageId)
                }
                album.itemId?.let {
                    DataBase.dbImpl.getLabelInfoDao().deleteAllLabel(it)
                }
            }
        }) {
            albumItemListChange.notifyChange()
            sendMessage("删除${albumList.size}个文件: 成功!")
        }
    }

    fun moveItems(moveTo: Album, albumList: Collection<AlbumItemRelation>) {
        CoroutineUtils.runIOTask({
            val moveList = albumList.map { it.albumItem.also { it.albumId = moveTo.albumId ?: it.albumId } }
            DataBase.dbImpl.getAlbumItemDao().updateAllAlbumItem(moveList)
        }) {
            albumItemListChange.notifyChange()
            sendMessage("移动${albumList.size}个文件: 成功!")
        }
    }

    fun addItem(
        album: Album,
        selectedUri: MultiplatformFile,
        itemName: String,
        itemRank: ItemRank,
        itemDesc: String,
        itemScore: Float,
        itemLabel: Set<String>,
        webSnapShot: ImageBitmap?
    ) {
        val albumId = album.albumId ?: return
        val uri = selectedUri
        CoroutineUtils.runIOTask({
            val mediaType = selectedUri.mediaType
            val imageStorage =
                FileStorageUtils.getStorage(mediaType) ?: return@runIOTask
            val nextId = imageStorage.asyncStore(uri)
            val (width, height) = getFileIntrinsicSize(uri, mediaType)
            val file = FileInfo(
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
            val labelInfo = itemLabel.map { LabelInfo(label = it, albumItemId = albumItemId) }
            DataBase.dbImpl.getLabelInfoDao().insertAllLabel(labelInfo)
            if (webSnapShot != null) {
                BitMapCachePool.loadImage(file) { webSnapShot }
            }
        }) {
            albumItemListChange.notifyChange()
            sendMessage("添加文件 ${album.albumName} 成功!")
        }
    }

    fun importFiles(album: Album, uri: MultiplatformFile) {
        CoroutineUtils.runIOTask({
            val fileInfoList: MutableList<Pair<FileInfo, AlbumItem>> = mutableListOf()
            traverseUri(album, uri, fileInfoList, ::sendMessage)
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
            sendMessage("批量导入 ${uri.fileName} 成功!")
        }
    }

    fun updateItem(
        albumItemRelation: AlbumItemRelation,
        selectedUri: MultiplatformFile,
        selectedFileType: FileType,
        itemName: String,
        itemRank: ItemRank,
        itemDesc: String,
        itemScore: Float,
        itemLabel: Set<String>,
        targetAlbumId: Long?,
        webBitmap: ImageBitmap?
    ) {
        val album = albumItemRelation.albumItem
        val albumId = targetAlbumId ?: album.albumId
        val uri = selectedUri
        val mediaType = selectedFileType
        val oldUri = albumItemRelation.fileInfo.getFileUri()

        CoroutineUtils.runIOTask({
            val nextId = if (uri != oldUri) {
                val storage = FileStorageUtils.getStorage(mediaType)
                if (oldUri != null) {
                    val oldMediaType = albumItemRelation.fileInfo.fileType
                    BitMapCachePool.invalid(albumItemRelation.fileInfo.storageId, oldMediaType)
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
            val file = FileInfo(id = albumItemRelation.fileInfo?.id, storageId = nextId, fileType = mediaType, intrinsicWidth = width, intrinsicHeight = height)
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
            DataBase.dbImpl.getLabelInfoDao().deleteAllLabel(albumItemId)
            DataBase.dbImpl.getLabelInfoDao().insertAllLabel(itemLabel.map { LabelInfo(albumItemId = albumItemId, label = it) })

            if (selectedFileType == FileType.HTML) {
                BitMapCachePool.loadImage(file) { webBitmap }
            }
        }) {
            albumItemListChange.notifyChange()
            sendMessage("更新文件 ${album.itemName} 成功!")
        }
    }
}