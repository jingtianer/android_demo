package com.jingtian.composedemo.dao.model

import androidx.room.Entity
import androidx.room.ForeignKey
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
    ]
)

class LabelInfo(
    var albumItemId: Long = DataBase.INVALID_ID,
    var label: Label = Label.DEFAULT
)

enum class Label(val value: Int) {
    DEFAULT(0)
    ;
    companion object {
        fun fromValue(value: Int): Label? {
            return when(value) {
                0 -> DEFAULT
                else -> null
            }
        }
    }
}