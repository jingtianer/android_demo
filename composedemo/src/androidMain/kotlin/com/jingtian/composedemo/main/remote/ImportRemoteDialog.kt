package com.jingtian.composedemo.main.remote

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.annotation.CallSuper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.ViewModelFactoryDsl
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
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
import com.jingtian.composedemo.utils.Base64Utils
import com.jingtian.composedemo.viewmodels.AlbumViewModel
import com.jingtian.composedemo.viewmodels.AndroidMigrateViewModel
import kotlinx.coroutines.launch
import java.nio.charset.Charset
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec

class ImportRemoteDialogStateHolder {
    val sftpServerHolderList = mutableStateListOf(*(ServerStorage.getStorage<SftpServer>(ServerType.SFTP).allServer().map { it.toHolder() }.toTypedArray()))
    var addServerDialog by mutableStateOf(false)
    var sftpEditServerDialog by mutableStateOf<SftpServerStateHolder?>(null)


    fun addSftpServer(viewModel: AndroidMigrateViewModel, serverHolder: SftpServerStateHolder, context: Context, onSuccess: ()->Unit) {
        sftpServerHolderList.add(0, serverHolder)
        val storage = ServerStorage.getStorage<SftpServer>(ServerType.SFTP)
        val server = serverHolder.get()
        storage.allocateServerId(server)
        val cipher = server.getCipher()
        val sk = server.getSecretKey(server)
        cipher.init(Cipher.ENCRYPT_MODE, sk)
        viewModel.bioticAuth.doAuth(cipher, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
//                when (result.authenticationType) {
//                    BiometricPrompt.AUTHENTICATION_RESULT_TYPE_UNKNOWN -> {
//                        return
//                    }
//                }
                result.cryptoObject?.cipher?.let { cipher->
                    cipher.doFinal(server.password.toByteArray(Charset.defaultCharset()))?.let { cipherResult->
                        val encodedPassword = Base64Utils.encrypt(cipherResult)
//                        Log.d("jingtian", "onAuthenticationSucceeded: password=${server.password}, encodedPassword=${encodedPassword}")
                        server.password = encodedPassword
                        server.passwordIv = Base64Utils.encrypt(cipher.iv)
                    }
                }
                storage.addServer(server)

                onSuccess()
            }
        })
    }

    fun updateHolder(viewModel: AndroidMigrateViewModel, serverStateHolder: SftpServerStateHolder, context: Context, onSuccess: () -> Unit) {
        val server = serverStateHolder.get()
        val storage = ServerStorage.getStorage<RemoteServer>(serverStateHolder.serverType)
        val cipher = server.getCipher()
        val sk = server.getSecretKey(server)
        cipher.init(Cipher.ENCRYPT_MODE, sk)
        viewModel.bioticAuth.doAuth(cipher, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
//                when (result.authenticationType) {
//                    BiometricPrompt.AUTHENTICATION_RESULT_TYPE_UNKNOWN -> {
//                        return
//                    }
//                }
                result.cryptoObject?.cipher?.let { cipher->
                    cipher.doFinal(server.password.toByteArray(Charset.defaultCharset()))?.let { cipherResult->
                        val encodedPassword = Base64Utils.encrypt(cipherResult)
//                        Log.d("jingtian", "onAuthenticationSucceeded: password=${server.password}, encodedPassword=${encodedPassword}")
                        server.password = encodedPassword
                        server.passwordIv = Base64Utils.encrypt(cipher.iv)
                    }
                }
                storage.addServer(server)
                onSuccess()
            }
        })
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

class SftpServerStateHolder(private val sftpServer: SftpServer = SftpServer()) : ServerStateHolder(sftpServer, ServerType.SFTP) {
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
        LazyColumn(
            Modifier
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
    val context = LocalContext.current
    val viewModel: AndroidMigrateViewModel = viewModel(factory = viewModelFactory { initializer { AndroidMigrateViewModel() } })
    if (!sftpServer.authed && isEdit) {
        val cipher = sftpServer.get().getDecryptCipher(sftpServer.get())
        viewModel.bioticAuth.doAuth(cipher, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                cipher?.doFinal(
                    Base64Utils.decryptAsByteArray(sftpServer.password)
                )?.toString(Charset.defaultCharset())?.let { password ->
                    sftpServer.password = password
                }
//                Log.d("jingtian", "newSession: pwd=${sftpServer.password}")
                sftpServer.authed = true
            }
        })
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
                addSftpServer(viewModel, sftpServer, context) {
                    onDismiss()
                }
            } else {
                updateHolder(viewModel, sftpServer, context) {
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
                OutlinedTextField(sftpServer.port.toString(),
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
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Password,
                        capitalization = KeyboardCapitalization.None
                    ),
                    visualTransformation = PasswordVisualTransformation()
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
fun ColumnScope.SftpServerDescView(sftpServer: SftpServerStateHolder, onClick: ()->Unit, onLongPress: ()->Unit) {
    val viewModel: AndroidMigrateViewModel = viewModel(factory = viewModelFactory { initializer { AndroidMigrateViewModel() } })
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .pointerInput(sftpServer) {
                detectTapGestures(
                    onTap = {
                        viewModel.bioticAuth.doAuth(object :
                            BiometricPrompt.AuthenticationCallback() {
                            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                super.onAuthenticationSucceeded(result)
                                onClick()
                            }
                        })
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