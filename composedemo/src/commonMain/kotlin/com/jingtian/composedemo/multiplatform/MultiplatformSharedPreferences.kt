package com.jingtian.composedemo.multiplatform

interface IMultiplatformSharedPreferences<T> {
    fun getValue(key: String, defaultValue: T): T
    fun setValue(key: String, t: T)
}