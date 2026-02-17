package com.jingtian.composedemo.dao

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.jingtian.composedemo.base.app
import com.jingtian.composedemo.dao.converter.DateTypeConverter
import com.jingtian.composedemo.dao.converter.FileTypeConverter
import com.jingtian.composedemo.dao.converter.LabelTypeConverter
import com.jingtian.composedemo.dao.model.Album
import com.jingtian.composedemo.dao.model.AlbumItem
import com.jingtian.composedemo.dao.model.FileInfo
import com.jingtian.composedemo.dao.model.LabelInfo

@Database(
    entities = [
        Album::class,
        AlbumItem::class,
        FileInfo::class,
        LabelInfo::class,
    ],
    version = 1
)
abstract class DataBase: RoomDatabase() {
    companion object {
        const val INVALID_ID = -1L
        val dbImpl = Room.databaseBuilder(app, DataBase::class.java, "app_db")
            .addTypeConverter(DateTypeConverter())
            .addTypeConverter(FileTypeConverter())
            .addTypeConverter(LabelTypeConverter())
            .build()
    }
    abstract fun getAlbumDao(): AlbumDao
    abstract fun getAlbumItemDao(): AlbumItemDao
    abstract fun getFileInfoDao(): FileInfoDao
    abstract fun getLabelInfoDao(): LabelInfoDao
}