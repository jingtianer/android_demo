package com.jingtian.composedemo.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.jingtian.composedemo.dao.model.Album
import com.jingtian.composedemo.dao.model.relation.AlbumRelation

@Dao
interface AlbumDao {
    companion object {
        const val TABLE_NAME = "TB_ALBUM"
    }

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAlbum(album: Album)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllAlbum(album: List<Album>)

    @Delete
    suspend fun deleteAlbum(album: Album)

    @Query("delete from $TABLE_NAME")
    suspend fun deleteAllAlbum()

    @Query("select * from $TABLE_NAME")
    suspend fun getAllAlbum(): List<Album>

    @Query("select * from $TABLE_NAME where albumId = :albumId")
    suspend fun getAlbum(albumId: Long): Album

    @Update
    suspend fun updateAlbum(album: Album)

    @Query("select * from $TABLE_NAME limit :maxSize")
    suspend fun getAlbumList(maxSize: Int): List<Album>

    @Query("select count(*) from $TABLE_NAME")
    suspend fun getSize(): Int

    @Transaction
    @Query("select * from $TABLE_NAME")
    fun getAllAlbumInfoWithExtra(): List<AlbumRelation>
}