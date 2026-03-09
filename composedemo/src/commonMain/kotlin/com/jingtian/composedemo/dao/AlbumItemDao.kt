package com.jingtian.composedemo.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.jingtian.composedemo.dao.model.AlbumItem
import com.jingtian.composedemo.dao.model.relation.AlbumItemRelation

@Dao
interface AlbumItemDao {
    companion object {
        const val TABLE_NAME = "TB_ALBUM_ITEM"
    }

    @Query("select * from $TABLE_NAME where albumId = :albumId")
    suspend fun getAllAlbumItem(albumId: Long): List<AlbumItem>

    @Delete
    suspend fun deleteAllAlbumItem(albumItem: AlbumItem)


    @Insert
    suspend fun insertAlbumItem(albumItem: AlbumItem): Long

    @Insert
    suspend fun insertAllAlbumItem(albumItem: List<AlbumItem>)

    @Update
    suspend fun updateAlbumItem(albumItem: AlbumItem)


    @Update
    suspend fun updateAllAlbumItem(albumItem: List<AlbumItem>)

    @Transaction
    @Query("select * from $TABLE_NAME where albumId = :albumId order by score desc")
    suspend fun getAllAlbumItemWithExtra(albumId: Long): List<AlbumItemRelation>


    @Transaction
    @Query("select * from $TABLE_NAME where albumId = :albumId order by score desc")
    suspend fun getAllAlbumItemListWithExtra(albumId: Long): List<AlbumItemRelation>

    @Query("select distinct `label` from ${LabelInfoDao.TABLE_NAME} A left join ${AlbumItemDao.TABLE_NAME} B on A.albumItemId = B.itemId where albumId = :albumId")
    suspend fun getLabelList(albumId: Long): List<String>
}