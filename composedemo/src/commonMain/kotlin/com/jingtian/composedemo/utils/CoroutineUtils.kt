package com.jingtian.composedemo.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


object CoroutineUtils {
    val globalScope = CoroutineScope(Dispatchers.Main + Job())

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
}