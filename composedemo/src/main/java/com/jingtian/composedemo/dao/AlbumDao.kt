package com.jingtian.composedemo.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jingtian.composedemo.dao.model.Album

@Dao
interface AlbumDao {
    companion object {
        const val TABLE_NAME = "TB_ALBUM"
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertAlbum(album: Album)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertAllAlbum(album: List<Album>)

    @Delete
    fun deleteAlbum(album: Album)

    @Query("select * from $TABLE_NAME")
    fun getAllAlbum(): List<Album>


    @Query("select * from $TABLE_NAME limit :maxSize")
    fun getAlbumList(maxSize: Int): List<Album>
}