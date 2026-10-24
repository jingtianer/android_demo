package com.jingtian.composedemo.main.remote

import androidx.annotation.CallSuper
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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
import com.jingtian.composedemo.ui.theme.LocalAppPalette
import com.jingtian.composedemo.ui.theme.LocalAppUIConstants
import com.jingtian.composedemo.ui.theme.LocalMiddleButtonConfig
import com.jingtian.composedemo.viewmodels.AlbumViewModel
import com.jingtian.composedemo.viewmodels.AuthParams
import com.jingtian.composedemo.viewmodels.AuthSuccess
import com.jingtian.composedemo.viewmodels.AuthViewModel
import com.jingtian.composedemo.viewmodels.authViewModel
import kotlinx.coroutines.launch

class ImportRemoteDialogStateHolder {
    val sftpServerHolderList = mutableStateListOf(
        *(ServerStorage.getStorage<SftpServer>(ServerType.SFTP).allServer().map { it.toHolder() }
            .toTypedArray())
    )
    var addServerDialog by mutableStateOf(false)
    var sftpEditServerDialog by mutableStateOf<SftpServerStateHolder?>(null)


    fun addSftpServer(
        viewModel: AuthViewModel,
        serverHolder: SftpServerStateHolder,
        onSuccess: () -> Unit
    ) {
        sftpServerHolderList.add(0, serverHolder)
        val storage = ServerStorage.getStorage<SftpServer>(ServerType.SFTP)
        val server = serverHolder.get()
        storage.allocateServerId(server)
        val cipher = server.getInitializedCipher()
        viewModel.doAuth(cipher) { authResult ->
            when (authResult) {
                is AuthSuccess -> {
                    server.onAddSftpServerAuthSuccess(authResult)
                    storage.addServer(server)
                    onSuccess()
                }
            }
        }
    }

    fun updateHolder(
        viewModel: AuthViewModel,
        serverStateHolder: SftpServerStateHolder,
        onSuccess: () -> Unit
    ) {
        val server = serverStateHolder.get()
        val storage = ServerStorage.getStorage<RemoteServer>(serverStateHolder.serverType)
        val cipher = server.getInitializedCipher()
        viewModel.doAuth(cipher) { authResult ->
            when (authResult) {
                is AuthSuccess -> {
                    server.onUpdateAuthSuccess(authResult)
                    storage.updateServer(server.serverId, server)
                    onSuccess()
                }
            }
        }
    }

    fun removeSftpServer(serverStateHolder: ServerStateHolder) {
        sftpServerHolderList.remove(serverStateHolder)
        ServerStorage.getStorage<SftpServer>(serverStateHolder.serverType)
            .deleteServer(serverStateHolder.serverId)
    }
}

fun SftpServer.toHolder(): SftpServerStateHolder {
    return SftpServerStateHolder(this)
}

open class ServerStateHolder(private val remoteServer: RemoteServer, val serverType: ServerType) {
    var serverId by mutableStateOf(remoteServer.serverId)
    var serverName by mutableStateOf(remoteServer.serverName)

    @CallSuper
    open fun reset() {
        serverId = remoteServer.serverId
        serverName = remoteServer.serverName
    }

    @CallSuper
    open fun get(): RemoteServer {
        remoteServer.serverName = serverName
        return remoteServer
    }
}

class SftpServerStateHolder(private val sftpServer: SftpServer = createSftpServer()) :
    ServerStateHolder(sftpServer, ServerType.SFTP) {
    var userName: String by mutableStateOf(sftpServer.userName)
    var port: Int by mutableIntStateOf(sftpServer.port)
    var ip: String by mutableStateOf(sftpServer.ip)
    var password: String by mutableStateOf(sftpServer.password)
    var path: String by mutableStateOf(sftpServer.path)
    var authed: Boolean by mutableStateOf(false)

    suspend fun import(viewModel: AlbumViewModel, album: Album) {
        sftpServer.importFiles(viewModel, album)
    }

    override fun reset() {
        super.reset()
        userName = sftpServer.userName
        port = sftpServer.port
        ip = sftpServer.ip
        password = sftpServer.password
        path = sftpServer.path
        authed = false
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

    fun onServerEditAuthSuccess(cipher: AuthParams) {
        onServerEditAuthSuccess(cipher, this)
    }
}

expect fun onServerEditAuthSuccess(cipher: AuthParams, sftpServer: SftpServerStateHolder)

@Composable
fun ImportRemoteDialogStateHolder.ImportRemoteDialog(album: Album, onDismiss: () -> Unit) {
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
        val authViewModel: AuthViewModel = authViewModel()
        LazyColumn(
            Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .clip(RoundedCornerShape(4.dp))
                .background(LocalAppPalette.current.dialogBg)
        ) {

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
                key = { index -> sftpServerHolderList[index].serverId }
            ) { index ->
                val serverItem = sftpServerHolderList[index]
                SftpServerDescView(serverItem, onClick = {
                    authViewModel.doAuth { authResult ->
                        when (authResult) {
                            is AuthSuccess -> {
                                viewModel.viewModelScope.launch {
                                    serverItem.import(viewModel, album)
                                }
                            }
                        }
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
    sftpEditServerDialog?.let { sftpEditServerDialog ->
        CompositionLocalProvider(
            LocalMiddleButtonConfig provides LocalMiddleButtonConfig.current.copy(
                text = "删除",
                colors = LocalMiddleButtonConfig.current.colors.copy(
                    containerColor = LocalAppPalette.current.deleteButtonColor,
                    contentColor = Color.White
                ),
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
                colors = LocalMiddleButtonConfig.current.colors.copy(
                    containerColor = LocalAppPalette.current.deleteButtonColor,
                    contentColor = Color.White
                ),
            )
        ) {
            SftpServerEditDialog(SftpServerStateHolder(), false) {
                this.addServerDialog = false
            }
        }
    }
}

@Composable
fun ImportRemoteDialogStateHolder.SftpServerEditDialog(
    sftpServer: SftpServerStateHolder,
    isEdit: Boolean,
    onDismiss: () -> Unit
) {
    val viewModel: AuthViewModel = authViewModel()
    if (!sftpServer.authed && isEdit) {
        val cipher = sftpServer.get().getDecryptCipher(sftpServer.get())
        viewModel.doAuth(cipher) { authResult ->
            when (authResult) {
                is AuthSuccess -> {
                    sftpServer.onServerEditAuthSuccess(cipher)
//                Log.d("jingtian", "newSession: pwd=${sftpServer.password}")
                    sftpServer.authed = true
                }
            }
        }
        return
    }
    AppThemeDialog(
        Modifier
            .fillMaxWidth(LocalAppUIConstants.current.dialogPercent)
            .wrapContentHeight()
            .clip(RoundedCornerShape(4.dp))
            .background(LocalAppPalette.current.dialogBg),
        onDismissRequest = {
            sftpServer.reset()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false),
        onNegative = {
            sftpServer.reset()
            onDismiss()
        },
        onMiddleClick = if (isEdit) {
            {
                sftpServer.reset()
                removeSftpServer(sftpServer)
                onDismiss()
            }
        } else {
            null
        },
        onPositive = {
            if (!isEdit) {
                addSftpServer(viewModel, sftpServer) {
                    sftpServer.authed = true
                    onDismiss()
                }
            } else {
                updateHolder(viewModel, sftpServer) {
                    onDismiss()
                }
            }
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
                }, modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(), maxLines = 1, label = {
                    AppThemeText("名称")
                })
            }

            item {
                OutlinedTextField(sftpServer.ip, {
                    sftpServer.ip = it
                }, modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(), maxLines = 1, label = {
                    AppThemeText("host")
                })
            }

            item {
                OutlinedTextField(
                    sftpServer.port.toString(),
                    {
                        sftpServer.port = it.toIntOrNull() ?: 22
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    maxLines = 1,
                    label = {
                        AppThemeText("port")
                    },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                )
            }

            item {
                OutlinedTextField(
                    sftpServer.path,
                    {
                        sftpServer.path = it
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    maxLines = 1,
                    label = {
                        AppThemeText("路径")
                    },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Uri),
                )
            }

            item {
                OutlinedTextField(sftpServer.userName, {
                    sftpServer.userName = it
                }, modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(), maxLines = 1, label = {
                    AppThemeText("用户名")
                })
            }

            item {
                var keyboardType by remember {
                    mutableIntStateOf(0)
                } // 0 password, 1 text
                val interactiveSource = remember { MutableInteractionSource() }
                if (interactiveSource.collectIsFocusedAsState().value) {
                    keyboardType = 1
                } else {
                    keyboardType = 0
                }
                OutlinedTextField(
                    sftpServer.password,
                    {
                        sftpServer.password = it
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    maxLines = 1,
                    label = {
                        AppThemeText("password")
                    },
                    keyboardOptions = when(keyboardType) {
                        1 -> {
                            KeyboardOptions.Default.copy(
                                capitalization = KeyboardCapitalization.None
                            )
                        }
                        else -> {
                            KeyboardOptions.Default.copy(
                                keyboardType = KeyboardType.Password,
                                capitalization = KeyboardCapitalization.None
                            )
                        }
                    },
                    visualTransformation = when(keyboardType) {
                        1 -> {
                            VisualTransformation.None
                        }
                        else -> {
                            PasswordVisualTransformation()
                        }
                    },
                    interactionSource = interactiveSource,
                )
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
fun SftpServerDescView(
    sftpServer: SftpServerStateHolder,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
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
        OutlinedTextField(sftpServer.serverName, {}, modifier = Modifier
            .fillMaxWidth(0.9f)
            .align(Alignment.CenterHorizontally)
            .wrapContentHeight(), enabled = false, readOnly = true, maxLines = 1, label = {
            AppThemeText("${sftpServer.ip}:${sftpServer.port}")
        })
        OutlinedTextField(sftpServer.userName, {}, modifier = Modifier
            .fillMaxWidth(0.9f)
            .align(Alignment.CenterHorizontally)
            .wrapContentHeight(), enabled = false, readOnly = true, maxLines = 1, label = {
            AppThemeText(sftpServer.path)
        })
    }
}