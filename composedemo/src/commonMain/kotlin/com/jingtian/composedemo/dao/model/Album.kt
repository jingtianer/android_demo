package com.jingtian.composedemo.dao.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jingtian.composedemo.dao.AlbumDao
import com.jingtian.composedemo.dao.DataBase
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable

@Entity(
    tableName = AlbumDao.TABLE_NAME
)
@Serializable
class Album(
    @PrimaryKey(autoGenerate = true)
    var albumId: Long? = null,
    var createTime: Long = Clock.System.now().toEpochMilliseconds(),
    var albumName: String = "",
)
