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
    return if (BuildKonfig.isRemote) {
        when(selectCount) {
            0 -> {
                listOf(GalleryFunctions.IMPORT_CIFS)
            }
            else -> {
                listOf(GalleryFunctions.DELETE_CIFS_FILE)
            }
        }
    } else listOf()
}


@Composable
actual fun RowScope.PlatformGalleryFunctionView(platformExtra: GalleryStateHolder, func: GalleryFunctions, onClick: ()->Unit) {
    when(func) {
        GalleryFunctions.IMPORT_CIFS -> {
            ImportRemoteFunctionView(platformExtra, func, onClick)
        }
        GalleryFunctions.DELETE_CIFS_FILE -> {
            DeleteRemoteFileFunctionView(platformExtra, func, onClick)
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

@Composable
private fun RowScope.DeleteRemoteFileFunctionView(platformExtra: GalleryStateHolder, func: GalleryFunctions, onClick: ()->Unit) {
    val model = platformExtra.platformExtraModel as PlatformExtra
    var showDialog by remember { model.showDeleteRemoteFileConfirmDialog }
    platformExtra.apply {

        @Composable
        fun ConfirmDialog() {
            val title = if (currentSelectedItem.size > 1) {
                "选择的${currentSelectedItem.size}项"
            } else if (currentSelectedItem.size == 1) {
                currentSelectedItem.values.firstOrNull()?.albumItem?.itemName ?: ""
            } else {
                showDialog = false
                return
            }
            CompositionLocalProvider(
                LocalMiddleButtonConfig provides LocalMiddleButtonConfig.current.copy(
                    text = "删除",
                    colors = LocalMiddleButtonConfig.current.colors.copy(containerColor = LocalAppPalette.current.deleteButtonColor, contentColor = Color.White),
                )
            ) {
                AppThemeConfirmDialog("确认删除$title", reversed = true, onPositive = null, onMiddleClick = {
                    CoroutineUtils.runIOTask({
                        val remoteFiles = currentSelectedItem.values.mapNotNull { it.fileInfo.getFileUri() as? RemoteSftpFileImpl }
                        val groupedFiles = remoteFiles
                            .map { RemoteUriUtils.serverTypeAndId(it) to it }
                            .groupBy { it.first?.first }
                        for (serverType in groupedFiles.keys) {
                            Log.d("jingtian", "remote deleteFiles: serverType=$serverType")
                            serverType ?: continue
                            val groupByServerId = groupedFiles[serverType]?.groupBy { it.first?.second } ?: continue
                            for (serverId in groupByServerId.keys) {
                                Log.d("jingtian", "remote deleteFiles: serverId=$serverId")
                                serverId ?: continue
                                ServerStorage.getStorage<RemoteServer>(serverType).getServer(serverId)?.deleteFiles(groupByServerId[serverId]?.map { it.second } ?: listOf())
                            }
                        }
                        viewModel.deleteItems(currentSelectedItem.values)
                    }, onFailure = { t->
                        Log.d("jingtian", "remote err=$t")
                    })
                    showDialog = false
                }, onNegative = {
                    showDialog = false
                }, onDismissRequest = {
                    showDialog = false
                })
            }
        }

        GalleryFunctionView(func) {
            showDialog = true
            onClick.invoke()
        }

        if (showDialog) {
            ConfirmDialog()
        }
    }

}