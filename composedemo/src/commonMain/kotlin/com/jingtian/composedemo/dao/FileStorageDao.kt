package com.jingtian.composedemo.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.jingtian.composedemo.dao.model.FileInfo
import com.jingtian.composedemo.dao.model.FileStorageInfo
import com.jingtian.composedemo.dao.model.FileType

@Dao
interface FileStorageDao {
    companion object {
        const val TABLE_NAME = "fileStorageTable"
    }

    @Query("select * from $TABLE_NAME where id = :id")
    fun queryFileStorageInfo(id: Long): FileStorageInfo?

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateFileStorageInfo(fileStorageInfo: FileStorageInfo)

    @Insert
    fun insertFileStoreInfo(fileStorageInfo: FileStorageInfo): Long

    @Query("delete from $TABLE_NAME where id = :id")
    fun deleteFileStoreInfo(id: Long)
}