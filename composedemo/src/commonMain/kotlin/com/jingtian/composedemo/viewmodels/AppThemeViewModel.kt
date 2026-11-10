package com.jingtian.composedemo.viewmodels

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jingtian.composedemo.utils.AppTheme
import com.jingtian.composedemo.utils.CoroutineUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

fun appThemeViewModel() = AppThemeViewModel

object AppThemeViewModel : SingletonViewModel() {

    private val currentAppThemeState = mutableStateOf(AppTheme.currentAppTheme())
    val currentAppTheme:State<AppTheme> = currentAppThemeState

    @Composable
    fun rememberIsDark(): State<Boolean> {
        val isSystemDark = isSystemInDarkTheme()
        return remember(isSystemDark) {
            derivedStateOf {
                AppTheme.isDark(currentAppTheme.value, isSystemDark)
            }
        }
    }

    fun setCurrentTheme(appTheme: AppTheme) {
        currentAppThemeState.value = appTheme
        viewModelScope.launch {
            AppTheme.setAppTheme(appTheme)
        }
    }
}