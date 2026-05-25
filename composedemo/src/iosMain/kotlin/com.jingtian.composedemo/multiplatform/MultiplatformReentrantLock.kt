package com.jingtian.composedemo.multiplatform

import platform.Foundation.NSRecursiveLock
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

actual fun newReentrantLock(): IMultiplatformReentrantLock = MultiplatformReentrantLock()

class MultiplatformReentrantLock : IMultiplatformReentrantLock{
    private val lock = NSRecursiveLock()
    override fun lock() {
        lock.lock()
    }

    override fun unlock() {
        lock.unlock()
    }
}
