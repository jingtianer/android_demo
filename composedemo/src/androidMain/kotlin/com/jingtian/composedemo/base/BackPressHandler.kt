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
import com.jingtian.composedemo.main.gallery.GalleryStateHolder
import com.jingtian.composedemo.viewmodels.AuthViewModel
import com.jingtian.composedemo.viewmodels.authViewModel
import kotlinx.coroutines.launch

@Composable
actual fun GalleryStateHolder.BackPressHandler() {

    val androidViewModel: AuthViewModel = authViewModel()
    val scope = rememberCoroutineScope()
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    class GalleryOnBackPressCallBack : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            scope.launch {
                val isEnabled = this@BackPressHandler.onBackPressed()
                this@GalleryOnBackPressCallBack.isEnabled = isEnabled
                if (!isEnabled) {
                    androidViewModel.isLocked.value = true
                    dispatcher?.onBackPressed()
                    this@GalleryOnBackPressCallBack.isEnabled = true
                }
            }
        }
    }

    var backPressedCallback by remember { mutableStateOf<GalleryOnBackPressCallBack?>(null) }

    DisposableEffect(dispatcher, this) {
        backPressedCallback?.apply {
            isEnabled = false
            remove()
        }
        val innerBackPressedCallback = GalleryOnBackPressCallBack()
        backPressedCallback = innerBackPressedCallback
        innerBackPressedCallback.isEnabled = true
        dispatcher?.addCallback(innerBackPressedCallback)
        onDispose {
            innerBackPressedCallback.remove()
        }
    }
}