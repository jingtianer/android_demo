package com.jingtian.composedemo.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,

)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

data class AppUIConstants(
    val drawerMaxPercent: Float = 0.8f,
    val dialogPercent: Float = 0.9f,
)

class AppPalette(
    val drawerBg: Color,
    val dialogBg: Color,
    val labelChecked: Color,
    val labelUnChecked: Color,
    val dividerColor: Color,
    val cardBg: Color,
    val galleryCardBg: Color,
    val strokeColor: Color,
)

private val darkAppPalette = AppPalette(
    drawerBg = color282828,
    dialogBg = color282828,
    labelChecked = Purple40,
    labelUnChecked = color484848,
    dividerColor = colorA8A8A8,
    cardBg = color686868,
    galleryCardBg = color282828,
    strokeColor = color484848
)
private val liteAppPalette = AppPalette(
    drawerBg = colorA8A8A8,
    dialogBg = colorA8A8A8,
    labelChecked = Purple80,
    labelUnChecked = colorA8A8A8,
    dividerColor = color282828,
    cardBg = colorC8c8c8,
    galleryCardBg = colorC8c8c8_a0,
    strokeColor = color686868,
)

val LocalAppPalette = compositionLocalOf(structuralEqualityPolicy()) { liteAppPalette }
val LocalAppUIConstants = compositionLocalOf(structuralEqualityPolicy()) { AppUIConstants() }
val LocalSecondaryTextStyle = compositionLocalOf(structuralEqualityPolicy()) { TextStyle.Default }

@Composable
fun DemoAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val customTextStyle = LocalTextStyle.current.copy(
        color = if (darkTheme) colorF8f8f8 else colorBlack
    )
    val customSecondaryTextStyle = LocalTextStyle.current.copy(
        color = if (darkTheme) colorA8A8A8 else color282828,
        fontStyle = FontStyle.Italic
    )
    val appPalette = if (darkTheme) {
        darkAppPalette
    } else {
        liteAppPalette
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = {
            CompositionLocalProvider(
                LocalTextStyle provides customTextStyle,
                LocalAppPalette provides appPalette,
                LocalAppUIConstants provides AppUIConstants(),
                LocalContentColor provides Color.Unspecified,
                LocalSecondaryTextStyle provides customSecondaryTextStyle,
                content = content
            )
        }
    )
}