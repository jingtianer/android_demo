package com.jingtian.composedemo.utils

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jingtian.composedemo.multiplatform.IMultiplatformSharedPreferences
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class SharedPreferenceUtils {
    open class StorageVariable<V, T>(
        private val sp: IMultiplatformSharedPreferences<T>,
        private val key: String,
        defaultValue: T,
    ) : ReadWriteProperty<V, T> {
        private var value = sp.getValue(key, defaultValue)

        override fun getValue(thisRef: V, property: KProperty<*>): T {
            return value
        }

        override fun setValue(thisRef: V, property: KProperty<*>, value: T) {
            this.value = value
            sp.setValue(key, value)
        }
    }

    class StorageBoolean<T>(
        sp: IMultiplatformSharedPreferences<Boolean>,
        key: String,
        defaultValue: Boolean
    ) : StorageVariable<T, Boolean>(sp, key, defaultValue)

    class StorageLong<T>(
        sp: IMultiplatformSharedPreferences<Long>,
        key: String,
        defaultValue: Long
    ) : StorageVariable<T, Long>(
        sp, key, defaultValue,
    )

    class StorageNullableString<T>(
        sp: IMultiplatformSharedPreferences<String?>,
        key: String,
        defaultValue: String?
    ) : StorageVariable<T, String?>(
        sp, key, defaultValue,
    )

    class StorageString<T>(
        sp: IMultiplatformSharedPreferences<String>,
        key: String,
        defaultValue: String
    ) : StorageVariable<T, String>(sp, key, defaultValue)

    class StorageJson<T, V>(
        sp: IMultiplatformSharedPreferences<String>,
        key: String,
        defaultValue: V,
        typeToken: TypeToken<V>,
        private val gson: Gson = Gson(),
    ) : ReadWriteProperty<T, V> {
        private var json by StorageString(sp, key, gson.toJson(defaultValue))
        private var value = gson.fromJson(json, typeToken.type) as V
        override fun getValue(thisRef: T, property: KProperty<*>): V {
            return value
        }

        override fun setValue(thisRef: T, property: KProperty<*>, value: V) {
            this.value = value
            json = gson.toJson(value)
        }
    }
}