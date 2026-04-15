package com.jingtian.composedemo.dao

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.jingtian.composedemo.utils.globalWorkDir
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.io.files.Path

actual fun dbImplCreator() : RoomDatabase.Builder<DataBase> {
    return Room.databaseBuilder<DataBase>(name = Path(globalWorkDir, "db.db").toString())
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
}

//actual object RoomConstructor: RoomDatabaseConstructor<DataBase> {
//    override fun initialize(): DataBase {
//        return DataBase.dbImpl
//    }
//}
