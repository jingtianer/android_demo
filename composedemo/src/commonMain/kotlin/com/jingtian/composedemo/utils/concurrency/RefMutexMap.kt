package com.jingtian.composedemo.utils.concurrency

import androidx.annotation.IntRange
import kotlinx.atomicfu.AtomicArray
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.atomicArrayOfNulls
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.yield
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class RefMutexMap<K: Any>(capacity: Int = 8) {
    class LockRef {
        val mutex = Mutex()
        var ref = 0
    }

    val lockMap = ConcurrentHashMap<K, LockRef>(capacity)

    @OptIn(ExperimentalContracts::class)
    suspend inline fun <T> withPermit(key: K, block: () -> T): T {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }
        val lock = lockMap.getOrPut(key) { cur ->
            (cur ?: LockRef()).also { it.ref++ }
        }
        return try {
            lock.mutex.withLock(action = block)
        } finally {
            lockMap.getOrCompute(key) { current->
                current?.also {
                    it.ref--
                }?.takeIf { it.ref > 0 }
            }
        }
    }
}

class ConcurrentHashMap<K: Any, V>(@IntRange(1, 31) capacity: Int) {
    val size = 2 shl capacity
    val mask = size - 1
    var values = atomicArrayOfNulls<Node<K, V>?>(size)

    companion object {
        private const val MAX_CAS_RETRY_COUNT = 5
    }
    class Node<K, V> {
        val lock: Mutex = Mutex()
        val list = hashMapOf<K, V>()
    }

    private fun hash(k: K): Int {
        val h = k.hashCode()
        return (h xor (h ushr 16)) and mask
    }

    private suspend fun getNode(k: K): Node<K, V> {
        val idx = hash(k)
        val slot = values[idx]
        val node = slot.value
        if (node != null) {
            return node
        }
        val newNode = Node<K, V>()
        var retryCount = MAX_CAS_RETRY_COUNT
        while (true) {
            val node = slot.value
            if (node != null) {
                return node
            }
            if (slot.compareAndSet(null, newNode)) {
                return newNode
            }
            retryCount--
            if (retryCount > 0) {
                continue
            }
            retryCount = MAX_CAS_RETRY_COUNT
            yield()
        }
    }

    suspend fun get(k: K): V? {
        val node = getNode(k)
        return node.lock.withLock {
            node.list[k]
        }
    }

    suspend fun set(k: K, v: V) {
        val node = getNode(k)
        node.lock.withLock {
            node.list[k] = v
        }
    }

    suspend fun getOrPut(k: K, value: (V?) -> V): V {
        val node = getNode(k)
        return node.lock.withLock {
            value(node.list[k]).also {
                node.list[k] = it
            }
        }
    }

    suspend fun getOrCompute(k: K, block: (V?) -> V?): V? {
        val node = getNode(k)
        return node.lock.withLock {
            val current = node.list[k]
            val next = block(current)
            if (next == null) {
                node.list.remove(k)
            } else {
                node.list[k] = next
            }
            next
        }
    }

    suspend fun remove(k: K): V? {
        val node = values[hash(k)].value ?: return null
        return node.lock.withLock {
            node.list.remove(k)
        }
    }

    fun dropOldMap() {
        values = atomicArrayOfNulls(size)
    }
}