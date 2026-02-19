package com.jingtian.composedemo.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.jingtian.composedemo.dao.model.AlbumItem
import com.jingtian.composedemo.dao.model.LabelInfo
import com.jingtian.composedemo.dao.model.relation.AlbumItemRelation
import kotlinx.coroutines.flow.Flow

@Dao
interface AlbumItemDao {
    companion object {
        const val TABLE_NAME = "TB_ALBUM_ITEM"
    }

    @Query("select * from $TABLE_NAME where albumId = :albumId")
    fun getAllAlbumItem(albumId: Long): List<AlbumItem>

    @Delete
    fun deleteAllAlbumItem(albumItem: AlbumItem)


    @Insert
    fun insertAlbumItem(albumItem: AlbumItem): Long

    @Update
    fun updateAlbumItem(albumItem: AlbumItem)

    @Transaction
    @Query("select * from $TABLE_NAME where albumId = :albumId order by score desc")
    fun getAllAlbumItemWithExtra(albumId: Long): Flow<List<AlbumItemRelation>>

    @Query("select distinct(label) from ${LabelInfoDao.TABLE_NAME} where albumItemId in (select albumItemId from $TABLE_NAME where albumId = :albumId)")
    fun getLabelList(albumId: Long): Flow<List<String>>
}