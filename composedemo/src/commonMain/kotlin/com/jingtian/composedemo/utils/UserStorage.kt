package com.jingtian.composedemo.utils

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.jingtian.composedemo.dao.model.User
import com.jingtian.composedemo.multiplatform.getJsonStorage
import com.jingtian.composedemo.multiplatform.getLongStorage

object UserStorage {
    var userInstance: User by SharedPreferenceUtils.StorageJson(
        getJsonStorage("user_info"),
        "user",
        User(),
        TypeToken.get(User::class.java),
        gson = GsonBuilder()
            .create(),
    )

    private var _userAppThemeConfig: Long by SharedPreferenceUtils.StorageLong(
        getLongStorage("user_config"),
        "theme_config",
        AppTheme.AUTO.value,
    )

    var userAppThemeConfig : AppTheme
        get() = AppTheme.parse(_userAppThemeConfig)
        set(value) {
            _userAppThemeConfig = value.value
        }
}