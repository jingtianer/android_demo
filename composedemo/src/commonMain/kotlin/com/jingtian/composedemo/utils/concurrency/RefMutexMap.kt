package com.jingtian.composedemo.utils.concurrency

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class RefMutexMap<K> {
    class LockRef {
        val mutex = Mutex()
        var ref = 0
    }
    val lockMap = mutableMapOf<K, LockRef>()
    val mapLock = Mutex()

    @OptIn(ExperimentalContracts::class)
    suspend inline fun <T> withPermit(key: K, block: () -> T): T {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }
        val lock = mapLock.withLock {
            lockMap.getOrPut(key) { LockRef() }.also {
                it.ref++
            }
        }
        return try {
            lock.mutex.withLock(action = block)
        } finally {
            mapLock.withLock {
                lock.ref--
                if (lock.ref <= 0) {
                    lockMap.remove(key)
                }
            }
        }
    }
}