package com.jingtian.composedemo.dao

import androidx.room.Dao

@Dao
interface FileInfoDao {
    companion object {
        const val TABLE_NAME = "TB_FILE"
    }
}