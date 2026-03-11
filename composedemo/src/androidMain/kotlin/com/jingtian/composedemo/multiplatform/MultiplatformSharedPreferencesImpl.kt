package com.jingtian.composedemo.multiplatform

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jingtian.composedemo.base.app
import com.jingtian.composedemo.utils.CoroutineUtils
import com.jingtian.composedemo.utils.copyDir
import com.jingtian.composedemo.utils.getFileStorageRootDir
import kotlinx.coroutines.Job
import java.io.File
import java.io.FileReader
import java.io.FileWriter

class MultiplatformSharedPreferencesImpl<T>(name: String, val async: Boolean = true) : IMultiplatformSharedPreferences<T> {
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
        if (async) {
            job?.cancel()
            job = CoroutineUtils.runIOTask({
                FileWriter(outfile).use {
                    it.write(Gson().toJson(value))
                }
            })
        } else {
            FileWriter(outfile).use {
                it.write(Gson().toJson(value))
            }
        }
    }

    companion object {
        val spDir = File(getFileStorageRootDir(), "properties")
        init {
            if (!spDir.exists()) {
                spDir.mkdirs()
            } else if (!spDir.isDirectory) {
                spDir.delete()
                spDir.mkdirs()
            }
//            arrayOf(
//                "user_info",
//                "user_config",
//                "file-store_id"
//            ).forEach {
//                migrate(it)
//            }
//            app.filesDir.parentFile?.let {
//                File(it, "shared_prefs").deleteRecursively()
//            }
        }

//        fun migrate(name: String) {
//            val sp = MultiplatformSharedPreferencesImpl<Any?>(name, async = false)
//            app.getSharedPreferences(name, Context.MODE_PRIVATE).all?.entries?.forEach { (k, v)->
//                sp.setValue(k, v)
//            }
//        }
    }
}

actual fun getJsonStorage(storageName: String): IMultiplatformSharedPreferences<String> {
    return MultiplatformSharedPreferencesImpl(storageName)
}

actual fun getLongStorage(storageName: String): IMultiplatformSharedPreferences<Long> {
    return MultiplatformSharedPreferencesImpl(storageName)
}