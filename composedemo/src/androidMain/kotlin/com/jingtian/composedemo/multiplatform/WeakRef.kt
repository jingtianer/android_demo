package com.jingtian.composedemo.multiplatform

import java.lang.ref.WeakReference

actual class WeakRef<T : Any> actual constructor(value: T) : WeakReference<T>(value)