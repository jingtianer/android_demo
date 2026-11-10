package com.jingtian.composedemo.main

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jingtian.composedemo.BuildKonfig
import com.jingtian.composedemo.launch.LaunchTasks
import com.jingtian.composedemo.ui.theme.DemoAppTheme
import com.jingtian.composedemo.utils.AppTheme
import com.jingtian.composedemo.viewmodels.AppThemeViewModel
import com.jingtian.composedemo.viewmodels.appThemeViewModel

fun MainViewController() = ComposeUIViewController {
    if (BuildKonfig.isDemo) {
        Main()
        return@ComposeUIViewController
    }
    LaunchTasks.onLaunch()
    DemoAppTheme {
        PasswordGuardedMain()
        val viewModel: AppThemeViewModel = appThemeViewModel()
        val currentAppTheme by remember { viewModel.currentAppTheme }
        val isSystemDark = isSystemInDarkTheme()
        LaunchedEffect(currentAppTheme, isSystemDark) {
            AppTheme.setAppTheme(currentAppTheme)
            val isDark = AppTheme.isDark(currentAppTheme, isSystemDark)
            this@DemoAppTheme.setCurrentDark(isDark)
        }
    }
}