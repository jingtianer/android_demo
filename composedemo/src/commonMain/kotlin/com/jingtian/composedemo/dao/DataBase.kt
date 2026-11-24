package com.jingtian.composedemo.dao

import androidx.room.Database
import androidx.room.ConstructedBy
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters
import com.jingtian.composedemo.dao.converter.FileTypeConverter
import com.jingtian.composedemo.dao.converter.ItemRankConverter
import com.jingtian.composedemo.dao.model.Album
import com.jingtian.composedemo.dao.model.AlbumItem
import com.jingtian.composedemo.dao.model.FileInfo
import com.jingtian.composedemo.dao.model.LabelInfo
import com.jingtian.composedemo.main.http.dao.HttpServerDao
import com.jingtian.composedemo.main.http.dao.HttpServerEntity

@Database(
    entities = [
        Album::class,
        AlbumItem::class,
        FileInfo::class,
        LabelInfo::class,
        HttpServerEntity::class,
    ],
    version = 2
)
@ConstructedBy(RoomConstructor::class)
@TypeConverters(/* DateTypeConverter::class, */FileTypeConverter::class, ItemRankConverter::class)
abstract class DataBase: RoomDatabase() {
    companion object {
        const val INVALID_ID = -1L
        val dbImpl: DataBase by lazy {
            dbImplCreator()
//            .addTypeConverter(DateTypeConverter())
            .addTypeConverter(FileTypeConverter())
            .addTypeConverter(ItemRankConverter())
            .build()
        }

    }
    abstract fun getAlbumDao(): AlbumDao
    abstract fun getAlbumItemDao(): AlbumItemDao
    abstract fun getFileInfoDao(): FileInfoDao
    abstract fun getLabelInfoDao(): LabelInfoDao
    abstract fun getHttpServerDao(): HttpServerDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object RoomConstructor: RoomDatabaseConstructor<DataBase>

expect fun dbImplCreator() : RoomDatabase.Builder<DataBase>
