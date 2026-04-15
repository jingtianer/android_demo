package com.jingtian.composedemo.utils

import androidx.compose.ui.window.WindowPlacement
import com.jingtian.composedemo.multiplatform.getJsonStorage
import com.jingtian.composedemo.utils.model.DesktopConfig
import kotlinx.serialization.json.Json

object DesktopStorage {


    var desktopConfig by SharedPreferenceUtils.StorageJson(
        getJsonStorage("desktop_config"),
        "desktop_config",
        DesktopConfig(),
        serializer = DesktopConfig.serializer(),
        jsonFormat = Json { ignoreUnknownKeys = true },
    )

    fun updateDesktopConfig(updater: DesktopConfig.() -> DesktopConfig) {
        desktopConfig = desktopConfig.updater()
    }
}
