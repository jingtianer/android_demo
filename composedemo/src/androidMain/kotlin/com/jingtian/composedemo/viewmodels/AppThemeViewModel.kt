package com.jingtian.composedemo.viewmodels

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.jingtian.composedemo.utils.AppTheme

class AppThemeViewModel : ViewModel() {
    val currentAppTheme = mutableStateOf(AppTheme.currentAppTheme())
}