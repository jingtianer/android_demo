package com.jingtian.composedemo.dao.model

import androidx.annotation.FloatRange
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.jingtian.composedemo.dao.AlbumItemDao
import com.jingtian.composedemo.dao.DataBase
import org.jetbrains.annotations.Range
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
    indices = [Index(value = ["itemId", "albumId", "fileId"])],
    primaryKeys = ["itemId"]
)
class AlbumItem(
    var itemId: Long? = null,
    var createTime: Date = Date(),
    var itemName: String = "",
    var rank: ItemRank = ItemRank.NONE,
    @FloatRange(from = 0.0, to = 5.0, fromInclusive = true, toInclusive = true)
    var score: Float = 0f,

    var albumId: Long = DataBase.INVALID_ID,

    var fileId: Long = DataBase.INVALID_ID,
)