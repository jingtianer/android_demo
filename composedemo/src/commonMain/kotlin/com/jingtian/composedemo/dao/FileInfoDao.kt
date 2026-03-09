package com.jingtian.composedemo.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.jingtian.composedemo.dao.model.FileInfo

@Dao
interface FileInfoDao {
    companion object {
        const val TABLE_NAME = "TB_FILE"
    }

    @Query("select * from $TABLE_NAME where id = :fileId")
    suspend fun getFileInfo(fileId: Long): FileInfo

    @Delete
    suspend fun deleteFileInfo(fileInfo: FileInfo)

    @Delete
    suspend fun deleteAllFileInfo(fileIdList: List<FileInfo>)

    @Update
    suspend fun updateFileInfo(fileInfo: FileInfo)

    @Insert
    suspend fun insertFileInfo(fileInfo: FileInfo): Long

    @Insert
    suspend fun insertAllFileInfo(fileInfo: List<FileInfo>): List<Long>
}