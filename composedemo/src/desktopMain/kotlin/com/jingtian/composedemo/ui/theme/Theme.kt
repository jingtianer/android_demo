package com.jingtian.composedemo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jingtian.composedemo.utils.AppTheme
import com.jingtian.composedemo.viewmodels.AppThemeViewModel


@Composable
actual fun Modifier.appBackground(heightScale: Float): Modifier {
    val viewModel: AppThemeViewModel = viewModel(factory = AppThemeViewModel.viewModelFactory)
    val currentTheme by remember { viewModel.currentAppTheme }
    val isSystemDark = isSystemInDarkTheme()
    return this
}

data class DesktopConst(
    val screenWidthDp: Dp = 800.dp,
    val screenHeightDp: Dp = 600.dp,
)

val LocalDesktopConst = compositionLocalOf(structuralEqualityPolicy()) { DesktopConst() }
