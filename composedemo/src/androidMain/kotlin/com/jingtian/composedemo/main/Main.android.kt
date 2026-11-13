package com.jingtian.composedemo.main

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.jingtian.composedemo.multiplatform.logD
import com.jingtian.composedemo.viewmodels.authViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.CancellationException

@Composable
actual fun rememberBackProgress(isEnable: Boolean): MutableFloatState {
    val backProgress = remember { mutableFloatStateOf(0f) }
    var enable by remember(isEnable) { mutableStateOf(isEnable) }
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val externalScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleObserver = remember {
        object : DefaultLifecycleObserver {
            private fun resetState() {
                enable = isEnable
                backProgress.value = 0f
            }
            override fun onStart(owner: LifecycleOwner) {
                resetState()
            }

            override fun onResume(owner: LifecycleOwner) {
                resetState()
            }
        }
    }
    DisposableEffect(Unit) {
        lifecycleOwner.lifecycle.addObserver(observer = lifecycleObserver)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer = lifecycleObserver)
        }
    }
    PredictiveBackHandler(enabled = enable) { backFlow ->
        try {
            backFlow.collect { event->
                backProgress.value = event.progress
            }
            backProgress.value = 1f
            enable = false
            withFrameNanos { }
            externalScope.launch(Dispatchers.Main) {
                backDispatcher?.onBackPressed()
//                authViewModel().lock()
            }
        } catch (e: CancellationException) {
            backProgress.value = 0f
            enable = true
            throw e
        } finally {

        }
    }
    return backProgress
}