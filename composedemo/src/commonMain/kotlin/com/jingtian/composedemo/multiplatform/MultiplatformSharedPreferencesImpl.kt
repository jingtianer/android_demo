package com.jingtian.composedemo.multiplatform

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jingtian.composedemo.utils.CoroutineUtils
import com.jingtian.composedemo.utils.getFileStorageRootDir
import kotlinx.coroutines.Job
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.util.concurrent.ConcurrentHashMap

private val spInstance = ConcurrentHashMap<String, MultiplatformSharedPreferences>()

class MultiplatformSharedPreferences(name: String, val async: Boolean = true) {
    private val outfile = File(spDir, name)
    init {
        if (!outfile.exists()) {
            outfile.createNewFile()
        } else if (outfile.isDirectory) {
            outfile.deleteRecursively()
            outfile.createNewFile()
        }
    }
    private val gson = Gson()
    private val value: MutableMap<String, Any?> = ConcurrentHashMap(
        FileReader(outfile).use {
            gson.fromJson(it, TypeToken.get(MutableMap::class.java).type) ?: mutableMapOf()
        }
    )
    private val lock = Any()
    private var job: Job? = null

    fun <T> editor(): IMultiplatformSharedPreferences<T> {
        return object : IMultiplatformSharedPreferences<T> {
            override fun getValue(key: String, defaultValue: T): T {
                return (value[key] as? T) ?: defaultValue
            }

            override fun toString(): String {
                return gson.toJson(value)
            }

            override fun setValue(key: String, t: T) {
                value[key] = t
                synchronized(lock) {
                    if (async) {
                        job?.cancel()
                        job = CoroutineUtils.runIOTask({
                            FileWriter(outfile).use {
                                gson.toJson(value, it)
                                it.flush()
                            }
                        })
                    } else {
                        FileWriter(outfile).use {
                            gson.toJson(value, it)
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

fun getJsonStorage(storageName: String): IMultiplatformSharedPreferences<String> {
    return spInstance.computeIfAbsent(storageName) { old->
        MultiplatformSharedPreferences(storageName)
    }.editor()
}

fun getLongStorage(storageName: String): IMultiplatformSharedPreferences<Long> {
    return spInstance.computeIfAbsent(storageName) { old->
        MultiplatformSharedPreferences(storageName)
    }.editor()
}