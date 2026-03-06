package com.jingtian.composedemo.utils

import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class SharedPreferenceUtils {
    open class StorageVariable<V, T>(
        private val sp: SharedPreferences,
        private val key: String,
        defaultValue: T,
        getValue: SharedPreferences.(String, T) -> T,
        private val putValue: SharedPreferences.Editor.(String, T) -> SharedPreferences.Editor,
    ) : ReadWriteProperty<V, T> {
        private var value = sp.getValue(key, defaultValue)

        override fun getValue(thisRef: V, property: KProperty<*>): T {
            return value
        }

        override fun setValue(thisRef: V, property: KProperty<*>, value: T) {
            this.value = value
            sp.edit().putValue(key, value).commit()
        }
    }

    class StorageBoolean<T>(
        sp: SharedPreferences,
        key: String,
        defaultValue: Boolean
    ) : StorageVariable<T, Boolean>(
        sp, key, defaultValue,
        SharedPreferences::getBoolean,
        SharedPreferences.Editor::putBoolean
    )

    class StorageLong<T>(
        sp: SharedPreferences,
        key: String,
        defaultValue: Long
    ) : StorageVariable<T, Long>(
        sp, key, defaultValue,
        SharedPreferences::getLong,
        SharedPreferences.Editor::putLong
    )

    class SynchronizedProperty<T, V>(
        private val property: ReadWriteProperty<T, V>
    ) : ReadWriteProperty<T, V> {

        @Synchronized
        override fun getValue(thisRef: T, property: KProperty<*>): V {
            return this.property.getValue(thisRef, property)
        }

        @Synchronized
        override fun setValue(thisRef: T, property: KProperty<*>, value: V) {
            this.property.setValue(thisRef, property, value)
        }

    }

    class StorageNullableString<T>(
        sp: SharedPreferences,
        key: String,
        defaultValue: String?
    ) : StorageVariable<T, String?>(
        sp, key, defaultValue,
        { k, v -> getString(k, v) ?: v },
        SharedPreferences.Editor::putString
    )

    class StorageString<T>(
        sp: SharedPreferences,
        key: String,
        defaultValue: String
    ) : StorageVariable<T, String>(
        sp, key, defaultValue,
        { k, v -> getString(k, v) ?: v },
        SharedPreferences.Editor::putString
    )

    class StorageJson<T, V>(
        sp: SharedPreferences,
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