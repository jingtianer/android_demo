package com.jingtian.composedemo.main.widget

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable

@Composable
actual fun ColumnScope.ScoreChooser(itemScore: Float, onScoreUpdate: (Float)->Unit) {
    DesktopStarRateView(enable = true, initialScore = itemScore, onScoreChange = onScoreUpdate)
}