package com.jingtian.composedemo.main

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeUIViewController
import com.jingtian.composedemo.BuildKonfig
import com.jingtian.composedemo.launch.LaunchTasks
import com.jingtian.composedemo.ui.theme.DemoAppTheme
import platform.CoreGraphics.CGFloat

fun screenRoundedCornerInsets(start: CGFloat, top: CGFloat, end: CGFloat, bottom: CGFloat): Float {
    val radius = minOf(start, top, end, bottom)
    return radius.toFloat()
}

val LocalScreenRoundedCornerInsets = compositionLocalOf<Shape>(structuralEqualityPolicy()) { RoundedCornerShape(0.dp) }

fun MainViewController(screenRoundedCornerInsets: Float) = ComposeUIViewController {
    CompositionLocalProvider(LocalScreenRoundedCornerInsets provides RoundedCornerShape(screenRoundedCornerInsets.dp)) {
        if (BuildKonfig.isDemo) {
            Main()
            return@CompositionLocalProvider
        }
        LaunchTasks.onLaunch()
        DemoAppTheme {
            PasswordGuardedMain()
        }
    }
}