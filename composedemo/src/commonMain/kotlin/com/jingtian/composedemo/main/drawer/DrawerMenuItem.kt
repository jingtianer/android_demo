package com.jingtian.composedemo.main.drawer

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jingtian.composedemo.base.AppThemeConfirmDialog
import com.jingtian.composedemo.base.AppThemeText
import com.jingtian.composedemo.base.resources.DrawableIcon
import com.jingtian.composedemo.dao.model.Album
import com.jingtian.composedemo.ui.theme.LocalAppPalette
import com.jingtian.composedemo.viewmodels.AlbumViewModel
import kotlinx.coroutines.Job
import com.jingtian.composedemo.base.resources.getPainter
import com.jingtian.composedemo.main.dialog.AddHttpServerDialog
import com.jingtian.composedemo.main.http.api.api
import com.jingtian.composedemo.main.http.dao.HttpServerEntity
import com.jingtian.composedemo.main.settings.CollapseDrawer
import com.jingtian.composedemo.main.settings.rememberCollapseDrawerState
import com.jingtian.composedemo.viewmodels.albumViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

@Composable
fun DrawerServerItem(
    item: HttpServerEntity,
    onItemClick: (album: Album) -> Unit,
) {
    var deleteConfirmDialogState by remember { mutableStateOf(false) }
    val api = remember(item) { item.api() }
    var childAlbum by remember(item) { mutableStateOf(emptyList<Album>()) }
    val drawerState = rememberCollapseDrawerState(false)
    val viewModel = albumViewModel()
    var editHttpDialogState by remember { mutableStateOf(false) }
    LaunchedEffect(item) {
        childAlbum = withContext(Dispatchers.IO) {
            api.getAlbumList()
        }
    }

    DrawerItem(title = item.serverName, leadingDrawableIcon = DrawableIcon.DrawableHttp, onItemClick = {
        drawerState.expand.value = !drawerState.expand.value
    }, onDelete = {
        deleteConfirmDialogState = true
    }, swipeContent = { content->
        CollapseDrawer(
            modifier = Modifier.fillMaxWidth(),
            collapseDrawerState = drawerState,
            header = {
                content()
            }
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(start = 38.dp).wrapContentHeight()) {
                Box(modifier = Modifier.fillMaxWidth().pointerInput(Unit) {
                    detectTapGestures(onLongPress = {
                        editHttpDialogState = true
                    })
                }) {
                    OutlinedTextField("Host: ${item.hostName}", { _ ->
                    }, label = {
                        AppThemeText("port: ${item.port}")
                    }, enabled = false, modifier = Modifier.fillMaxWidth())
                }

                childAlbum.forEach { album: Album ->
                    AppThemeText(
                        text = album.albumName.trim(),
                        style = LocalTextStyle.current.copy(fontSize = 18.sp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(LocalAppPalette.current.drawerBg)
                            .height(26.dp)
                            .padding(horizontal = 8.dp)
                            .clickable { onItemClick(album) },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    })

    if (deleteConfirmDialogState) {
        AppThemeConfirmDialog(
            "确认删除server: ${item.serverName}",
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnClickOutside = false
            ),
            onNegative = {
                deleteConfirmDialogState = false
            },
            onPositive = {
                deleteConfirmDialogState = false
                viewModel.deleteServer(item)
            },
            onDismissRequest = {
                deleteConfirmDialogState = false
            },
            content = {})
    }

    if (editHttpDialogState) {
        AddHttpServerDialog(httpServerEntity = item) {
            editHttpDialogState = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawerMenuItem(
    item: Album,
    onItemClick: () -> Unit,
) {
    var deleteConfirmDialogState by remember { mutableStateOf(false) }
    val albumName = item.albumName
    val viewModel: AlbumViewModel = albumViewModel()

    DrawerItem(albumName, leadingDrawableIcon = DrawableIcon.DrawableGallery, onItemClick = onItemClick, onDelete = {
        deleteConfirmDialogState = true
    })
    if (deleteConfirmDialogState) {
        AppThemeConfirmDialog(
            "确认删除合集: ${item.albumName}",
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnClickOutside = false
            ),
            onNegative = {
                deleteConfirmDialogState = false
            },
            onPositive = {
                deleteConfirmDialogState = false
                viewModel.deleteAlbum(item)
            },
            onDismissRequest = {
                deleteConfirmDialogState = false
            },
            content = {})
    }
}

@Composable
private fun DrawerItem(
    title: String,
    leadingDrawableIcon: DrawableIcon,
    swipeContent: @Composable (@Composable () -> Unit) -> Unit = { it.invoke() },
    onItemClick: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    val size = 36.dp
    val modifier = Modifier
        .fillMaxWidth()
        .wrapContentHeight()
        .clip(RectangleShape)
        .clickable { onItemClick() }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val swipeToDismissBoxState = rememberSwipeToDismissBoxState(confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    onDelete()
                    false
                }

                else -> false
            }
        })
        SwipeToDismissBox(
            swipeToDismissBoxState,
            modifier = Modifier.fillMaxHeight(),
            enableDismissFromEndToStart = false,
            backgroundContent = {
                Row {
                    Image(
                        painter = getPainter(DrawableIcon.DrawableTrashBin),
                        contentDescription = "删除",
                        Modifier
                            .align(Alignment.CenterVertically)
                            .size(size)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(size))
                            .padding(4.dp),
                    )
                }
            }) {
            swipeContent {
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .background(LocalAppPalette.current.drawerBg)
                        .wrapContentHeight()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(getPainter(leadingDrawableIcon), contentDescription = "", modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(10.dp))
                    AppThemeText(
                        text = title.trim(),
                        style = LocalTextStyle.current.copy(fontSize = 20.sp),
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}