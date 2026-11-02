package com.jingtian.composedemo.utils

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.jingtian.composedemo.multiplatform.IMultiplatformSharedPreferences
import com.jingtian.composedemo.multiplatform.logD
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

class SharedPreferenceUtils {
    open class StorageVariable<V, T>(
        private val sp: IMultiplatformSharedPreferences<T>,
        private val key: String,
        defaultValue: T,
    ) : ReadWriteProperty<V, T> {
        private var value = sp.getValue(key, defaultValue)

        override operator fun getValue(thisRef: V, property: KProperty<*>): T {
            return value
        }

        override operator fun setValue(thisRef: V, property: KProperty<*>, value: T) {
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
        private val serializer: KSerializer<V>,
        private val jsonFormat: Json = Json { ignoreUnknownKeys = true },
    ) : ReadWriteProperty<T, V> {
        private var json by StorageString(sp, key, jsonFormat.encodeToString(serializer, defaultValue))
        private var value: V = runCatching {
            jsonFormat.decodeFromString(serializer, json)
        }.getOrElse {
            defaultValue
        }
        override fun getValue(thisRef: T, property: KProperty<*>): V {
            return value
        }

        override fun setValue(thisRef: T, property: KProperty<*>, value: V) {
            this.value = value
            json = jsonFormat.encodeToString(serializer, value)
        }
    }

    class StateFullStorageJson<V>(
        sp: IMultiplatformSharedPreferences<String>,
        key: String,
        defaultValue: V,
        serializer: KSerializer<V>,
        jsonFormat: Json = Json { ignoreUnknownKeys = true },
    ): ReadWriteProperty<Any?, V> {
        var value by StorageJson(sp, key, defaultValue, serializer, jsonFormat)
        var state = mutableStateOf(value)
        override fun setValue(thisRef: Any?, property: KProperty<*>, value: V) {
            this.value = value
            state.value = value
        }

        override fun getValue(thisRef: Any?, property: KProperty<*>): V {
            return state.value
        }
    }
}
