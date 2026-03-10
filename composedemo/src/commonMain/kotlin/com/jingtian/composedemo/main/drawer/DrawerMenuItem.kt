package com.jingtian.composedemo.main.drawer

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawerMenuItem(
    item: Album,
    enableEdit: Boolean,
    onItemClick: () -> Unit,
) {
    var deleteConfirmDialogState by remember { mutableStateOf(false) }
    var albumName by remember { mutableStateOf(item.albumName) }
    var changeNameJob by remember { mutableStateOf<Job?>(null) }
    val viewModel: AlbumViewModel = viewModel(factory = AlbumViewModel.viewModelFactory)
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
                    deleteConfirmDialogState = true
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
            AppThemeText(
                text = albumName.trim(),
                style = LocalTextStyle.current.copy(fontSize = 20.sp),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LocalAppPalette.current.drawerBg)
                    .wrapContentHeight()
                    .padding(8.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }

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