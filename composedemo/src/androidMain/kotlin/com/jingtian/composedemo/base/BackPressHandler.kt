package com.jingtian.composedemo.base

import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import com.jingtian.composedemo.main.gallery.GalleryStateHolder
import com.jingtian.composedemo.viewmodels.AuthViewModel
import com.jingtian.composedemo.viewmodels.authViewModel
import kotlinx.coroutines.launch

@Composable
actual fun GalleryStateHolder.BackPressHandler(isOnTop: Boolean) {
    val androidViewModel: AuthViewModel = authViewModel()
    val scope = rememberCoroutineScope()
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    class GalleryOnBackPressCallBack(isEnable : Boolean) : OnBackPressedCallback(isEnable) {
        override fun handleOnBackPressed() {
            scope.launch {
                withFrameNanos { }
                val isEnabled = this@BackPressHandler.onBackPressed()
                this@GalleryOnBackPressCallBack.isEnabled = isEnabled
                if (!isEnabled) {
                    androidViewModel.lock()
                    dispatcher?.onBackPressed()
                    this@GalleryOnBackPressCallBack.isEnabled = true
                }
            }
        }
    }

    var backPressedCallback by remember { mutableStateOf<GalleryOnBackPressCallBack?>(null) }

    DisposableEffect(dispatcher, this@BackPressHandler, backEnable, isOnTop) {
        backPressedCallback?.apply {
            isEnabled = false
            remove()
        }
        val innerBackPressedCallback = GalleryOnBackPressCallBack(isEnable = backEnable && isOnTop)
        backPressedCallback = innerBackPressedCallback
        dispatcher?.addCallback(innerBackPressedCallback)
        onDispose {
            innerBackPressedCallback.remove()
        }
    }
}