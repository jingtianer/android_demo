package com.jingtian.composedemo.main

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jingtian.composedemo.launch.LaunchTasks
import com.jingtian.composedemo.ui.theme.DemoAppTheme
import com.jingtian.composedemo.ui.theme.LocalDesktopConst
import com.jingtian.composedemo.utils.AppTheme
import com.jingtian.composedemo.viewmodels.AppThemeViewModel

fun MainViewController() = ComposeUIViewController {
    LaunchTasks.onLaunch()
    DemoAppTheme {
        Main()
        val viewModel: AppThemeViewModel = viewModel(factory = AppThemeViewModel.viewModelFactory)
        val currentAppTheme by remember { viewModel.currentAppTheme }
        val isSystemDark = isSystemInDarkTheme()
        LaunchedEffect(currentAppTheme) {
            AppTheme.setAppTheme(currentAppTheme)
            val isDark = AppTheme.isDark(currentAppTheme, isSystemDark)
            this@DemoAppTheme.setCurrentDark(isDark)
        }
    }
}