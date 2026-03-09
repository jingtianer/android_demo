package com.jingtian.composedemo.multiplatform

@Suppress("UNCHECKED_CAST")
class FakeMultiplatformSharedPreferencesImpl<T> : IMultiplatformSharedPreferences<T> {
    val value: MutableMap<String, Any?> = mutableMapOf()
    override fun getValue(key: String, defaultValue: T): T {
        return value[key] as T
    }

    override fun setValue(key: String, t: T) {
        value[key] = t
    }

    companion object {
        val fakeMap: MutableMap<String, FakeMultiplatformSharedPreferencesImpl<*>> = mutableMapOf()
    }
}

actual fun getJsonStorage(storageName: String): IMultiplatformSharedPreferences<String> {
    return FakeMultiplatformSharedPreferencesImpl.fakeMap.getOrPut(
        storageName
    ) {
        FakeMultiplatformSharedPreferencesImpl<String>()
    } as FakeMultiplatformSharedPreferencesImpl<String>
}

actual fun getLongStorage(storageName: String): IMultiplatformSharedPreferences<Long> {
    return FakeMultiplatformSharedPreferencesImpl.fakeMap.getOrPut(
        storageName
    ) {
        FakeMultiplatformSharedPreferencesImpl<Long>()
    } as FakeMultiplatformSharedPreferencesImpl<Long>
}