package com.jingtian.composedemo.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Shape
import com.jingtian.composedemo.main.currentScreenRoundedCornerInsets

@Composable
actual fun rememberDeviceCornerRadii(): Shape {
    return RoundedCornerShape(
        topStart = currentScreenRoundedCornerInsets.topStart,
        topEnd = currentScreenRoundedCornerInsets.topEnd,
        bottomStart = currentScreenRoundedCornerInsets.bottomStart,
        bottomEnd = currentScreenRoundedCornerInsets.bottomEnd,
    )
}