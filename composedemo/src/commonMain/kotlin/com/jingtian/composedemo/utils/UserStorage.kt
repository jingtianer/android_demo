package com.jingtian.composedemo.utils

import androidx.annotation.FloatRange
import androidx.compose.runtime.State
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.jingtian.composedemo.dao.model.User
import com.jingtian.composedemo.multiplatform.getJsonStorage
import com.jingtian.composedemo.multiplatform.getLongStorage
import com.jingtian.composedemo.multiplatform.getRawStorage
import kotlinx.serialization.json.Json
import kotlin.math.roundToLong

@kotlinx.serialization.Serializable
data class BackgroundColor(
    @FloatRange(0.0, 1.0)
    val startX: Float = 0f,
    val rawColor: Long,
) {
    fun stop() = startX to this.color

    fun withColor(color: Color): BackgroundColor = this.copy(rawColor = color.toRawColor())
}

private fun Color.toRawColor(): Long {
    return ((this.alpha * 255).roundToLong() shl 24) or
            ((this.red * 255).roundToLong() shl 16) or
            ((this.green * 255).roundToLong() shl 8) or
            ((this.blue * 255).roundToLong() shl 0)
}

@kotlinx.serialization.Serializable
data class BackgroundConfig(
    val startColor: BackgroundColor,
    val endColor: BackgroundColor,
    val middleColor: BackgroundColor? = null,
) {
    companion object {
        val defaultLiteBackgroundColor = BackgroundConfig(
            startColor = BackgroundColor(
                rawColor = Color(0xCAEFD7ee).toRawColor()
            ),
            middleColor = BackgroundColor(
                rawColor = Color(0xF5BFD7ee).toRawColor(),
                startX = 0.5f,
            ),
            endColor = BackgroundColor(
                rawColor = Color(0xABC9E9ee).toRawColor(),
                startX = 1f,
            ),
        )

        val defaultDarkBackgroundColor = BackgroundConfig(
            startColor = BackgroundColor(
                rawColor = Color(alpha = 0x5f, red = 0xca, green = 0xef, blue = 0xd7).toRawColor()
            ),
            middleColor = BackgroundColor(
                rawColor = Color(alpha = 0x5f, red = 0xf5, green = 0xbf, blue = 0xd7).toRawColor(),
                startX = 0.5f,
            ),
            endColor = BackgroundColor(
                rawColor = Color(alpha = 0x5f, red = 0xab, green = 0xc9, blue = 0xe9).toRawColor(),
                startX = 1f,
            ),
        )
    }

    fun horizontalGradient() = if (middleColor == null) {
        Brush.horizontalGradient(startColor.stop(), endColor.stop())
    } else {
        Brush.horizontalGradient(startColor.stop(), middleColor.stop(), endColor.stop())
    }
}

val BackgroundColor.color: Color get() = Color(this.rawColor)

fun BackgroundConfig.mainColor(): Color {
    return if (middleColor == null) {
        startColor.color.mul(startColor.startX).add(
            endColor.color.mul(1 - endColor.startX).add(
                startColor.color.mul(0.5f).add(endColor.color.mul(0.5f))
                    .mul((endColor.startX - startColor.startX))
            )
        )
    } else {
        startColor.color.mul(startColor.startX).add(
            endColor.color.mul(1 - endColor.startX).add(
                endColor.color.mul(0.5f).add(middleColor.color.mul(0.5f))
                    .mul((endColor.startX - middleColor.startX)).add(
                        middleColor.color.mul(0.5f).add(startColor.color.mul(0.5f))
                            .mul((middleColor.startX - startColor.startX))
                    )
            )
        )
    }
}

fun BackgroundConfig.colorOf(startX: Float): Long {
    return (startColor.color.mul((startX - startColor.startX) / (endColor.startX - startColor.startX))
        .add(endColor.color.mul((endColor.startX - startX) / (endColor.startX - startColor.startX)))).toRawColor()
}

private fun Color.mul(percent: Float): Color {
    return Color(
        alpha = this.alpha * percent,
        red = this.red * percent,
        green = this.green * percent,
        blue = this.blue * percent,
    )
}

private fun Color.add(other: Color): Color {
    return Color(
        alpha = this.alpha + other.alpha,
        red = this.red + other.red,
        green = this.green + other.green,
        blue = this.blue + other.blue,
    )
}


object UserStorage {
    const val DEFAULT_MAIN_COLOR_LITE = 0x6e6ebadcL
    const val DEFAULT_MAIN_COLOR_DARK = 0x73bb95cbL
    var userInstance: User by SharedPreferenceUtils.StorageJson(
        getJsonStorage("user_info"),
        "user",
        User(),
        serializer = User.serializer(),
        jsonFormat = Json { ignoreUnknownKeys = true },
    )

    private var _userAppThemeConfig: Long by SharedPreferenceUtils.StorageLong(
        getLongStorage("user_config"),
        "theme_config",
        AppTheme.AUTO.value,
    )

    var userAppThemeConfig: AppTheme
        get() = AppTheme.parse(_userAppThemeConfig)
        set(value) {
            _userAppThemeConfig = value.value
        }

    val liteBackgroundConfig = SharedPreferenceUtils.StateFullStorageJson(
        getJsonStorage("user_config"),
        "lite_background_config",
        defaultValue = BackgroundConfig.defaultLiteBackgroundColor,
        serializer = BackgroundConfig.serializer(),
    )

    val darkBackgroundConfig = SharedPreferenceUtils.StateFullStorageJson(
        getJsonStorage("user_config"),
        "dark_background_config",
        defaultValue = BackgroundConfig.defaultDarkBackgroundColor,
        serializer = BackgroundConfig.serializer(),
    )

    val liteMainColor = SharedPreferenceUtils.StateFullStorageLong(
        getLongStorage("user_config"),
        "lite_main_color_config",
        DEFAULT_MAIN_COLOR_LITE
    )

    val darkMainColor = SharedPreferenceUtils.StateFullStorageLong(
        getLongStorage("user_config"),
        "dark_main_color_config",
        DEFAULT_MAIN_COLOR_DARK
    )

    val liteBackgroundConfigState: State<BackgroundConfig> get() = liteBackgroundConfig.state
    val darkBackgroundConfigState: State<BackgroundConfig> get() = darkBackgroundConfig.state

    val liteMainColorState: State<Long> get() = liteMainColor.state
    val darkMainColorState: State<Long> get() = darkMainColor.state

    fun resetColors(isDark: Boolean) {
        if (isDark) {
            var mainColor by darkMainColor
            var backgroundConfig by darkBackgroundConfig
            mainColor = DEFAULT_MAIN_COLOR_DARK
            backgroundConfig = BackgroundConfig.defaultDarkBackgroundColor
        } else {
            var mainColor by liteMainColor
            var backgroundConfig by liteBackgroundConfig
            mainColor = DEFAULT_MAIN_COLOR_LITE
            backgroundConfig = BackgroundConfig.defaultLiteBackgroundColor
        }
    }
}
