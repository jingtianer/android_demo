package com.jingtian.composedemo.base

import android.app.Application
import com.jingtian.composedemo.utils.FileStorageUtils

lateinit var app: BaseApplication

class BaseApplication : Application() {
    init {
        app = this
    }

    override fun onCreate() {
        super.onCreate()
        FileStorageUtils.checkRootDir()
    }

}