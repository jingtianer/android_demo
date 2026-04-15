package com.jingtian.composedemo.utils

import com.jingtian.composedemo.dao.model.User
import com.jingtian.composedemo.multiplatform.getJsonStorage
import com.jingtian.composedemo.multiplatform.getLongStorage
import kotlinx.serialization.json.Json

object UserStorage {
    var userInstance: User by SharedPreferenceUtils.StorageJson(
        getJsonStorage("user_info"),
        "user",
        User(),
        serializer = User.serializer(),
        jsonFormat = Json { ignoreUnknownKeys = true },
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
