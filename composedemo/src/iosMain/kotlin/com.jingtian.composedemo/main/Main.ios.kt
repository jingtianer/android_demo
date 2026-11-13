package com.jingtian.composedemo.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.FloatState
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember

@Composable
actual fun rememberBackProgress(isEnable: Boolean): FloatState {
    return remember { mutableFloatStateOf(0f) }
}