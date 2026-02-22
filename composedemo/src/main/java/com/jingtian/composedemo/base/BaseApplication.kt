package com.jingtian.composedemo.base

import android.app.Application
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import com.jingtian.composedemo.utils.AppTheme
import com.jingtian.composedemo.utils.FileStorageUtils
import com.jingtian.composedemo.utils.UserStorage

lateinit var app: BaseApplication

class BaseApplication : Application() {
    init {
        app = this
    }

    override fun onCreate() {
        super.onCreate()
        FileStorageUtils.checkRootDir()
        when (UserStorage.userAppThemeConfig) {
            AppTheme.AUTO -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            AppTheme.Dark -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            AppTheme.Lite -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

}