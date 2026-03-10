package com.jingtian.composedemo.base

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jingtian.composedemo.viewmodels.DesktopViewModel

@Composable
actual fun screenWidth(): Int {
    val viewModel: DesktopViewModel = viewModel()
    return viewModel.windowState.size.width.value.toInt()
}

@Composable
actual fun screenHeight(): Int {
    val viewModel: DesktopViewModel = viewModel()
    return viewModel.windowState.size.height.value.toInt()
}