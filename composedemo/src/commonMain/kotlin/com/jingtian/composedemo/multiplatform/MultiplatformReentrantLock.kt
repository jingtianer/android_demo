package com.jingtian.composedemo.multiplatform

import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.internal.SynchronizedObject
import kotlinx.coroutines.internal.synchronized
import kotlinx.coroutines.sync.Mutex
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

fun newReentrantLock(): IMultiplatformReentrantLock = IMultiplatformReentrantLock()

class IMultiplatformReentrantLock {
    private val mutex = Mutex()
    private var ownerContext: CoroutineContext? = null
    private var holdCount = 0

    /**
     * 加锁：可重入，挂起等待不阻塞线程
     */
    suspend fun lock() {
        val currentContext = coroutineContext
        // 同一协程：重入，计数+1
        if (ownerContext === currentContext) {
            holdCount++
            return
        }
        // 不同协程：正常抢占锁
        mutex.lock()
        ownerContext = currentContext
        holdCount = 1
    }

    /**
     * 解锁：必须和 lock 成对调用
     */
    suspend fun unlock() {
        val currentContext = coroutineContext
        check(ownerContext === currentContext) {
            "当前协程未持有锁，非法解锁（对应 Java IllegalMonitorStateException）"
        }
        holdCount--
        if (holdCount <= 0) {
            ownerContext = null
            mutex.unlock()
        }
    }
    suspend fun isHeldByCurrentCoroutine(): Boolean {
        return ownerContext === coroutineContext
    }
}

@OptIn(ExperimentalContracts::class, InternalCoroutinesApi::class)
suspend inline fun <R> IMultiplatformReentrantLock.use(block: ()->R): R {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    lock()
    return try {
        block()
    } finally {
        unlock()
    }
}