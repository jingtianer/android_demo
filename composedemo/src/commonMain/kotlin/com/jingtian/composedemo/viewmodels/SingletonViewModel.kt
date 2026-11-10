package com.jingtian.composedemo.viewmodels

import com.jingtian.composedemo.utils.CoroutineUtils

abstract class SingletonViewModel {
    val viewModelScope get() = CoroutineUtils.globalScope
}