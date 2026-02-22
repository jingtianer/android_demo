package com.jingtian.composedemo

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerDefaults
import androidx.compose.runtime.Composable
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
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
import com.jingtian.composedemo.dao.model.ItemRank
import com.jingtian.composedemo.dao.model.LabelInfo
import com.jingtian.composedemo.dao.model.relation.AlbumItemRelation
import com.jingtian.composedemo.ui.theme.LocalAppPalette
import com.jingtian.composedemo.ui.theme.LocalAppUIConstants
import com.jingtian.composedemo.ui.theme.LocalMiddleButtonConfig
import com.jingtian.composedemo.ui.theme.LocalSecondaryTextStyle
import com.jingtian.composedemo.ui.theme.appBackground
import com.jingtian.composedemo.ui.theme.drawerBackground
import com.jingtian.composedemo.ui.theme.goldenRatio
import com.jingtian.composedemo.ui.widget.RankTypeChooser
import com.jingtian.composedemo.ui.widget.RankTypeChooser.Companion.createBg
import com.jingtian.composedemo.ui.widget.StarRateView
import com.jingtian.composedemo.utils.BitMapCachePool
import com.jingtian.composedemo.utils.CoroutineUtils
import com.jingtian.composedemo.utils.FileStorageUtils
import com.jingtian.composedemo.utils.FileStorageUtils.getFileNameFromUri
import com.jingtian.composedemo.utils.FileStorageUtils.getMediaType
import com.jingtian.composedemo.utils.FileStorageUtils.getVideoThumbnail
import com.jingtian.composedemo.utils.FileStorageUtils.safeToFile
import com.jingtian.composedemo.utils.UserStorage
import com.jingtian.composedemo.utils.ViewUtils.commonConfig
import com.jingtian.composedemo.utils.ViewUtils.commonEditableConfig
import com.jingtian.composedemo.utils.ViewUtils.dpValue
import com.jingtian.composedemo.viewmodels.AlbumViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
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
                val canClose = !menuItemsEntity.isEmpty()
                if (!canClose) {
                    viewModel.sendMessage("没有任何相册，先创建相册吧")
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
            gesturesEnabled = menuItemsEntity.isNotEmpty()
        ) {
            Gallery(currentSelectedAlbum) {
                scope.launch {
                    if (drawerState.isOpen) {
                        drawerState.close()
                    } else {
                        drawerState.open()
                    }
                }
            }
            val editDialogAlbum by viewModel.editDialogAlbum.observeAsState()
            val editDialogAlbumItem by viewModel.editDialogAlbumItem.observeAsState()

            editDialogAlbumItem?.let { editDialogAlbumItem->
                editDialogAlbum?.let { editDialogAlbum->
                    EditDialog(editDialogAlbumItem, editDialogAlbum, menuItemsEntity) {
                        viewModel.editDialogAlbum.value = null
                        viewModel.editDialogAlbumItem.value = null
                    }
                }
            }
        }
    }



}

class LabelCheckInfo<T>(val label: T, val name: String, var isChecked: MutableLiveData<Boolean> = MutableLiveData(false))

fun <T> LazyListScope.roundRectTabFilter(checkedList: List<LabelCheckInfo<T>>, onCheckStateChange: (List<T>)->Unit) {
    items(checkedList.size) { index->
        RoundRectCheckableLabel(
            checkedList[index],
            checkedList,
            true,
            onCheckStateChange
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Gallery(album: IndexedValue<Album>?, openDrawer: ()->Unit) {
    if (album == null) {
        return
    }

    var addImageDialogState by remember { mutableStateOf(false) }
    var itemList by remember { mutableStateOf(emptyList<AlbumItemRelation>()) }
    var filteredItemList by remember { mutableStateOf(emptyList<AlbumItemRelation>()) }
    val viewModel: AlbumViewModel = viewModel()
    var filterLabels by remember { mutableStateOf<Set<String>>(emptySet()) }
    var filterFileTypes by remember { mutableStateOf<List<FileType>>(emptyList()) }
    var itemRankFilter by remember { mutableStateOf<List<ItemRank>>(emptyList()) }
    var showLabelFilter by remember { mutableStateOf(false) }
    val coroutine = rememberCoroutineScope()
    val albumItemDataChange by viewModel.albumItemListChange.observeAsState()

    var labelFilterCheckedInfo by remember { mutableStateOf<List<LabelCheckInfo<String>>>(emptyList()) }
    val fileTypeCheckState by remember { mutableStateOf(FileType.entries.map { LabelCheckInfo(it, it.typeName) }) }
    val itemRankTypeCheckState by remember { mutableStateOf(ItemRank.entries.map { LabelCheckInfo(it, it.name) }) }

    var albumName by remember { mutableStateOf(album.value.albumName) }

    var editAlbumDialogState by remember { mutableStateOf(false) }
    LaunchedEffect(album) {
        withContext(Dispatchers.IO) {
            viewModel.getLabelList(album.value).collect { value->
                withContext(Dispatchers.Main) {
                    albumName = album.value.albumName
                    labelFilterCheckedInfo = value.map { LabelCheckInfo(it, it) }
                }
            }
        }
    }

    LaunchedEffect(albumItemDataChange, album) {
        withContext(Dispatchers.IO) {
            viewModel.getAllAlbumItem(album.value).collect {
                withContext(Dispatchers.Main) {
                    itemList = it
                    filteredItemList = it
                }
            }
        }
    }

    val albumNameChange by viewModel.albumNameChange.observeAsState()

    LaunchedEffect(albumNameChange) {
        withContext(Dispatchers.IO) {
            val newAlbum = viewModel.getAlbumName(album.value.albumId ?: return@withContext)
            album.value.albumName = newAlbum.albumName
            albumName = newAlbum.albumName
        }
    }

    val importDirLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) {uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        viewModel.importFiles(album.value, uri)
    }

    Column(
        Modifier
            .fillMaxSize()
            .appBackground()
            .windowInsetsPadding(WindowInsets.systemBars)) {
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
                    .clickable { openDrawer() }
                    .padding(4.dp)
            )
            Spacer(Modifier.width(12.dp))
            AppThemeText(albumName,
                Modifier
                    .align(Alignment.CenterVertically)
                    .fillMaxWidth()
                    .weight(1f), style = LocalTextStyle.current.copy(fontSize = 24.sp, fontWeight = FontWeight(600)))
            Spacer(Modifier.width(6.dp))
            Row(
                Modifier
                    .wrapContentSize()
                    .align(Alignment.CenterVertically)) {
                Icon(
                    painter = painterResource(R.drawable.edit_normal),
                    contentDescription = "编辑名称",
                    modifier = Modifier
                        .size(LocalAppUIConstants.current.filterLabelHeight)
                        .background(LocalAppPalette.current.labelUnChecked, shape = CircleShape)
                        .clickable { editAlbumDialogState = true }
                        .padding(4.dp)
                )
                Spacer(Modifier.width(6.dp))
                Icon(
                    painter = painterResource(R.drawable.add),
                    contentDescription = "添加图片",
                    modifier = Modifier
                        .size(LocalAppUIConstants.current.filterLabelHeight)
                        .background(LocalAppPalette.current.labelUnChecked, shape = CircleShape)
                        .clickable { addImageDialogState = true }
                        .padding(4.dp)
                )
                Spacer(Modifier.width(6.dp))
                Icon(
                    painter = painterResource(R.drawable.import_icon),
                    contentDescription = "批量导入",
                    modifier = Modifier
                        .size(LocalAppUIConstants.current.filterLabelHeight)
                        .background(LocalAppPalette.current.labelUnChecked, shape = CircleShape)
                        .clickable {
                            importDirLauncher.launch(null)
                        }
                        .padding(4.dp)
                )
            }
        }
        Row(Modifier.padding(start = 4.dp, end = 8.dp)) {
            LazyRow(
                Modifier
                    .wrapContentHeight()
                    .weight(1f)) {
                roundRectTabFilter(labelFilterCheckedInfo) { checkInfo->
                    val targetLabelSet = checkInfo.toSet()
                    filterLabels = targetLabelSet
                }
                roundRectTabFilter(fileTypeCheckState) { checkedFileType->
                    filterFileTypes = checkedFileType
                }
                roundRectTabFilter(itemRankTypeCheckState) { checkedItemRank->
                    itemRankFilter = checkedItemRank
                }
            }
//            Image(painter = painterResource(R.drawable.trash_bin),
//                contentDescription = "清空筛选",
//                Modifier
//                    .size(24.dp)
//                    .clickable {
//                        filterLabels = emptySet()
//                        filterFileTypes = emptyList()
//                        itemRankFilter = emptyList()
//                        labelFilterCheckedInfo.forEach { it.isChecked.value = false }
//                        fileTypeCheckState.forEach { it.isChecked.value = false }
//                        itemRankTypeCheckState.forEach { it.isChecked.value = false }
//                    })

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
        LaunchedEffect(filterFileTypes, filterLabels, itemRankFilter) {
            coroutine.launch(Dispatchers.Default) {
                val intersectList = itemList.filter {
                    (filterLabels.isEmpty() || it.labelInfos.map { it.label }.toSet().intersect(filterLabels).isNotEmpty())
                            && (filterFileTypes.isEmpty() || (it.fileInfo?.let { fileInfo -> filterFileTypes.contains(fileInfo.fileType) } ?: true))
                            && (itemRankFilter.isEmpty() || itemRankFilter.contains(it.albumItem.rank))
                }
                withContext(Dispatchers.Main) {
                    filteredItemList = intersectList
                }
            }
        }
        val size = 160.dp
        val galleryItemPadding = 4.dp
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Adaptive((size + galleryItemPadding*2)),
            Modifier
                .fillMaxSize()
                .weight(1f)
                .padding(horizontal = galleryItemPadding)) {
            items(filteredItemList.size, key = { index-> filteredItemList[index].hashCode() }) { index: Int ->
                AlbumItemView(filteredItemList[index], album.value, size, galleryItemPadding)
            }
        }
    }

    if (addImageDialogState) {
        AddItemDialog(album.value) {
            addImageDialogState = false
        }
    }

    if (editAlbumDialogState) {
        AddOrEditAlbumDialog(album.value) {
            editAlbumDialogState = false
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

   LaunchedEffect(showLabelFilter) {
       if (showLabelFilter) {
           scope.launch {
               sheetState.expand()
           }
       } else {
           scope.launch {
               sheetState.hide()
           }
       }
   }

    if (showLabelFilter) {
        FilterPanel(
            sheetState,
            fileTypeCheckState,
            { checkedFileType -> filterFileTypes = checkedFileType },
            itemRankTypeCheckState,
            { checkedItemRank ->
                itemRankFilter = checkedItemRank
            },
            labelFilterCheckedInfo,
            { checkInfo ->
                val targetLabelSet = checkInfo.toSet()
                filterLabels = targetLabelSet
            }
        ) {
            showLabelFilter = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterPanel(
    sheetState: SheetState,
    fileTypeCheckStateList: List<LabelCheckInfo<FileType>>,
    onFileTypeCheckStateChange: (List<FileType>)->Unit,
    itemRankCheckStateList: List<LabelCheckInfo<ItemRank>>,
    onItemRankCheckStateChange: (List<ItemRank>)->Unit,
    labelCheckStateList: List<LabelCheckInfo<String>>,
    onLabelCheckStateChange: (List<String>)->Unit,
    onDismiss: () -> Unit,
) {
    val horizontalPadding = 6.dp
    val verticalPadding = 6.dp
    val horizontalInnerPadding = LocalAppUIConstants.current.filterLabelPaddings[2]

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(
                sqrt(1 - goldenRatio)
            ),
        containerColor = LocalAppPalette.current.bottomSheetBackgroundColor
    ) {
        val scope = rememberCoroutineScope()


        LazyVerticalGrid(
            columns = GridCells.Adaptive(LocalAppUIConstants.current.filterLabelHeight * LocalAppUIConstants.current.filterLabelAspectRatio),
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(horizontal = horizontalPadding)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                AppThemeText(text = "类型筛选", Modifier.padding(horizontal = horizontalInnerPadding, vertical = verticalPadding), style = LocalTextStyle.current.copy(fontWeight = FontWeight(600), fontSize = 16.sp))
            }
            items(fileTypeCheckStateList.size) { index ->
                RoundRectCheckableLabel(
                    fileTypeCheckStateList[index],
                    fileTypeCheckStateList,
                    false,
                    onFileTypeCheckStateChange
                )
            }
            item(span = { GridItemSpan(this.maxLineSpan) }) {
                AppThemeText(text = "排行筛选", Modifier.padding(horizontal = horizontalInnerPadding, vertical = verticalPadding), style = LocalTextStyle.current.copy(fontWeight = FontWeight(600), fontSize = 16.sp))
            }
            items(itemRankCheckStateList.size) { index ->
                RoundRectCheckableLabel(
                    itemRankCheckStateList[index],
                    itemRankCheckStateList,
                    false,
                    onItemRankCheckStateChange
                )
            }
            if (labelCheckStateList.isNotEmpty()) {
                item(span = { GridItemSpan(this.maxLineSpan) }) {
                    AppThemeText(text = "标签筛选", Modifier.padding(horizontal = horizontalInnerPadding, vertical = verticalPadding), style = LocalTextStyle.current.copy(fontWeight = FontWeight(600), fontSize = 16.sp))
                }
            }
        }

        LazyHorizontalStaggeredGrid(
            StaggeredGridCells.Adaptive(LocalAppUIConstants.current.filterLabelHeight + LocalAppUIConstants.current.filterLabelPaddings[1] + LocalAppUIConstants.current.filterLabelPaddings[3]),
            Modifier
                .fillMaxHeight()
                .fillMaxWidth()
                .weight(1f),
        ) {
            items(labelCheckStateList.size) { index ->
                val item = labelCheckStateList[index]
                RoundRectCheckableLabel(
                    item,
                    labelCheckStateList,
                    true,
                    onLabelCheckStateChange
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding)) {
            Button(onClick = {
                scope.launch {
                    fileTypeCheckStateList.forEach { it.isChecked.value = !(it.isChecked.value ?: false) }
                    itemRankCheckStateList.forEach { it.isChecked.value = !(it.isChecked.value ?: false) }
                    labelCheckStateList.forEach { it.isChecked.value = !(it.isChecked.value ?: false) }
                    onFileTypeCheckStateChange(emptyList())
                    onItemRankCheckStateChange(emptyList())
                    onLabelCheckStateChange(emptyList())
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
                    fileTypeCheckStateList.forEach { it.isChecked.value = false }
                    itemRankCheckStateList.forEach { it.isChecked.value = false }
                    labelCheckStateList.forEach { it.isChecked.value = false }
                    onFileTypeCheckStateChange(emptyList())
                    onItemRankCheckStateChange(emptyList())
                    onLabelCheckStateChange(emptyList())
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
fun <T> RoundRectCheckableLabel(item: LabelCheckInfo<T>, checkedList: List<LabelCheckInfo<T>>, wrapContent: Boolean, onCheckStateChange: (List<T>)->Unit) {
    val checked by item.isChecked.observeAsState()
    val isChecked = checked ?: false
    val paddings = LocalAppUIConstants.current.filterLabelPaddings
    val paddingInnerHorizontal = paddings[0]
    val paddingInnerVertical = paddings[1]
    val paddingOuterHorizontal = paddings[2]
    val paddingOuterVertical = paddings[3]
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
                item.isChecked.value = !isChecked
                onCheckStateChange(checkedList.mapNotNull { checkedInfo ->
                    checkedInfo.label.takeIf {
                        checkedInfo.isChecked.value ?: false
                    }
                })
            }) {
        AppThemeText(
            text = item.name,
            Modifier
                .wrapContentSize()
                .align(Alignment.Center)
                .padding(horizontal = paddingInnerHorizontal, vertical = paddingInnerVertical)
        )
    }
}
@Composable
fun AlbumItemView(albumItemRelation: AlbumItemRelation, album: Album, size: Dp, padding: Dp) {
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    var itemName by remember { mutableStateOf(albumItemRelation.albumItem.itemName) }
    var itemDesc by remember { mutableStateOf(albumItemRelation.albumItem.desc) }
    var itemRank by remember { mutableStateOf(albumItemRelation.albumItem.rank) }
    var itemScore by remember { mutableStateOf(albumItemRelation.albumItem.score) }
    var itemLabel by remember { mutableStateOf(albumItemRelation.labelInfos.toMutableList()) }
    var itemLabelSize by remember { mutableStateOf(albumItemRelation.labelInfos.size) }

    val scope = rememberCoroutineScope()
    var imageResource by remember { mutableStateOf(R.drawable.load_failed) }

    fun FileInfo.aspectRatio(): Float? {
        return (this.intrinsicWidth.toFloat() / this.intrinsicHeight.toFloat()).takeIf {
            this.intrinsicHeight > 0 && this.intrinsicWidth > 0
        }
    }

    fun ImageBitmap.aspectRatio(): Float? {
        return (this.width.toFloat() / this.height.toFloat()).takeIf {
            this.width > 0 && this.height > 0
        }
    }

    var intrinsicRatio by remember {
        mutableStateOf(
            albumItemRelation.fileInfo?.aspectRatio()
        )
    }

    suspend fun fetchImage() {
        withContext(Dispatchers.IO) {
            val uri = albumItemRelation.fileInfo?.getFileUri() ?: return@withContext
            val fileType = albumItemRelation.fileInfo.fileType
            when(fileType) {
                FileType.IMAGE -> {
                    val (_, bitmap) = BitMapCachePool.loadImage(
                        albumItemRelation.fileInfo,
                        size.dpValue.toInt(),
                    )
                    withContext(Dispatchers.Main) {
                        imageBitmap = bitmap?.asImageBitmap()
                        imageBitmap?.aspectRatio()?.let {
                            intrinsicRatio = it
                        }
                    }
                }
                FileType.VIDEO -> {
                    getVideoThumbnail(
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
                FileType.AUDIO -> {
                    imageResource = R.drawable.music
                }
                FileType.RegularFile -> {
                    imageResource = R.drawable.file
                }
            }
        }
    }
    LaunchedEffect(Unit) {
        fetchImage()
    }

    val context = LocalContext.current
    val playIntent = remember(albumItemRelation) {
        val fileInfo = albumItemRelation.fileInfo ?: return@remember null
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
        } ?: return@remember null
        Intent(Intent.ACTION_VIEW).apply {
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

    var showEditDialog by remember { mutableStateOf(false) }
    val viewModel: AlbumViewModel = viewModel()

    Column(Modifier
        .width(size)
        .padding(padding)
        .pointerInput(Unit) {
            detectTapGestures(onLongPress = {
                showEditDialog = true
                viewModel.editDialogAlbumItem.value = albumItemRelation
                viewModel.editDialogAlbum.value = album
            })
        }
        .background(
            color = LocalAppPalette.current.galleryCardBg, shape = RoundedCornerShape(padding)
        )
        .clip(RoundedCornerShape(padding))) {

        val currentPickedImage = imageBitmap

        fun Modifier.aspectRatioOrNull(intrinsicRatio: Float?): Modifier {
            return if (intrinsicRatio != null) {
                aspectRatio(intrinsicRatio)
            } else {
                this
            }
        }

        Box(Modifier
            .clip(RoundedCornerShape(padding))) {
            if (currentPickedImage == null) {
                Image(
                    painter = painterResource(imageResource),
                    contentDescription = "文件缩略图",
                    Modifier
                        .clickable {
                            scope.launch(Dispatchers.IO) {
                                fetchImage()
                            }
                            if (playIntent != null) {
                                context.startActivity(playIntent)
                            }
                        }
                        .fillMaxWidth()
                        .aspectRatioOrNull(intrinsicRatio)
                        .align(Alignment.Center),
                    contentScale = ContentScale.FillWidth
                )
            } else {
                Image(
                    bitmap = currentPickedImage,
                    contentDescription = "文件缩略图",
                    Modifier
                        .clickable {
                            if (playIntent != null) {
                                context.startActivity(playIntent)
                            }
                        }
                        .fillMaxWidth()
                        .aspectRatioOrNull(intrinsicRatio)
                        .align(Alignment.Center),
                    contentScale = ContentScale.FillWidth
                )
            }

            if (itemRank != null && itemRank != ItemRank.NONE) {
                fun View.initRankView(): View {
                    val bg = createBg(itemRank)
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
                items(itemLabelSize, key = { index: Int ->
                    itemLabel[index].label
                }) { index: Int ->
                    LabelView(itemLabel[index], editable = false) {
                        itemLabel.removeAt(index)
                        itemLabelSize = itemLabel.size
                    }
                }
            }
        }
    }
}

@Composable
fun EditDialog(albumItemRelation: AlbumItemRelation, relatedAlbum: Album, albumData: List<Album>, onDismiss: ()->Unit) {
    val album = albumItemRelation.albumItem
    val viewModel : AlbumViewModel = viewModel()

    var pickedImage by remember { mutableStateOf<ImageBitmap?>(null) }
    var itemName by remember { mutableStateOf(album.itemName) }
    var itemDesc by remember { mutableStateOf(album.desc) }
    var itemRank by remember { mutableStateOf(album.rank) }
    var itemScore by remember { mutableStateOf(album.score) }
    val itemLabel by remember { mutableStateOf(albumItemRelation.labelInfos.toMutableList()) }
    var itemLabelSize by remember { mutableStateOf(albumItemRelation.labelInfos.size) }

    var selectedUri by remember { mutableStateOf(albumItemRelation.fileInfo?.getFileUri()) }
    var selectedFileType by remember { mutableStateOf(albumItemRelation.fileInfo?.fileType) }
    val scope = rememberCoroutineScope()
    var imageResource by remember { mutableStateOf(R.drawable.upload_to_cloud) }

    fun deleteItem() {
        viewModel.deleteItem(albumItemRelation)
    }

    var currentSelectedAlbum by remember { mutableStateOf(relatedAlbum) }

    fun saveItem(context: Context) {
        val selectedUri = selectedUri
        val selectedFileType = selectedFileType
        if (selectedUri == null || selectedFileType == null || itemName.isNullOrBlank()) {
            Toast.makeText(context, "数据不合法，缺少文件或标题", Toast.LENGTH_SHORT).show()
            return
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
            itemLabel,
            currentSelectedAlbum?.albumId,
        )
    }

    fun updateImage(uri: Uri, fileType: FileType, fileInfo: FileInfo?) {
        when (fileType) {
            FileType.IMAGE -> {
                scope.launch(Dispatchers.IO) {
                    val bitmap = if (fileInfo != null) {
                        BitMapCachePool.loadImage(fileInfo).second?.asImageBitmap()
                    } else {
                        BitMapCachePool.toBitMap(uri).second?.asImageBitmap()
                    }
                    withContext(Dispatchers.Main) {
                        pickedImage = bitmap
                    }
                }
            }

            FileType.VIDEO -> {
                if (fileInfo != null) {
                    getVideoThumbnail(fileInfo, scope, uri) { bitmap: Bitmap? ->
                        withContext(Dispatchers.Main) {
                            pickedImage = bitmap?.asImageBitmap()
                        }
                    }
                } else {
                    getVideoThumbnail(scope, uri) { bitmap: Bitmap? ->
                        withContext(Dispatchers.Main) {
                            pickedImage = bitmap?.asImageBitmap()
                        }
                    }
                }
            }

            FileType.AUDIO -> {
                imageResource = R.drawable.music
                pickedImage = null
            }

            FileType.RegularFile -> {
                imageResource = R.drawable.file
                pickedImage = null
            }
        }
    }

    fun onSelectedUriChange() {
        val uri = selectedUri
        val fileType = selectedFileType
        if (uri != null && fileType != null) {
            updateImage(uri, fileType, null)
        } else {
            imageResource = R.drawable.load_failed
        }
        if (itemName.isNullOrBlank() && uri != null) {
            itemName = getFileNameFromUri(uri) ?:""
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
            uri ?: return@rememberLauncherForActivityResult
            selectedFileType = getMediaType(uri)
            selectedUri = uri
            onSelectedUriChange()
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
                .fillMaxWidth(LocalAppUIConstants.current.dialogPercent)
                .fillMaxHeight(LocalAppUIConstants.current.dialogPercent)
                .clip(RoundedCornerShape(4.dp))
                .background(LocalAppPalette.current.dialogBg)
                .wrapContentHeight(),
            onNegative = onDismiss,
            onMiddleClick = {
                deleteItem()
                onDismiss()
            },
            onPositive = {
                saveItem(context)
            },
            onDismissRequest = {},
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) { _, actionButtons->
            var openAlbumList by remember { mutableStateOf(false) }

            LazyColumn(
                Modifier
                    .fillMaxWidth()
//                    .verticalScroll(rememberScrollState())
                    .wrapContentHeight()
                    .background(LocalAppPalette.current.dialogBg)
//                    .padding(12.dp)
                    .clip(RectangleShape)
            ) {

                item {
                    val currentPickedImage = pickedImage
                    if (currentPickedImage == null) {
                        Spacer(Modifier.height(12.dp))
                        Image(
                            painter = painterResource(imageResource),
                            contentDescription = "上传照片",
                            Modifier
                                .clickable {
                                    pickImage()
                                }
                                .fillMaxWidth()
                                .wrapContentHeight(),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Image(
                            bitmap = currentPickedImage,
                            contentDescription = "上传照片",
                            Modifier
                                .clickable {
                                    pickImage()
                                }
                                .fillMaxWidth()
                                .wrapContentHeight(),
                            contentScale = ContentScale.Fit
                        )
                    }

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
                    items(
                        albumData.size,
                        key = { index: Int ->
                            albumData[index].albumId ?: DataBase.INVALID_ID
                        }) { index ->
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

                        EditLabelView {
                            itemLabel.add(0, LabelInfo(label = it))
                            itemLabelSize = itemLabel.size
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }

                item {
                    LazyRow(Modifier.padding(horizontal = 6.dp)) {
                        items(itemLabelSize, key = { index: Int ->
                            itemLabel[index].label
                        }) { index: Int ->
                            LabelView(itemLabel[index]) {
                                itemLabel.removeAt(index)
                                itemLabelSize = itemLabel.size
                            }
                        }
                    }
                    Column(
                        Modifier
                            .padding(horizontal = 6.dp)
                            .fillMaxWidth()) {
                        actionButtons()
                    }
                }
            }
        }
    }
}

@Composable
fun AddItemDialog(album: Album, onDismiss: () -> Unit) {

    var pickedImage by remember { mutableStateOf<ImageBitmap?>(null) }
    var itemName by remember { mutableStateOf("") }
    var itemDesc by remember { mutableStateOf("") }
    var itemRank by remember { mutableStateOf(ItemRank.NONE) }
    var itemScore by remember { mutableStateOf(0.0f) }
    val itemLabel by remember { mutableStateOf<MutableList<LabelInfo>>(mutableListOf()) }
    var itemLabelSize by remember { mutableStateOf<Int>(0) }

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    val scope = rememberCoroutineScope()
    var imageResource by remember { mutableStateOf(R.drawable.upload_to_cloud) }
    val viewModel: AlbumViewModel = viewModel()

    fun saveItem(context: Context) {
        if (selectedUri == null || itemName.isNullOrBlank()) {
            Toast.makeText(context, "数据不合法，缺少文件或标题", Toast.LENGTH_SHORT).show()
            return
        }
        onDismiss()
        viewModel.addItem(album, selectedUri, itemName, itemRank, itemDesc, itemScore, itemLabel)
    }

    val multipleImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? ->
            uri ?: return@rememberLauncherForActivityResult
            when (getMediaType(uri)) {
                FileType.IMAGE -> {
                    BitMapCachePool.toBitMap(scope, uri) { _, bitmap->
                        pickedImage = bitmap?.asImageBitmap()
                    }
                }

                FileType.VIDEO -> {
                    getVideoThumbnail(scope, uri) { bitmap: Bitmap? ->
                        pickedImage = bitmap?.asImageBitmap()
                    }
                }

                FileType.AUDIO -> {
                    imageResource = R.drawable.music
                }

                FileType.RegularFile -> {
                    imageResource = R.drawable.file
                }
            }
            if (itemName.isNullOrBlank()) {
                itemName = getFileNameFromUri(uri) ?:""
            }
            selectedUri = uri
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
            .wrapContentHeight()
            .clip(RoundedCornerShape(4.dp))
            .background(LocalAppPalette.current.dialogBg),
        onDismissRequest = {},
        properties = DialogProperties(usePlatformDefaultWidth = false),
        onNegative = onDismiss,
        onPositive = { saveItem(context) }
    ) { _, actionButton ->
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            val currentPickedImage = pickedImage
            if (currentPickedImage == null) {
                Spacer(Modifier.height(12.dp))
                Image(
                    painter = painterResource(imageResource),
                    contentDescription = "上传照片",
                    Modifier
                        .clickable {
                            pickImage()
                        }
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    contentScale = ContentScale.Fit
                )
            } else {
                Image(
                    bitmap = currentPickedImage,
                    contentDescription = "上传照片",
                    Modifier
                        .clickable {
                            pickImage()
                        }
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    contentScale = ContentScale.Fit
                )
            }


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

            Spacer(Modifier.height(4.dp))
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
                    .padding(horizontal = 6.dp)
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
                    .padding(horizontal = 6.dp)
                    .align(Alignment.CenterHorizontally),
                update = {
                    it.setRankType(itemRank)
                })

            Spacer(Modifier.height(4.dp))
            EditLabelView {
                itemLabel.add(0, LabelInfo(label = it))
                itemLabelSize = itemLabel.size
            }
            LazyRow(Modifier.padding(horizontal = 6.dp)) {
                items(itemLabelSize, key = { index: Int ->
                    itemLabel[index].label
                }) { index: Int ->
                    LabelView(itemLabel[index]) {
                        itemLabel.removeAt(index)
                        itemLabelSize = itemLabel.size
                    }
                }
            }
        }
        Column(
            Modifier
                .padding(horizontal = 6.dp)
                .wrapContentHeight()
                .fillMaxWidth()) {
            actionButton()
        }
    }
}

@Composable
fun CheckableLabelView(label: String, isChecked:Boolean, onCheckStateChange: (Boolean) -> Unit) {
    Box(
        Modifier
            .padding(2.dp)
            .background(
                color = if (isChecked) LocalAppPalette.current.labelChecked else LocalAppPalette.current.labelUnChecked,
                shape = RoundedCornerShape(4.dp)
            )
            .wrapContentSize()
            .clickable {
                onCheckStateChange(!isChecked)
            }, contentAlignment = Alignment.Center) {
        AppThemeText(label,
            Modifier
                .padding(horizontal = 4.dp, vertical = 2.dp)
                .wrapContentSize(), style = LocalTextStyle.current.copy(fontSize = 14.sp))
    }
}

@Composable
fun LabelView(label: LabelInfo, editable: Boolean = true, onRemove: ()->Unit) {
    if (editable) {
        Row(
            Modifier
                .padding(2.dp)
                .background(
                    color = LocalAppPalette.current.labelUnChecked, shape = RoundedCornerShape(4.dp)
                )
                .padding(horizontal = 4.dp, vertical = 2.dp)
                .wrapContentSize(), verticalAlignment = Alignment.CenterVertically) {
            AppThemeText(label.label,
                Modifier
                    .wrapContentSize(), style = LocalTextStyle.current.copy(fontSize = 16.sp, color = LocalAppPalette.current.labelTextColor))
            Spacer(Modifier.padding(2.dp))
            Image(
                painter = painterResource(R.drawable.close),
                contentDescription = "删除标签",
                Modifier
                    .size(16.dp)
                    .clickable { onRemove() },
            )
        }
    } else {
        Box(
            Modifier
                .padding(2.dp)
                .background(
                    color = LocalAppPalette.current.labelUnChecked, shape = RoundedCornerShape(4.dp)
                )
                .wrapContentSize(), contentAlignment = Alignment.Center) {
            AppThemeText(label.label,
                Modifier
                    .padding(horizontal = 4.dp, vertical = 2.dp)
                    .wrapContentSize(), style = LocalTextStyle.current.copy(fontSize = 14.sp, color = LocalAppPalette.current.labelTextColor))
        }
    }
}

@Composable
fun EditLabelView(onAddLabel: (String)->Unit) {
    var labelText by remember { mutableStateOf("") }
    Row(
        Modifier
            .wrapContentSize()
            .padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        CompositionLocalProvider(LocalTextStyle provides LocalTextStyle.current.copy(fontSize = 24.sp)) {
            AppThemeBasicTextField(labelText, {value-> labelText = value},
                Modifier
                    .wrapContentWidth()
                    .wrapContentHeight(), hint = "添加标签")
        }
        Spacer(Modifier.padding(2.dp))
        Icon(
            painter = painterResource(R.drawable.add),
            contentDescription = "添加标签",
            Modifier
                .size(24.dp)
                .clickable {
                    if (labelText.isNotBlank()) {
                        onAddLabel(labelText)
                        labelText = ""
                    }
                },
        )
    }
}

@Composable
fun DrawerHeader() {
    var userName by remember { mutableStateOf(DEFAULT_USER_NAME) }
    var userDesc by remember { mutableStateOf(DEFAULT_DESC) }
    var userAvatarImage by remember { mutableStateOf<ImageBitmap?>(null) }

    val avatarSize = DrawerDefaults.MaximumDrawerWidth * (1 - goldenRatio)
    val borderSize = avatarSize / 25f
    val iconSize = avatarSize / 8f

    val scope = rememberCoroutineScope()
    var editUserInfoJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(Unit) {
        if (true) {
            withContext(Dispatchers.IO) {
                val innerUserInfo = UserStorage.userInstance
                val fileInfo = innerUserInfo.userAvatar
                withContext(Dispatchers.Main) {
                    userName = innerUserInfo.userName
                    userDesc = innerUserInfo.userDesc
                }
                val (_, image) = BitMapCachePool.loadImage(
                    fileInfo,
                    avatarSize.dpValue.toInt(),
                    avatarSize.dpValue.toInt()
                )
                withContext(Dispatchers.Main) {
                    userAvatarImage = image?.asImageBitmap()
                }
            }
        }
    }

    val multipleImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri: Uri? ->
            uri ?: return@rememberLauncherForActivityResult
            scope.launch(Dispatchers.IO) {
                val imageStorage = FileStorageUtils.getStorage(FileType.IMAGE) ?: return@launch
                val currentUser = UserStorage.userInstance
                val currentImageId =
                    currentUser.userAvatar.storageId.takeIf { it != DataBase.INVALID_ID }
                val nextId = if (currentImageId != null) {
                    BitMapCachePool.invalid(currentImageId, FileType.IMAGE)
                    imageStorage.asyncStore(currentImageId, uri)
                } else {
                    imageStorage.asyncStore(uri)
                }
                currentUser.userAvatar.storageId = nextId
                currentUser.userAvatar.fileType = FileType.IMAGE
                currentUser.userAvatar.uri = uri
                UserStorage.userInstance = currentUser
                val (_, image) = BitMapCachePool.loadImage(
                    currentUser.userAvatar,
                    avatarSize.dpValue.toInt(),
                    avatarSize.dpValue.toInt()
                )
                withContext(Dispatchers.Main) {
                    userAvatarImage = image?.asImageBitmap()
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
        if (enableEdit) {
            multipleImagePickerLauncher.launch(
                PickVisualMediaRequest
                    .Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    .build()
            )
        }
    }

    fun circleOffset(R: Dp, r: Dp, halfBorder: Dp) : Dp {
        return R * (1 - sqrt(.5f)) - r + halfBorder * sqrt(0.5f)
    }

    Column(
        Modifier.fillMaxWidth()
    ) {
        val currentUserAvatarImage = userAvatarImage

        val imageModifier = Modifier
            .size(avatarSize, avatarSize)
            .clip(CircleShape)
            .border(borderSize, LocalAppPalette.current.strokeColor, CircleShape)
            .clickable {
                pickImage()
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
                                    pickImage()
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
        text = "添加相册",
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
                AppThemeText("相册名称")
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
            .drawerBackground()
            .windowInsetsPadding(WindowInsets.systemBars)
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
//        DrawerFunctionView(onClick = { enableEdit = !enableEdit }, R.drawable.edit, "编辑合集")
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
                            .size(size)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(size))
                            .padding(4.dp),
                    )
                }
            }) {
            AppThemeText(
                text = albumName,
                style = LocalTextStyle.current.copy(fontSize = 16.sp),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LocalAppPalette.current.drawerBg)
                    .fillMaxHeight()
                    .padding(8.dp),
            )
        }
    }

    if (deleteConfirmDialogState) {
        AppThemeConfirmDialog("确认删除相册: ${item.albumName}", properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false), onNegative = {
            deleteConfirmDialogState = false
        }, onPositive = {
            deleteConfirmDialogState = false
            viewModel.deleteAlbum(item)
        }, onDismissRequest = {
            deleteConfirmDialogState = false
        }, content = {})
    }
}