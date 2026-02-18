package com.jingtian.composedemo.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer

@Composable
inline fun <T> MutableLiveData<T>.composeObserve(crossinline onUpdate: (T?)->Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = Observer<T> {
            onUpdate(it)
        }
        this@composeObserve.observe(lifecycleOwner, observer)
        onDispose {
            this@composeObserve.removeObserver(observer)
        }
    }
}