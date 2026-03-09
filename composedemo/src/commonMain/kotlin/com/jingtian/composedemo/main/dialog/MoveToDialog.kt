package com.jingtian.composedemo.main.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jingtian.composedemo.base.AppThemeDialog
import com.jingtian.composedemo.base.AppThemeText
import com.jingtian.composedemo.dao.model.Album
import com.jingtian.composedemo.dao.model.relation.AlbumItemRelation
import com.jingtian.composedemo.main.drawer.ImmutableDrawerMenuItem
import com.jingtian.composedemo.ui.theme.LocalAppPalette
import com.jingtian.composedemo.ui.theme.LocalAppUIConstants
import com.jingtian.composedemo.viewmodels.AlbumViewModel

@Composable
fun MoveToDialog(albumItemRelation: Collection<AlbumItemRelation>, album: Album, albumList: List<Album>, onDismiss: () -> Unit) {
    var currentSelectedAlbum by remember { mutableStateOf(album) }
    val viewModel: AlbumViewModel = viewModel(factory = AlbumViewModel.viewModelFactory)
    AppThemeDialog(
        Modifier
            .fillMaxWidth(LocalAppUIConstants.current.dialogPercent)
            .background(LocalAppPalette.current.dialogBg)
            .padding(horizontal = 8.dp),
        onNegative = onDismiss, onPositive = {
            if (album.albumId != currentSelectedAlbum.albumId) {
                viewModel.moveItems(currentSelectedAlbum, albumItemRelation)
            }
            onDismiss()
        }) { _, actionButtons ->
        Column(Modifier.fillMaxHeight(LocalAppUIConstants.current.dialogPercent)) {
            LazyColumn(
                Modifier
                    .padding(horizontal = 8.dp, vertical = 12.dp)
                    .fillMaxSize()
                    .weight(1f)
            ) {
                item {
                    AppThemeText(
                        "移动到: ${currentSelectedAlbum.albumName}",
                        style = LocalTextStyle.current.copy(fontSize = 16.sp)
                    )
                    Spacer(Modifier.height(8.dp))
                }
                items(albumList.size) { index ->
                    val item = albumList[index]
                    ImmutableDrawerMenuItem(
                        item,
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 10.dp)
                    ) {
                        currentSelectedAlbum = item
                    }
                }
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                actionButtons()
            }
        }
    }
}