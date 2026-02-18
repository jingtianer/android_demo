package com.jingtian.composedemo.dao.model

import androidx.room.Entity
import androidx.room.ForeignKey
import com.jingtian.composedemo.dao.AlbumItemDao
import com.jingtian.composedemo.dao.DataBase
import java.util.Date

@Entity(
    tableName = AlbumItemDao.TABLE_NAME,
    foreignKeys = [
        ForeignKey(
            entity = FileInfo::class,
            childColumns = ["fileId"],
            parentColumns = ["id"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Album::class,
            childColumns = ["albumId"],
            parentColumns = ["albumId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
    ],
    primaryKeys = ["itemId"]
)
class AlbumItem(
    var itemId: Long = DataBase.INVALID_ID,
    var createTime: Date = Date(),
    var itemName: String = "",

    var albumId: Long = DataBase.INVALID_ID,

    var fileId: Long = DataBase.INVALID_ID,
)