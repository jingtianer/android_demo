package com.jingtian.composedemo.base

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jingtian.composedemo.ui.theme.DemoAppTheme
import com.jingtian.composedemo.utils.AppTheme
import com.jingtian.composedemo.utils.UserStorage
import com.jingtian.composedemo.utils.python.PyInitializer
import com.jingtian.composedemo.viewmodels.AppThemeViewModel

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
        PyInitializer.init()
    }

    @Composable
    fun AppThemeSwitcher() {
        val viewModel: AppThemeViewModel = viewModel()
        val currentAppTheme by viewModel.currentAppTheme.observeAsState()
        var lastAppTheme by remember { mutableStateOf(currentAppTheme ?: AppTheme.AUTO) }
        LaunchedEffect(currentAppTheme) {
            val appTheme = viewModel.currentAppTheme.value ?: AppTheme.AUTO
            if (AppTheme.currentAppTheme() == appTheme) {
                return@LaunchedEffect
            }
            AppTheme.setAppTheme(appTheme)
            recreate()
            lastAppTheme = appTheme
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
}