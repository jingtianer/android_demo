package com.jingtian.composedemo.viewmodels

import com.jingtian.composedemo.utils.CoroutineUtils
import kotlinx.coroutines.launch

abstract class SingletonViewModel {
    val viewModelScope get() = CoroutineUtils.globalScope

    protected fun launch(task : suspend () -> Unit) {
        viewModelScope.launch {
            task()
        }
    }
}