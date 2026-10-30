package com.jingtian.composedemo.main

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.ComposeUIViewController
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jingtian.composedemo.BuildKonfig
import com.jingtian.composedemo.base.JumpOnPurposeMarkHolder
import com.jingtian.composedemo.launch.LaunchTasks
import com.jingtian.composedemo.multiplatform.logD
import com.jingtian.composedemo.ui.theme.DemoAppTheme
import com.jingtian.composedemo.utils.AppTheme
import com.jingtian.composedemo.viewmodels.AppThemeViewModel
import com.jingtian.composedemo.viewmodels.AuthSuccess
import com.jingtian.composedemo.viewmodels.AuthViewModel
import com.jingtian.composedemo.viewmodels.authViewModel

fun MainViewController() = ComposeUIViewController {
    if (BuildKonfig.isDemo) {
        Main()
        return@ComposeUIViewController
    }
    LaunchTasks.onLaunch()
    DemoAppTheme {
        var isVisible by remember { mutableStateOf(false) }
        var isAuthed by remember { mutableStateOf(false) }
        val authViewModel: AuthViewModel = authViewModel()
        val doAuth = remember {
            {
                authViewModel.doAuth { authResult ->
                    when(authResult) {
                        is AuthSuccess -> {
                            isVisible = true
                            isAuthed = true
                        }
                    }
                }
            }
        }
        val lifecycleObserver by remember {
            mutableStateOf(object : DefaultLifecycleObserver {
                override fun onStop(owner: LifecycleOwner) {
                    isAuthed = false
                }

                override fun onPause(owner: LifecycleOwner) {
                    isVisible = false
                }

                override fun onResume(owner: LifecycleOwner) {
                    if (!isAuthed && !JumpOnPurposeMarkHolder.isJumpOnPurpose.getAndSet(false)) {
                        doAuth.invoke()
                    } else {
                        isAuthed = true
                        isVisible = true
                    }
                }

                override fun onStart(owner: LifecycleOwner) {
                    if (!isAuthed && !JumpOnPurposeMarkHolder.isJumpOnPurpose.getAndSet(false)) {
                        doAuth.invoke()
                    } else {
                        isAuthed = true
                        isVisible = true
                    }
                }
            })
        }
        Main(visible = isVisible && isAuthed) {
            if (isAuthed) {
                isVisible = true
            } else {
                doAuth()
            }
        }
        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(Unit) {
            lifecycleOwner.lifecycle.addObserver(observer = lifecycleObserver)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer = lifecycleObserver)
            }
        }
        val viewModel: AppThemeViewModel = viewModel(factory = AppThemeViewModel.viewModelFactory)
        val currentAppTheme by remember { viewModel.currentAppTheme }
        val isSystemDark = isSystemInDarkTheme()
        LaunchedEffect(currentAppTheme, isSystemDark) {
            AppTheme.setAppTheme(currentAppTheme)
            val isDark = AppTheme.isDark(currentAppTheme, isSystemDark)
            this@DemoAppTheme.setCurrentDark(isDark)
        }
    }
}