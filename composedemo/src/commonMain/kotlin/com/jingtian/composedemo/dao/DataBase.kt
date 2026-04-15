package com.jingtian.composedemo.dao

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters
import com.jingtian.composedemo.dao.converter.FileTypeConverter
import com.jingtian.composedemo.dao.converter.ItemRankConverter
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
@TypeConverters(/* DateTypeConverter::class, */FileTypeConverter::class, ItemRankConverter::class)
@ConstructedBy(RoomConstructor::class)
abstract class DataBase: RoomDatabase() {
    companion object {
        const val INVALID_ID = -1L
        val dbImpl = dbImplCreator()
//            .addTypeConverter(DateTypeConverter())
            .addTypeConverter(FileTypeConverter())
            .addTypeConverter(ItemRankConverter())
            .build()

    }
    abstract fun getAlbumDao(): AlbumDao
    abstract fun getAlbumItemDao(): AlbumItemDao
    abstract fun getFileInfoDao(): FileInfoDao
    abstract fun getLabelInfoDao(): LabelInfoDao
}

expect object RoomConstructor: RoomDatabaseConstructor<DataBase>

expect fun dbImplCreator() : RoomDatabase.Builder<DataBase>