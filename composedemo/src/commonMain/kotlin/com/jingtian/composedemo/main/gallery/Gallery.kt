package com.jingtian.composedemo.main.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jingtian.composedemo.base.AppThemeConfirmDialog
import com.jingtian.composedemo.base.AppThemeText
import com.jingtian.composedemo.base.BackPressHandler
import com.jingtian.composedemo.base.resources.DrawableIcon
import com.jingtian.composedemo.dao.DataBase
import com.jingtian.composedemo.dao.model.Album
import com.jingtian.composedemo.dao.model.relation.AlbumItemRelation
import com.jingtian.composedemo.main.dialog.AddItemDialog
import com.jingtian.composedemo.main.dialog.AddOrEditAlbumDialog
import com.jingtian.composedemo.main.albumItem.AlbumItemView
import com.jingtian.composedemo.main.dialog.EditDialog
import com.jingtian.composedemo.main.gallery.GalleryFunctions.ADD
import com.jingtian.composedemo.main.gallery.GalleryFunctions.DELETE
import com.jingtian.composedemo.main.gallery.GalleryFunctions.EDIT
import com.jingtian.composedemo.main.gallery.GalleryFunctions.EXIT
import com.jingtian.composedemo.main.gallery.GalleryFunctions.IMPORT
import com.jingtian.composedemo.main.gallery.GalleryFunctions.MOVE
import com.jingtian.composedemo.main.gallery.GalleryFunctions.RENAME
import com.jingtian.composedemo.main.gallery.GalleryFunctions.SELECT_ALL
import com.jingtian.composedemo.main.gallery.GalleryFunctions.SELECT_NONE
import com.jingtian.composedemo.main.dialog.MoveToDialog
import com.jingtian.composedemo.main.albumItem.AlbumItemViewStateHolder
import com.jingtian.composedemo.navigation.rememberDocumentTreePicker
import com.jingtian.composedemo.ui.theme.LocalAppColorScheme
import com.jingtian.composedemo.ui.theme.LocalAppPalette
import com.jingtian.composedemo.ui.theme.LocalAppUIConstants
import com.jingtian.composedemo.ui.theme.LocalMiddleButtonConfig
import com.jingtian.composedemo.ui.theme.appBackground
import com.jingtian.composedemo.utils.dpValue
import com.jingtian.composedemo.utils.getOrPutRef
import com.jingtian.composedemo.viewmodels.AlbumViewModel
import com.jingtian.composedemo.viewmodels.AlbumViewModel.Companion.observeAsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.ref.SoftReference
import com.jingtian.composedemo.base.resources.getPainter

@Composable
fun Gallery(galleryStateHolderMap: SnapshotStateMap<Long, SoftReference<GalleryStateHolder>>, album: IndexedValue<Album>?, albumList: List<Album>, drawerState: DrawerState) {
    if (album == null) {
        return
    }
    val albumId: Long = album.value.albumId ?: return
    val viewModel: AlbumViewModel = viewModel(factory = AlbumViewModel.viewModelFactory)
    val galleryStateHolder by remember(album, albumList) {
        mutableStateOf(
            galleryStateHolderMap.getOrPutRef(albumId) {
                GalleryStateHolder(album, albumList, drawerState, viewModel)
            }
        )
    }
    galleryStateHolder.Gallery()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryStateHolder.Gallery() {
    val albumItemDataChange by viewModel.albumItemListChange.observeAsState()
    val scope = rememberCoroutineScope()
    val albumNameChange by viewModel.albumNameChange.observeAsState()
//    LaunchedEffect(album) {
//        initAlbumInfo()
//    }

    LaunchedEffect(albumItemDataChange) {
        onAlbumDataChanged()
    }

    LaunchedEffect(albumNameChange) {
        onAlbumNameChanged()
    }

    val importDirLauncher by rememberDocumentTreePicker { uri->
        uri ?: return@rememberDocumentTreePicker
        viewModel.importFiles(album.value, uri)
    }

    val filterChanged by viewModel.filterCheckChanged.observeAsState()
    LaunchedEffect(filterChanged, itemList) {
        updateFilterList()
    }

    Column(
        Modifier
            .fillMaxSize()
            .appBackground()
            .windowInsetsPadding(WindowInsets.systemBars)) {
        CompositionLocalProvider(LocalContentColor provides LocalAppPalette.current.galleryHeaderColor, LocalTextStyle provides LocalTextStyle.current.copy(color = LocalAppPalette.current.galleryHeaderColor)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp)
                    .wrapContentHeight()
            ) {
                Icon(
                    painter = getPainter(DrawableIcon.DrawableDrawer),
                    contentDescription = "打开drawer",
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .size(LocalAppUIConstants.current.filterLabelHeight)
                        .background(LocalAppPalette.current.labelUnChecked, shape = CircleShape)
                        .clickable {
                            scope.launch {
                                if (drawerState.isOpen) {
                                    drawerState.close()
                                } else {
                                    drawerState.open()
                                }
                            }
                        }
                        .padding(4.dp)
                )
                Spacer(Modifier.width(12.dp))
                AppThemeText(albumName.trim(),
                    Modifier
                        .align(Alignment.CenterVertically)
                        .fillMaxWidth()
                        .weight(1f), style = LocalTextStyle.current.copy(fontSize = 24.sp, fontWeight = FontWeight(600)),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.width(6.dp))
                Icon(
                    painter = getPainter(DrawableIcon.DrawableEditNormal),
                    contentDescription = "编辑模式",
                    modifier = Modifier
                        .size(LocalAppUIConstants.current.filterLabelHeight)
                        .background(LocalAppPalette.current.labelUnChecked, shape = CircleShape)
                        .clickable { enterEditMode = !enterEditMode }
                        .padding(4.dp)
                        .align(Alignment.CenterVertically)
                )
            }

        }
        CompositionLocalProvider(LocalContentColor provides LocalAppPalette.current.galleryHeaderColor, LocalTextStyle provides LocalTextStyle.current.copy(color = LocalAppPalette.current.galleryHeaderColor)) {
            if (enterEditMode) {
                Row(
                    Modifier
                        .padding(start = 4.dp, end = 8.dp)
                        .fillMaxWidth()) {
                    LazyRow(
                        Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .weight(1f)) {
                        roundRectTabFilter(labelFilterCheckedInfo, totalLabelList)
                        roundRectTabFilter(fileTypeCheckState, totalFileTypeList)
                        roundRectTabFilter(itemRankTypeCheckState, totalItemRankList)
                    }

                    Icon(
                        painter = getPainter(DrawableIcon.DrawableDown),
                        contentDescription = "过滤器",
                        modifier = Modifier
                            .size(LocalAppUIConstants.current.filterLabelHeight)
                            .background(LocalAppPalette.current.labelUnChecked, shape = CircleShape)
                            .clickable { showLabelFilter = !showLabelFilter }
                            .padding(4.dp)
                            .align(Alignment.CenterVertically)
                    )
                }
            }
        }
        fun getScrollOffset(): Float {
            return galleryScrollState.layoutInfo.visibleItemsInfo.map { it.index - it.offset.y.toFloat() / it.size.height }.average().toFloat()
        }

        Box(
            Modifier
                .fillMaxSize()
                .weight(1f)) {
            val bottomBarHeight = 62.dp
            val shadesHeight = 0.dp
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Adaptive((size + galleryItemPadding * 2)),
                Modifier
                    .fillMaxSize()
                    .padding(start = galleryItemPadding, end = galleryItemPadding)
                    .nestedScroll(object : NestedScrollConnection {
                        override fun onPostScroll(
                            consumed: Offset,
                            available: Offset,
                            source: NestedScrollSource
                        ): Offset {
//                            val info =
//                                galleryScrollState.layoutInfo.visibleItemsInfo
//                                    .map { "{${it.index}, ${it.offset.y}, ${it.size.height}}" }
//                                    .fastJoinToString { it }
                            scrollOffsetY = getScrollOffset() / galleryScrollState.layoutInfo.totalItemsCount * galleryScrollState.layoutInfo.viewportSize.height.toFloat()
                            updateScrollOffset()
//                            Log.d("jingtian", "onPostScroll: $info")
                            return super.onPostScroll(consumed, available, source)
                        }
                    }),
                contentPadding = PaddingValues(bottom = if (enterEditMode) bottomBarHeight + shadesHeight else 0.dp),
                state = galleryScrollState,
            ) {
                items(filteredItemList.size, key = { index-> filteredItemList[index].hashCode() }, contentType = { index-> filteredItemList[index].fileInfo.fileType.value }) { index: Int ->
                    AlbumItemView(albumViewMap, filteredItemList[index], size, galleryItemPadding, currentSelectedItem, enterEditModeState, itemSelectStateChangeState, showEditDialogStateOnLongClickState)
                }
            }
            val scrollBarColor = LocalAppPalette.current.labelTextColor.copy(alpha = 0.85f)
            Box(
                Modifier
                    .width(scrollAreaWidth)
                    .fillMaxHeight()
                    .align(Alignment.TopEnd)
                    .pointerInput(this@Gallery) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                scrollOffsetY = offset.y
                                scrollBarSize[0] = 16.dp.dpValue()
                                scrollBarOffset[0] = scrollAreaWidth.dpValue() - scrollBarSize[0]
                                updateScrollOffset()
                                scope.launch {
                                    updateScrollItem()
                                }
                            },
                            onDrag = { pointerInputChange, offset ->
                                scrollOffsetY += offset.y
                                scrollBarSize[0] = 16.dp.dpValue()
                                scrollBarOffset[0] = scrollAreaWidth.dpValue() - scrollBarSize[0]
                                updateScrollOffset()
                                scope.launch {
                                    updateScrollItem()
                                }
                            },
                            onDragEnd = {
                                scrollBarSize[0] = 6.dp.dpValue()
                                scrollBarOffset[0] = scrollAreaWidth.dpValue() - scrollBarSize[0]
                            },
                            onDragCancel = {
                                scrollBarSize[0] = 6.dp.dpValue()
                                scrollBarOffset[0] = scrollAreaWidth.dpValue() - scrollBarSize[0]
                            }
                        )
                    }
                    .pointerInput(this@Gallery) {
                        awaitEachGesture {
                            val downEvent = awaitFirstDown()
                            scrollOffsetY = downEvent.position.y
                            scope.launch {
                                withContext(Dispatchers.Main) {
                                    scrollBarSize[0] = 16.dp.dpValue()
                                    scrollBarOffset[0] = scrollAreaWidth.dpValue() - scrollBarSize[0]
                                    updateScrollOffset()
                                    updateScrollItem()
                                }
                            }
                        }
                    }
                    .pointerInput(this@Gallery) {
                        awaitEachGesture {
                            val upOrCancel = waitForUpOrCancellation()
                            scrollOffsetY = upOrCancel?.position?.y ?: scrollOffsetY
                            scope.launch {
                                withContext(Dispatchers.Main) {
                                    scrollBarSize[0] = 6.dp.dpValue()
                                    scrollBarOffset[0] = scrollAreaWidth.dpValue() - scrollBarSize[0]
                                    updateScrollOffset()
                                    updateScrollItem()
                                }
                            }
                        }
                    }
                    .drawWithContent {
                        drawContent()
                        drawRoundRect(
                            scrollBarColor,
                            Offset(scrollBarOffset[0], scrollBarOffset[1]),
                            Size(scrollBarSize[0], scrollBarSize[1]),
                            cornerRadius = CornerRadius(scrollBarSize[0] / 2)
                        )
                    }
            )
            if (enterEditMode) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(bottomBarHeight)
                        .background(
                            LocalAppColorScheme.current.background.copy(alpha = 0.90f),
                        )
                        .align(Alignment.BottomCenter)
                ) {
                    Row(Modifier.align(Alignment.CenterStart)) {
                        if (selectAll) {
                            GalleryFunctionView(SELECT_ALL) {
                                scope.launch(Dispatchers.Default) {
                                    val map = HashMap<Long, AlbumItemRelation>().apply {
                                        putAll(filteredItemList.map { (it.albumItem.itemId?: DataBase.INVALID_ID) to it })
                                    }
                                    withContext(Dispatchers.IO) {
                                        currentSelectedItem.putAll(map)
                                    }
                                }
                                itemSelectStateChange++
                            }
                        } else if (selectNone) {
                            GalleryFunctionView(SELECT_NONE) {
                                currentSelectedItem.clear()
                                itemSelectStateChange++
                            }
                        }
                    }
                    Row(
                        Modifier
                            .wrapContentSize()
                            .padding(vertical = 6.dp)
                            .align(Alignment.Center)
                    ) {
                        for (item in currentFunctions) {
                            val func = item.key
                            when (func) {
                                ADD -> {
                                    GalleryFunctionView(func) {
                                        addImageDialogState = true
                                    }
                                }
                                IMPORT -> {
                                    GalleryFunctionView(func) {
                                        importDirLauncher.launch(null)
                                    }
                                }
                                RENAME -> {
                                    GalleryFunctionView(func) {
                                        editAlbumDialogState = true
                                    }
                                }
                                EDIT -> {
                                    GalleryFunctionView(func) {
                                        showEditDialog = true
                                    }
                                }
                                DELETE -> {
                                    GalleryFunctionView(func) {
                                        showConfirmDeleteDialog = true
                                    }
                                }
                                MOVE -> {
                                    GalleryFunctionView(func) {
                                        showMoveToDialog = true
                                    }
                                }
                                else -> {}
                            }
                        }
                    }
                    Row(Modifier.align(Alignment.CenterEnd)) {
                        GalleryFunctionView(EXIT) {
                            enterEditMode = false
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(currentSelectedItem, enterEditMode, itemSelectStateChange) {
        currentFunctions.clear()
        if (currentSelectedItem.size == filteredItemList.size) {
            selectNone = true
            selectAll = false
        } else {
            selectAll = true
            selectNone = false
        }
        if (enterEditMode) {
            val selectedCount = currentSelectedItem.size
            when(selectedCount) {
                0 -> {
                    currentFunctions.putAll(GalleryFunctions.albumFunctions)
                }
                1 -> {
                    currentFunctions.putAll(GalleryFunctions.itemFunctions)
                }
                else -> {
                    currentFunctions.putAll(GalleryFunctions.batchFunctions)
                }
            }
        } else {
            currentSelectedItem.clear()
        }
    }

    if (addImageDialogState) {
        AddItemDialog(album.value, totalLabelList, albumList) {
            addImageDialogState = false
        }
    }

    @Composable
    fun ShowDeleteConfirmDialog() {
        val title = if (currentSelectedItem.size > 1) {
            "选择的${currentSelectedItem.size}项"
        } else if (currentSelectedItem.size == 1) {
            currentSelectedItem.values.firstOrNull()?.albumItem?.itemName ?: ""
        } else {
            showConfirmDeleteDialog = false
            return
        }
        CompositionLocalProvider(
            LocalMiddleButtonConfig provides LocalMiddleButtonConfig.current.copy(
                text = "删除",
                colors = LocalMiddleButtonConfig.current.colors.copy(containerColor = LocalAppPalette.current.deleteButtonColor, contentColor = Color.White),
            )
        ) {
            AppThemeConfirmDialog("确认删除$title", reversed = true, onPositive = null, onMiddleClick = {
                viewModel.deleteItems(currentSelectedItem.values)
                showConfirmDeleteDialog = false
            }, onNegative = {
                showConfirmDeleteDialog = false
            }, onDismissRequest = {
                showConfirmDeleteDialog = false
            })
        }
    }

    if (showConfirmDeleteDialog) {
        ShowDeleteConfirmDialog()
    }

    if (editAlbumDialogState) {
        AddOrEditAlbumDialog(album.value) {
            editAlbumDialogState = false
        }
    }


    if (showEditDialog) {
        currentSelectedItem.keys.firstOrNull()?.let { firstKey->
            (currentSelectedItem[firstKey])?.let { albumItemRelation->
                EditDialog(albumItemRelation, album.value, albumList, totalLabelList) {
                    showEditDialog = false
                }
            }
        }
    }

    showEditDialogOnLongClick?.let {
        EditDialog(it, album.value, albumList, totalLabelList) {
            showEditDialogOnLongClick = null
        }
    }

    if (showMoveToDialog) {
        MoveToDialog(currentSelectedItem.values, album.value, albumList) {
            showMoveToDialog = false
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(showLabelFilter) {
        if (showLabelFilter) {
            sheetState.expand()
        } else {
            sheetState.hide()
        }
    }

    if (showLabelFilter) {
        FilterPanel(
            sheetState,
            fileTypeCheckState,
            totalFileTypeList,
            itemRankTypeCheckState,
            totalItemRankList,
            labelFilterCheckedInfo,
            totalLabelList,
        ) {
            showLabelFilter = false
        }
    }

    BackPressHandler(drawerState, enterEditMode) {
        if (drawerState.isOpen) {
            scope.launch {
                drawerState.close()
            }
        } else if (enterEditMode) {
            enterEditMode = false
        }
    }
}