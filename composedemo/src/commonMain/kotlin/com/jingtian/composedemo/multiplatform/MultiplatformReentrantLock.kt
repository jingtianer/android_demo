package com.jingtian.composedemo.multiplatform

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

expect fun newReentrantLock(): IMultiplatformReentrantLock

interface IMultiplatformReentrantLock {
    fun lock()

    fun unlock()
}

@OptIn(ExperimentalContracts::class)
inline fun <R> IMultiplatformReentrantLock.use(block: ()->R): R {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    return try {
        this.lock()
        block()
    } finally {
        this.unlock()
    }
}