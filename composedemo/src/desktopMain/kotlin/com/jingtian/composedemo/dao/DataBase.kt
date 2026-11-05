package com.jingtian.composedemo.dao

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.jingtian.composedemo.utils.getDBStorageRootDir
import com.jingtian.composedemo.utils.globalWorkDir
import kotlinx.coroutines.Dispatchers
import java.io.File

actual fun dbImplCreator() : RoomDatabase.Builder<DataBase> {
    return Room.databaseBuilder<DataBase>(name = File(getDBStorageRootDir().toString(), "db.db").absolutePath)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
}
//
//actual object RoomConstructor: RoomDatabaseConstructor<DataBase> {
//    override fun initialize(): DataBase {
//        return DataBase.dbImpl
//    }
//}
