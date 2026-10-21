package com.jingtian.composedemo.main.gallery

import android.util.Log
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import com.jingtian.composedemo.BuildKonfig
import com.jingtian.composedemo.base.AppThemeConfirmDialog
import com.jingtian.composedemo.main.remote.ImportRemoteDialog
import com.jingtian.composedemo.main.remote.RemoteServer
import com.jingtian.composedemo.main.remote.RemoteSftpFileImpl
import com.jingtian.composedemo.main.remote.RemoteUriUtils
import com.jingtian.composedemo.main.remote.ServerStorage
import com.jingtian.composedemo.main.remote.ServerType
import com.jingtian.composedemo.ui.theme.LocalAppPalette
import com.jingtian.composedemo.ui.theme.LocalMiddleButtonConfig
import com.jingtian.composedemo.utils.CoroutineUtils

actual fun platformExtraAlbumFunctions(selectCount: Int) : List<GalleryFunctions> {
    return listOf()
}


@Composable
actual fun RowScope.PlatformGalleryFunctionView(platformExtra: GalleryStateHolder, func: GalleryFunctions, onClick: ()->Unit) {
    when(func) {
        else -> {

        }
    }
}