package com.jingtian.composedemo.main.gallery

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jingtian.composedemo.base.AppThemeConfirmDialog
import com.jingtian.composedemo.base.AppThemeText
import com.jingtian.composedemo.main.remote.ImportRemoteDialog
import com.jingtian.composedemo.main.remote.RemoteServer
import com.jingtian.composedemo.main.remote.RemoteSftpFile
import com.jingtian.composedemo.main.remote.ServerStorage
import com.jingtian.composedemo.main.remote.serverTypeAndId
import com.jingtian.composedemo.ui.theme.LocalAppPalette
import com.jingtian.composedemo.ui.theme.LocalMiddleButtonConfig
import com.jingtian.composedemo.utils.CoroutineUtils

@Composable
fun RowScope.GalleryFunctionView(func: GalleryFunctions, onClick: ()->Unit) {
    Column(
        Modifier
            .wrapContentSize()
            .padding(horizontal = 6.dp)
            .clickable { onClick() }) {
        Icon(
            painter = func.iconProvider(),
            contentDescription = "${func.functionName}功能",
            modifier = Modifier
                .size(32.dp)
                .align(Alignment.CenterHorizontally),
        )
        AppThemeText(
            func.functionName,
            Modifier.align(Alignment.CenterHorizontally),
            style = LocalTextStyle.current.copy(fontSize = 10.sp)
        )
    }
}

@Composable
public fun RowScope.ImportRemoteFunctionView(platformExtra: GalleryStateHolder, func: GalleryFunctions, onClick: ()->Unit) {
    val model = platformExtra
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
public fun RowScope.DeleteRemoteFileFunctionView(platformExtra: GalleryStateHolder, func: GalleryFunctions, onClick: ()->Unit) {
    val model = platformExtra
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
                        val remoteFiles = currentSelectedItem.values.mapNotNull { it.fileInfo.getFileUri() as? RemoteSftpFile }
                        val groupedFiles = remoteFiles
                            .map { serverTypeAndId(it) to it }
                            .groupBy { it.first?.first }
                        for (serverType in groupedFiles.keys) {
//                            Log.d("jingtian", "remote deleteFiles: serverType=$serverType")
                            serverType ?: continue
                            val groupByServerId = groupedFiles[serverType]?.groupBy { it.first?.second } ?: continue
                            for (serverId in groupByServerId.keys) {
//                                Log.d("jingtian", "remote deleteFiles: serverId=$serverId")
                                serverId ?: continue
                                ServerStorage.getStorage<RemoteServer>(serverType).getServer(serverId)?.deleteFiles(groupByServerId[serverId]?.map { it.second } ?: listOf())
                            }
                        }
                        viewModel.deleteItems(currentSelectedItem.values)
                    }, onFailure = { t->
//                        Log.d("jingtian", "remote err=$t")
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