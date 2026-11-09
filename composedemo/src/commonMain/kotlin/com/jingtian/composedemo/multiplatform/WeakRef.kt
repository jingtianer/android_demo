package com.jingtian.composedemo.multiplatform

expect class WeakRef<T : Any>(value: T) {
    fun get(): T?
}

expect class SoftRef<T : Any>(value: T) {
    fun get(): T?
}