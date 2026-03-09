package com.jingtian.composedemo.utils

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.jingtian.composedemo.utils.CoroutineUtils.globalScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext


object CoroutineUtilsAndroid {

    fun LifecycleOwner.activityLifecycleLaunch(
        context: CoroutineContext = globalScope.coroutineContext,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend CoroutineScope.() -> Unit
    ) {
        this.lifecycleScope.launch(context, start, block)
    }
}