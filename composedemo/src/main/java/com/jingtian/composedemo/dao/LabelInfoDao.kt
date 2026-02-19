package com.jingtian.composedemo.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.jingtian.composedemo.dao.model.LabelInfo

@Dao
interface LabelInfoDao {
    companion object {
        const val TABLE_NAME = "TB_label"
    }

    @Insert
    fun insertAllLabel(labelList: List<LabelInfo>)

    @Query("delete from $TABLE_NAME where albumItemId = :albumItemId")
    fun deleteAllLabel(albumItemId: Long)
}