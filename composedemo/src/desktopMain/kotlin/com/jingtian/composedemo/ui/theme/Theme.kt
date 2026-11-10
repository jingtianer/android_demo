package com.jingtian.composedemo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jingtian.composedemo.viewmodels.AppThemeViewModel
import com.jingtian.composedemo.viewmodels.appThemeViewModel


@Composable
actual fun Modifier.appBackgroundV1(heightScale: Float): Modifier {
    val viewModel: AppThemeViewModel = appThemeViewModel()
    val currentTheme by remember { viewModel.currentAppTheme }
    val isSystemDark = isSystemInDarkTheme()
    return this
}

data class DesktopConst(
    val screenWidthDp: Dp = 0.dp,
    val screenHeightDp: Dp = 0.dp,
)

val LocalDesktopConst = compositionLocalOf(structuralEqualityPolicy()) { DesktopConst() }
