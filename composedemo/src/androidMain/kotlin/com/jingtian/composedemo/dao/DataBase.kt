package com.jingtian.composedemo.dao

import androidx.room.Room
import com.jingtian.composedemo.base.app
import com.jingtian.composedemo.dao.converter.DateTypeConverter
import com.jingtian.composedemo.dao.converter.FileTypeConverter
import com.jingtian.composedemo.dao.converter.ItemRankConverter

actual fun dbImplCreator() : DataBase {
    return Room.databaseBuilder(app, DataBase::class.java, "app_db")
        .addTypeConverter(DateTypeConverter())
        .addTypeConverter(FileTypeConverter())
        .addTypeConverter(ItemRankConverter())
        .build()
}