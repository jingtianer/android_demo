package com.jingtian.composedemo.dao.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jingtian.composedemo.dao.AlbumDao
import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Entity(
    tableName = AlbumDao.TABLE_NAME
)
@Serializable
@OptIn(ExperimentalTime::class)
class Album(
    @PrimaryKey(autoGenerate = true)
    var albumId: Long? = null,
    var createTime: Long = Clock.System.now().toEpochMilliseconds(),
    var albumName: String = "",
)
