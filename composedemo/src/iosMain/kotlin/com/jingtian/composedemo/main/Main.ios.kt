package com.jingtian.composedemo.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

@Composable
actual fun rememberNativeBackInvoker(): (suspend () -> Unit) {
    return remember { {} }
}