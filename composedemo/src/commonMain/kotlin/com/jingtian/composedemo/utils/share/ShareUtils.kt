package com.jingtian.composedemo.utils.share

import com.jingtian.composedemo.dao.DataBase
import com.jingtian.composedemo.dao.converter.FileTypeConverter
import com.jingtian.composedemo.dao.converter.ItemRankConverter
import com.jingtian.composedemo.dao.model.AlbumItem
import com.jingtian.composedemo.dao.model.FileType
import com.jingtian.composedemo.dao.model.ItemRank
import com.jingtian.composedemo.dao.model.LabelInfo
import com.jingtian.composedemo.dao.model.relation.AlbumRelation
import com.jingtian.composedemo.multiplatform.MultiplatformFile
import com.jingtian.composedemo.multiplatform.getMultiplatformFileFactory
import com.jingtian.composedemo.multiplatform.readAllBytesOrNull
import com.jingtian.composedemo.utils.createNewFile
import com.jingtian.composedemo.utils.delete
import com.jingtian.composedemo.utils.deleteRecursively
import com.jingtian.composedemo.utils.exists
import com.jingtian.composedemo.utils.getFileCacheStorageRootDir
import com.jingtian.composedemo.utils.isDirectory
import com.jingtian.composedemo.utils.isFile
import com.jingtian.composedemo.utils.mkdirs
import com.jingtian.composedemo.viewmodels.AlbumViewModel
import com.jingtian.composedemo.viewmodels.AlbumViewModel.Companion.notifyChange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlinx.io.writeString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.random.Random
import kotlin.random.nextULong

object ShareUtils {
    private val shareFileRootDir =  Path(Path(getFileCacheStorageRootDir()), "share")
    suspend fun shareDataBase(): MultiplatformFile {
        val file = withContext(Dispatchers.IO) {
            getShareFile()
        }
        return getMultiplatformFileFactory().shareFile(file)
    }

    private val json = Json { ignoreUnknownKeys = true }

    private suspend fun getShareFile(): Path {
        return withContext(Dispatchers.IO) {
            val file = Path(shareFileRootDir, "share_${Random.nextULong()}.json")
            if (SystemFileSystem.exists(file)) {
                if (file.isFile) {
                    file.delete()
                } else if (file.isDirectory) {
                    file.deleteRecursively()
                }
            }
            file.parent?.let { parent->
                if (parent.exists()) {
                    if (parent.isFile) {
                        parent.delete()
                        parent.mkdirs()
                    }
                } else {
                    parent.mkdirs()
                }
            }
            file.createNewFile()
            val dataList = DataBase.dbImpl.getAlbumDao().getAllAlbumInfoWithExtra()
            SystemFileSystem.sink(file).buffered().use {
                it.writeString(json.encodeToString(dataList))
                it.flush()
            }
            file
        }
    }

    suspend fun importSharedDb(albumViewModel: AlbumViewModel, file: MultiplatformFile) {
        withContext(Dispatchers.IO) {
            val importedAlbumList = file.inputStream?.buffered()?.readString()?.let { bytes ->
                json.decodeFromString<List<AlbumRelation>>(bytes).associate { albumRelation ->
                    val albumMap = albumRelation.albumItemList.withIndex().associate { (index, albumItem) ->
                        albumItem.albumItem.itemName to albumRelation.albumItemList[index]
                    }
                    albumRelation.albumItem.albumName to albumMap
                }
            } ?: return@withContext
            val albumList = DataBase.dbImpl.getAlbumDao().getAllAlbumInfoWithExtra()
            for (album in albumList) {
                val importedAlbumItemMap = importedAlbumList[album.albumItem.albumName] ?: continue
                for (albumRelation in album.albumItemList) {
                    val importedAlbumRelation = importedAlbumItemMap[albumRelation.albumItem.itemName] ?: continue
                    val importedAlbumItem = importedAlbumRelation.albumItem
                    val albumItem = albumRelation.albumItem
                    DataBase.dbImpl.getAlbumItemDao().updateAlbumItem(
                        AlbumItem(
                            itemId = albumItem.itemId,
                            createTime = albumItem.createTime,
                            itemName = albumItem.itemName,
                            rank = importedAlbumItem.rank,
                            desc = importedAlbumItem.desc,
                            score = importedAlbumItem.score,
                            albumId = albumItem.albumId,
                            fileId = albumItem.fileId,
                        )
                    )
                    DataBase.dbImpl.getLabelInfoDao().insertAllLabel(importedAlbumRelation.labelInfos.map {
                        LabelInfo(
                            albumItemId = albumItem.itemId ?: DataBase.INVALID_ID,
                            label = it.label
                        )
                    })
                }
            }
        }
        withContext(Dispatchers.Main) {
            albumViewModel.albumItemListChange.notifyChange()
        }
    }
}
