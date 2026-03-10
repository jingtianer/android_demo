package com.jingtian.composedemo

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jingtian.composedemo.main.Main
import com.jingtian.composedemo.ui.theme.DemoAppTheme
import com.jingtian.composedemo.utils.AppTheme
import com.jingtian.composedemo.viewmodels.AppThemeViewModel
import com.jingtian.composedemo.viewmodels.DesktopViewModel

fun main(args: Array<String>) {
    application {
        MainWindow()
    }
}
@Composable
fun ApplicationScope.MainWindow() {
    val windowState = rememberWindowState()
    windowState.position = WindowPosition(Alignment.Center)
    Window(onCloseRequest = {
        this.exitApplication()
    },
        state = windowState,
        title = "ComposeDemo"
    ) {
        DemoAppTheme {
            val desktopViewModel: DesktopViewModel = viewModel(factory = DesktopViewModel.Factory(windowState))
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
}