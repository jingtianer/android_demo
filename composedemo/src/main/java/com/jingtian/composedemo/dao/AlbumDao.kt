package com.jingtian.composedemo.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.jingtian.composedemo.dao.model.Album
import kotlinx.coroutines.flow.Flow

@Dao
interface AlbumDao {
    companion object {
        const val TABLE_NAME = "TB_ALBUM"
    }

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insertAlbum(album: Album)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertAllAlbum(album: List<Album>)

    @Delete
    fun deleteAlbum(album: Album)


    @Query("delete from $TABLE_NAME")
    fun deleteAllAlbum()

    @Query("select * from $TABLE_NAME")
    fun getAllAlbum(): Flow<List<Album>>

    @Query("select * from $TABLE_NAME where albumId = :albumId")
    fun getAlbum(albumId: Long): Album

    @Update
    fun updateAlbum(album: Album)

    @Query("select * from $TABLE_NAME limit :maxSize")
    fun getAlbumList(maxSize: Int): List<Album>

    @Query("select count(*) from $TABLE_NAME")
    fun getSize(): Int
}