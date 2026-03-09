package com.jingtian.composedemo.dao

import androidx.room.RoomDatabase

actual fun dbImplCreator() : DataBase {
    throw NotImplementedError()
}