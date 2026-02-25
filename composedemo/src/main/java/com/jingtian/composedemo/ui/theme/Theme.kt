package com.jingtian.composedemo.ui.theme

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import com.jingtian.composedemo.R
import kotlin.math.roundToInt
import kotlin.math.sqrt

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xff09476F),
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = Color(red = 28, green = 27, blue = 31),

)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xaf32c4c0),
    secondary = PurpleGrey40,
    tertiary = Pink40,
    background = Color(red = 255, green = 251, blue = 254),

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
    val filterLabelHeight: Dp = 38.dp,
    val filterLabelAspectRatio: Float = 1 / (1 - goldenRatio),
    val filterLabelPaddings: List<Dp> = listOf(8.dp, 0.dp, 4.dp, 4.dp)
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
    val bottomSheetBackgroundColor: Color,
    val labelTextColor: Color,
    val deleteButtonColor: Color,
    val galleryHeaderColor: Color,
)

private val darkAppPalette = AppPalette(
    drawerBg = Color(red = 28, green = 27, blue = 31),
    dialogBg = Color(red = 28, green = 27, blue = 31),
    labelChecked = Color(0x663B79A2),
    labelUnChecked = colorD8D8D8_2f,
    dividerColor = colorA8A8A8,
//    cardBg = colorA8A8A8_7f,
    cardBg = Color.Transparent,
    galleryCardBg = colorD8D8D8_2f,
    strokeColor = Color(0x3fffffff),
    bottomSheetBackgroundColor = Color(red = 28, green = 27, blue = 31),
    labelTextColor = colorA8A8A8,
    deleteButtonColor = Color(0xffFF5959),
    galleryHeaderColor = colorF8f8f8,
)
private val liteAppPalette = AppPalette(
    drawerBg = Color(red = 255, green = 251, blue = 254),
    dialogBg = Color(red = 255, green = 251, blue = 254),
    labelChecked = Color(0x664CB9A1),
//    labelChecked = Color(0x4f90CCE5),
    labelUnChecked = Color(0x1A3F3C3C),
//    labelUnChecked = colorA8A8A8_7f,
    dividerColor = color282828,
//    cardBg = colorA8A8A8_7f,
    cardBg = Color.Transparent,
//    galleryCardBg = colorA8A8A8_7f,
    galleryCardBg = Color(0x1A3C3636),
    strokeColor = Color(0x3fffffff),
    bottomSheetBackgroundColor = Color(red = 255, green = 251, blue = 254),
    labelTextColor = color686868,
    deleteButtonColor = Color(0xffFF5959),
    galleryHeaderColor = Color(0xFF272727),
)


val darkButtonColors = ButtonColors(
    containerColor = Purple80,
    contentColor = colorC8c8c8,
    disabledContainerColor = colorC8c8c8,
    disabledContentColor = colorF8f8f8
)
val liteButtonColors = ButtonColors(
    containerColor = Purple40,
    contentColor = colorBlack,
    disabledContainerColor = color686868,
    disabledContentColor = colorF8f8f8,
)
data class MiddleButtonConfig(
    val colors: ButtonColors,
    val text: String = "",
)

private val darkMiddleButtonConfig = MiddleButtonConfig(
    colors = darkButtonColors
)
private val liteMiddleButtonConfig = MiddleButtonConfig(
    colors = liteButtonColors
)

val LocalAppPalette = compositionLocalOf(structuralEqualityPolicy()) { liteAppPalette }
val LocalAppUIConstants = compositionLocalOf(structuralEqualityPolicy()) { AppUIConstants() }
val LocalSecondaryTextStyle = compositionLocalOf(structuralEqualityPolicy()) { TextStyle.Default }
val LocalMiddleButtonConfig = compositionLocalOf(structuralEqualityPolicy()) { liteMiddleButtonConfig }
val LocalAppColorScheme = compositionLocalOf(structuralEqualityPolicy()) { LightColorScheme }

val goldenRatio = 2f / (sqrt(5f) + 1)

fun Modifier.appBackground(context: Context, @DrawableRes resource: Int, widthScale: Float = 1f, heightScale : Float = 1-goldenRatio): Modifier {
    val bg = ResourcesCompat.getDrawable(context.resources, resource, context.theme) ?: return this
    val paint = Paint()
    return this.drawBehind {
        val width = this.size.width * widthScale
        val height = this.size.height * heightScale
        val bitmap = bg.toBitmap(width = width.roundToInt(), height = height.roundToInt()).asImageBitmap()
        drawIntoCanvas { canvas->
            canvas.drawImage(bitmap, Offset(0f, 0f), paint)
        }
    }
}
@Composable
fun Modifier.appBackground() = appBackground(LocalContext.current, R.drawable.rectangle_16)
@Composable
fun Modifier.drawerBackground() = appBackground(LocalContext.current, R.drawable.rectangle_16, heightScale = 1f)
@Composable
fun Modifier.dialogBackground() = appBackground(LocalContext.current, R.drawable.rectangle_16, heightScale = goldenRatio)

@Composable
fun DemoAppTheme(
    systemDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        systemDarkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val customTextStyle = LocalTextStyle.current.copy(
        color = if (systemDarkTheme) colorF8f8f8 else colorBlack
    )
    val customSecondaryTextStyle = LocalTextStyle.current.copy(
        color = if (systemDarkTheme) colorA8A8A8 else color686868,
        fontStyle = FontStyle.Italic
    )
    val appPalette = if (systemDarkTheme) {
        darkAppPalette
    } else {
        liteAppPalette
    }
    val middleButtonConfig = if (systemDarkTheme) {
        darkMiddleButtonConfig
    } else {
        liteMiddleButtonConfig
    }
    val contentColor = if (systemDarkTheme) {
        Color.Black
    } else {
        Color.White
    }


    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = {
            CompositionLocalProvider(
                LocalAppColorScheme provides colorScheme,
                LocalTextStyle provides customTextStyle,
                LocalAppPalette provides appPalette,
                LocalAppUIConstants provides AppUIConstants(),
                LocalContentColor provides contentColor,
                LocalSecondaryTextStyle provides customSecondaryTextStyle,
                LocalMiddleButtonConfig provides middleButtonConfig,
                content = { content() }
            )
        }
    )
}