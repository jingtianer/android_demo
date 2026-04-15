package com.jingtian.composedemo.multiplatform

import androidx.collection.arrayMapOf
import com.jingtian.composedemo.dao.model.relation.AlbumRelation
import com.jingtian.composedemo.utils.CoroutineUtils
import com.jingtian.composedemo.utils.getFileStorageRootDir
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.internal.synchronized
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.util.concurrent.ConcurrentHashMap

class MultiplatformSharedPreferences<V>(
    name: String,
    val json: Json = Json { ignoreUnknownKeys = true },
    val async: Boolean = true
) {
    private val outfile = File(spDir, name)
    init {
        if (!outfile.exists()) {
            outfile.createNewFile()
        } else if (outfile.isDirectory) {
            outfile.deleteRecursively()
            outfile.createNewFile()
        }
    }
    private val value: MutableMap<String, V?> = ConcurrentHashMap(
        try {
            json.decodeFromString<Map<String, V>>("")
        } catch (e : Exception) {
            arrayMapOf()
        }
    )
    private val lock = Any()
    private var job: Job? = null

    fun all(): Map<String, V?> = value

    @OptIn(InternalCoroutinesApi::class)
    fun <T : V?> editor(): IMultiplatformSharedPreferences<T> {
        return object : IMultiplatformSharedPreferences<T> {
            override fun getValue(key: String, defaultValue: T): T {
                return (value[key] as? T) ?: let {
                    setValue(key, defaultValue)
                    defaultValue
                }
            }

            override fun toString(): String {
                return json.encodeToString(value)
            }

            override fun setValue(key: String, t: T) {
                value.put(key, t)
                updateStore()
            }

            override fun delete(key: String) {
                value.remove(key)
                updateStore()
            }

            private fun updateStore() {
                synchronized(lock) {
                    if (async) {
                        job?.cancel()
                        job = CoroutineUtils.runIOTask({
                            FileWriter(outfile).use {
                                it.write(json.encodeToString(value))
                                it.flush()
                            }
                        })
                    } else {
                        FileWriter(outfile).use {
                            it.write(json.encodeToString(value))
                            it.flush()
                        }
                    }
                }
            }
        }
    }

    companion object {
        private val spDir = File(getFileStorageRootDir(), "properties")
        init {
            if (!spDir.exists()) {
                spDir.mkdirs()
            } else if (!spDir.isDirectory) {
                spDir.delete()
                spDir.mkdirs()
            }
        }
    }
}

private val spInstance = ConcurrentHashMap<String, MultiplatformSharedPreferences<*>>()

private inline fun <V> getStorage(storageName: String, crossinline initializer: ()->MultiplatformSharedPreferences<V>) : MultiplatformSharedPreferences<V>{
    return spInstance.computeIfAbsent(storageName) { old->
        initializer()
    } as MultiplatformSharedPreferences<V>
}

fun getJsonStorage(storageName: String): IMultiplatformSharedPreferences<String> {
    return getStorage(storageName) {
        MultiplatformSharedPreferences<Any?>(storageName)
    }.editor()
}

fun getLongStorage(storageName: String): IMultiplatformSharedPreferences<Long> {
    return getStorage(storageName) {
        MultiplatformSharedPreferences<Any?>(storageName)
    }.editor()
}

fun <V> getRawStorage(
    storageName: String,
    typeToken: Json = Json { ignoreUnknownKeys = true },
): MultiplatformSharedPreferences<V> {
    return getStorage(storageName) {
        MultiplatformSharedPreferences(storageName, typeToken)
    }
}