package com.jingtian.composedemo.base

import androidx.compose.runtime.Composable
import com.jingtian.composedemo.ui.theme.LocalDesktopConst

@Composable
actual fun screenWidth(): Int {
    return LocalDesktopConst.current.screenWidthDp.value.toInt()
}

@Composable
actual fun screenHeight(): Int {
    return LocalDesktopConst.current.screenWidthDp.value.toInt()
}