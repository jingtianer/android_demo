package com.jingtian.composedemo.main.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jingtian.composedemo.BuildKonfig
import com.jingtian.composedemo.base.AppThemeDialog
import com.jingtian.composedemo.base.AppThemeText
import com.jingtian.composedemo.dao.model.Album
import com.jingtian.composedemo.main.http.dao.HttpServerEntity
import com.jingtian.composedemo.ui.theme.LocalAppPalette
import com.jingtian.composedemo.ui.theme.LocalAppUIConstants
import com.jingtian.composedemo.viewmodels.AlbumViewModel
import com.jingtian.composedemo.viewmodels.albumViewModel
import kotlin.random.Random

@Composable
fun AddHttpServerDialog(httpServerEntity: HttpServerEntity?, onDismiss: () -> Unit) {
    if (BuildKonfig.isDesktop) {
        return
    }
    val viewModel: AlbumViewModel = albumViewModel()
    var serverName by remember {
        mutableStateOf(
            httpServerEntity?.hostName ?: "新建 ${Random.nextLong(from = 0L, until = Long.MAX_VALUE)}"
        )
    }
    var host by remember { mutableStateOf(httpServerEntity?.hostName ?: "localhost") }
    var port by remember { mutableStateOf(httpServerEntity?.port ?: "8000") }
    val focusRequester = remember { FocusRequester() }
    AppThemeDialog(
        Modifier
            .fillMaxWidth(LocalAppUIConstants.current.dialogPercent)
            .wrapContentHeight()
            .clip(RoundedCornerShape(4.dp))
            .background(LocalAppPalette.current.dialogBg),
        onDismissRequest = onDismiss,
        onNegative = onDismiss,
        onPositive = onPositive@{
            onDismiss()
            if (port.toIntOrNull() == null) {
                viewModel.sendMessage("$port 不是合法的端口")
            } else {
                if (httpServerEntity != null) {
                    viewModel.editHttpServer(
                        HttpServerEntity(
                            serverId = httpServerEntity.serverId,
                            serverName = serverName,
                            hostName = host,
                            port = port
                        )
                    )
                } else {
                    viewModel.addHttpServer(HttpServerEntity(
                        serverName = serverName,
                        hostName = host,
                        port = port
                    ))
                }
            }
        }
    ) { _, actionButtons ->
        Column(Modifier.padding(horizontal = 8.dp)) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(serverName, { value ->
                serverName = value
            }, label = {
                AppThemeText("服务器名称")
            }, modifier = Modifier.focusRequester(focusRequester))
            OutlinedTextField(host, { value ->
                host = value
            }, label = {
                AppThemeText("服务器host")
            })
            OutlinedTextField(port, { value ->
                port = value
            }, label = {
                AppThemeText("服务器端口")
            }, keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number))
            actionButtons()
        }
    }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}