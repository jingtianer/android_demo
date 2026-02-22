package com.jingtian.composedemo.base

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jingtian.composedemo.ui.theme.AppThemeScope
import com.jingtian.composedemo.ui.theme.DemoAppTheme
import com.jingtian.composedemo.utils.AppTheme
import com.jingtian.composedemo.utils.UserStorage
import com.jingtian.composedemo.viewmodels.AppThemeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

abstract class BaseActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        when (UserStorage.userAppThemeConfig) {
            AppTheme.AUTO -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            AppTheme.Dark -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            AppTheme.Lite -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DemoAppTheme {
                AppThemeSwitcher()
                Content()
            }
        }
        if (shouldFitSystemBars()) {
            fitSystemBars()
        }
    }

    @Composable
    fun AppThemeScope.AppThemeSwitcher() {
        val viewModel: AppThemeViewModel = viewModel()
        val currentAppTheme by viewModel.currentAppTheme.observeAsState()
        var lastAppTheme by remember { mutableStateOf(currentAppTheme) }
        LaunchedEffect(currentAppTheme) {
            val appTheme = viewModel.currentAppTheme.value ?: return@LaunchedEffect
            if (AppTheme.currentAppTheme() == appTheme) {
                return@LaunchedEffect
            }
            this@AppThemeSwitcher.setAppTheme(appTheme)
            if (AppTheme.isDarkTheme(isSystemDark(), lastAppTheme ?: AppTheme.AUTO) != AppTheme.isDarkTheme(isSystemDark(), appTheme)) {
                recreate()
            }
        }
    }

    @Composable
    abstract fun Content()

    open fun shouldFitSystemBars(): Boolean {
        return true
    }

    open fun fitSystemBars() {
        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun isDarkModeEnabled(): Boolean {
        return when (AppCompatDelegate.getDefaultNightMode()) {
            AppCompatDelegate.MODE_NIGHT_YES -> true // App强制深色
            AppCompatDelegate.MODE_NIGHT_NO -> false  // App强制浅色
            else -> {
                isSystemDark()
            }
        }
    }

    private fun isSystemDark(): Boolean {
        val uiMode = resources.configuration.uiMode
        return (uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }
}