package com.jingtian.composedemo.dao

import androidx.room.Room
import androidx.room.RoomDatabase
import com.jingtian.composedemo.base.app

actual fun dbImplCreator() : RoomDatabase.Builder<DataBase>  {
    return Room.databaseBuilder(app, DataBase::class.java, "app_db")
}