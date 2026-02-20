package com.jingtian.composedemo.ui.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jingtian.composedemo.Gallery
import com.jingtian.composedemo.MainDrawer
import com.jingtian.composedemo.dao.model.Album
import kotlinx.coroutines.launch

@Preview
@Composable
fun MainPreView() {
    val menuItemsEntity = (0 until 10).map { Album(albumName = "测试$it") }
    var currentSelectedAlbum by remember { mutableStateOf(IndexedValue(0, menuItemsEntity[0])) }
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    ModalNavigationDrawer(
        {
//            MainDrawer(menuItemsEntity) { index, album ->
//                currentSelectedAlbum = IndexedValue(index, album)
//                scope.launch {
//                    drawerState.close()
//                }
//            }
            ModalDrawerSheet(Modifier.requiredWidth(LocalConfiguration.current.screenWidthDp.dp)) {
                Box(Modifier.fillMaxSize().background(Color.Red))
            }
        },
        Modifier.fillMaxSize(),
        drawerState = drawerState,
        gesturesEnabled = menuItemsEntity.isNotEmpty()
    ) {
//        Gallery(currentSelectedAlbum)
        Box(Modifier.fillMaxSize().background(Color.Blue))
    }
}