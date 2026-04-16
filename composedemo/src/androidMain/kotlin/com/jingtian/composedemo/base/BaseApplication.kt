package com.jingtian.composedemo.base

import android.app.Application
import com.jingtian.composedemo.launch.LaunchTasks
import com.jingtian.composedemo.utils.FileLinkProvider
import com.jingtian.composedemo.utils.FileStorageUtils
//import com.jingtian.composedemo.utils.python.PyInitializer

lateinit var app: BaseApplication

class BaseApplication : Application() {
    init {
        app = this
    }

    override fun onCreate() {
        super.onCreate()
        LaunchTasks.onLaunch()
        FileLinkProvider.init()
//        PyInitializer.init()
    }

}