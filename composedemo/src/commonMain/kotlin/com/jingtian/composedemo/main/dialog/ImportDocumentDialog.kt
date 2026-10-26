package com.jingtian.composedemo.main.dialog

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.staggeredgrid.LazyHorizontalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jingtian.composedemo.BuildKonfig
import com.jingtian.composedemo.base.AppThemeDialog
import com.jingtian.composedemo.base.AppThemeText
import com.jingtian.composedemo.base.resources.getPainter
import com.jingtian.composedemo.dao.DataBase
import com.jingtian.composedemo.dao.model.Album
import com.jingtian.composedemo.dao.model.FileType
import com.jingtian.composedemo.main.drawer.ImmutableDrawerMenuItem
import com.jingtian.composedemo.main.labels.CheckableLabelView
import com.jingtian.composedemo.main.labels.EditableLabelView
import com.jingtian.composedemo.main.remote.ServerStorage
import com.jingtian.composedemo.main.remote.ServerType
import com.jingtian.composedemo.main.remote.SftpServer
import com.jingtian.composedemo.main.remote.SftpServerDescView
import com.jingtian.composedemo.main.remote.toHolder
import com.jingtian.composedemo.main.widget.RankChooser
import com.jingtian.composedemo.main.widget.ScoreChooser
import com.jingtian.composedemo.multiplatform.MultiplatformFile
import com.jingtian.composedemo.multiplatform.logD
import com.jingtian.composedemo.navigation.IDocumentTreePicker
import com.jingtian.composedemo.navigation.rememberDocumentTreePicker
import com.jingtian.composedemo.ui.theme.LocalAppPalette
import com.jingtian.composedemo.ui.theme.LocalAppUIConstants
import com.jingtian.composedemo.utils.splitByWhiteSpace
import com.jingtian.composedemo.viewmodels.AlbumViewModel
import com.jingtian.composedemo.web.CommonWebView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class ImportDocumentDialogState(val album: Album) {
    val selectedUri = mutableStateOf<MultiplatformFile?>(null)
    val selectedServer = mutableStateOf<SftpServer?>(null)
    val serverList = ServerStorage.getStorage<SftpServer>(ServerType.SFTP).allServer()
    var selectedFileName = mutableStateOf("None")
    var selectedFilePath = mutableStateOf("None")
    var isUploadingFile = mutableStateOf(false)

    fun importFile(albumViewModel: AlbumViewModel, onFinish:() -> Unit) {
        if (isUploadingFile.value) {
            sendBusyMessage(albumViewModel)
            return
        }
        val selectedUri = selectedUri.value
        if (selectedUri == null) {
            albumViewModel.sendMessage("还没有选择目录呢")
            return
        }
        if (BuildKonfig.isRemote) {
            val selectedServer = selectedServer.value
            if (selectedServer == null) {
                albumViewModel.sendMessage("还没有选择服务器呢")
                return
            }
            isUploadingFile.value = true
            albumViewModel.uploadFiles(album, selectedServer, selectedUri) {
                isUploadingFile.value = true
                onFinish()
            }
        } else {
            albumViewModel.importFiles(album, selectedUri)
        }
    }

    fun sendBusyMessage(albumViewModel: AlbumViewModel) {
        albumViewModel.sendMessage("正在上传中 请稍后~")
    }
}

@Composable
fun rememberDocumentDialogState(album: Album): ImportDocumentDialogState {
    val state = remember(album) { ImportDocumentDialogState(album) }
    LaunchedEffect(album, state, state.selectedUri.value) {
        val (selectedFileName, selectedFilePath) = withContext(Dispatchers.IO) {
            state.selectedUri.value?.let {
                (it.fileName() ?: "null") to it.path()
            } ?: ("None" to "None")
        }
        state.selectedFileName.value = selectedFileName
        state.selectedFilePath.value = selectedFilePath
    }
    return state
}

@Composable
fun ImportDocumentDialog(dialogState: ImportDocumentDialogState, onDismiss: () -> Unit) {
    var currentSelectedServer by dialogState.selectedServer
    val serverList = dialogState.serverList
    val selectedFileName by dialogState.selectedFileName
    val selectedFilePath by dialogState.selectedFilePath
    var isUploadingFile by dialogState.isUploadingFile
    val viewModel: AlbumViewModel = viewModel(factory = AlbumViewModel.viewModelFactory)

    val scope = rememberCoroutineScope()
    val importDirLauncher by rememberDocumentTreePicker { uri->
        dialogState.selectedUri.value = uri
        scope.launch {
            val path = uri?.path()
            logD("ImportDocumentDialog") {
                "import: $path"
            }
        }
    }

    AppThemeDialog(
        Modifier
            .fillMaxWidth(LocalAppUIConstants.current.dialogPercent)
            .fillMaxHeight(LocalAppUIConstants.current.dialogPercent)
            .wrapContentHeight(),
        onDismissRequest = {},
        properties = DialogProperties(usePlatformDefaultWidth = false),
        onNegative = {
            if (isUploadingFile) {
                dialogState.sendBusyMessage(viewModel)
                return@AppThemeDialog
            }
            onDismiss()
         },
        onPositive = {
            dialogState.importFile(viewModel) {
                onDismiss()
            }
        }
    ) { _, actionButtons ->
        var openServerList by remember { mutableStateOf(false) }
        Box {
            LazyColumn(
                Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(LocalAppPalette.current.dialogBg)
            ) {
                item {
                    AppThemeText("选择目录: ${selectedFileName}\n $selectedFilePath",
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                importDirLauncher.launch(null)
                            }
                            .padding(horizontal = 6.dp, vertical = 10.dp))
                }
                if (BuildKonfig.isRemote) {
                    item {
                        AppThemeText("选择server: ${currentSelectedServer?.serverName ?: "None"}",
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    openServerList = !openServerList
                                }
                                .padding(horizontal = 6.dp, vertical = 10.dp))
                    }
                }

                if (openServerList) {
                    items(count = serverList.size) { serverIdx->
                        val server = serverList[serverIdx]
                        SftpServerDescView(
                            sftpServer = server.toHolder(),
                            onClick = {
                                if (!isUploadingFile) {
                                    currentSelectedServer = server
                                    openServerList = false
                                } else {
                                    dialogState.sendBusyMessage(viewModel)
                                }
                            },
                            onLongPress = {

                            }
                        )
                    }
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
    }
}