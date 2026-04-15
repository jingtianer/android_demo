package com.jingtian.composedemo.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import platform.UIKit.UIScreen

@Composable
actual fun Dp.dpValue(): Float = this.value * getPhysicalDpi()

@Composable
fun getPhysicalDpi(): Float {
    val systemScaledDpi = UIScreen.mainScreen.scale.toFloat() // e.g., 96 or 120
    val displayScalingFactor = LocalDensity.current.density // e.g., 1.0, 1.25, 1.5, 2.0
    // The actual physical DPI is approximated by this calculation
    val physicalDpi = displayScalingFactor * systemScaledDpi
    return physicalDpi
}