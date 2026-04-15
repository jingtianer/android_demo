package com.jingtian.composedemo.multiplatform

import com.jingtian.composedemo.utils.CoroutineUtils
import com.jingtian.composedemo.utils.createNewFile
import com.jingtian.composedemo.utils.delete
import com.jingtian.composedemo.utils.deleteRecursively
import com.jingtian.composedemo.utils.exists
import com.jingtian.composedemo.utils.getFileStorageRootDir
import com.jingtian.composedemo.utils.isDirectory
import com.jingtian.composedemo.utils.mkdirs
import com.jingtian.composedemo.utils.synchronized
import kotlinx.coroutines.Job
import kotlinx.coroutines.sync.Mutex
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.writeString
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

class MultiplatformSharedPreferences<V>(
    name: String,
    val json: Json = Json { ignoreUnknownKeys = true },
    val async: Boolean = true
) {
    private val outfile = Path(spDir, name)
    init {
        if (!outfile.exists()) {
            outfile.createNewFile()
        } else if (outfile.isDirectory) {
            outfile.deleteRecursively()
            outfile.createNewFile()
        }
    }
    private val value: MutableMap<String, V?> = HashMap(
        try {
            json.decodeFromString<Map<String, V>>("")
        } catch (e : Exception) {
            hashMapOf()
        }
    )
    private val lock = Mutex()
    private var job: Job? = null

    fun all(): Map<String, V?> = value

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
                            SystemFileSystem.sink(outfile).buffered().use {
                                it.writeString(json.encodeToString(value))
                                it.flush()
                            }
                        })
                    } else {
                        SystemFileSystem.sink(outfile).buffered().use {
                            it.writeString(json.encodeToString(value))
                            it.flush()
                        }
                    }
                }
            }
        }
    }

    companion object {
        private val spDir = Path(getFileStorageRootDir(), "properties")
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

private val spInstance: MutableMap<String, MultiplatformSharedPreferences<*>> = mutableMapOf()
private val spInstanceLock = Mutex()

private inline fun <V> getStorage(storageName: String, crossinline initializer: ()->MultiplatformSharedPreferences<V>) : MultiplatformSharedPreferences<V>{
    return synchronized(spInstanceLock) {
        spInstance.getOrPut(storageName) {
            initializer()
        } as MultiplatformSharedPreferences<V>
    }
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