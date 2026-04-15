package com.jingtian.composedemo.multiplatform

import androidx.compose.ui.node.WeakReference

actual class WeakRef<T : Any> actual constructor(value: T) {
    private val ref = WeakReference(value)
    actual fun get(): T? {
        return ref.get()
    }
}