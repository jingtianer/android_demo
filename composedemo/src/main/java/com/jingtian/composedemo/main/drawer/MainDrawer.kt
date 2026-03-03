package com.jingtian.composedemo.main.drawer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jingtian.composedemo.base.AppThemeHorizontalDivider
import com.jingtian.composedemo.dao.DataBase
import com.jingtian.composedemo.dao.model.Album
import com.jingtian.composedemo.main.AppThemeSwitcher
import com.jingtian.composedemo.ui.theme.LocalAppPalette

@Composable
fun MainDrawer(
    albumData: List<Album>,
    onAlbumSelected: (Int, Album) -> Unit,
) {
    val enableEdit = false
//    var enableEdit by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxHeight()
            .fillMaxWidth()
            .background(LocalAppPalette.current.drawerBg)
//            .fillMaxWidth(LocalAppUIConstants.current.drawerMaxPercent)
    ) {
        LazyColumn(
            Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            item {
                DrawerHeader()
            }
            item {
                Box(Modifier.fillMaxWidth()) {
                    AppThemeHorizontalDivider(
                        modifier = Modifier
                            .height(1.dp)
                            .fillMaxWidth(0.95f)
                            .align(Alignment.Center)
                    )
                }
            }
            items(
                albumData.size,
                key = { index: Int ->
                    (albumData[index].albumId ?: DataBase.INVALID_ID) to albumData[index].albumName
                }) { index ->
                val item = albumData[index]
                DrawerMenuItem(item, enableEdit) {
                    onAlbumSelected(index, item)
                }
            }
        }
        AppThemeSwitcher()
    }
}