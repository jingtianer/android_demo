package com.jingtian.composedemo.utils

import androidx.compose.ui.window.WindowPlacement
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import com.jingtian.composedemo.multiplatform.getJsonStorage

class DesktopConfig {
    var screenWidthDp: Float = 380f
    var screenHeightDp: Float = 820f
    var windowPlacement: WindowPlacement = WindowPlacement.Floating
}

class WindowPlacementConverter: TypeAdapter<WindowPlacement>() {
    fun WindowPlacement.toInt(): Int {
        return when(this) {
            WindowPlacement.Floating -> 0
            WindowPlacement.Maximized -> 1
            WindowPlacement.Fullscreen -> 2
        }
    }
    fun Int.fromInt(): WindowPlacement {
        return when(this) {
            0 -> WindowPlacement.Floating
            1 -> WindowPlacement.Maximized
            2 -> WindowPlacement.Fullscreen
            else -> WindowPlacement.Floating
        }
    }
    override fun write(out: JsonWriter?, value: WindowPlacement?) {
        out?.value((value ?: WindowPlacement.Floating).toInt())
    }

    override fun read(`in`: JsonReader?): WindowPlacement {
        return `in`?.nextInt()?.fromInt() ?: WindowPlacement.Floating
    }

}

object DesktopStorage {


    var desktopConfig by SharedPreferenceUtils.SynchronizedProperty(
        SharedPreferenceUtils.StorageJson(
            getJsonStorage("desktop_config"),
            "desktop_config",
            DesktopConfig(),
            TypeToken.get(DesktopConfig::class.java),
            gson = GsonBuilder()
                .registerTypeAdapter(TypeToken.get(WindowPlacement::class.java).type, WindowPlacementConverter())
                .create(),
        )
    )

    fun updateDesktopConfig(updater: DesktopConfig.()->DesktopConfig) {
        desktopConfig = desktopConfig.updater()
    }
}