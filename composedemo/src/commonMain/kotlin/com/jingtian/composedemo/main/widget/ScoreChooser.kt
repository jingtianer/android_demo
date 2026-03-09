package com.jingtian.composedemo.main.widget

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable

@Composable
expect fun ColumnScope.ScoreChooser(itemScore: Float, onScoreUpdate: (Float)->Unit)