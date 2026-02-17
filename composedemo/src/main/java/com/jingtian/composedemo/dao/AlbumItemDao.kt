package com.jingtian.composedemo.dao

import androidx.room.Dao
import androidx.room.Query
import com.jingtian.composedemo.dao.model.AlbumItem

@Dao
interface AlbumItemDao {
    companion object {
        const val TABLE_NAME = "TB_ALBUM_ITEM"
    }

    @Query("select * from $TABLE_NAME where albumId = :albumId")
    fun getAllItemByAlbum(albumId: Long): List<AlbumItem>

}