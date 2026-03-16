package com.jingtian.composedemo.utils.share

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.jingtian.composedemo.base.app
import com.jingtian.composedemo.dao.DataBase
import com.jingtian.composedemo.dao.converter.DateTypeConverter
import com.jingtian.composedemo.dao.converter.FileTypeConverter
import com.jingtian.composedemo.dao.converter.ItemRankConverter
import com.jingtian.composedemo.dao.model.AlbumItem
import com.jingtian.composedemo.dao.model.FileType
import com.jingtian.composedemo.dao.model.ItemRank
import com.jingtian.composedemo.dao.model.LabelInfo
import com.jingtian.composedemo.dao.model.relation.AlbumRelation
import com.jingtian.composedemo.utils.CoroutineUtils
import com.jingtian.composedemo.utils.ensureFile
import com.jingtian.composedemo.utils.getFileCacheStorageRootDir
import com.jingtian.composedemo.viewmodels.AlbumViewModel
import com.jingtian.composedemo.viewmodels.AlbumViewModel.Companion.notifyChange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.io.InputStreamReader
import java.util.Date
import kotlin.random.Random
import kotlin.random.nextULong

object ShareUtils {
    private val shareFileRootDir = File(app.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: getFileCacheStorageRootDir(), "share")
    suspend fun shareDataBase(context: Context) {
        val file = withContext(Dispatchers.IO) {
            getShareFile()
        }
        val uri = FileProvider.getUriForFile(
            app,
            app.packageName + ".fileprovider",
            file
        )
        val shareIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, uri)
            type = "*/*"
        }
        context.startActivity(Intent.createChooser(shareIntent, "导出数据"))
    }

    val gson = {
        GsonBuilder()
            .registerTypeAdapter(Date::class.java, DateTypeConverter())
            .registerTypeAdapter(FileType::class.java, FileTypeConverter())
            .registerTypeAdapter(ItemRank::class.java, ItemRankConverter())
            .create()
    }

    private suspend fun getShareFile(): File {
        return withContext(Dispatchers.IO) {
            val file = File(shareFileRootDir, "share_${Random.nextULong()}.json").ensureFile()
            val dataList = DataBase.dbImpl.getAlbumDao().getAllAlbumInfoWithExtra()
            FileWriter(file).use { fw ->
                gson().toJson(dataList, fw)
                fw.flush()
            }
            file
        }
    }

    suspend fun importSharedDb(albumViewModel: AlbumViewModel, uri: Uri) {
        withContext(Dispatchers.IO) {
            app.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val importedAlbumList = app.contentResolver.openInputStream(uri)?.use { `is` ->
                gson()
                    .fromJson<List<AlbumRelation>>(
                        JsonReader(InputStreamReader(`is`)),
                        TypeToken.getParameterized(List::class.java, AlbumRelation::class.java).type
                    ).associate { albumRelation ->
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