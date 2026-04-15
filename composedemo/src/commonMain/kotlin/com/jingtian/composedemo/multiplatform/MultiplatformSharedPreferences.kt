package com.jingtian.composedemo.multiplatform

interface IMultiplatformSharedPreferences<T> {
    fun getValue(key: String, defaultValue: T): T
    fun getValue(key: String): T?
    fun setValue(key: String, t: T?)
    fun delete(key: String)
}