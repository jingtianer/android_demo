package com.jingtian.composedemo.multiplatform

import androidx.compose.runtime.Composable
import java.lang.ref.SoftReference
import java.lang.ref.WeakReference

actual class WeakRef<T : Any> actual constructor(value: T) : WeakReference<T>(value)
actual class SoftRef<T : Any> actual constructor(value: T) : SoftReference<T>(value)
@Composable
actual fun LowMemorySideEffect(key: Any?, onLowMem: suspend () -> Unit) {}