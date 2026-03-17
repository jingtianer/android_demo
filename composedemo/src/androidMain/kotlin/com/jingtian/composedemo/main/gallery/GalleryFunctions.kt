package com.jingtian.composedemo.main.gallery

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jingtian.composedemo.BuildKonfig
import com.jingtian.composedemo.main.remote.ImportRemoteDialog

actual fun platformExtraAlbumFunctions() : List<GalleryFunctions> = if (BuildKonfig.isRemote) listOf(GalleryFunctions.IMPORT_CIFS) else listOf()


@Composable
actual fun RowScope.PlatformGalleryFunctionView(platformExtra: GalleryStateHolder, func: GalleryFunctions, onClick: ()->Unit) {
    when(func) {
        GalleryFunctions.IMPORT_CIFS -> {
            ImportRemoteFunctionView(platformExtra, func, onClick)
        }
        else -> {

        }
    }
}

@Composable
private fun RowScope.ImportRemoteFunctionView(platformExtra: GalleryStateHolder, func: GalleryFunctions, onClick: ()->Unit) {
    val model = platformExtra.platformExtraModel as PlatformExtra
    var showDialog by remember { model.showImportRemoteDialog }
    val showImportRemoteDialogStateHolder by remember { model.showImportRemoteDialogStateHolder }
    GalleryFunctionView(func) {
        showDialog = true
        onClick.invoke()
    }

    if (showDialog) {
        showImportRemoteDialogStateHolder.ImportRemoteDialog(platformExtra.album.value) {
            showDialog = false
        }
    }
}