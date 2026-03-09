package com.jingtian.composedemo.base

import androidx.compose.runtime.Composable

@Composable
expect fun screenWidth(): Int

@Composable
expect fun screenHeight(): Int