package com.jingtian.composedemo.dao.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.jingtian.composedemo.dao.DataBase
import com.jingtian.composedemo.dao.FileInfoDao
import com.jingtian.composedemo.dao.FileStorageDao
import kotlinx.serialization.Serializable

@Entity(
    tableName = FileStorageDao.TABLE_NAME,
)
@Serializable
class FileStorageInfo(
    @PrimaryKey(autoGenerate = true)
    var id: Long? = null,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    var content: ByteArray? = null
)