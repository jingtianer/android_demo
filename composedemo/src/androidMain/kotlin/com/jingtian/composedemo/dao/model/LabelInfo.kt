package com.jingtian.composedemo.dao.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.jingtian.composedemo.dao.DataBase
import com.jingtian.composedemo.dao.LabelInfoDao

@Entity(
    tableName = LabelInfoDao.TABLE_NAME,
    primaryKeys = ["albumItemId", "label"],
    foreignKeys = [
        ForeignKey(
            entity = AlbumItem::class,
            childColumns = ["albumItemId"],
            parentColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["albumItemId", "label"])]
)

class LabelInfo(
    var albumItemId: Long = DataBase.INVALID_ID,
    var label: String = ""
)