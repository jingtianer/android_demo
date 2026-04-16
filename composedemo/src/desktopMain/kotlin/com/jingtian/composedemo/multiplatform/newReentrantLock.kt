package com.jingtian.composedemo.multiplatform

import java.util.concurrent.locks.ReentrantLock

actual fun newReentrantLock(): IMultiplatformReentrantLock = MultiplatformReentrantLock()

class MultiplatformReentrantLock : IMultiplatformReentrantLock{
    private val lock = ReentrantLock()

    override fun <R> use(block: ()->R): R {
        return try {
            lock.lock()
            block()
        } finally {
            lock.unlock()
        }
    }
}