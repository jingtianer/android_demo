package com.jingtian.composedemo.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

@Composable
fun Dp.dpValue(): Float = with(LocalDensity.current) {
    this@dpValue.roundToPx().toFloat()
}