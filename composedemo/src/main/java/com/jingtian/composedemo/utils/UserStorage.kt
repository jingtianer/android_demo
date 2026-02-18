package com.jingtian.composedemo.utils

import android.content.Context
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.jingtian.composedemo.base.app
import com.jingtian.composedemo.dao.model.User

object UserStorage {
    var userInstance by SharedPreferenceUtils.SynchronizedProperty(
        SharedPreferenceUtils.StorageJson(
            app.getSharedPreferences(
                "user_info",
                Context.MODE_PRIVATE
            ),
            "user",
            User(),
            TypeToken.get(User::class.java),
            gson = GsonBuilder()
                .create(),
        )
    )
}