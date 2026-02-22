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

    private fun isDarkModeEnabled(): Boolean {
        return when (AppCompatDelegate.getDefaultNightMode()) {
            AppCompatDelegate.MODE_NIGHT_YES -> true // App强制深色
            AppCompatDelegate.MODE_NIGHT_NO -> false  // App强制浅色
            else -> {
                isSystemDark()
            }
        }
    }

    private fun isSystemDark(): Boolean {
        val uiMode = resources.configuration.uiMode
        return (uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }

}