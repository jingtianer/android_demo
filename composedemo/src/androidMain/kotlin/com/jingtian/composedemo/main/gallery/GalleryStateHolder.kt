package com.jingtian.composedemo.main.gallery

import androidx.compose.runtime.mutableStateOf
import com.jingtian.composedemo.main.remote.ImportRemoteDialogStateHolder

class PlatformExtra : IPlatformExtra {
}

actual fun getPlatformExtra(): IPlatformExtra = PlatformExtra()