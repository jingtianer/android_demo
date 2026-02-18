package com.jingtian.composedemo.dao

import androidx.room.Dao
import androidx.room.Insert
import com.jingtian.composedemo.dao.model.LabelInfo

@Dao
interface LabelInfoDao {
    companion object {
        const val TABLE_NAME = "TB_label"
    }

    @Insert
    fun insertAllLabel(labelList: List<LabelInfo>)
}