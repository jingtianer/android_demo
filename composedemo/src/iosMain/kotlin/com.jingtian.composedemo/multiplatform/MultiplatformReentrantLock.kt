package com.jingtian.composedemo.multiplatform

import platform.Foundation.NSRecursiveLock

actual fun newReentrantLock(): IMultiplatformReentrantLock = MultiplatformReentrantLock()

class MultiplatformReentrantLock : IMultiplatformReentrantLock{
    private val lock = NSRecursiveLock()

    override fun <R> use(block: ()->R): R {
        return try {
            lock.lock()
            block()
        } finally {
            lock.unlock()
        }
    }
}