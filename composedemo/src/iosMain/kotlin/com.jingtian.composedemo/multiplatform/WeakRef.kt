package com.jingtian.composedemo.multiplatform

import kotlin.experimental.ExperimentalNativeApi

actual class WeakRef<T : Any> actual constructor(value: T) {
    @OptIn(ExperimentalNativeApi::class)
    private val ref = kotlin.native.ref.WeakReference(value)
    @OptIn(ExperimentalNativeApi::class)
    actual fun get(): T? {
        return ref.get()
    }
}

actual class SoftRef<T : Any> actual constructor(val value: T) {
    actual fun get(): T? {
        return value
    }
}
