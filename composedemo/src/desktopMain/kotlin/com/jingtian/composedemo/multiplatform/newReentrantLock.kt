package com.jingtian.composedemo.multiplatform

import java.util.concurrent.locks.ReentrantLock
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

actual fun newReentrantLock(): IMultiplatformReentrantLock = MultiplatformReentrantLock()

class MultiplatformReentrantLock : IMultiplatformReentrantLock{
    private val lock = ReentrantLock()
    override fun lock() {
        lock.lock()
    }

    override fun unlock() {
        lock.unlock()
    }
}