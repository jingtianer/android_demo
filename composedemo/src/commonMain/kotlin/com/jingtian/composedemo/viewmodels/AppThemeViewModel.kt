package com.jingtian.composedemo.viewmodels

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jingtian.composedemo.utils.AppTheme

fun appThemeViewModel() = AppThemeViewModel

object AppThemeViewModel : SingletonViewModel() {
    val currentAppTheme = mutableStateOf(AppTheme.currentAppTheme())
}