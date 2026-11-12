package com.jingtian.composedemo.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember

@Composable
actual fun rememberBackProgress(): MutableFloatState {
    return remember { mutableFloatStateOf(0f) }
}