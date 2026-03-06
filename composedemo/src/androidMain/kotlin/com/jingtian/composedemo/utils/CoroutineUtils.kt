package com.jingtian.composedemo.utils

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.LinkedList
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.coroutines.CoroutineContext


object CoroutineUtils {
    private val globalScope = CoroutineScope(Dispatchers.Main + Job())

    class LowPriorityThreadFactory : ThreadFactory {
        private val group = ThreadGroup("LowPriorityGroup")

        override fun newThread(r: Runnable): Thread {
            val thread = Thread(group, r)
            thread.priority = Thread.MIN_PRIORITY
            thread.isDaemon = true
            return thread
        }
    }

    private val lowPriorityDispatcher: CoroutineDispatcher = Executors.newFixedThreadPool(2, LowPriorityThreadFactory())
        .asCoroutineDispatcher()

    fun <T> runIOTaskLowPriority(block: suspend ()->T, callback: (T) -> Unit = {}): Job {
        return globalScope.launch {
            val ret = withContext(lowPriorityDispatcher) {
                block()
            }
            withContext(Dispatchers.Main) {
                callback.invoke(ret)
            }
        }
    }

    class CoroutineTaskFailException : Exception()

    fun <T> runIOTask(block: suspend ()->T, onFailure: suspend (t: Throwable)->Unit = {}, callback: suspend (T) -> Unit = {}): Job {
        return globalScope.launch {
            try {
                val ret = try {
                    withContext(Dispatchers.IO) {
                        block()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        onFailure(e)
                    }
                    throw CoroutineTaskFailException()
                }
                withContext(Dispatchers.Main) {
                    callback.invoke(ret)
                }
            } catch (_: CoroutineTaskFailException) {

            }
        }
    }

    class Producer<K, T>(private val capacity: Int, private var taskCnt: Int) {
        private val lock: ReentrantLock = ReentrantLock()

        private val notEmpty: Condition = lock.newCondition()

        private val notFull: Condition = lock.newCondition()

        private val queue = LinkedList<Pair<K, T>>()

        private fun notAllComplete(): Boolean {
            return taskCnt > 0
        }

        fun complete() {
            lock.lock()
            try {
                taskCnt--
//                    Log.d("Producer", "complete, $taskCnt")
                if (!notAllComplete()) {
//                        Log.d("Producer", "complete, notify all $taskCnt")
                    notEmpty.signalAll()
                }
            } finally {
                lock.unlock()
            }
        }

        fun poll(): Pair<K, T>? {
            lock.lock()
            var get: Pair<K, T>?
            try {
                get = queue.pollFirst()
                while (get == null) {
                    if (!notAllComplete()) {
                        break
                    }
                    notEmpty.await()
                    get = queue.pollFirst()
                }
                notFull.signal()
//                    Log.d("Producer", "poll: $get")
            } finally {
                lock.unlock()
            }
            return get
        }

        fun offer(k: K, t: T) {
            lock.lock()
            try {
                var size = queue.size
                while (size >= capacity) {
                    notFull.await()
                    size = queue.size
                }
//                    Log.d("Producer", "offer: $k, $t")
                queue.addLast(k to t)
                notEmpty.signal()
            } finally {
                lock.unlock()
            }
        }

        class ProducerBuilder<K, T>(private val capacity: Int) {
            private val taskList = mutableListOf<Pair<CoroutineContext, (Producer<K, T>) -> Unit>>()
            fun producer(
                context: CoroutineContext,
                block: (Producer<K, T>) -> Unit
            ): ProducerBuilder<K, T> {
                taskList.add(context to block)
                return this
            }

            fun ioProducer(
                block: (Producer<K, T>) -> Unit
            ): ProducerBuilder<K, T> {
                return producer(Dispatchers.IO, block)
            }

            fun build() : Producer<K, T> {
                val producer = Producer<K, T>(capacity, taskList.size)
                for ((context, block) in taskList) {
                    globalScope.launch {
                        withContext(context) {
                            block(producer)
                        }
                    }
                }
                taskList.clear()
                return producer
            }
        }
    }

    fun <K, T> buildProducer(capacity : Int = 1024) = Producer.ProducerBuilder<K, T>(capacity)

    fun LifecycleOwner.activityLifecycleLaunch(
        context: CoroutineContext = globalScope.coroutineContext,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend CoroutineScope.() -> Unit
    ) {
        this.lifecycleScope.launch(context, start, block)
    }
}