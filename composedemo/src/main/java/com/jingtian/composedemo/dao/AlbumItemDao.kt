package com.jingtian.composedemo.dao

import androidx.room.Dao
import androidx.room.Delete
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
    fun getAllAlbumItem(albumId: Long): List<AlbumItem>

    @Delete
    fun deleteAllAlbumItem(albumItem: AlbumItem)

    @Update
    fun updateAlbumItem(albumItem: AlbumItem)

    @Transaction
    @Query("select * from $TABLE_NAME where albumId = :albumId")
    fun getAllAlbumItemWithExtra(albumId: Long): List<AlbumItemRelation>
}