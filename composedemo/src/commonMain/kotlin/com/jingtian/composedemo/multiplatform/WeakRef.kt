package com.jingtian.composedemo.multiplatform

import androidx.compose.runtime.Composable

expect class WeakRef<T : Any>(value: T) {
    fun get(): T?
}

expect class SoftRef<T : Any>(value: T) {
    fun get(): T?
}

@Composable
expect fun LowMemorySideEffect(key: Any? = Unit, onLowMem: suspend () -> Unit)