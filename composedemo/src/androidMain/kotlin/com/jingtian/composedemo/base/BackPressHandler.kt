package com.jingtian.composedemo.base

import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.material3.DrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.jingtian.composedemo.main.gallery.GalleryStateHolder
import kotlinx.coroutines.launch

@Composable
actual fun GalleryStateHolder.BackPressHandler(drawerState: DrawerState, enterEditMode: Boolean, onBackPressed: ()->Unit) {
    var backPressedCallback by remember {
        mutableStateOf<OnBackPressedCallback>(
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    onBackPressed()
                }
            }
        )
    }
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    DisposableEffect(dispatcher, this) {
        backPressedCallback.remove()
        backPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                onBackPressed()
            }
        }
        dispatcher?.addCallback(backPressedCallback)
        onDispose {
            backPressedCallback.isEnabled = false
            backPressedCallback.remove()
        }
    }
    backPressedCallback.isEnabled = drawerState.isOpen || enterEditMode
}