package com.jingtian.composedemo.viewmodels

import androidx.compose.ui.window.WindowState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import kotlin.reflect.KClass

class DesktopViewModel(val windowState: WindowState) : ViewModel() {
    class Factory(val windowState: WindowState) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T {
            return DesktopViewModel(windowState) as T
        }
    }
}