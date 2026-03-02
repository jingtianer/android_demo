package com.jingtian.composedemo

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.staggeredgrid.LazyHorizontalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerDefaults
import androidx.compose.material3.DrawerState
import androidx.compose.runtime.Composable
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jingtian.composedemo.GalleryFunctions.*
import com.jingtian.composedemo.base.AppThemeBasicTextField
import com.jingtian.composedemo.base.AppThemeConfirmDialog
import com.jingtian.composedemo.base.AppThemeDialog
import com.jingtian.composedemo.base.AppThemeHorizontalDivider
import com.jingtian.composedemo.base.AppThemeText
import com.jingtian.composedemo.base.BaseActivity
import com.jingtian.composedemo.dao.DataBase
import com.jingtian.composedemo.dao.model.Album
import com.jingtian.composedemo.dao.model.DEFAULT_DESC
import com.jingtian.composedemo.dao.model.DEFAULT_USER_NAME
import com.jingtian.composedemo.dao.model.FileInfo
import com.jingtian.composedemo.dao.model.FileType
import com.jingtian.composedemo.dao.model.FileType.*
import com.jingtian.composedemo.dao.model.ItemRank
import com.jingtian.composedemo.dao.model.relation.AlbumItemRelation
import com.jingtian.composedemo.ui.theme.LocalAppColorScheme
import com.jingtian.composedemo.ui.theme.LocalAppPalette
import com.jingtian.composedemo.ui.theme.LocalAppUIConstants
import com.jingtian.composedemo.ui.theme.LocalMiddleButtonConfig
import com.jingtian.composedemo.ui.theme.LocalSecondaryTextStyle
import com.jingtian.composedemo.ui.theme.appBackground
import com.jingtian.composedemo.ui.theme.drawerBackground
import com.jingtian.composedemo.ui.theme.goldenRatio
import com.jingtian.composedemo.ui.widget.FollowTailLayout
import com.jingtian.composedemo.ui.widget.RankTypeChooser
import com.jingtian.composedemo.ui.widget.RankTypeChooser.Companion.createBg
import com.jingtian.composedemo.ui.widget.StarRateView
import com.jingtian.composedemo.utils.AppTheme
import com.jingtian.composedemo.utils.BitMapCachePool
import com.jingtian.composedemo.utils.CoroutineUtils
import com.jingtian.composedemo.utils.FileStorageUtils
import com.jingtian.composedemo.utils.FileStorageUtils.getFileNameFromUri
import com.jingtian.composedemo.utils.FileStorageUtils.getMediaType
import com.jingtian.composedemo.utils.FileStorageUtils.getThumbnail
import com.jingtian.composedemo.utils.FileStorageUtils.isHidden
import com.jingtian.composedemo.utils.FileStorageUtils.safeToFile
import com.jingtian.composedemo.utils.UserStorage
import com.jingtian.composedemo.utils.ViewUtils.commonConfig
import com.jingtian.composedemo.utils.ViewUtils.commonEditableConfig
import com.jingtian.composedemo.utils.ViewUtils.dpValue
import com.jingtian.composedemo.utils.splitByWhiteSpace
import com.jingtian.composedemo.viewmodels.AlbumViewModel
import com.jingtian.composedemo.viewmodels.AlbumViewModel.Companion.notifyChange
import com.jingtian.composedemo.viewmodels.AppThemeViewModel
import com.jingtian.composedemo.web.CommonWebView
import com.jingtian.composedemo.web.WebViewActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

class MainActivity : BaseActivity() {
    @Composable
    override fun Content() = Main()

    override fun shouldFitSystemBars(): Boolean = false
}

@Preview
@Composable
fun Main() {
    val viewModel: AlbumViewModel = viewModel()
    var menuItemsEntity by remember { mutableStateOf(emptyList<Album>()) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed, confirmStateChange = { drawerValue: DrawerValue ->
        when(drawerValue) {
            DrawerValue.Closed -> {
                val canClose = menuItemsEntity.isNotEmpty()
                if (!canClose) {
                    val noAlbumToast = "没有任何合集，先创建合集吧"
                    if (viewModel.currentBackgroundTask.value?.equals(noAlbumToast) != true) {
                        viewModel.sendMessage(noAlbumToast)
                    }
                }
                canClose
            }
            DrawerValue.Open -> true
        }
    })
//    val rememberScope = rememberCoroutineScope()
    var currentSelectedAlbum by remember { mutableStateOf<IndexedValue<Album>?>(null) }
    val albumListChange by viewModel.albumListChange.observeAsState()

    fun updateSelectedValue(list: List<Album>): IndexedValue<Album>? {
        val currentSelectedAlbum = currentSelectedAlbum
        if (currentSelectedAlbum != null) {
            val lastSelectedIndex = currentSelectedAlbum.index
            val lastSelectedAlbumId = currentSelectedAlbum.value.albumId
            val findId = list.withIndex().find { it.value.albumId == lastSelectedAlbumId }
            if (findId != null) {
                return findId
            }
            if (lastSelectedIndex < list.size) {
                return IndexedValue(lastSelectedIndex, list.get(lastSelectedIndex))
            }
            return list.asReversed().withIndex().firstOrNull()
        } else {
            return list.withIndex().firstOrNull()
        }
    }

    LaunchedEffect(albumListChange) {
        withContext(Dispatchers.IO) {
            viewModel.menuItemsFlow.collect { value ->
                val nextSelectedAlbum = updateSelectedValue(value)
                withContext(Dispatchers.Main) {
                    menuItemsEntity = value
                    currentSelectedAlbum = nextSelectedAlbum
                }
                if (value.isEmpty()) {
                    drawerState.snapTo(DrawerValue.Open)
                }
            }
        }
    }
    val snackBarMessage by viewModel.currentBackgroundTask.observeAsState()

    var showSnackBar by remember { mutableStateOf(snackBarMessage != null) }
    var showSnackBarAnim by remember { mutableStateOf(true) }

    LaunchedEffect(snackBarMessage) {
        if (showSnackBar && snackBarMessage.isNullOrBlank()) {
            showSnackBarAnim = true
        } else if (!showSnackBar && !snackBarMessage.isNullOrBlank()) {
            showSnackBarAnim = true
        } else {
            showSnackBarAnim = false
        }
        showSnackBar = !snackBarMessage.isNullOrBlank()
    }
    val scope = rememberCoroutineScope()
    Scaffold(
        snackbarHost = {
            AnimatedVisibility(
                visible = showSnackBar,
                enter = if(showSnackBarAnim) fadeIn() + expandIn() else EnterTransition.None,
                exit = if(showSnackBarAnim) shrinkOut() + fadeOut() else ExitTransition.None,
            ) {
                Snackbar(
                    modifier = Modifier.padding(8.dp)
                ) {
                    AppThemeText(snackBarMessage ?: "", style = LocalTextStyle.current.copy(color = LocalAppPalette.current.dialogBg), maxLines = 1)
                }
            }
        }
    ) { _->
        ModalNavigationDrawer(
            drawerContent = {
                ModalDrawerSheet(drawerContainerColor = LocalAppPalette.current.drawerBg, windowInsets = WindowInsets.navigationBars) {
                    MainDrawer(menuItemsEntity) { index, album ->
                        currentSelectedAlbum = IndexedValue(index, album)
                        scope.launch {
                            drawerState.close()
                        }
                    }
                }
            },
            Modifier.fillMaxSize(),
            drawerState = drawerState,
            gesturesEnabled = true
        ) {
            Gallery(currentSelectedAlbum, menuItemsEntity, drawerState)
        }
    }
}

class LabelCheckInfo<T>(val label: T, val name: String, var isChecked: MutableLiveData<Boolean> = MutableLiveData(false))

fun LazyListScope.roundRectTabFilter(checkedList: SnapshotStateMap<String, Boolean>, filerList: List<String>) {
    items(filerList.size, key = { index-> filerList[index] }) { index->
        val item = filerList[index]
        RoundRectCheckableLabel(
            item,
            checkedList[item] ?: false,
            checkedList,
            true,
        )
    }
}

enum class GalleryFunctions(val functionName: String, @DrawableRes val resId: Int) {
    ADD("添加", R.drawable.add),
    IMPORT("导入", R.drawable.import_icon),
    RENAME("重命名", R.drawable.edit_normal),
    EDIT("编辑", R.drawable.edit_normal),
    DELETE("删除", R.drawable.delete),
    EXIT("退出", R.drawable.exit),
    SELECT_ALL("全选", R.drawable.select_all),
    SELECT_NONE("全不选", R.drawable.select_none),
    MOVE("移动", R.drawable.move);
    companion object {
        // 0 -> 添加, 导入, 修改相册名称
        // 1 -> 编辑, 删除, 移动
        // >1 -> 删除, 移动
        val albumFunctions = listOf(RENAME, ADD, IMPORT).map { it to it }
        val itemFunctions = listOf(EDIT, DELETE, MOVE).map { it to it }
        val batchFunctions = listOf(DELETE, MOVE).map { it to it }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Gallery(album: IndexedValue<Album>?, albumList: List<Album>, drawerState: DrawerState) {
    if (album == null) {
        return
    }

    var addImageDialogState by remember { mutableStateOf(false) }
    var itemList by remember { mutableStateOf(emptyList<AlbumItemRelation>()) }
    val filteredItemList = remember { mutableStateListOf<AlbumItemRelation>() }
    val viewModel: AlbumViewModel = viewModel()
    var showLabelFilter by remember { mutableStateOf(false) }
    val albumItemDataChange by viewModel.albumItemListChange.observeAsState()

    val totalLabelList = remember { mutableStateListOf<String>() }
    val totalFileTypeList = remember { FileType.entries.map { it.typeName } }
    val totalItemRankList = remember { ItemRank.entries.map { it.name } }
    var labelFilterCheckedInfo by remember { mutableStateOf<SnapshotStateMap<String, Boolean>?>(null) }
    val fileTypeCheckState = remember { mutableStateMapOf<String, Boolean>() }
    val itemRankTypeCheckState = remember { mutableStateMapOf<String, Boolean>() }
    var albumName by remember { mutableStateOf(album.value.albumName) }
    val currentSelectedItem = remember { mutableStateMapOf<Long, AlbumItemRelation>() }
    val itemSelectStateChangeState = remember { mutableStateOf(0L) }
    var itemSelectStateChange by remember { itemSelectStateChangeState }
    val currentFunctions = remember { mutableStateMapOf<GalleryFunctions, GalleryFunctions>() }
    val enterEditModeState = remember { mutableStateOf(false) }
    var enterEditMode by remember { enterEditModeState }
    var editAlbumDialogState by remember { mutableStateOf(false) }
    val showEditDialogStateOnLongClick = remember { mutableStateOf<AlbumItemRelation?>(null) }
    var showEditDialogOnLongClick by remember { showEditDialogStateOnLongClick }
    var selectAll by remember { mutableStateOf(false) }
    var selectNone by remember { mutableStateOf(false) }

    val size = 160.dp
    val galleryItemPadding = 4.dp
    val scope = rememberCoroutineScope()

    val galleryScrollState = rememberLazyStaggeredGridState()
    var scrollOffsetY by remember { mutableFloatStateOf(0f) }
    val scrollBarSize = remember { mutableStateListOf(6.dp.dpValue, 64.dp.dpValue) }
    val scrollAreaWidth = 28.dp
    val scrollBarOffset = remember { mutableStateListOf(scrollAreaWidth.dpValue - scrollBarSize[0], 0f) }
    suspend fun updateFilterList() {
        withContext(Dispatchers.Default) {
            val labelFilterCheckedInfo = labelFilterCheckedInfo
            val filteredList = mutableListOf<AlbumItemRelation>()
            for (item in itemList) {
                val labelCheck = labelFilterCheckedInfo.isNullOrEmpty() || item.labelInfos.map { it.label }.intersect(labelFilterCheckedInfo.keys).isNotEmpty()
                val fileTypeCheck = fileTypeCheckState.isEmpty() || fileTypeCheckState.get(item.fileInfo.fileType.typeName) == true
                val itemRankCheck = itemRankTypeCheckState.isEmpty() || itemRankTypeCheckState.get(item.albumItem.rank.name) == true
                if (labelCheck && fileTypeCheck && itemRankCheck) {
                    filteredList.add(item)
                }
            }
            withContext(Dispatchers.Main) {
                filteredItemList.clear()
                filteredItemList.addAll(filteredList)
            }
        }
    }

    fun updateScrollItem() {
        scope.launch {
            val y = scrollOffsetY
            val totalHeight = galleryScrollState.layoutInfo.viewportSize.height.toFloat()
            val totalItemCount = galleryScrollState.layoutInfo.totalItemsCount
            val scrollPercent = (y / totalHeight) * totalItemCount
            val targetItem = scrollPercent
                .toInt()
                .coerceIn(0, totalItemCount)
            Log.d("jingtian", "updateScrollOffset: $y, $totalHeight, $scrollPercent, $targetItem")
            galleryScrollState.scrollToItem(targetItem)
        }
    }

    fun updateScrollOffset() {
        val y = scrollOffsetY - scrollBarSize[1] / 2
        Log.d("jingtian", "updateScrollOffset: $y")
        scrollBarOffset[1] = y.coerceIn(0f, galleryScrollState.layoutInfo.viewportSize.height.toFloat() - scrollBarSize[1])
    }

    LaunchedEffect(Unit, album) {
        currentSelectedItem.clear()
        enterEditMode = false
        withContext(Dispatchers.IO) {
            viewModel.getLabelList(album.value).collect { value->
                withContext(Dispatchers.Main) {
                    albumName = album.value.albumName
                    labelFilterCheckedInfo = SnapshotStateMap<String, Boolean>()
                    totalLabelList.clear()
                    totalLabelList.addAll(value)
                }
                updateFilterList()
            }
        }
    }


    LaunchedEffect(albumItemDataChange, album) {
        withContext(Dispatchers.IO) {
            viewModel.getAllAlbumItem(album.value).collect {
                withContext(Dispatchers.Main) {
                    itemList = it
                }
            }
        }
        viewModel.getLabelList(album.value).collect { value->
            val checkList = SnapshotStateMap<String, Boolean>()
            labelFilterCheckedInfo?.let {
                for ((k, v) in it) {
                    if (v) {
                        checkList[k] = true
                    }
                }
            }
            withContext(Dispatchers.Main) {
                albumName = album.value.albumName
                labelFilterCheckedInfo = checkList
                totalLabelList.clear()
                totalLabelList.addAll(value)
            }
            updateFilterList()
            currentSelectedItem.clear()
            itemSelectStateChange++
        }
    }

    val albumNameChange by viewModel.albumNameChange.observeAsState()

    val showEditDialogState = remember { mutableStateOf(false) }
    var showEditDialog by remember { showEditDialogState }
    var showConfirmDeleteDialog by remember { mutableStateOf(false) }
    var showMoveToDialog by remember { mutableStateOf(false) }
    LaunchedEffect(albumNameChange) {
        withContext(Dispatchers.IO) {
            val newAlbum = viewModel.getAlbumName(album.value.albumId ?: return@withContext)
            album.value.albumName = newAlbum.albumName
            withContext(Dispatchers.Main) {
                albumName = newAlbum.albumName
            }
        }
    }

    val importDirLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) {uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
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
                    painter = painterResource(R.drawable.drawer),
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
                    painter = painterResource(R.drawable.edit_normal),
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
                        labelFilterCheckedInfo?.let { labelFilterCheckedInfo->
                            roundRectTabFilter(labelFilterCheckedInfo, totalLabelList)
                            roundRectTabFilter(fileTypeCheckState, totalFileTypeList)
                            roundRectTabFilter(itemRankTypeCheckState, totalItemRankList)
                        }
                    }

                    Icon(
                        painter = painterResource(R.drawable.down),
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
                            scrollOffsetY =
                                getScrollOffset() / galleryScrollState.layoutInfo.totalItemsCount * galleryScrollState.layoutInfo.viewportSize.height.toFloat()
                            updateScrollOffset()
//                            Log.d("jingtian", "onPostScroll: $info")
                            return super.onPostScroll(consumed, available, source)
                        }
                    }),
                contentPadding = PaddingValues(bottom = if (enterEditMode) bottomBarHeight + shadesHeight else 0.dp),
                state = galleryScrollState,
            ) {
                items(filteredItemList.size, key = { index-> filteredItemList[index].hashCode() }, contentType = { index-> filteredItemList[index].fileInfo.fileType.value }) { index: Int ->
                    AlbumItemView(filteredItemList[index], size, galleryItemPadding, currentSelectedItem, enterEditModeState, itemSelectStateChangeState, showEditDialogStateOnLongClick)
                }
            }
            val scrollBarColor = LocalAppPalette.current.labelTextColor.copy(alpha = 0.85f)
            Box(
                Modifier
                    .width(scrollAreaWidth)
                    .fillMaxHeight()
                    .align(Alignment.TopEnd)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                scrollOffsetY = offset.y
                                scrollBarSize[0] = 16.dp.dpValue
                                scrollBarOffset[0] = scrollAreaWidth.dpValue - scrollBarSize[0]
                                updateScrollOffset()
                                updateScrollItem()
                            },
                            onDrag = { pointerInputChange, offset ->
                                scrollOffsetY += offset.y
                                scrollBarSize[0] = 16.dp.dpValue
                                scrollBarOffset[0] = scrollAreaWidth.dpValue - scrollBarSize[0]
                                updateScrollOffset()
                                updateScrollItem()
                            },
                            onDragEnd = {
                                scrollBarSize[0] = 6.dp.dpValue
                                scrollBarOffset[0] = scrollAreaWidth.dpValue - scrollBarSize[0]
                            },
                            onDragCancel = {
                                scrollBarSize[0] = 6.dp.dpValue
                                scrollBarOffset[0] = scrollAreaWidth.dpValue - scrollBarSize[0]
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val downEvent = awaitFirstDown()
                            scrollOffsetY = downEvent.position.y
                            scope.launch {
                                withContext(Dispatchers.Main) {
                                    scrollBarSize[0] = 16.dp.dpValue
                                    scrollBarOffset[0] = scrollAreaWidth.dpValue - scrollBarSize[0]
                                    updateScrollOffset()
                                    updateScrollItem()
                                }
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val upOrCancel = waitForUpOrCancellation()
                            scrollOffsetY = upOrCancel?.position?.y ?: scrollOffsetY
                            scope.launch {
                                withContext(Dispatchers.Main) {
                                    scrollBarSize[0] = 6.dp.dpValue
                                    scrollBarOffset[0] = scrollAreaWidth.dpValue - scrollBarSize[0]
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
                                        putAll(filteredItemList.map { (it.albumItem.itemId?:DataBase.INVALID_ID) to it })
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
            "${currentFunctions.values.firstOrNull() ?: ""}"
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
    val backPressedCallback = remember {
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerState.isOpen) {
                    scope.launch {
                        drawerState.close()
                    }
                } else if (enterEditMode) {
                    enterEditMode = false
                }
            }

        }
    }
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    DisposableEffect(dispatcher) {
        dispatcher?.addCallback(backPressedCallback)
        onDispose {
            backPressedCallback.remove()
        }
    }
    backPressedCallback.isEnabled = drawerState.isOpen || enterEditMode
}

@Composable
fun RowScope.GalleryFunctionView(func: GalleryFunctions, onClick: ()->Unit) {
    Column(
        Modifier
            .align(Alignment.CenterVertically)
            .padding(horizontal = 6.dp)
            .clickable { onClick() }) {
        Icon(
            painter = painterResource(func.resId),
            contentDescription = "${func.functionName}功能",
            modifier = Modifier
                .size(32.dp)
                .align(Alignment.CenterHorizontally),
        )
        AppThemeText(
            func.functionName,
            Modifier.align(Alignment.CenterHorizontally),
            style = LocalTextStyle.current.copy(fontSize = 10.sp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterPanel(
    sheetState: SheetState,
    fileTypeCheckStateList: SnapshotStateMap<String, Boolean>,
    fileTypeList: List<String>,
    itemRankCheckStateList: SnapshotStateMap<String, Boolean>,
    itemRankList: List<String>,
    labelCheckStateList: SnapshotStateMap<String, Boolean>?,
    labelList: List<String>,
    onDismiss: () -> Unit,
) {
    val horizontalPadding = 6.dp
    val verticalPadding = 6.dp
    val horizontalInnerPadding = LocalAppUIConstants.current.filterLabelPaddings[2]
    val viewModel: AlbumViewModel = viewModel()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(sqrt(goldenRatio)),
        containerColor = LocalAppPalette.current.bottomSheetBackgroundColor,
        windowInsets = WindowInsets.navigationBars
    ) {
        val scope = rememberCoroutineScope()

        LazyVerticalGrid(
            columns = GridCells.Adaptive(LocalAppUIConstants.current.filterLabelHeight * LocalAppUIConstants.current.filterLabelAspectRatio),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .weight(1f)
                .padding(horizontal = horizontalPadding)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                AppThemeText(text = "类型筛选", Modifier.padding(horizontal = horizontalInnerPadding, vertical = verticalPadding), style = LocalTextStyle.current.copy(fontWeight = FontWeight(600), fontSize = 16.sp))
            }
            items(fileTypeList.size, key = { index-> fileTypeList[index] }) { index ->
                RoundRectCheckableLabel(
                    fileTypeList[index],
                    fileTypeCheckStateList[fileTypeList[index]] ?: false,
                    fileTypeCheckStateList,
                    false,
                )
            }
            item(span = { GridItemSpan(this.maxLineSpan) }) {
                AppThemeText(text = "排行筛选", Modifier.padding(horizontal = horizontalInnerPadding, vertical = verticalPadding), style = LocalTextStyle.current.copy(fontWeight = FontWeight(600), fontSize = 16.sp))
            }
            items(itemRankList.size, key = { index-> itemRankList[index] }) { index ->
                RoundRectCheckableLabel(
                    itemRankList[index],
                    itemRankCheckStateList[itemRankList[index]] ?: false,
                    itemRankCheckStateList,
                    false,
                )
            }
            if (labelCheckStateList != null && labelList.isNotEmpty()) {
                item(span = { GridItemSpan(this.maxLineSpan) }) {
                    AppThemeText(text = "标签筛选", Modifier.padding(horizontal = horizontalInnerPadding, vertical = verticalPadding), style = LocalTextStyle.current.copy(fontWeight = FontWeight(600), fontSize = 16.sp))
                }
                item(span = { GridItemSpan(this.maxLineSpan) }) {

                    val labelItemHeight = LocalAppUIConstants.current.filterLabelHeight + (LocalAppUIConstants.current.filterLabelPaddings[1] + LocalAppUIConstants.current.filterLabelPaddings[3]) * 2
                    LazyHorizontalStaggeredGrid(
//            StaggeredGridCells.Adaptive(labelItemHeight),
                        StaggeredGridCells.Fixed(3),
                        Modifier
                            .height(labelItemHeight * 3)
                            .fillMaxWidth(),
                        horizontalItemSpacing = horizontalInnerPadding,
                    ) {
                        items(labelList.size, { index-> labelList[index] }) { index ->
                            val item = labelList[index]
                            RoundRectCheckableLabel(
                                item,
                                labelCheckStateList[item] ?: false,
                                labelCheckStateList,
                                true,
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding)) {
            Button(onClick = {
                scope.launch {
                    withContext(Dispatchers.Default) {
                        fun SnapshotStateMap<String, Boolean>.reverseList(totalList: List<String>) {
                            val reveredList = totalList.toSet() - this.keys
                            this.clear()
                            this.putAll(reveredList.map { it to true })
                        }
                        fileTypeCheckStateList.reverseList(fileTypeList)
                        itemRankCheckStateList.reverseList(itemRankList)
                        labelCheckStateList?.reverseList(labelList)
                    }
                    viewModel.filterCheckChanged.notifyChange()
                }
            },
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .weight(1f)
                    .padding(horizontal = horizontalInnerPadding)
            ) {
                AppThemeText(text = "反转")
            }
            Button(onClick = {
                scope.launch {
                    withContext(Dispatchers.Default) {
                        fileTypeCheckStateList.clear()
                        itemRankCheckStateList.clear()
                        labelCheckStateList?.clear()
                    }
                    viewModel.filterCheckChanged.notifyChange()
                }
            },
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .weight(1f)
                    .padding(horizontal = horizontalInnerPadding)
            ) {
                AppThemeText(text = "清空")
            }
            Button(onClick = {
                onDismiss()
            },
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .weight(1f)
                    .padding(horizontal = horizontalInnerPadding)
            ) {
                AppThemeText(text = "确认")
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun RoundRectCheckableLabel(item: String, isChecked: Boolean, checkedList: SnapshotStateMap<String, Boolean>, wrapContent: Boolean) {
    val paddings = LocalAppUIConstants.current.filterLabelPaddings
    val paddingInnerHorizontal = paddings[0]
    val paddingInnerVertical = paddings[1]
    val paddingOuterHorizontal = paddings[2]
    val paddingOuterVertical = paddings[3]
    val viewModel: AlbumViewModel = viewModel()
    val modifier = if (wrapContent) {
        Modifier
            .padding(horizontal = paddingOuterHorizontal, vertical = paddingOuterVertical)
            .wrapContentWidth()
            .height(LocalAppUIConstants.current.filterLabelHeight)
            .background(
                color = if (isChecked) LocalAppPalette.current.labelChecked else LocalAppPalette.current.labelUnChecked,
                shape = RoundedCornerShape(100)
            )
            .widthIn(LocalAppUIConstants.current.filterLabelHeight * LocalAppUIConstants.current.filterLabelAspectRatio)
            .clip(
                RoundedCornerShape(100),
            )
    } else {
        Modifier
            .padding(horizontal = paddingOuterHorizontal, vertical = paddingOuterVertical)
            .fillMaxWidth()
            .height(LocalAppUIConstants.current.filterLabelHeight)
            .background(
                color = if (isChecked) LocalAppPalette.current.labelChecked else LocalAppPalette.current.labelUnChecked,
                shape = RoundedCornerShape(100)
            )
            .clip(
                RoundedCornerShape(100),
            )
    }
    Box(
        modifier
            .clickable {
                checkedList[item] = !isChecked
                if (isChecked) {
                    checkedList.remove(item)
                } else {
                    checkedList[item] = true
                }
                viewModel.filterCheckChanged.value = (viewModel.filterCheckChanged.value ?: 0) + 1
            }) {
        AppThemeText(
            text = item,
            Modifier
                .wrapContentSize()
                .align(Alignment.Center)
                .padding(horizontal = paddingInnerHorizontal, vertical = paddingInnerVertical)
        )
    }
}

fun systemFallbackIntent(context: Context, fileInfo: FileInfo): Intent? {
    val mediaType = fileInfo.fileType.mimeType
    val originFileUri = fileInfo.getFileUri()
    val originFile = originFileUri?.safeToFile()
    val mediaUri: Uri = if (originFile != null) {
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            originFile
        )
    } else {
        originFileUri
    } ?: return null
    return Intent(Intent.ACTION_VIEW).apply {
        // 设置Uri和媒体类型
        setDataAndType(mediaUri, mediaType)
        // 关键：授予系统应用访问该Uri的权限
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        // 适配Android 12+的前台服务权限（可选，避免部分播放器启动失败）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}

fun webIntent(context: Context, fileInfo: FileInfo) : Intent {
    return Intent(context, WebViewActivity::class.java).apply {
        putExtra(WebViewActivity.KEY_WEB_URI, fileInfo.getFileUri())
        putExtra(WebViewActivity.KEY_STORAGE_ID, fileInfo.storageId)
    }
}

fun playIntent(context: Context, fileInfo: FileInfo): Intent? {
    val mediaType = fileInfo.fileType
    return when (mediaType) {
        HTML -> {
            webIntent(context, fileInfo)
        }

        IMAGE, VIDEO, AUDIO, RegularFile -> systemFallbackIntent(context, fileInfo)
    }
}

@Composable
fun AlbumItemView(albumItemRelation: AlbumItemRelation, size: Dp, padding: Dp, currentSelectedItem: SnapshotStateMap<Long, AlbumItemRelation>, enterEditState: MutableState<Boolean>, itemSelectStateChangeState: MutableState<Long>, showEditDialogState: MutableState<AlbumItemRelation?>) {
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    var itemName by remember { mutableStateOf(albumItemRelation.albumItem.itemName) }
    var itemDesc by remember { mutableStateOf(albumItemRelation.albumItem.desc) }
    var itemRank by remember { mutableStateOf(albumItemRelation.albumItem.rank) }
    var itemScore by remember { mutableStateOf(albumItemRelation.albumItem.score) }
    var itemLabel by remember { mutableStateOf(albumItemRelation.labelInfos) }

    val scope = rememberCoroutineScope()
    var imageResource by remember { mutableStateOf(R.drawable.load_failed) }

    fun FileInfo.aspectRatio(): Float? {
        return (this.intrinsicWidth.toFloat() / this.intrinsicHeight.toFloat()).takeIf {
            this.intrinsicHeight > 0 && this.intrinsicWidth > 0
        } ?: 1f
    }

    fun ImageBitmap.aspectRatio(): Float? {
        return (this.width.toFloat() / this.height.toFloat()).takeIf {
            this.width > 0 && this.height > 0
        } ?: 1f
    }

    var intrinsicRatio by remember {
        mutableStateOf(
            albumItemRelation.fileInfo.aspectRatio()
        )
    }

    suspend fun fetchImage() {
        withContext(Dispatchers.IO) {
            val uri = albumItemRelation.fileInfo?.getFileUri() ?: return@withContext
            val fileType = albumItemRelation.fileInfo.fileType
            when(fileType) {
                IMAGE -> {
                    val (_, image) = BitMapCachePool.loadImage(
                        albumItemRelation.fileInfo,
                        size.dpValue.toInt(),
                    )
                    val bitmap = image?.asImageBitmap()
                    withContext(Dispatchers.Main) {
                        imageBitmap = bitmap
                        imageBitmap?.aspectRatio()?.let {
                            intrinsicRatio = it
                        }
                    }
                }
                VIDEO -> {
                    getThumbnail(
                        albumItemRelation.fileInfo,
                        scope, uri,
                        maxWidth = size.dpValue.toInt(),
                    ) { bitmap->
                        imageBitmap = bitmap?.asImageBitmap()
                        imageBitmap?.aspectRatio()?.let {
                            intrinsicRatio = it
                        }
                    }
                }
                AUDIO -> {
                    imageResource = R.drawable.music
                    getThumbnail(
                        albumItemRelation.fileInfo,
                        scope, uri,
                        maxWidth = size.dpValue.toInt(),
                    ) { bitmap->
                        imageBitmap = bitmap?.asImageBitmap()
                        imageBitmap?.aspectRatio()?.let {
                            intrinsicRatio = it
                        }
                    }
                }
                RegularFile -> {
                    intrinsicRatio = 1f
                    imageResource = R.drawable.file
                    imageBitmap = null
                }
                HTML -> {
                    intrinsicRatio = 1f
                    imageResource = R.drawable.chrome
                    imageBitmap = null
                    getThumbnail(
                        albumItemRelation.fileInfo,
                        scope, uri,
                        maxWidth = size.dpValue.toInt(),
                    ) { bitmap->
                        imageBitmap = bitmap?.asImageBitmap()
                    }
                }
            }
        }
    }
    LaunchedEffect(Unit) {
        fetchImage()
    }

    val context = LocalContext.current
    val playIntent = remember(albumItemRelation) {
        val fileInfo = albumItemRelation.fileInfo
        playIntent(context, fileInfo)
    }

    val fileTypeIcon = when(albumItemRelation.fileInfo.fileType) {
        IMAGE -> R.drawable.pic_icon
        VIDEO -> R.drawable.video_icon
        AUDIO -> R.drawable.music_icon
        HTML -> R.drawable.web_icon
        RegularFile -> R.drawable.doc_icon
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            scope.launch {
                fetchImage()
            }
        }
    }
    val itemId = albumItemRelation.albumItem.itemId ?: DataBase.INVALID_ID
    val isSelected = currentSelectedItem.containsKey(itemId)

    Column(Modifier
        .width(size)
        .padding(padding)
        .pointerInput(Unit) {
            detectTapGestures(onLongPress = {
//                enterEditState.value = true
//                itemSelectStateChangeState.value += 1
                showEditDialogState.value = albumItemRelation
            },
                onTap = {
                    if (enterEditState.value) {
                        val itemId = albumItemRelation.albumItem.itemId ?: DataBase.INVALID_ID
                        if (currentSelectedItem.containsKey(itemId)) {
                            currentSelectedItem.remove(itemId)
                        } else {
                            currentSelectedItem[itemId] = albumItemRelation
                        }
                        itemSelectStateChangeState.value += 1
                    } else {
                        if (playIntent != null) {
                            if (albumItemRelation.fileInfo.fileType == HTML) {
                                launcher.launch(playIntent)
                            } else {
                                context.startActivity(playIntent)
                            }
                        }
                        scope.launch(Dispatchers.IO) {
                            fetchImage()
                        }
                    }
                })
        }
        .background(
            color = if (isSelected) LocalAppPalette.current.labelChecked else LocalAppPalette.current.galleryCardBg,
            shape = RoundedCornerShape(padding * 2)
        )
        .clip(RoundedCornerShape(padding * 2))) {

        val currentPickedImage = imageBitmap

        fun Modifier.aspectRatioOrNull(intrinsicRatio: Float?): Modifier {
            return if (intrinsicRatio != null) {
                aspectRatio(intrinsicRatio)
            } else {
                this
            }
        }


        Box(Modifier.fillMaxWidth()) {
            Icon(
                painter = painterResource(fileTypeIcon),
                contentDescription = "文件类型图标",
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .size(24.dp)
                    .align(Alignment.Center)
            )
            if (enterEditState.value) {
                Icon(painter = painterResource(R.drawable.check), contentDescription = "复选框",
                    Modifier
                        .padding(4.dp)
                        .align(Alignment.CenterEnd)
                        .size(24.dp)
                        .background(
                            color = if (isSelected) {
                                LocalAppPalette.current.labelChecked
                            } else {
                                LocalAppPalette.current.labelUnChecked
                            },
                            CircleShape
                        )
                        .clickable {
                            if (isSelected) {
                                currentSelectedItem.remove(itemId)
                            } else {
                                currentSelectedItem[itemId] = albumItemRelation
                            }
                            itemSelectStateChangeState.value += 1
                        }
                )
            }
        }

        Box(
            Modifier
                .clip(RoundedCornerShape(padding * 2))
                .fillMaxWidth()) {
            val imageResource = imageResource
            if (currentPickedImage != null) {
                Image(
                    bitmap = currentPickedImage,
                    contentDescription = "文件缩略图",
                    Modifier
                        .fillMaxWidth()
                        .aspectRatioOrNull(intrinsicRatio)
                        .align(Alignment.Center),
                    contentScale = ContentScale.FillWidth
                )
            } else if (imageResource != null) {
                Image(
                    painter = painterResource(imageResource),
                    contentDescription = "文件缩略图",
                    Modifier
                        .fillMaxWidth()
                        .aspectRatioOrNull(intrinsicRatio)
                        .padding(6.dp)
                        .align(Alignment.Center),
                    contentScale = ContentScale.FillWidth
                )
            } else if (albumItemRelation.fileInfo.fileType == HTML) {
//                CommonWebView(Modifier
//                    .fillMaxWidth()
//                    .aspectRatioOrNull(intrinsicRatio)
//                    .align(Alignment.Center),
//                    uri = albumItemRelation.fileInfo.getFileUri(),
//                    enabled = false,
//                )
            }

            if (itemRank != null && itemRank != ItemRank.NONE) {
                fun View.initRankView(): View {
                    val bg = createBg(itemRank, context)
                    val paddingHorizontal = 4.dp.dpValue.roundToInt()
                    val width = max(bg.getWidth(), bg.getHeight()).roundToInt() + paddingHorizontal + paddingHorizontal
                    layoutParams = ViewGroup.LayoutParams(width, bg.getHeight().roundToInt())
                    setPadding(paddingHorizontal, 0, paddingHorizontal, 0)
                    background = bg
                    return this
                }
                AndroidView({ context ->
                    View(context).initRankView()
                },
                    Modifier
                        .wrapContentSize()
                        .align(Alignment.TopEnd)
                        .clip(RoundedCornerShape(bottomStart = padding * 2))
                    ,
                    update = {
                        it.initRankView()
                    })
            }
        }

        AppThemeText(
            itemName,
            modifier = Modifier
                .wrapContentWidth()
                .padding(bottom = 4.dp, start = padding * 2, end = padding * 2, top = padding),
            maxLines = 2,
            style = LocalTextStyle.current.copy(textAlign = TextAlign.Start, fontSize = 16.sp, fontWeight = FontWeight(700)),
            overflow = TextOverflow.Ellipsis
        )

        if (!itemDesc.isNullOrBlank()) {
            OutlinedTextField(itemDesc, { value->
                itemDesc = value
            }, modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp, start = padding, end = padding), label = {
                AppThemeText("评论")
            }, maxLines = Int.MAX_VALUE, enabled = false, textStyle = LocalSecondaryTextStyle.current)
        }

        Box(
            Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .wrapContentHeight()
                .align(Alignment.CenterHorizontally)) {
            AndroidView({ context ->
                StarRateView(context).commonConfig().apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
                Modifier
                    .wrapContentWidth()
                    .height(30.dp)
                    .align(Alignment.Center)
                    .padding(bottom = 4.dp, start = padding, end = padding),
                update = {
                    it.setScore(itemScore)
                })
        }

        if (itemLabel.isNotEmpty()) {
            LazyRow(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp, start = padding, end = padding)) {
                items(itemLabel.size, key = { index: Int ->
                    itemLabel[index].label
                }) { index: Int ->
                    LabelView(itemLabel[index].label)
                }
            }
        }
    }

}

@Composable
fun MoveToDialog(albumItemRelation: Collection<AlbumItemRelation>, album: Album, albumList: List<Album>, onDismiss: () -> Unit) {
    var currentSelectedAlbum by remember { mutableStateOf(album) }
    val viewModel: AlbumViewModel = viewModel()
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
    }) { _, actionButtons->
        Column(Modifier.fillMaxHeight(LocalAppUIConstants.current.dialogPercent)) {
            LazyColumn(
                Modifier
                    .padding(horizontal = 8.dp, vertical = 12.dp)
                    .fillMaxSize()
                    .weight(1f)
            ) {
                item {
                    AppThemeText("移动到: ${currentSelectedAlbum.albumName}", style = LocalTextStyle.current.copy(fontSize = 16.sp))
                    Spacer(Modifier.height(8.dp))
                }
                items(albumList.size) { index->
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
                    .wrapContentHeight()) {
                actionButtons()
            }
        }
    }
}

@Composable
fun EditDialog(albumItemRelation: AlbumItemRelation, relatedAlbum: Album, albumData: List<Album>, totalLabelList: List<String>, onDismiss: ()->Unit) {
    val album = albumItemRelation.albumItem
    val viewModel : AlbumViewModel = viewModel()

    var pickedImage by remember { mutableStateOf<ImageBitmap?>(null) }
    var itemName by remember { mutableStateOf(album.itemName) }
    var itemDesc by remember { mutableStateOf(album.desc) }
    var itemRank by remember { mutableStateOf(album.rank) }
    var itemScore by remember { mutableStateOf(album.score) }
    val itemLabel = remember { mutableStateListOf(*albumItemRelation.labelInfos.map { it.label }.toTypedArray()) }
    val itemLabelSet = remember { mutableStateMapOf(*albumItemRelation.labelInfos.map { it.label to it.label }.toTypedArray()) }

    var selectedUri by remember { mutableStateOf(albumItemRelation.fileInfo?.getFileUri()) }
    var selectedFileType by remember { mutableStateOf(albumItemRelation.fileInfo?.fileType) }
    val scope = rememberCoroutineScope()
    var imageResource by remember { mutableStateOf(R.drawable.upload_to_cloud) }

    val totalLabelList = remember { mutableStateMapOf(*(totalLabelList.map { it to it }).toTypedArray()) }
    val filteredTotalLabelList = remember { mutableStateListOf(*((totalLabelList.keys - albumItemRelation.labelInfos.map { it.label }.toSet()).toTypedArray())) }
    val selectedTotalLabelList = remember { mutableStateMapOf<String, String>() }

    val imageWidth = min(LocalConfiguration.current.let { min(it.screenWidthDp, it.screenHeightDp)/2 }, 180).dp

    var webSnapShotTaker: (suspend ()->Bitmap)? by remember { mutableStateOf(null) }

    var currentSelectedAlbum by remember { mutableStateOf(relatedAlbum) }

    fun saveItem(context: Context) {
        val selectedUri = selectedUri
        val selectedFileType = selectedFileType
        if (selectedUri == null || selectedFileType == null || itemName.isNullOrBlank()) {
            Toast.makeText(context, "数据不合法，缺少文件或标题", Toast.LENGTH_SHORT).show()
            return
        }
        scope.launch {
            val webSnapShot = if (selectedFileType == HTML) {
                webSnapShotTaker?.invoke()
            } else {
                null
            }
            onDismiss()
            viewModel.updateItem(
                albumItemRelation,
                selectedUri,
                selectedFileType,
                itemName,
                itemRank,
                itemDesc,
                itemScore,
                itemLabelSet.keys,
                currentSelectedAlbum.albumId,
                webSnapShot,
            )
        }
    }

    suspend fun updateImage(uri: Uri, fileType: FileType, fileInfo: FileInfo?) {
        withContext(Dispatchers.IO) {
            when (fileType) {
                IMAGE -> {
                    val bitmap = if (fileInfo != null) {
                        BitMapCachePool.loadImage(fileInfo, maxWidth = imageWidth.dpValue.toInt()).second?.asImageBitmap()
                    } else {
                        BitMapCachePool.toBitMap(uri, maxWidth = imageWidth.dpValue.toInt()).second?.asImageBitmap()
                    }
                    withContext(Dispatchers.Main) {
                        pickedImage = bitmap
                    }
                }

                VIDEO -> {
                    if (fileInfo != null) {
                        getThumbnail(fileInfo, scope, uri, maxWidth = imageWidth.dpValue.toInt()) { bitmap ->
                            withContext(Dispatchers.Main) {
                                pickedImage = bitmap?.asImageBitmap()
                            }
                        }
                    } else {
                        getThumbnail(VIDEO, scope, uri, maxWidth = imageWidth.dpValue.toInt()) { bitmap: Bitmap? ->
                            withContext(Dispatchers.Main) {
                                pickedImage = bitmap?.asImageBitmap()
                            }
                        }
                    }
                }

                AUDIO -> {
                    imageResource = R.drawable.music
                    if (fileInfo != null) {
                        getThumbnail(fileInfo, scope, uri, maxWidth = imageWidth.dpValue.toInt()) { bitmap ->
                            withContext(Dispatchers.Main) {
                                pickedImage = bitmap?.asImageBitmap()
                            }
                        }
                    } else {
                        getThumbnail(AUDIO, scope, uri, maxWidth = imageWidth.dpValue.toInt()) { bitmap: Bitmap? ->
                            withContext(Dispatchers.Main) {
                                pickedImage = bitmap?.asImageBitmap()
                            }
                        }
                    }
                }

                HTML -> {
                    withContext(Dispatchers.Main) {
                        imageResource = R.drawable.chrome
                        pickedImage = null
                    }
                    if (fileInfo != null) {
                        getThumbnail(fileInfo, scope, uri, maxWidth = imageWidth.dpValue.toInt()) { bitmap ->
                            withContext(Dispatchers.Main) {
                                pickedImage = bitmap?.asImageBitmap()
                            }
                        }
                    } else {
                        getThumbnail(HTML, scope, uri, maxWidth = imageWidth.dpValue.toInt()) { bitmap: Bitmap? ->
                            withContext(Dispatchers.Main) {
                                pickedImage = bitmap?.asImageBitmap()
                            }
                        }
                    }
                }

                RegularFile -> {
                    withContext(Dispatchers.Main) {
                        imageResource = R.drawable.file
                        pickedImage = null
                    }
                }
            }
        }
    }

    suspend fun onSelectedUriChange() {
        val uri = selectedUri
        val fileType = selectedFileType
        if (uri != null && fileType != null) {
            updateImage(uri, fileType, null)
        } else {
            imageResource = R.drawable.load_failed
        }
        if (itemName.isBlank() && uri != null) {
            itemName = getFileNameFromUri(uri) ?: ""
        }
    }

    LaunchedEffect(Unit) {
        val uri = selectedUri
        val fileType = selectedFileType
        if (uri != null && fileType != null) {
            updateImage(uri, fileType, albumItemRelation.fileInfo)
        } else {
            imageResource = R.drawable.load_failed
        }
        if (itemName.isNullOrBlank() && uri != null) {
            itemName = getFileNameFromUri(uri) ?:""
        }
    }

    val multipleImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? ->
            uri?.takeIf { !it.isHidden() } ?: return@rememberLauncherForActivityResult
            selectedFileType = getMediaType(uri)
            selectedUri = uri
            scope.launch {
                onSelectedUriChange()
            }
        }
    )

    fun pickImage() {
        multipleImagePickerLauncher.launch(FileType.mimes)
    }


    val context = LocalContext.current
    CompositionLocalProvider(
        LocalMiddleButtonConfig provides LocalMiddleButtonConfig.current.copy(
            text = "删除",
            colors = LocalMiddleButtonConfig.current.colors.copy(containerColor = LocalAppPalette.current.deleteButtonColor, contentColor = Color.White),
        )
    ) {
        AppThemeDialog(
            Modifier
                .fillMaxSize(0.9f)
//                .clip(RoundedCornerShape(4.dp))
//                .background(LocalAppPalette.current.dialogBg)
//                .dialogBackground()
            ,
            onNegative = onDismiss,
//            onMiddleClick = {
//                deleteItem()
//                onDismiss()
//            },
            onMiddleClick = null,
            onPositive = {
                saveItem(context)
            },
            onDismissRequest = {},
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) { _, actionButtons->
            var openAlbumList by remember { mutableStateOf(false) }

            Box {
                LazyColumn(
                    Modifier
                        .padding(top = imageWidth / 2)
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(LocalAppPalette.current.dialogBg)
                        .padding(top = imageWidth / 2)
                ) {
                    item {
                        OutlinedTextField(itemName, { value->
                            itemName = value
                        }, modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp), label = {
                            AppThemeText("文件名称")
                        }, maxLines = Int.MAX_VALUE)

                        OutlinedTextField(itemDesc, { value->
                            itemDesc = value
                        }, modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp), label = {
                            AppThemeText("文件描述")
                        }, maxLines = Int.MAX_VALUE)

                        AppThemeText("选择合集: ${currentSelectedAlbum.albumName}",
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    openAlbumList = !openAlbumList
                                }
                                .padding(horizontal = 6.dp, vertical = 10.dp))
                    }

                    if (openAlbumList) {
                        items(albumData.size,
                            key = { index: Int -> albumData[index].let { it.albumId ?: DataBase.INVALID_ID to it.albumName } }
                        ) { index ->
                            val item = albumData[index]
                            ImmutableDrawerMenuItem(
                                item,
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 6.dp, vertical = 10.dp)
                            ) {
                                currentSelectedAlbum = item
                                openAlbumList = false
                            }
                        }
                    }

                    item {
                        Column(Modifier.fillMaxWidth()) {
                            AndroidView({ context ->
                                StarRateView(context).commonEditableConfig().apply {
                                    onScoreChange = StarRateView.Companion.OnScoreChange { score: Float ->
                                        itemScore = score
                                    }
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.WRAP_CONTENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                }
                            },
                                Modifier
                                    .wrapContentWidth()
                                    .height(30.dp)
                                    .align(Alignment.CenterHorizontally),
                                update = {
                                    it.setScore(itemScore)
                                })
                            Spacer(Modifier.height(4.dp))

                            AndroidView({ context ->
                                RankTypeChooser(context).apply {
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.WRAP_CONTENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                    )
                                    onRankChange = RankTypeChooser.Companion.OnRankTypeChange { value ->
                                        itemRank = value
                                    }
                                }
                            },
                                Modifier
                                    .wrapContentWidth()
                                    .height(30.dp)
                                    .align(Alignment.CenterHorizontally),
                                update = {
                                    it.setRankType(itemRank)
                                })
                            Spacer(Modifier.height(4.dp))
                        }
                    }

                    item {
                        LazyHorizontalStaggeredGrid(rows = StaggeredGridCells.FixedSize(30.dp),
                            Modifier
                                .padding(horizontal = 6.dp)
                                .height(90.dp)
                                .fillMaxWidth()) {
                            item {
                                var addItemValue by remember { mutableStateOf("") }
                                EditableLabelView(addItemValue, enableEdit = true, onRemove = {
                                    val addLabelList = addItemValue
                                        .trim()
                                        .splitByWhiteSpace()
                                        .toSet()
                                        .filter { it !in itemLabelSet.keys }
                                    itemLabelSet.putAll(addLabelList.map { it to it })
                                    itemLabel.addAll(0, addLabelList)
                                    addItemValue = ""
                                }) { value->
                                    addItemValue = value
                                }
                            }
                            items(filteredTotalLabelList.size, key = { index-> filteredTotalLabelList[index] }) { index->
                                val item = filteredTotalLabelList[index]
                                val isChecked = selectedTotalLabelList.containsKey(item)
                                CheckableLabelView(label = item, isChecked = isChecked) {
                                    if (it && !itemLabelSet.containsKey(item)) {
                                        itemLabelSet[item] = item
                                        itemLabel.add(0, item)
                                    } else if (!it && itemLabelSet.containsKey(item)) {
                                        itemLabelSet.remove(item)
                                        itemLabel.remove(item)
                                    }
                                    if (it) {
                                        selectedTotalLabelList[item] = item
                                    } else {
                                        selectedTotalLabelList.remove(item)
                                    }
                                }
                            }
                        }
                        LazyHorizontalStaggeredGrid(rows = StaggeredGridCells.FixedSize(30.dp),
                            Modifier
                                .padding(horizontal = 6.dp)
                                .height(30.dp)
                                .fillMaxWidth()) {
                            items(itemLabel.size, key = { index: Int ->
                                itemLabel[index]
                            }) { index: Int ->
                                val item = itemLabel[index]
                                EditableLabelView(item, enableEdit = false, onRemove = {
                                    itemLabel.remove(item)
                                    itemLabelSet.remove(item)
                                    selectedTotalLabelList.remove(item)
                                    if (totalLabelList.containsKey(item)) {
                                        filteredTotalLabelList.add(item)
                                    }
                                }) { }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                    }

                    item {
                        Column(
                            Modifier
                                .padding(horizontal = 6.dp)
                                .fillMaxWidth()) {
                            actionButtons()
                        }
                    }
                }

                val currentPickedImage = pickedImage
                if (currentPickedImage != null) {
                    Image(
                        bitmap = currentPickedImage,
                        contentDescription = "上传照片",
                        Modifier
                            .size(imageWidth)
                            .clickable {
                                pickImage()
                            }
                            .clip(RoundedCornerShape(12.dp))
                            .align(Alignment.TopCenter),
                        contentScale = ContentScale.FillWidth
                    )
                } else if (selectedFileType == HTML && selectedUri != null) {
                    CommonWebView(
                        Modifier
                            .size(imageWidth)
                            .clickable {
                                pickImage()
                            }
                            .clip(RoundedCornerShape(12.dp))
                            .align(Alignment.TopCenter)
                        ,
                        selectedUri,
                        false,
                        width = imageWidth,
                        height = imageWidth,
                    ) {
                        initForSnapShot(imageWidth.dpValue.toInt(), imageWidth.dpValue.toInt())
                        webSnapShotTaker = suspend {
                            this.takeSnapShot()
                        }
                    }
                } else {
                    Image(
                        painter = painterResource(imageResource),
                        contentDescription = "上传照片",
                        Modifier
                            .size(imageWidth)
                            .clickable {
                                pickImage()
                            }
                            .clip(RoundedCornerShape(12.dp))
                            .align(Alignment.TopCenter),
                        contentScale = ContentScale.FillWidth
                    )
                }
            }
        }
    }
}

@Composable
fun AddItemDialog(album: Album, totalLabelList: List<String>, albumData: List<Album>, onDismiss: () -> Unit) {

    var pickedImage by remember { mutableStateOf<ImageBitmap?>(null) }
    var itemName by remember { mutableStateOf("") }
    var itemDesc by remember { mutableStateOf("") }
    var itemRank by remember { mutableStateOf(ItemRank.NONE) }
    val totalLabelList = remember { mutableStateListOf(*(totalLabelList.map { LabelCheckInfo(it, it) }.toTypedArray())) }
    var itemScore by remember { mutableStateOf(0.0f) }
    val itemLabel = remember { mutableStateListOf<String>() }
    val itemLabelSet = remember { mutableStateMapOf<String, String>() }
    var webSnapShotTaker: (suspend ()->Bitmap)? by remember { mutableStateOf(null) }

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    val scope = rememberCoroutineScope()
    var imageResource by remember { mutableStateOf(R.drawable.upload_to_cloud) }
    val viewModel: AlbumViewModel = viewModel()
    val imageWidth = min(LocalConfiguration.current.let { min(it.screenWidthDp, it.screenHeightDp)/2 }, 180).dp
    var currentSelectedAlbum by remember { mutableStateOf(album) }
    var selectedFileType by remember { mutableStateOf<FileType?>(null) }
    fun saveItem(context: Context) {
        if (selectedUri == null || itemName.isBlank()) {
            Toast.makeText(context, "数据不合法，缺少文件或标题", Toast.LENGTH_SHORT).show()
            return
        }
        scope.launch {
            selectedUri?.let { selectedUri->
                val webSnapShot = if (selectedFileType == HTML) {
                    webSnapShotTaker?.invoke()
                } else {
                    null
                }
                onDismiss()
                viewModel.addItem(currentSelectedAlbum, selectedUri, itemName, itemRank, itemDesc, itemScore, itemLabelSet.keys, webSnapShot)
            }
        }
    }

    val multipleImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? ->
            uri?.takeIf { !it.isHidden() } ?: return@rememberLauncherForActivityResult
            when (getMediaType(uri)) {
                IMAGE -> {
                    BitMapCachePool.toBitMap(scope, uri, maxWidth = imageWidth.dpValue.toInt()) { _, bitmap->
                        pickedImage = bitmap?.asImageBitmap()
                    }
                }

                VIDEO -> {
                    getThumbnail(VIDEO, scope, uri, maxWidth = imageWidth.dpValue.toInt()) { bitmap: Bitmap? ->
                        pickedImage = bitmap?.asImageBitmap()
                    }
                }

                AUDIO -> {
                    imageResource = R.drawable.music
                    pickedImage = null
                    getThumbnail(AUDIO, scope, uri, maxWidth = imageWidth.dpValue.toInt()) { bitmap: Bitmap? ->
                        pickedImage = bitmap?.asImageBitmap()
                    }
                }

                HTML -> {
                    imageResource = R.drawable.chrome
                    pickedImage = null
                    getThumbnail(HTML, scope, uri, maxWidth = imageWidth.dpValue.toInt()) { bitmap: Bitmap? ->
                        pickedImage = bitmap?.asImageBitmap()
                    }
                }

                RegularFile -> {
                    imageResource = R.drawable.file
                    pickedImage = null
                }
            }
            if (itemName.isNullOrBlank()) {
                itemName = getFileNameFromUri(uri) ?:""
            }
            selectedUri = uri
            selectedUri?.let { selectedUri->
                selectedFileType = getMediaType(selectedUri)
            }
        }
    )

    fun pickImage() {
        multipleImagePickerLauncher.launch(FileType.mimes)
    }

    val context = LocalContext.current
    AppThemeDialog(
        Modifier
            .fillMaxWidth(LocalAppUIConstants.current.dialogPercent)
            .fillMaxHeight(LocalAppUIConstants.current.dialogPercent)
            .wrapContentHeight(),
        onDismissRequest = {},
        properties = DialogProperties(usePlatformDefaultWidth = false),
        onNegative = onDismiss,
        onPositive = { saveItem(context) }
    ) { _, actionButtons ->
        var openAlbumList by remember { mutableStateOf(false) }
        Box {
            LazyColumn(
                Modifier
                    .padding(top = imageWidth / 2)
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(LocalAppPalette.current.dialogBg)
                    .padding(top = imageWidth / 2)
            ) {
                item {
                    OutlinedTextField(itemName, { value->
                        itemName = value
                    }, modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp), label = {
                        AppThemeText("文件名称")
                    }, maxLines = Int.MAX_VALUE)

                    OutlinedTextField(itemDesc, { value->
                        itemDesc = value
                    }, modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp), label = {
                        AppThemeText("文件描述")
                    }, maxLines = Int.MAX_VALUE)

                    AppThemeText("选择合集: ${currentSelectedAlbum.albumName}",
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                openAlbumList = !openAlbumList
                            }
                            .padding(horizontal = 6.dp, vertical = 10.dp))
                }

                if (openAlbumList) {
                    items(albumData.size,
                        key = { index: Int -> albumData[index].let { it.albumId ?: DataBase.INVALID_ID to it.albumName } }
                    ) { index ->
                        val item = albumData[index]
                        ImmutableDrawerMenuItem(
                            item,
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 10.dp)
                        ) {
                            currentSelectedAlbum = item
                            openAlbumList = false
                        }
                    }
                }

                item {
                    Column(Modifier.fillMaxWidth()) {
                        AndroidView({ context ->
                            StarRateView(context).commonEditableConfig().apply {
                                onScoreChange = StarRateView.Companion.OnScoreChange { score: Float ->
                                    itemScore = score
                                }
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.WRAP_CONTENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            }
                        },
                            Modifier
                                .wrapContentWidth()
                                .height(30.dp)
                                .align(Alignment.CenterHorizontally),
                            update = {
                                it.setScore(itemScore)
                            })
                        Spacer(Modifier.height(4.dp))

                        AndroidView({ context ->
                            RankTypeChooser(context).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.WRAP_CONTENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                )
                                onRankChange = RankTypeChooser.Companion.OnRankTypeChange { value ->
                                    itemRank = value
                                }
                            }
                        },
                            Modifier
                                .wrapContentWidth()
                                .height(30.dp)
                                .align(Alignment.CenterHorizontally),
                            update = {
                                it.setRankType(itemRank)
                            })
                        Spacer(Modifier.height(4.dp))
                    }
                }

                item {
                    LazyHorizontalStaggeredGrid(rows = StaggeredGridCells.FixedSize(30.dp),
                        Modifier
                            .padding(horizontal = 6.dp)
                            .height(60.dp)
                            .fillMaxWidth()) {
                        item {
                            var addItemValue by remember { mutableStateOf("") }
                            EditableLabelView(addItemValue, enableEdit = true, onRemove = {
                                val addLabelList = addItemValue
                                    .trim()
                                    .splitByWhiteSpace()
                                    .toSet()
                                    .filter { it !in itemLabelSet.keys }
                                itemLabelSet.putAll(addLabelList.map { it to it })
                                itemLabel.addAll(0, addLabelList)
                                addItemValue = ""
                            }) { value->
                                addItemValue = value
                            }
                        }
                        items(totalLabelList.size, key = { index-> totalLabelList[index].name to 1 }) { index->
                            val item = totalLabelList[index]
                            val isChecked by item.isChecked.observeAsState()
                            CheckableLabelView(label = item.label, isChecked = isChecked ?: false) {
                                if (it && !itemLabelSet.containsKey(item.label)) {
                                    itemLabelSet[item.label] = item.label
                                    itemLabel.add(0, item.label)
                                } else if (!it && itemLabelSet.containsKey(item.label)) {
                                    itemLabelSet.remove(item.label)
                                    itemLabel.remove(item.label)
                                }
                                item.isChecked.value = it
                            }
                        }
                    }
                    LazyHorizontalStaggeredGrid(rows = StaggeredGridCells.FixedSize(30.dp),
                        Modifier
                            .padding(horizontal = 6.dp)
                            .height(60.dp)
                            .fillMaxWidth()) {
                        items(itemLabel.size, key = { index: Int ->
                            itemLabel[index]
                        }) { index: Int ->
                            val item = itemLabel[index]
                            EditableLabelView(item, enableEdit = false, onRemove = {
                                itemLabel.remove(item)
                                itemLabelSet.remove(item)
                            }) { }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }

                item {
                    Column(
                        Modifier
                            .padding(horizontal = 6.dp)
                            .fillMaxWidth()) {
                        actionButtons()
                    }
                }
            }

            val currentPickedImage = pickedImage
            if (currentPickedImage != null) {
                Image(
                    bitmap = currentPickedImage,
                    contentDescription = "上传照片",
                    Modifier
                        .size(imageWidth)
                        .clickable {
                            pickImage()
                        }
                        .clip(RoundedCornerShape(12.dp))
                        .align(Alignment.TopCenter),
                    contentScale = ContentScale.FillWidth
                )
            } else if (selectedFileType == HTML && selectedUri != null) {
                CommonWebView(
                    Modifier
                        .size(imageWidth)
                        .clickable {
                            pickImage()
                        }
                        .clip(RoundedCornerShape(12.dp))
                        .align(Alignment.TopCenter)
                    ,
                    selectedUri,
                    false,
                    width = imageWidth,
                    height = imageWidth,
                ) {
                    initForSnapShot(imageWidth.dpValue.toInt(), imageWidth.dpValue.toInt())
                    webSnapShotTaker = suspend {
                        this.takeSnapShot()
                    }
                }
            } else {
                Image(
                    painter = painterResource(imageResource),
                    contentDescription = "上传照片",
                    Modifier
                        .size(imageWidth)
                        .clickable {
                            pickImage()
                        }
                        .clip(RoundedCornerShape(12.dp))
                        .align(Alignment.TopCenter),
                    contentScale = ContentScale.FillWidth
                )
            }
        }
    }
}

@Composable
fun CheckableLabelView(label: String, isChecked: Boolean, onCheckStateChange: (Boolean) -> Unit) {
    LabelViewImpl(label = label, editable = false, checkable = true, isChecked = isChecked, onRemove = {}, onCheckStateChange = onCheckStateChange, onValueChange = { })
}

@Composable
fun LabelView(label: String) {
    LabelViewImpl(label = label, editable = false, checkable = false, isChecked = false, onRemove = { }, onCheckStateChange = {}, onValueChange = { })
}

@Composable
fun EditableLabelView(label: String, editable: Boolean = true, enableEdit: Boolean = editable, onRemove: ()->Unit, onValueChange: (String) -> Unit) {
    LabelViewImpl(label = label, editable = editable, enableEdit = enableEdit, checkable = false, isChecked = false, onRemove = onRemove, onCheckStateChange = {}, onValueChange = onValueChange)
}

@Composable
fun LabelViewImpl(label: String, editable: Boolean = false, enableEdit: Boolean = false, checkable: Boolean = false, isChecked: Boolean = false, onRemove: ()->Unit = {}, onCheckStateChange: (Boolean) -> Unit = {}, onValueChange: (String)->Unit = {}) {
    fun Modifier.onClickListener(): Modifier {
        return if (checkable) {
            this.clickable {
                onCheckStateChange(!isChecked)
            }
        } else {
            this
        }
    }
    @Composable
    fun Modifier.viewBackground(): Modifier {
        return if (isChecked) {
            this.background(
                color = LocalAppPalette.current.labelChecked,
                shape = RoundedCornerShape(4.dp)
            )
        } else {
            this.background(
                color = LocalAppPalette.current.labelUnChecked,
                shape = RoundedCornerShape(4.dp)
            )
        }
    }
    fun Modifier.viewHeight(): Modifier {
        return if (editable) {
            this.height(26.dp)
        } else {
            this.height(24.dp)
        }
    }
    val fontSize = if (editable) {
        16.sp
    } else {
        14.sp
    }
    Row(
        Modifier
            .padding(2.dp)
            .viewHeight()
            .viewBackground()
            .wrapContentWidth()
            .onClickListener(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (editable && enableEdit) {
            AppThemeBasicTextField(
                label,
                onValueChange = onValueChange,
                modifier = Modifier
                    .padding(horizontal = 4.dp, vertical = 2.dp)
                    .align(Alignment.CenterVertically)
                    .wrapContentSize(),
                textStyle = LocalTextStyle.current.copy(
                    fontSize = fontSize,
                    color = LocalAppPalette.current.labelTextColor
                ),
                hint = "新建标签"
            )
        } else {
            AppThemeText(
                label,
                Modifier
                    .padding(horizontal = 4.dp, vertical = 2.dp)
                    .align(Alignment.CenterVertically)
                    .wrapContentSize(),
                style = LocalTextStyle.current.copy(
                    fontSize = fontSize,
                    color = LocalAppPalette.current.labelTextColor
                )
            )
        }
        if (editable) {
            Spacer(Modifier.padding(2.dp))
            Image(
                painter = painterResource(
                    if (enableEdit) R.drawable.add_green
                    else R.drawable.close
                ),
                contentDescription = if (enableEdit) "添加标签" else "删除标签",
                Modifier
                    .padding(end = 4.dp)
                    .size(16.dp)
                    .align(Alignment.CenterVertically)
                    .clickable { onRemove() },
            )
        }
    }
}

@Composable
fun EditLabelView(onAddLabel: (List<String>)->Unit) {
    var labelText by remember { mutableStateOf("") }
    AndroidView(
        factory = { context->
            val followTailLayout = FollowTailLayout(context)
            followTailLayout.orientation = Gravity.LEFT // right不行
            val headView = ComposeView(context)
            headView.setContent {
                CompositionLocalProvider(LocalTextStyle provides LocalTextStyle.current.copy(fontSize = 24.sp)) {
                    AppThemeBasicTextField(labelText, onValueChange = {value-> labelText = value},
                        Modifier.wrapContentSize(),
                        hint = "添加标签",
                    )
                }
            }

            val tailView = ComposeView(context)
            tailView.setContent {
                Spacer(Modifier.width(2.dp))
                Image(
                    painter = painterResource(R.drawable.add_green),
                    contentDescription = "添加标签",
                    Modifier
                        .size(24.dp)
                        .clickable {
                            if (labelText.isNotBlank()) {
                                onAddLabel(
                                    labelText
                                        .trim()
                                        .splitByWhiteSpace()
                                )
                                labelText = ""
                            }
                        },
                )
            }
            tailView.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            followTailLayout.addHead(headView, ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ))
            followTailLayout.addTail(tailView)
            followTailLayout
        },
        modifier = Modifier
            .fillMaxSize()
            .padding(6.dp)
    )
}

@Composable
fun DrawerHeader() {
    var userName by remember { mutableStateOf(DEFAULT_USER_NAME) }
    var userDesc by remember { mutableStateOf(DEFAULT_DESC) }
    var userAvatarImage by remember { mutableStateOf<ImageBitmap?>(null) }

    val avatarSize = DrawerDefaults.MaximumDrawerWidth * (1 - goldenRatio)
    val borderSize = 2.dp
    val iconSize = avatarSize / 8f

    val scope = rememberCoroutineScope()
    var editUserInfoJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(Unit) {
        val innerUserInfo = UserStorage.userInstance
        userName = innerUserInfo.userName
        userDesc = innerUserInfo.userDesc
    }
    LaunchedEffect(Unit) {
        val fileInfo = UserStorage.userInstance.userAvatar
        val bitmap = withContext(Dispatchers.IO) {
            val (_, image) = BitMapCachePool.loadImage(
                fileInfo,
                avatarSize.dpValue.toInt(),
                avatarSize.dpValue.toInt()
            )
            image
        }
        userAvatarImage = bitmap?.asImageBitmap()
    }

    val multipleImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri: Uri? ->
            uri?.takeIf { !it.isHidden() } ?: return@rememberLauncherForActivityResult
            scope.launch(Dispatchers.IO) {
                val imageStorage = FileStorageUtils.getStorage(IMAGE) ?: return@launch
                val currentUser = UserStorage.userInstance
                val currentImageId =
                    currentUser.userAvatar.storageId.takeIf { it != DataBase.INVALID_ID }
                val nextId = if (currentImageId != null) {
                    BitMapCachePool.invalid(currentImageId, IMAGE)
                    imageStorage.asyncStore(currentImageId, uri)
                } else {
                    imageStorage.asyncStore(uri)
                }
                currentUser.userAvatar.storageId = nextId
                currentUser.userAvatar.fileType = IMAGE
                currentUser.userAvatar.uri = uri
                UserStorage.userInstance = currentUser
                val (_, image) = BitMapCachePool.loadImage(
                    currentUser.userAvatar,
                    avatarSize.dpValue.toInt(),
                    avatarSize.dpValue.toInt()
                )
                val bitmap = image?.asImageBitmap()
                withContext(Dispatchers.Main) {
                    userAvatarImage = bitmap
                }
            }
        }
    )

    var enableEdit by remember { mutableStateOf(false) }

    fun onUserNameChange(value: String) {
        val userInstance = UserStorage.userInstance
        userInstance.userName = value
        userName = value
        editUserInfoJob?.cancel()
        editUserInfoJob = CoroutineUtils.runIOTask({
            UserStorage.userInstance = userInstance
        })
    }

    fun onUserDescChange(value: String) {
        val userInstance = UserStorage.userInstance
        userInstance.userDesc = value
        userDesc = value
        editUserInfoJob?.cancel()
        editUserInfoJob = CoroutineUtils.runIOTask({
            UserStorage.userInstance = userInstance
        })
    }

    fun pickImage() {
        multipleImagePickerLauncher.launch(
            PickVisualMediaRequest
                .Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly)
                .build()
        )
    }

    fun circleOffset(R: Dp, r: Dp, halfBorder: Dp) : Dp {
        return R * (1 - sqrt(.5f)) - r + halfBorder * sqrt(0.5f)
    }

    val context = LocalContext.current
    val playIntent = remember(UserStorage.userInstance) {
        playIntent(context, UserStorage.userInstance.userAvatar)
    }

    Column(
        Modifier
            .fillMaxWidth()
            .drawerBackground()
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        val currentUserAvatarImage = userAvatarImage

        val imageModifier = Modifier
            .size(avatarSize, avatarSize)
            .clip(CircleShape)
            .border(borderSize, LocalAppPalette.current.strokeColor, CircleShape)
            .clickable {
                if (enableEdit) {
                    pickImage()
                } else {
                    if (playIntent != null) {
                        context.startActivity(playIntent)
                    }
                }
            }

        Box(
            Modifier
                .fillMaxWidth()
                .padding(start = 6.dp, top = 6.dp, end = 6.dp)
                .background(
                    color = LocalAppPalette.current.cardBg, shape = RoundedCornerShape(4.dp)
                )
                .align(Alignment.CenterHorizontally)
                .padding(vertical = 6.dp, horizontal = 6.dp)
        ) {
            Row {
                Box {
                    if (currentUserAvatarImage == null) {
                        Image(
                            painter = painterResource(R.drawable.user),
                            contentDescription = "头像",
                            modifier = imageModifier,
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Image(
                            bitmap = currentUserAvatarImage,
                            contentDescription = "头像",
                            modifier = imageModifier,
                            contentScale = ContentScale.Crop,
                        )
                    }
                    if (enableEdit) {
                        Image(
                            painter = painterResource(
                                R.drawable.edit
                            ),
                            contentDescription = "编辑用户信息",
                            Modifier
                                .size(iconSize, iconSize)
                                .clickable {
                                    enableEdit = !enableEdit
                                }
                                .align(Alignment.BottomEnd)
                                .clickable {
                                    if (enableEdit) {
                                        pickImage()
                                    } else {
                                        if (playIntent != null) {
                                            context.startActivity(playIntent)
                                        }
                                    }
                                }
                                .offset(
                                    -circleOffset(
                                        avatarSize / 2f, iconSize / 2f, borderSize / 2f
                                    ),
                                    -circleOffset(avatarSize / 2f, iconSize / 2f, borderSize / 2f)
                                )
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Column(Modifier.align(Alignment.CenterVertically)) {
                    CompositionLocalProvider(LocalTextStyle provides LocalTextStyle.current.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight(600),
                    )) {
                        if (enableEdit) {
                            AppThemeBasicTextField(
                                value = userName,
                                onValueChange = { value->
                                    onUserNameChange(value)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 2,
                                hint = "输入用户名",
                            )
                        } else {
                            AppThemeText(
                                text = userName,
                                modifier = Modifier.fillMaxWidth(),
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 2,
                                hint = "输入用户名",
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    CompositionLocalProvider(LocalTextStyle provides LocalSecondaryTextStyle.current.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight(600),
                    )) {

                        if (enableEdit) {
                            AppThemeBasicTextField(
                                value = userDesc,
                                onValueChange = { value->
                                    onUserDescChange(value)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 2,
                                hint = "输入个性签名",
                            )
                        } else {
                            AppThemeText(
                                text = userDesc,
                                modifier = Modifier.fillMaxWidth(),
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 2,
                                hint = "输入个性签名",
                            )
                        }
                    }
                }
            }
            Image(
                painter = painterResource(
                    if (enableEdit) {
                        R.drawable.close
                    } else {
                        R.drawable.edit
                    }
                ),
                contentDescription = "编辑用户信息",
                Modifier
                    .size(16.dp)
                    .clickable {
                        enableEdit = !enableEdit
                    }
                    .align(Alignment.BottomEnd)
            )
        }
        DrawerFunctionArea()
    }
}

@Composable
fun DrawerFunctionView(onClick: () -> Unit, @DrawableRes drawableId: Int, text: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 8.dp)
    ) {
        Icon(
            painter = painterResource(drawableId),
            contentDescription = "leadingIcon",
            Modifier
                .size(26.dp)
                .align(Alignment.CenterVertically),
        )
        AppThemeText(text,
            Modifier
                .align(Alignment.CenterVertically)
                .padding(horizontal = 6.dp),
            style = LocalTextStyle.current.copy(fontSize = 16.sp)
        )
    }
}

@Composable
fun DrawerFunctionArea() {
    var dialogState by remember { mutableStateOf(false) }
    DrawerFunctionView(
        onClick = {
            dialogState = true
        },
        drawableId = R.drawable.add,
        text = "添加合集",
    )

    if (dialogState) {
        AddOrEditAlbumDialog {
            dialogState = false
        }
    }
}

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
        onPositive = onPositive@ {
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
    ) { _, actionButtons->
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
                    AppThemeHorizontalDivider(modifier = Modifier
                        .height(1.dp)
                        .fillMaxWidth(0.95f)
                        .align(Alignment.Center))
                }
            }
            items(
                albumData.size,
                key = { index: Int -> (albumData[index].albumId ?: DataBase.INVALID_ID) to albumData[index].albumName }) { index ->
                val item = albumData[index]
                DrawerMenuItem(item, enableEdit) {
                    onAlbumSelected(index, item)
                }
            }
        }
        AppThemeSwitcher()
    }
}

@Composable
fun AppThemeSwitcher() {
    val viewModel: AppThemeViewModel = viewModel()
    val currentTheme by viewModel.currentAppTheme.observeAsState()
    @Composable
    fun Modifier.modifier(appTheme: AppTheme):Modifier {
        val modifier = this
        return if (appTheme == (currentTheme ?: AppTheme.AUTO)) {
            modifier.background(
                color = LocalAppPalette.current.labelChecked,
                shape = RoundedCornerShape(100)
            )
        } else {
            modifier.background(
                color = LocalAppPalette.current.labelUnChecked,
                shape = RoundedCornerShape(100)
            )
        }
            .clip(RoundedCornerShape(100))
            .clickable {
                viewModel.currentAppTheme.value = appTheme
            }
            .padding(8.dp)
    }

    val size = 42.dp
    val autoWidth = size * 1.6464f

    Box(
        Modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .fillMaxWidth()
            .wrapContentHeight()) {
//        AppThemeText(
//            "主题颜色",
//            Modifier
//                .wrapContentSize()
//                .align(Alignment.CenterStart),
//            style = LocalTextStyle.current.copy(fontSize = 16.sp)
//        )
        Row(
            Modifier
                .wrapContentSize()
                .align(Alignment.CenterEnd)
        ) {
            Image(
                painter = painterResource(R.drawable.app_theme_auto),
                contentDescription = "自动",
                Modifier
                    .size(width = autoWidth, size)
                    .modifier(AppTheme.AUTO)
            )
            Spacer(Modifier.width(8.dp))
            Image(
                painter = painterResource(R.drawable.app_theme_light),
                contentDescription = "浅色",
                Modifier
                    .size(size)
                    .modifier(AppTheme.Lite)
            )
            Spacer(Modifier.width(8.dp))
            Image(
                painter = painterResource(R.drawable.app_theme_night),
                contentDescription = "深色",
                Modifier
                    .size(size)
                    .modifier(AppTheme.Dark)
            )
        }
    }
}

@Composable
fun ImmutableDrawerMenuItem(
    item: Album,
    modifier: Modifier,
    onItemClick: () -> Unit
) {
    Box(
        Modifier
            .wrapContentSize()
            .clickable { onItemClick() }) {
        AppThemeText(item.albumName, modifier)
    }
}

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
    val viewModel: AlbumViewModel = viewModel()
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
                        painter = painterResource(R.drawable.trash_bin),
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
        AppThemeConfirmDialog("确认删除合集: ${item.albumName}", properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false), onNegative = {
            deleteConfirmDialogState = false
        }, onPositive = {
            deleteConfirmDialogState = false
            viewModel.deleteAlbum(item)
        }, onDismissRequest = {
            deleteConfirmDialogState = false
        }, content = {})
    }
}
