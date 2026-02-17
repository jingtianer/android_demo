package com.jingtian.composedemo.dao.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jingtian.composedemo.dao.AlbumDao
import com.jingtian.composedemo.dao.DataBase
import java.util.Date

@Entity(
    tableName = AlbumDao.TABLE_NAME
)
class Album(
    @PrimaryKey(autoGenerate = true)
    var albumId: Long = DataBase.INVALID_ID,
    var createTime: Date = Date(),
    var albumName: String = "",
)