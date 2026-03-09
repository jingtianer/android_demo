package com.jingtian.composedemo.viewmodels

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jingtian.composedemo.utils.AppTheme

class AppThemeViewModel : ViewModel() {
    val currentAppTheme = mutableStateOf(AppTheme.currentAppTheme())

    companion object {
        val viewModelFactory = viewModelFactory {
            initializer {
                AppThemeViewModel()
            }
        }
    }
}