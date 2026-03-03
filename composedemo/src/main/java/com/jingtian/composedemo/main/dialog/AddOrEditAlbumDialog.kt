package com.jingtian.composedemo.main.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jingtian.composedemo.base.AppThemeDialog
import com.jingtian.composedemo.base.AppThemeText
import com.jingtian.composedemo.dao.model.Album
import com.jingtian.composedemo.ui.theme.LocalAppPalette
import com.jingtian.composedemo.ui.theme.LocalAppUIConstants
import com.jingtian.composedemo.viewmodels.AlbumViewModel

@Composable
fun AddOrEditAlbumDialog(album: Album? = null, onDismiss: () -> Unit) {
    val viewModel: AlbumViewModel = viewModel()
    var albumName by remember { mutableStateOf(album?.albumName ?: "") }
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
            if (albumName.isNullOrBlank()) {
                viewModel.sendMessage("添加/编辑失败: 合集名称不能为空")
                return@onPositive
            }
            onDismiss()
            if (album != null) {
                viewModel.editAlbum(Album(albumName = albumName, albumId = album.albumId))
            } else {
                viewModel.addAlbum(Album(albumName = albumName))
            }
        }
    ) { _, actionButtons ->
        Column(Modifier.padding(horizontal = 8.dp)) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(albumName, { value ->
                albumName = value
            }, label = {
                AppThemeText("合集名称")
            }, modifier = Modifier.focusRequester(focusRequester))
            actionButtons()
        }
    }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}