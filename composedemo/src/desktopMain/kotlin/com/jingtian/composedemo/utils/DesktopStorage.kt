package com.jingtian.composedemo.utils

import androidx.compose.ui.window.WindowPlacement
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.jingtian.composedemo.multiplatform.getJsonStorage
import com.jingtian.composedemo.utils.model.DesktopConfig
import com.jingtian.composedemo.utils.model.converter.WindowPlacementConverter

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