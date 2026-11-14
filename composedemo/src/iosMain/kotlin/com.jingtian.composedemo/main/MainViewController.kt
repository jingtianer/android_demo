package com.jingtian.composedemo.main

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeUIViewController
import com.jingtian.composedemo.BuildKonfig
import com.jingtian.composedemo.launch.LaunchTasks
import com.jingtian.composedemo.ui.theme.DemoAppTheme
import platform.CoreGraphics.CGFloat


// px
class ScreenRoundedCornerInsets(val topStart: Float = 0f, val topEnd: Float = 0f, val bottomStart: Float = 0f, val bottomEnd: Float = 0f)

private val _currentScreenRoundedCornerInsets = mutableStateOf(ScreenRoundedCornerInsets())

val currentScreenRoundedCornerInsets by _currentScreenRoundedCornerInsets

fun screenRoundedCornerInsets(topStart: CGFloat, topEnd: CGFloat, bottomStart: CGFloat, bottomEnd: CGFloat) {
    _currentScreenRoundedCornerInsets.value = ScreenRoundedCornerInsets(topStart.toFloat(), topEnd.toFloat(), bottomStart.toFloat(), bottomEnd.toFloat())
}

fun MainViewController() = ComposeUIViewController {
    if (BuildKonfig.isDemo) {
        Main()
        return@ComposeUIViewController
    }
    LaunchTasks.onLaunch()
    DemoAppTheme {
        PasswordGuardedMain()
    }
}