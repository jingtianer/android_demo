package com.jingtian.composedemo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.DrawerState
import androidx.compose.runtime.Composable
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jingtian.composedemo.base.AppThemeText
import com.jingtian.composedemo.base.BaseActivity
import com.jingtian.composedemo.dao.DataBase.Companion.dbImpl
import com.jingtian.composedemo.dao.model.Album
import com.jingtian.composedemo.ui.theme.AppPalette
import com.jingtian.composedemo.ui.theme.LocalAppPalette
import com.jingtian.composedemo.ui.theme.LocalAppUIConstants
import com.jingtian.composedemo.viewmodels.AlbumViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MainActivity : BaseActivity() {
    @Composable
    override fun content() = Main()
}

@Preview
@Composable
fun Main() {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Open)
    val viewModel: AlbumViewModel = viewModel()
    var menuItemsEntity by remember { mutableStateOf(emptyList<Album>()) }
    val rememberScope = rememberCoroutineScope()
    var currentSelectedAlbum by remember { mutableStateOf<Album?>(null) }
    var currentCollectJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(drawerState.isOpen) {
        if (drawerState.isOpen) {
            currentCollectJob?.cancel()
            currentCollectJob = rememberScope.launch {
                viewModel.menuItemsFlow.collect { value ->
                    menuItemsEntity = value
                }
            }
        } else {
            currentCollectJob?.cancel()
        }
    }

    ModalNavigationDrawer(
        {
            MainDrawer(drawerState, menuItemsEntity) { currentSelectedAlbum = it }
        },
        Modifier.fillMaxSize(),
        drawerState = drawerState
    ) {
        Column(Modifier.fillMaxSize()) {
            AppThemeText(
                currentSelectedAlbum?.albumName ?: "没有选择",
                Modifier
                    .fillMaxSize()
                    .align(Alignment.CenterHorizontally),
            )
        }
    }
}

@Composable
fun DrawerHeader() {

}

@Composable
fun MainDrawer(
    drawerState: DrawerState,
    albumData: List<Album>,
    onAlbumSelected: (Album) -> Unit
) {
    if (drawerState.isClosed) {
        return
    }
    val rememberScope = rememberCoroutineScope()
    Column(
        Modifier
            .fillMaxHeight()
            .fillMaxWidth(LocalAppUIConstants.current.drawerMaxPercent)
            .background(LocalAppPalette.current.drawerBg)) {
        DrawerHeader()
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn {
            items(albumData.size) { index ->
                val item = albumData[index]
                key(item.albumId) {
                    DrawerMenuItem(item, drawerState) {
                        rememberScope.launch {
                            onAlbumSelected(item)
                            drawerState.close()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DrawerMenuItem(
    item: Album,
    drawerState: DrawerState,
    onItemClick: () -> Unit
) {
    val modifier = if (drawerState.isOpen) {
        Modifier.clickable {
            onItemClick()
        }
    } else {
        Modifier
    }
        .padding(16.dp)
        .fillMaxWidth()
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppThemeText(
            text = item.albumName,
        )
    }
}

@Composable
fun AddAlbumDialog() {

}