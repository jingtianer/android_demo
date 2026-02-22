package com.jingtian.composedemo.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class AppTheme(val value: Long) {
    AUTO(0), Dark(1), Lite(2);

   companion object {
       fun parse(value: Long):AppTheme {
           return when(value) {
               0L -> AUTO
               1L -> Dark
               2L -> Lite
               else -> AUTO
           }
       }

       suspend fun isDarkTheme(systemDarkTheme: Boolean): Boolean {
           return isDarkTheme(systemDarkTheme, UserStorage.userAppThemeConfig)
       }

       fun isDarkTheme(systemDarkTheme: Boolean, appTheme: AppTheme): Boolean {
           return when(appTheme) {
               Dark -> true
               Lite -> false
               AUTO -> systemDarkTheme
           }
       }

       fun currentAppTheme(): AppTheme {
           return UserStorage.userAppThemeConfig
       }

       suspend fun setAppTheme(appTheme: AppTheme) {
           withContext(Dispatchers.IO) {
               UserStorage.userAppThemeConfig = appTheme
           }
       }
   }
}