package com.jingtian.composedemo.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.jingtian.composedemo.multiplatform.MultiplatformFile


class FileShareLauncher : IFileShareLauncher {
    override fun launch(file: MultiplatformFile) {
    }
}
@Composable
actual fun rememberFileShare(onResult: (Long)->Unit): MutableState<IFileShareLauncher> {
    return mutableStateOf(FileShareLauncher())
}