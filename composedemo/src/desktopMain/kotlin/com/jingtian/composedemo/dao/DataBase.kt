package com.jingtian.composedemo.dao

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import java.io.File

actual fun dbImplCreator() : RoomDatabase.Builder<DataBase> {
    return Room.databaseBuilder<DataBase>(name = File("./db.db").absolutePath)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
}