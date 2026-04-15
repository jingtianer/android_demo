package com.jingtian.composedemo.base

import androidx.compose.runtime.Composable
import com.jingtian.composedemo.ui.theme.LocalDesktopConst
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.UIKit.UIScreen

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun screenWidth(): Int {
    return UIScreen.mainScreen.bounds.useContents { size.width }.toInt()
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun screenHeight(): Int {
    return UIScreen.mainScreen.bounds.useContents { size.height }.toInt()
}