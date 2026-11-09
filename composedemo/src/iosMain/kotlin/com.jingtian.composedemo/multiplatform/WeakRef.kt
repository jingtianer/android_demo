package com.jingtian.composedemo.multiplatform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import com.jingtian.composedemo.utils.CoroutineUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import platform.Foundation.NSNotificationCenter
import platform.UIKit.UIApplicationDidReceiveMemoryWarningNotification
import platform.darwin.NSObjectProtocol
import kotlin.experimental.ExperimentalNativeApi

actual class WeakRef<T : Any> actual constructor(value: T) {
    @OptIn(ExperimentalNativeApi::class)
    private val ref = kotlin.native.ref.WeakReference(value)
    @OptIn(ExperimentalNativeApi::class)
    actual fun get(): T? {
        return ref.get()
    }
}

actual class SoftRef<T : Any> actual constructor(value: T) {
    companion object {
        init {

        }
    }
    var instance: T? = value
    actual fun get(): T? {
        return instance
    }
}

@Composable
actual fun LowMemorySideEffect(key: Any?, onLowMem: suspend () -> Unit) {
    DisposableEffect(key) {
        val nSObjectProtocol: NSObjectProtocol? = NSNotificationCenter.defaultCenter.addObserverForName(
            UIApplicationDidReceiveMemoryWarningNotification,
            null,
            null,
        ) {
            CoroutineUtils.globalScope.launch(Dispatchers.Main) {
                onLowMem.invoke()
            }
        }
        onDispose {
            nSObjectProtocol?.let {
                NSNotificationCenter.defaultCenter.removeObserver(nSObjectProtocol)
            }
        }
    }
}