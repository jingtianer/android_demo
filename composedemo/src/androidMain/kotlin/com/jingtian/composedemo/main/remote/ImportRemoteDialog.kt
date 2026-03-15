package com.jingtian.composedemo.main.remote

import androidx.annotation.CallSuper
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jingtian.composedemo.base.AppThemeDialog
import com.jingtian.composedemo.base.AppThemeText
import com.jingtian.composedemo.base.resources.DrawableIcon
import com.jingtian.composedemo.base.resources.getPainter
import com.jingtian.composedemo.dao.model.Album
import com.jingtian.composedemo.main.drawer.DrawerFunctionView
import com.jingtian.composedemo.main.gallery.PlatformExtra
import com.jingtian.composedemo.ui.theme.LocalAppPalette
import com.jingtian.composedemo.ui.theme.LocalAppUIConstants
import com.jingtian.composedemo.ui.theme.LocalMiddleButtonConfig
import com.jingtian.composedemo.ui.theme.dialogBackground
import com.jingtian.composedemo.viewmodels.AlbumViewModel
import kotlinx.coroutines.launch

class ImportRemoteDialogStateHolder {
    val sftpServerHolderList = mutableStateListOf(*(ServerStorage.getStorage<SftpServer>(ServerType.SFTP).allServer().map { it.toHolder() }.toTypedArray()))
    var addServerDialog by mutableStateOf(false)
    var sftpEditServerDialog by mutableStateOf<SftpServerStateHolder?>(null)

    fun addSftpServer(server: SftpServerStateHolder) {
        sftpServerHolderList.add(0, server)
        ServerStorage.getStorage<SftpServer>(ServerType.SFTP).addServer(server.get())
    }

    fun updateHolder(serverStateHolder: ServerStateHolder) {
        ServerStorage.getStorage<RemoteServer>(serverStateHolder.serverType).updateServer(serverStateHolder.serverId, serverStateHolder.get())
    }

    fun removeSftpServer(serverStateHolder: ServerStateHolder) {
        sftpServerHolderList.remove(serverStateHolder)
        ServerStorage.getStorage<SftpServer>(serverStateHolder.serverType).deleteServer(serverStateHolder.serverId)
    }
}

private fun SftpServer.toHolder(): SftpServerStateHolder {
    return SftpServerStateHolder(this)
}

open class ServerStateHolder(private val remoteServer: RemoteServer, val serverType: ServerType) {
    var serverId by mutableStateOf(remoteServer.serverId)
    var serverName by mutableStateOf(remoteServer.serverName)

    @CallSuper
    open fun get(): RemoteServer {
        remoteServer.serverName = serverName
        return remoteServer
    }
}

class SftpServerStateHolder(private val sftpServer: SftpServer = SftpServer()) : ServerStateHolder(sftpServer, ServerType.SFTP) {
    var userName: String by mutableStateOf(sftpServer.userName)
    var port: Int by mutableIntStateOf(sftpServer.port)
    var ip: String by mutableStateOf(sftpServer.ip)
    var password: String by mutableStateOf(sftpServer.password)
    var path: String by mutableStateOf(sftpServer.path)

    suspend fun import(viewModel: AlbumViewModel, album: Album) {
        sftpServer.importFiles(viewModel, album)
    }

    override fun get(): SftpServer {
        super.get()
        sftpServer.userName = userName
        sftpServer.port = port
        sftpServer.ip = ip
        sftpServer.password = password
        sftpServer.path = path
        return sftpServer
    }
}

@Composable
fun ImportRemoteDialogStateHolder.ImportRemoteDialog(album: Album, onDismiss: ()->Unit) {
    AppThemeDialog(
        Modifier
            .fillMaxWidth(LocalAppUIConstants.current.dialogPercent)
            .wrapContentHeight()
            .clip(RoundedCornerShape(4.dp))
            .background(LocalAppPalette.current.dialogBg),
        onDismissRequest = {},
        properties = DialogProperties(usePlatformDefaultWidth = false),
        onNegative = onDismiss,
        onPositive = {
            onDismiss()
        }
    ) { _, actionButtons ->
        val viewModel: AlbumViewModel = viewModel(factory = AlbumViewModel.viewModelFactory)
        LazyColumn(Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .clip(RoundedCornerShape(4.dp))
            .background(LocalAppPalette.current.dialogBg)) {

            item {
                DrawerFunctionView(
                    onClick = {
                        addServerDialog = true
                    },
                    painter = getPainter(DrawableIcon.DrawableCIFS),
                    text = "添加SFTP Server",
                )
            }

            items(
                sftpServerHolderList.size,
                key = { index-> sftpServerHolderList[index].serverId }
            ) { index->
                val serverItem = sftpServerHolderList[index]
                SftpServerDescView(serverItem, onClick = {
                    viewModel.viewModelScope.launch {
                        serverItem.import(viewModel, album)
                    }
                    onDismiss()
                }, onLongPress = {
                    sftpEditServerDialog = serverItem
                })
            }

            item {
                Column(
                    Modifier
                        .padding(horizontal = 6.dp)
                        .fillMaxWidth()
                ) {
                    actionButtons()
                }
            }
        }

    }
    sftpEditServerDialog?.let { sftpEditServerDialog->
        CompositionLocalProvider(
            LocalMiddleButtonConfig provides LocalMiddleButtonConfig.current.copy(
                text = "删除",
                colors = LocalMiddleButtonConfig.current.colors.copy(containerColor = LocalAppPalette.current.deleteButtonColor, contentColor = Color.White),
            )
        ) {
            SftpServerEditDialog(sftpEditServerDialog, true) {
                this.sftpEditServerDialog = null
            }
        }
    }

    if (addServerDialog) {
        CompositionLocalProvider(
            LocalMiddleButtonConfig provides LocalMiddleButtonConfig.current.copy(
                text = "删除",
                colors = LocalMiddleButtonConfig.current.colors.copy(containerColor = LocalAppPalette.current.deleteButtonColor, contentColor = Color.White),
            )
        ) {
            SftpServerEditDialog(SftpServerStateHolder(), false) {
                this.addServerDialog = false
            }
        }
    }
}

@Composable
fun ImportRemoteDialogStateHolder.SftpServerEditDialog(sftpServer: SftpServerStateHolder, isEdit: Boolean, onDismiss: ()->Unit) {
    AppThemeDialog(
        Modifier
            .fillMaxWidth(LocalAppUIConstants.current.dialogPercent)
            .wrapContentHeight()
            .clip(RoundedCornerShape(4.dp))
            .background(LocalAppPalette.current.dialogBg),
        onDismissRequest = {},
        properties = DialogProperties(usePlatformDefaultWidth = false),
        onNegative = onDismiss,
        onMiddleClick = if (isEdit) {
            {
                removeSftpServer(sftpServer)
                onDismiss()
            }
        } else {
            null
        },
        onPositive = {
            if (!isEdit) {
                addSftpServer(sftpServer)
            } else {
                updateHolder(sftpServer)
            }
            onDismiss()
        }
    ) { _, actionButtons ->
        LazyColumn(
            Modifier
                .padding(horizontal = 6.dp)
                .fillMaxWidth()
                .wrapContentHeight()
                .clip(RoundedCornerShape(4.dp))
                .background(LocalAppPalette.current.dialogBg)
        ) {

            item {
                OutlinedTextField(sftpServer.serverName, {
                    sftpServer.serverName = it
                }, modifier = Modifier.fillMaxWidth().wrapContentHeight(), maxLines = 1, label = {
                    AppThemeText("serverName")
                })
            }

            item {
                OutlinedTextField(sftpServer.ip, {
                    sftpServer.ip = it
                }, modifier = Modifier.fillMaxWidth().wrapContentHeight(), maxLines = 1, label = {
                    AppThemeText("host")
                })
            }

            item {
                OutlinedTextField(sftpServer.port.toString(), {
                    sftpServer.port = it.toIntOrNull() ?: 22
                }, modifier = Modifier.fillMaxWidth().wrapContentHeight(), maxLines = 1, label = {
                    AppThemeText("port")
                })
            }

            item {
                OutlinedTextField(sftpServer.path, {
                    sftpServer.path = it
                }, modifier = Modifier.fillMaxWidth().wrapContentHeight(), maxLines = 1, label = {
                    AppThemeText("path")
                })
            }

            item {
                OutlinedTextField(sftpServer.userName, {
                    sftpServer.userName = it
                }, modifier = Modifier.fillMaxWidth().wrapContentHeight(), maxLines = 1, label = {
                    AppThemeText("userName")
                })
            }

            item {
                OutlinedTextField(sftpServer.password, {
                    sftpServer.password = it
                }, modifier = Modifier.fillMaxWidth().wrapContentHeight(), maxLines = 1, label = {
                    AppThemeText("password")
                })
            }

            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                ) {
                    actionButtons()
                }
            }
        }
    }
}

@Composable
fun ColumnScope.SftpServerDescView(sftpServer: SftpServerStateHolder, onClick: ()->Unit, onLongPress: ()->Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize(0.9f)
            .pointerInput(sftpServer) {
                detectTapGestures(
                    onTap = {
                        onClick()
                    },
                    onLongPress = {
                        onLongPress()
                    }
                )
            }
    ) {
        OutlinedTextField(sftpServer.serverName, {}, modifier = Modifier.fillMaxWidth().wrapContentHeight(), enabled = false, readOnly = true, maxLines = 1, label = {
            AppThemeText(sftpServer.serverId)
        })
        OutlinedTextField(sftpServer.userName, {}, modifier = Modifier.fillMaxWidth().wrapContentHeight(), enabled = false, readOnly = true, maxLines = 1, label = {
            AppThemeText(sftpServer.path)
        })
    }
}