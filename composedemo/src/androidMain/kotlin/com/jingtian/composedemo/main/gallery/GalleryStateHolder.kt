package com.jingtian.composedemo.main.gallery

import androidx.compose.runtime.mutableStateOf
import com.jingtian.composedemo.main.remote.ImportRemoteDialogStateHolder

class PlatformExtra : IPlatformExtra {
    var showImportRemoteDialog = mutableStateOf(false)
    var showImportRemoteDialogStateHolder = mutableStateOf(ImportRemoteDialogStateHolder())
}

actual fun getPlatformExtra(): IPlatformExtra = PlatformExtra()