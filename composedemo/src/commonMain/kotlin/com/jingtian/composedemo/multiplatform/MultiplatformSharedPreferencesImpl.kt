package com.jingtian.composedemo.multiplatform

expect fun getJsonStorage(storageName: String): IMultiplatformSharedPreferences<String>

expect fun getLongStorage(storageName: String): IMultiplatformSharedPreferences<Long>