package com.jingtian.composedemo.ui.theme

import android.os.Build
import android.view.RoundedCorner
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
actual fun rememberDeviceCornerRadii(): Shape {
    val view = LocalView.current
    return remember {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return@remember RoundedCornerShape(0.dp)
        }
        val insets = view.rootWindowInsets ?: return@remember RoundedCornerShape(0.dp)
        val density = view.resources.displayMetrics.density

        fun getRadius(pos: Int): Dp {
            val rc: RoundedCorner? = insets.getRoundedCorner(pos)
            val px = rc?.radius ?: 0
            return (px / density).dp
        }

        RoundedCornerShape(
            topStart = getRadius(RoundedCorner.POSITION_TOP_LEFT),
            topEnd = getRadius(RoundedCorner.POSITION_TOP_RIGHT),
            bottomStart = getRadius(RoundedCorner.POSITION_BOTTOM_LEFT),
            bottomEnd = getRadius(RoundedCorner.POSITION_BOTTOM_RIGHT),
        )
    }
}