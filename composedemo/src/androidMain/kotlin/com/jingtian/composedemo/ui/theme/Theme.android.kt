package com.jingtian.composedemo.ui.theme

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jingtian.composedemo.R
import com.jingtian.composedemo.utils.AppTheme
import com.jingtian.composedemo.viewmodels.AppThemeViewModel
import kotlin.math.roundToInt

private fun Modifier.appBackground(context: Context, @DrawableRes resource: Int, widthScale: Float = 1f, heightScale : Float = 1-goldenRatio): Modifier {
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
actual fun Modifier.appBackground(heightScale: Float): Modifier {
    val viewModel: AppThemeViewModel = viewModel(factory = AppThemeViewModel.viewModelFactory)
    val currentTheme by remember { viewModel.currentAppTheme }
    val isSystemDark = isSystemInDarkTheme()
    return appBackground(LocalContext.current, if (AppTheme.isDark(currentTheme, isSystemDark)) R.drawable.rectangle_16_night else R.drawable.rectangle_16, heightScale = heightScale)
}
