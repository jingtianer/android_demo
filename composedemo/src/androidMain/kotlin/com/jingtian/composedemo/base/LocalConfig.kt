package com.jingtian.composedemo.base

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

@Composable
actual fun screenWidth(): Int = LocalConfiguration.current.screenWidthDp

@Composable
actual fun screenHeight(): Int = LocalConfiguration.current.screenHeightDp