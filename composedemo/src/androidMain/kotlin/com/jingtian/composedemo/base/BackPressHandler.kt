package com.jingtian.composedemo.base

import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.material3.DrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jingtian.composedemo.main.gallery.GalleryStateHolder
import com.jingtian.composedemo.viewmodels.AndroidMigrateViewModel

@Composable
actual fun GalleryStateHolder.BackPressHandler(
    drawerState: DrawerState,
    enterEditMode: Boolean,
    onBackPressed: () -> Unit
) {

    val androidViewModel: AndroidMigrateViewModel = viewModel(factory = viewModelFactory {
        initializer {
            AndroidMigrateViewModel()
        }
    })
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    class GalleryOnBackPressCallBack : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (!drawerState.isOpen && !enterEditMode) {
                this.isEnabled = false
                androidViewModel.isPasswordChecked.value = false
                dispatcher?.onBackPressed()
                this.isEnabled = true
                return
            }
            onBackPressed()
        }
    }
    var backPressedCallback by remember {
        mutableStateOf<OnBackPressedCallback>(GalleryOnBackPressCallBack())
    }
    DisposableEffect(dispatcher, this) {
        backPressedCallback.isEnabled = false
        backPressedCallback.remove()
        backPressedCallback = GalleryOnBackPressCallBack()
        dispatcher?.addCallback(backPressedCallback)
        onDispose {
            backPressedCallback.isEnabled = false
            backPressedCallback.remove()
        }
    }
}