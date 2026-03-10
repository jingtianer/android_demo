package com.jingtian.composedemo.multiplatform

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jingtian.composedemo.utils.CoroutineUtils
import com.jingtian.composedemo.utils.globalWorkDir
import kotlinx.coroutines.Job
import java.io.File
import java.io.FileReader
import java.io.FileWriter

class MultiplatformSharedPreferencesImpl<T>(name: String) : IMultiplatformSharedPreferences<T> {
    private val outfile = File(spDir, name)
    init {
        if (!outfile.exists()) {
            outfile.createNewFile()
        } else if (outfile.isDirectory) {
            outfile.deleteRecursively()
            outfile.createNewFile()
        }
    }
    private val value: MutableMap<String, T?> = FileReader(outfile).use {
        Gson().fromJson(it, TypeToken.get(MutableMap::class.java).type) ?: mutableMapOf()
    }

    private var job: Job? = null
    override fun getValue(key: String, defaultValue: T): T {
        return value[key] ?: defaultValue
    }

    override fun setValue(key: String, t: T) {
        value[key] = t
        job?.cancel()
        job = CoroutineUtils.runIOTask({
            FileWriter(outfile).use {
                it.write(Gson().toJson(value))
            }
        })
    }

    companion object {
        val spDir = File(globalWorkDir, "properties")
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

actual fun getJsonStorage(storageName: String): IMultiplatformSharedPreferences<String> {
    return MultiplatformSharedPreferencesImpl(storageName)
}

actual fun getLongStorage(storageName: String): IMultiplatformSharedPreferences<Long> {
    return MultiplatformSharedPreferencesImpl(storageName)
}