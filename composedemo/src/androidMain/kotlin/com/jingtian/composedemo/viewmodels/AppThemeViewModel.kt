package com.jingtian.composedemo.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.jingtian.composedemo.utils.AppTheme

class AppThemeViewModel : ViewModel() {
    val currentAppTheme = MutableLiveData(AppTheme.currentAppTheme())
}