package com.jingtian.composedemo.dao

import androidx.room.Dao

@Dao
interface LabelInfoDao {
    companion object {
        const val TABLE_NAME = "TB_label"
    }
}