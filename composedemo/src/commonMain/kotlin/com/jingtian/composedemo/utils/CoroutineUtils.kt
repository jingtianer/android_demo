package com.jingtian.composedemo.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


object CoroutineUtils {
    val globalScope = CoroutineScope(Dispatchers.Main + Job())

    fun <T> runIOTask(block: suspend ()->T, onFailure: suspend (t: Throwable)->Unit = {}, callback: suspend (T) -> Unit = {}): Job {
        return globalScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    block()
                }
            }.onSuccess { ret->
                withContext(Dispatchers.Main) {
                    callback.invoke(ret)
                }
            }.onFailure { e->
                withContext(Dispatchers.Main) {
                    onFailure(e)
                    throw e
                }
            }
        }
    }
}