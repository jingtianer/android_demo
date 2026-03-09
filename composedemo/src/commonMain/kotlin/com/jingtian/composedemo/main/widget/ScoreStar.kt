package com.jingtian.composedemo.main.widget

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp

@Composable
expect fun ColumnScope.ScoreStar(itemScore: Float, padding: Dp)