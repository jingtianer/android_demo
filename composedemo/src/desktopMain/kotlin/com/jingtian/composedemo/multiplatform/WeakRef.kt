package com.jingtian.composedemo.multiplatform

import java.lang.ref.SoftReference
import java.lang.ref.WeakReference

actual class WeakRef<T : Any> actual constructor(value: T) : WeakReference<T>(value)
actual class SoftRef<T : Any> actual constructor(value: T) : SoftReference<T>(value)
