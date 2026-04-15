package com.jingtian.composedemo.dao

import android.os.Build
import android.util.Log
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import com.jingtian.composedemo.base.app
import com.jingtian.composedemo.utils.copyDir
import com.jingtian.composedemo.utils.getFileStorageRootDir
import java.io.File

actual fun dbImplCreator() : RoomDatabase.Builder<DataBase>  {
    val dbDir = File(getFileStorageRootDir().toString(), "database")
    val dbFile = File(dbDir, "app_db")
    dbFile.parentFile?.let { parent->
        if (parent.exists()) {
            if (parent.isFile) {
                parent.delete()
                parent.mkdirs()
            }
        } else {
            parent.mkdirs()
        }
        Unit
    }
//    val oldDb = app.getDatabasePath("app_db")
//    if (oldDb.exists()) {
//        oldDb.parentFile?.let { privateDir->
//            val targetDir = dbDir
//            copyDir(privateDir, targetDir)
//            privateDir.deleteRecursively()
//        }
//    }
    val openHelperFactory = SupportSQLiteOpenHelper.Factory { config ->
        FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(config.context)
                .name(dbFile.absolutePath)
                .callback(config.callback)
                .build()
        )
    }
    return Room.databaseBuilder(app, DataBase::class.java, "app_db")
        .openHelperFactory(openHelperFactory)
}

//actual object RoomConstructor: RoomDatabaseConstructor<DataBase> {
//    override fun initialize(): DataBase {
//        return DataBase.dbImpl
//    }
//}
