package com.jingtian.composedemo.multiplatform

import com.jingtian.composedemo.utils.CoroutineUtils
import com.jingtian.composedemo.utils.createNewFile
import com.jingtian.composedemo.utils.delete
import com.jingtian.composedemo.utils.deleteRecursively
import com.jingtian.composedemo.utils.exists
import com.jingtian.composedemo.utils.getFileStorageRootDir
import com.jingtian.composedemo.utils.isDirectory
import com.jingtian.composedemo.utils.mkdirs
import kotlinx.coroutines.Job
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlinx.io.writeString
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

@Serializable
private class SharedPreferenceStorage<V>(val content: MutableMap<String, V> = mutableMapOf())

class MultiplatformSharedPreferences<V>(
    val name: String,
    val kSerializer: KSerializer<V>,
    val json: Json = Json { ignoreUnknownKeys = true },
    val async: Boolean = true
) {
    private val outfile = Path(spDir, name)
    init {
        if (!outfile.exists()) {
            outfile.createNewFile()
//            println("MultiplatformSharedPreferences: $name !exists")
        } else if (outfile.isDirectory) {
            outfile.deleteRecursively()
            outfile.createNewFile()
//            println("MultiplatformSharedPreferences: $name isDirectory")
        }
//        println("MultiplatformSharedPreferences: $name ${outfile}")
    }
    private val storage: SharedPreferenceStorage<V> =
        try {
            val jsonData = SystemFileSystem.source(outfile).buffered().readString()
            json.decodeFromString(SharedPreferenceStorage.serializer(kSerializer), jsonData)
        } catch (e : Exception) {
            SharedPreferenceStorage()
        }

    private val value get() = storage.content

    init {
//        println("MultiplatformSharedPreferences: value=$value")
    }
    private val lock = newReentrantLock()
    private var job: Job? = null

    fun all(): Map<String, V?> = value

    fun editor(): IMultiplatformSharedPreferences<V> {
        return object : IMultiplatformSharedPreferences<V> {
            override fun getValue(key: String, defaultValue: V): V {
                return value[key] ?: let {
                    setValue(key, defaultValue)
                    defaultValue
                }
            }

            override fun getValue(key: String): V? {
                return value[key]
            }

            override fun toString(): String {
                return json.encodeToString(value)
            }

            override fun setValue(key: String, t: V?) {
                if (t == null) {
                    value.remove(key)
                } else {
                    value[key] = t
                }
//                println("set: $name, $key $t, $value")
                updateStore()
            }

            override fun delete(key: String) {
                value.remove(key)
                updateStore()
            }

            private fun updateStore() {
                lock.use {
                    if (async) {
                        job?.cancel()
                        job = CoroutineUtils.runIOTask({
                            SystemFileSystem.sink(outfile).buffered().use {
                                it.writeString(json.encodeToString(SharedPreferenceStorage.serializer(kSerializer), storage).apply {
//                                    println("write: $name, $this")
                                })
                                it.flush()
                            }
                        }, {
//                            println("updateStore: error $it")
                        })
                    } else {
                        SystemFileSystem.sink(outfile).buffered().use {
                            it.writeString(json.encodeToString(value).apply {
//                                println("write: $name, $this")
                            })
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


private inline fun <V> getStorage(storageName: String, crossinline initializer: ()->MultiplatformSharedPreferences<V>) : MultiplatformSharedPreferences<V>{
    return initializer()
}

fun getJsonStorage(storageName: String): IMultiplatformSharedPreferences<String> {
    return getStorage(storageName) {
        MultiplatformSharedPreferences(storageName, String.serializer())
    }.editor()
}

fun getLongStorage(storageName: String): IMultiplatformSharedPreferences<Long> {
    return getStorage(storageName) {
        MultiplatformSharedPreferences(storageName, Long.serializer(), json = Json {
            ignoreUnknownKeys = true
        })
    }.editor()
}

fun <V> getRawStorage(
    storageName: String,
    kSerializer: KSerializer<V>,
    json: Json = Json { ignoreUnknownKeys = true },
): MultiplatformSharedPreferences<V> {
    return getStorage(storageName) {
        MultiplatformSharedPreferences(storageName, kSerializer, json)
    }
}