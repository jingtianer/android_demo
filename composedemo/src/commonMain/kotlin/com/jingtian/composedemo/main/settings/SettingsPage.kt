package com.jingtian.composedemo.main.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jingtian.composedemo.ui.theme.appBackgroundV2

@Composable
fun SettingsPage() {
    LazyColumn(modifier = Modifier.fillMaxSize().appBackgroundV2().windowInsetsPadding(WindowInsets.statusBars)) {
        item {

        }
    }
}