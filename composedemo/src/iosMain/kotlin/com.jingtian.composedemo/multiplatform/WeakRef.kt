package com.jingtian.composedemo.multiplatform

import androidx.compose.ui.node.WeakReference
import kotlin.experimental.ExperimentalNativeApi

actual class WeakRef<T : Any> actual constructor(value: T) {
    @OptIn(ExperimentalNativeApi::class)
    private val ref = kotlin.native.ref.WeakReference(value)
    @OptIn(ExperimentalNativeApi::class)
    actual fun get(): T? {
        return ref.get()
    }
}