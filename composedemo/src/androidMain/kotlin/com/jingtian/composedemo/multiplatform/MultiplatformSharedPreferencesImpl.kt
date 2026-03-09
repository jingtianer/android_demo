package com.jingtian.composedemo.multiplatform

import android.content.Context
import android.content.SharedPreferences
import com.jingtian.composedemo.base.app

open class MultiplatformSharedPreferencesImpl<T>(
    private val sp: SharedPreferences,
    private val getValue: SharedPreferences.(String, T) -> T,
    private val putValue: SharedPreferences.Editor.(String, T) -> SharedPreferences.Editor,
) : IMultiplatformSharedPreferences<T> {
    override fun getValue(key: String, defaultValue: T): T {
        return sp.getValue(key, defaultValue)
    }

    override fun setValue(key: String, t: T) {
        sp.edit().putValue(key, t).commit()
    }
}

class BooleanSharedPreferencesImpl(
    private val sp: SharedPreferences,
) : MultiplatformSharedPreferencesImpl<Boolean>(
    sp,
    SharedPreferences::getBoolean,
    SharedPreferences.Editor::putBoolean,
)

class LongSharedPreferencesImpl(
    private val sp: SharedPreferences,
) : MultiplatformSharedPreferencesImpl<Long>(
    sp,
    SharedPreferences::getLong,
    SharedPreferences.Editor::putLong,
)

class StringSharedPreferencesImpl(
    private val sp: SharedPreferences,
) : MultiplatformSharedPreferencesImpl<String>(
    sp,
    { key, def ->
        sp.getString(key, def) ?: def
    },
    SharedPreferences.Editor::putString,
)

class NullableStringSharedPreferencesImpl(
    private val sp: SharedPreferences,
) : MultiplatformSharedPreferencesImpl<String?>(
    sp,
    SharedPreferences::getString,
    SharedPreferences.Editor::putString,
)

fun getJsonStorage(storageName: String): MultiplatformSharedPreferencesImpl<String> {
    return StringSharedPreferencesImpl(
        app.getSharedPreferences(
            storageName,
            Context.MODE_PRIVATE
        )
    )
}

fun getLongStorage(storageName: String): MultiplatformSharedPreferencesImpl<Long> {
    return LongSharedPreferencesImpl(
        app.getSharedPreferences(
            storageName,
            Context.MODE_PRIVATE
        )
    )
}