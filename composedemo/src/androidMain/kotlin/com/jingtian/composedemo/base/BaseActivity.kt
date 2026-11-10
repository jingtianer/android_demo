package com.jingtian.composedemo.base

import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsetsController
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jingtian.composedemo.ui.theme.DemoAppTheme
import com.jingtian.composedemo.utils.AppTheme
import com.jingtian.composedemo.viewmodels.AppThemeViewModel
import com.jingtian.composedemo.viewmodels.appThemeViewModel

abstract class BaseActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DemoAppTheme {
                Content()
            }
            val appThemeViewModel = appThemeViewModel()
            val isNight by appThemeViewModel.rememberIsDark()
            LaunchedEffect(isNight) {
                updateStateBar(isNight)
            }
        }
        val isDark = AppTheme.isDark(AppTheme.currentAppTheme(), isSystemInDarkTheme())
        updateStateBar(isDark)
//        fitSystemBars()
    }

    private fun updateStateBar(isDark: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.setSystemBarsAppearance(
                if (!isDark) WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS else 0,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            )
            window.insetsController?.setSystemBarsAppearance(
                if (!isDark) WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS else 0,
                WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
            )
        } else {
            val decorView = window.decorView
            val currentFlags = decorView.systemUiVisibility
            decorView.systemUiVisibility = if (!isDark) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    currentFlags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                } else {
                    currentFlags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    (currentFlags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()) and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                } else {
                    currentFlags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
                }
            }
        }
    }

    fun AppThemeSwitcher(newConfig: Configuration) {
        val isSystemDark = isSystemInDarkTheme()
        val uiMode = newConfig.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val isDark = when (uiMode) {
            Configuration.UI_MODE_NIGHT_YES -> {
                AppTheme.isDark(AppTheme.currentAppTheme(), true)
            }
            Configuration.UI_MODE_NIGHT_NO -> {
                AppTheme.isDark(AppTheme.currentAppTheme(), false)
            }
            Configuration.UI_MODE_NIGHT_UNDEFINED -> {
                AppTheme.isDark(AppTheme.currentAppTheme(), isSystemDark)
            }
            else -> {
                AppTheme.isDark(AppTheme.currentAppTheme(), isSystemDark)
            }
        }
        updateStateBar(isDark)
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

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        AppThemeSwitcher(newConfig)
    }

    private fun isSystemInDarkTheme(): Boolean {
        val uiMode = resources.configuration.uiMode
        return (uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }
}