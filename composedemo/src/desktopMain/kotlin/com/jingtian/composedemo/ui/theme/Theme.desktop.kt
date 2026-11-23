package com.jingtian.composedemo.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.unit.Dp


data class DesktopConst(
    val screenWidthDp: Dp = 0.dp,
    val screenHeightDp: Dp = 0.dp,
)

val LocalDesktopConst = compositionLocalOf(structuralEqualityPolicy()) { DesktopConst() }

@Composable
actual fun rememberDeviceCornerRadii(): Shape {
    return remember { RoundedCornerShape(0.dp) }
}