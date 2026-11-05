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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jingtian.composedemo.base.AppThemeHorizontalDivider
import com.jingtian.composedemo.base.AppDrawerBottomFunctionLine
import com.jingtian.composedemo.base.resources.DrawableIcon
import com.jingtian.composedemo.base.resources.getPainter
import com.jingtian.composedemo.dao.DataBase
import com.jingtian.composedemo.dao.model.Album
import com.jingtian.composedemo.main.route.LocalRouterController
import com.jingtian.composedemo.ui.theme.LocalAppPalette
import kotlinx.coroutines.launch

@Composable
fun MainDrawer(
    albumData: List<Album>,
    onAlbumSelected: (Int, Album) -> Unit,
) {
    Column(
        Modifier
            .fillMaxHeight()
            .fillMaxWidth()
            .background(LocalAppPalette.current.drawerBg)
//            .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Vertical + WindowInsetsSides.Start))
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
                DrawerMenuItem(item) {
                    onAlbumSelected(index, item)
                }
            }
        }
        val routerController = LocalRouterController.current
        val scope = rememberCoroutineScope()
        DrawerFunctionView(
            onClick = {
                scope.launch {
                    routerController.toSettings()
                }
            },
            painter = getPainter(DrawableIcon.DrawableSettings),
            text = "设置",
        )
        AppDrawerBottomFunctionLine()
    }
}