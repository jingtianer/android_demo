package com.jingtian.composedemo.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Composable
actual fun rememberDeviceCornerRadii(): Shape {
    return remember { RoundedCornerShape(0.dp) }
}