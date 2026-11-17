package com.jingtian.composedemo.main.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jingtian.composedemo.BuildKonfig
import com.jingtian.composedemo.base.AppThemeConfirmDialog
import com.jingtian.composedemo.base.AppThemeText
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
import com.jingtian.composedemo.navigation.rememberDocumentTreePicker
import com.jingtian.composedemo.ui.theme.LocalAppColorScheme
import com.jingtian.composedemo.ui.theme.LocalAppPalette
import com.jingtian.composedemo.ui.theme.LocalAppUIConstants
import com.jingtian.composedemo.ui.theme.LocalMiddleButtonConfig
import com.jingtian.composedemo.ui.theme.appBackground
import com.jingtian.composedemo.utils.dpValue
import com.jingtian.composedemo.viewmodels.AlbumViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.jingtian.composedemo.base.resources.getPainter
import com.jingtian.composedemo.demonstration.gradient.HorizontalShadowWrapper
import com.jingtian.composedemo.main.dialog.ImportDocumentDialog
import com.jingtian.composedemo.main.dialog.rememberDocumentDialogState
import com.jingtian.composedemo.main.widget.SimpleNavHostController
import com.jingtian.composedemo.multiplatform.LowMemorySideEffect
import com.jingtian.composedemo.multiplatform.SoftRef
import com.jingtian.composedemo.multiplatform.logD
import com.jingtian.composedemo.ui.theme.clipDeviceRoundCorner
import com.jingtian.composedemo.ui.theme.goldenRatio
import com.jingtian.composedemo.utils.getOrPutSoftRef
import com.jingtian.composedemo.viewmodels.albumViewModel
import com.jingtian.composedemo.viewmodels.appThemeViewModel
import com.jingtian.composedemo.viewmodels.observeAsState
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.IO
import kotlin.math.pow

@Composable
fun Gallery(
    galleryStateHolderMap: SnapshotStateMap<Long, SoftRef<GalleryStateHolder>>,
    album: IndexedValue<Album>?,
    albumList: List<Album>,
    drawerState: DrawerState,
    naviController: SimpleNavHostController
) {
    if (album == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .appBackground()
        )
        return
    }
    val currentSelectedAlbumState = remember(album.value) { mutableStateOf(album.value) }
    val albumId: Long = album.value.albumId ?: return
    val scrollAreaWidth = 28.dp
    val galleryDpConstants = GalleryDpConstants(
        scrollBarWidthDp = 6.dp.dpValue(),
        scrollBarHeightDp = 64.dp.dpValue(),
        scrollAreaWidthDp = scrollAreaWidth.dpValue()
    )
    val viewModel: AlbumViewModel = albumViewModel()

    val galleryStateHolder = remember(album, albumList) {
        galleryStateHolderMap.getOrPutSoftRef(albumId) {
            GalleryStateHolder(
                album,
                currentSelectedAlbumState,
                albumList,
                drawerState,
                viewModel,
                galleryDpConstants,
                naviController
            )
        }
    }
    galleryStateHolder.Gallery()
}

@Composable
fun Modifier.predictiveBackVisual(
    predictiveBackVisualState: State<Float>
): Modifier {
    val lockIcon = getPainter(DrawableIcon.DrawableLock)
    return this.clipDeviceRoundCorner()
        .drawWithContent {
            val imageSize = Size(size.minDimension, size.minDimension) * goldenRatio
            val canvasSize = size
            drawContent()
            drawIntoCanvas {
                inset(
                    horizontal = (canvasSize.width - imageSize.width) / 2,
                    vertical = (canvasSize.height - imageSize.height) / 2
                ) {
                    with(lockIcon) {
                        draw(size = imageSize, alpha = predictiveBackVisualState.value)
                    }
                }
            }
        }
        .blur((predictiveBackVisualState.value * 40).dp)
}

fun ImageBitmap.sampleAverage(rect: IntRect): Color {
    var sumR = 0L
    var sumG = 0L
    var sumB = 0L
    val count = rect.width * rect.height
    val buffer = IntArray(count)
    this.readPixels(
        buffer = buffer,
        startX = rect.left,
        startY = rect.top,
        width = rect.width,
        height = rect.height,
        bufferOffset = 0,
        stride = rect.width
    )
    for (pixel in buffer) {
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF
        sumR += r
        sumG += g
        sumB += b
    }
    if (count == 0) return Color.Transparent
    val avgR = sumR / count.toFloat() / 255f
    val avgG = sumG / count.toFloat() / 255f
    val avgB = sumB / count.toFloat() / 255f
    return Color(avgR, avgG, avgB)
}

fun Color.relativeLuminance(): Float {
    fun convert(v: Float): Float = if (v <= 0.03928f) v / 12.92f else (v + 0.055f)/1.055f.pow(2.4f)
    return 0.2126f * convert(red) + 0.7152f * convert(green) + 0.0722f * convert(blue)
}

fun Color.adaptiveTextColor() = when {
    relativeLuminance() > 0.55f -> Color.Black
    relativeLuminance() < 0.45f -> Color.White
    else -> null
}

@Composable
fun Modifier.ovalGlass(hazeState: HazeState, color: Color? = null): Modifier {
    return this.clip(RoundedCornerShape(100))
        .hazeEffect(state = hazeState) {
            progressive = HazeProgressive.verticalGradient(
                startIntensity = 0.7f, endIntensity = 0.7f
            )
        }
        .background(
            color = color ?: LocalAppPalette.current.drawerBg.copy(alpha = 0.4f),
        )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryStateHolder.Gallery() {
    val albumItemDataChange by viewModel.albumItemListChange.observeAsState()
    val scope = rememberCoroutineScope()
    val graphicsLayer = rememberGraphicsLayer()
    val albumNameChange by viewModel.albumNameChange.observeAsState()
//    LaunchedEffect(album) {
//        initAlbumInfo()
//    }
    val density = LocalDensity.current
    val bottomBarHeight = 76.dp

    LaunchedEffect(albumItemDataChange) {
        onAlbumDataChanged()
    }

    LowMemorySideEffect(this) {
        this.albumViewMap.clear()
    }

    LaunchedEffect(albumNameChange) {
        onAlbumNameChanged()
    }

    val importDirLauncher = rememberDocumentTreePicker { uri ->
        uri ?: return@rememberDocumentTreePicker
        viewModel.importFiles(album.value, uri)
    }

    val filterChanged by viewModel.filterCheckChanged.observeAsState()
    LaunchedEffect(
        filterChanged,
        itemList,
        gallerySearchWord,
        gallerySearchEnabled,
        filterConfig.value.labelOr.value
    ) {
        updateFilterList()
        withFrameMillis {
        }
        if (gallerySearchEnabled) {
            scrollOffsetY = 0f
            searchGalleryScrollState.scrollToItem(0, 0)
        }
    }

    val hazeState = rememberHazeState()
    Box(
        Modifier
            .fillMaxSize()
    ) {

        fun getScrollOffset(): Float {
            return galleryScrollState.layoutInfo.visibleItemsInfo.map { it.index - it.offset.y.toFloat() / it.size.height }
                .average().toFloat()
        }
        val naviBarInsets = WindowInsets.navigationBars.asPaddingValues()
        val shadesHeight = 0.dp
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Adaptive((size + galleryItemPadding * 2)),
            Modifier
                .fillMaxSize()
                .hazeSource(state = hazeState, zIndex = 2f)
                .drawWithContent {
                    graphicsLayer.record {
                        this@drawWithContent.drawContent()
                    }
                    drawLayer(graphicsLayer)
                }
                .appBackground()
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
            contentPadding = PaddingValues(
                start = naviBarInsets.calculateStartPadding(LocalLayoutDirection.current),
                top = headerHeight.dp + naviBarInsets.calculateTopPadding(),
                end = naviBarInsets.calculateEndPadding(LocalLayoutDirection.current),
                bottom = naviBarInsets.calculateBottomPadding() + bottomBarHeight
            ),
            state = galleryScrollState,
        ) {
            items(
                filteredItemList.size,
                key = { index ->
                    filteredItemList[index].let {
                        Triple(
                            it.albumItem.itemId ?: DataBase.INVALID_ID,
                            it.albumItem.fileId,
                            it.fileInfo.storageId
                        ) to it.hashCode()
                    }
                },
                contentType = { index -> filteredItemList[index].fileInfo.fileType.value }) { index: Int ->
                AlbumItemView(
                    albumViewMap,
                    filteredItemList[index],
                    size,
                    galleryItemPadding,
                    currentSelectedItem,
                    enterEditModeState,
                    itemSelectStateChangeState,
                    showEditDialogStateOnLongClickState
                )
            }
        }

        Box(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter).height(headerHeight.dp).hazeEffect(state = hazeState) {
                progressive = HazeProgressive.verticalGradient(
                    startIntensity = 1f, endIntensity = 0f
                )
            }
        )
        Box(modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).height(bottomBarHeight).hazeEffect(state = hazeState) {
                progressive = HazeProgressive.verticalGradient(
                    startIntensity = 0f, endIntensity = 1f
                )
            }
        )
        val scrollBarColor = LocalAppPalette.current.labelTextColor.copy(alpha = 0.85f)
        val dp16 = 16.dp.dpValue()
        val dp6 = 6.dp.dpValue()
        val scrollAreaWidthDp = scrollAreaWidth.dpValue()
        Box(
            Modifier
                .width(scrollAreaWidth)
                .fillMaxHeight()
                .padding(top = headerHeight.dp, bottom = naviBarInsets.calculateBottomPadding())
                .align(Alignment.TopEnd)
                .pointerInput(this@Gallery) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            scrollOffsetY = offset.y
                            scrollBarSize[0] = dp16
                            scrollBarOffset[0] = scrollAreaWidthDp - scrollBarSize[0]
                            updateScrollOffset()
                            scope.launch {
                                updateScrollItem()
                            }
                        },
                        onDrag = { pointerInputChange, offset ->
                            scrollOffsetY += offset.y
                            scrollBarSize[0] = dp16
                            scrollBarOffset[0] = scrollAreaWidthDp - scrollBarSize[0]
                            updateScrollOffset()
                            scope.launch {
                                updateScrollItem()
                            }
                        },
                        onDragEnd = {
                            scrollBarSize[0] = dp6
                            scrollBarOffset[0] = scrollAreaWidthDp - scrollBarSize[0]
                        },
                        onDragCancel = {
                            scrollBarSize[0] = dp6
                            scrollBarOffset[0] = scrollAreaWidthDp - scrollBarSize[0]
                        }
                    )
                }
                .pointerInput(this@Gallery) {
                    awaitEachGesture {
                        val downEvent = awaitFirstDown()
                        scrollOffsetY = downEvent.position.y
                        scope.launch {
                            withContext(Dispatchers.Main) {
                                scrollBarSize[0] = dp16
                                scrollBarOffset[0] = scrollAreaWidthDp - scrollBarSize[0]
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
                                scrollBarSize[0] = dp6
                                scrollBarOffset[0] = scrollAreaWidthDp - scrollBarSize[0]
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
        Column(modifier = Modifier.fillMaxWidth()
            .onSizeChanged { with(density) { headerHeight = it.height.toDp().value } }
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(
                        start = galleryItemPadding * 2,
                        end = galleryItemPadding * 2,
                        bottom = galleryItemPadding * 2
                    )
                    .wrapContentHeight()
            ) {
                if (!gallerySearchEnabled) {
                    Icon(
                        painter = getPainter(DrawableIcon.DrawableDrawer),
                        contentDescription = "打开drawer",
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .ovalGlass(hazeState)
                            .size(LocalAppUIConstants.current.filterLabelHeight)
                            .padding(4.dp)
                            .clickable {
                                scope.launch {
                                    if (drawerState.isOpen) {
                                        drawerState.close()
                                    } else {
                                        drawerState.open()
                                    }
                                }
                            }
                    )
                    Spacer(Modifier.width(12.dp))
                    Box(modifier = Modifier.fillMaxWidth().align(Alignment.CenterVertically).weight(1f)) {
                        AppThemeText(
                            albumName.trim(),
                            Modifier
                                .ovalGlass(hazeState)
                                .padding(horizontal = 6.dp)
                                .height(LocalAppUIConstants.current.filterLabelHeight)
                                .widthIn(min = LocalAppUIConstants.current.filterLabelHeight)
                                .padding(4.dp),
                            style = LocalTextStyle.current.copy(
                                fontSize = 20.sp,
                                fontWeight = FontWeight(600)
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                }
                if (gallerySearchEnabled) {
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.CenterVertically)
                        .padding(horizontal = 4.dp)
                        .ovalGlass(hazeState)
                        .weight(1f)
                        .height(height = LocalAppUIConstants.current.filterLabelHeight),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        BasicTextField(
                            value = gallerySearchWord,
                            onValueChange = {
                                scope.launch {
                                    updateScrollOffset()
                                    updateGallerySearchWord(it)
                                }
                            },
                            modifier = Modifier
                                .padding(4.dp)
                                .padding(start = 12.dp, end = 6.dp),
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 16.sp,
                                fontWeight = FontWeight(400),
                                color = LocalTextStyle.current.color,
                            ),
                            cursorBrush = SolidColor(LocalTextStyle.current.color)
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                }
                Icon(
                    painter = getPainter(DrawableIcon.DrawableSearch),
                    contentDescription = "搜索",
                    modifier = Modifier
                        .clickable {
                            gallerySearchEnabled = !gallerySearchEnabled
                            scope.launch {
                                if (gallerySearchEnabled) {
                                    scrollOffsetY = 0f
                                    searchGalleryScrollState.scrollToItem(0, 0)
                                }
                                updateScrollOffset()
                                if (!gallerySearchEnabled) {
                                    updateGallerySearchWord("")
                                }
                            }
                        }
                        .ovalGlass(hazeState)
                        .size(LocalAppUIConstants.current.filterLabelHeight)
                        .padding(4.dp)
                        .align(Alignment.CenterVertically)
                )
                Spacer(Modifier.width(6.dp))
                Icon(
                    painter = getPainter(DrawableIcon.DrawableEditNormal),
                    contentDescription = "编辑模式",
                    modifier = Modifier
                        .size(LocalAppUIConstants.current.filterLabelHeight)
                        .clickable { enterEditMode = !enterEditMode }
                        .ovalGlass(hazeState)
                        .size(LocalAppUIConstants.current.filterLabelHeight)
                        .padding(4.dp)
                        .align(Alignment.CenterVertically)
                )
            }
            if (enterEditMode) {
                Row(
                    Modifier
                        .fillMaxWidth()
                ) {
                    HorizontalShadowWrapper(
                        leftShadow = galleryItemPadding * 2,
                        rightShadow = galleryItemPadding * 4,
                        leftShadowContent = {},
                        rightShadowContent = {
                            Icon(
                                painter = getPainter(DrawableIcon.DrawableDown),
                                contentDescription = "过滤器",
                                modifier = Modifier
                                    .size(LocalAppUIConstants.current.filterLabelHeight)
                                    .clickable { showLabelFilter = !showLabelFilter }
                                    .ovalGlass(hazeState)
                                    .size(LocalAppUIConstants.current.filterLabelHeight)
                                    .padding(4.dp)
                            )
                            Spacer(modifier = Modifier.width(galleryItemPadding * 2))
                        }
                    ) {
                        CompositionLocalProvider(
                            LocalAppUIConstants provides LocalAppUIConstants.current.copy(
                                filterLabelPaddings = listOf(0f.dp, 0f.dp, 0f.dp, 0f.dp)
                            )
                        ) {
                            LazyRow(
                                Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight()
                                    .padding(bottom = galleryItemPadding * 2)
                                    .weight(1f),
                                contentPadding = PaddingValues(horizontal = galleryItemPadding * 2),
                                horizontalArrangement = Arrangement.spacedBy(galleryItemPadding * 2)
                            ) {
                                roundRectTabFilter(hazeState, labelFilterCheckedInfo, totalLabelList)
                                roundRectTabFilter(hazeState, fileTypeCheckState, totalFileTypeList)
                                roundRectTabFilter(hazeState, itemRankTypeCheckState, totalItemRankList)
                            }
                        }
                    }
                }
            }
        }
        if (enterEditMode) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .height(bottomBarHeight)
                    .align(Alignment.BottomCenter)
            ) {
                Row(
                    Modifier
                        .wrapContentSize()
                        .padding(start = galleryItemPadding * 2)
                        .ovalGlass(hazeState)
                        .padding(12.dp)
                        .align(Alignment.CenterStart)
                ) {
                    if (selectAll) {
                        GalleryFunctionView(SELECT_ALL) {
                            scope.launch(Dispatchers.Default) {
                                val map = HashMap<Long, AlbumItemRelation>().apply {
                                    putAll(filteredItemList.map {
                                        (it.albumItem.itemId ?: DataBase.INVALID_ID) to it
                                    })
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
                        .ovalGlass(hazeState)
                        .padding(12.dp)
                        .align(Alignment.Center)
                ) {
                    for (func in currentFunctions) {
                        when (func) {
                            ADD -> {
                                GalleryFunctionView(func) {
                                    addImageDialogState = true
                                }
                            }

                            IMPORT -> {
                                GalleryFunctionView(func) {
                                    if (BuildKonfig.isRemote) {
                                        importDocumentTreeDialog = true
                                    } else {
                                        importDirLauncher.launch(null)
                                    }
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

                            GalleryFunctions.IMPORT_CIFS -> {
                                ImportRemoteFunctionView(this@Gallery, func) {}
                            }

                            GalleryFunctions.DELETE_CIFS_FILE -> {
                                DeleteRemoteFileFunctionView(this@Gallery, func) {}
                            }

                            else -> {}
                        }
                    }
                    for (func in platformFunctions) {
                        PlatformGalleryFunctionView(this@Gallery, func) { }
                    }
                }

                Row(
                    Modifier
                        .wrapContentSize()
                        .padding(end = galleryItemPadding * 2)
                        .ovalGlass(hazeState)
                        .padding(12.dp)
                        .align(Alignment.CenterEnd)
                ) {
                    GalleryFunctionView(EXIT) {
                        enterEditMode = false
                    }
                }
            }
        }
    }

    LaunchedEffect(currentSelectedItem, enterEditMode, itemSelectStateChange) {
        currentFunctions.clear()
        platformFunctions.clear()
        if (currentSelectedItem.size == filteredItemList.size) {
            selectNone = true
            selectAll = false
        } else {
            selectAll = true
            selectNone = false
        }
        if (enterEditMode) {
            val selectedCount = currentSelectedItem.size
            when (selectedCount) {
                0 -> {
                    currentFunctions.addAll(GalleryFunctions.albumFunctions)
                    platformFunctions.addAll(platformExtraAlbumFunctions(selectedCount))
                }

                1 -> {
                    currentFunctions.addAll(GalleryFunctions.itemFunctions)
                    platformFunctions.addAll(platformExtraAlbumFunctions(selectedCount))
                }

                else -> {
                    currentFunctions.addAll(GalleryFunctions.batchFunctions)
                    platformFunctions.addAll(platformExtraAlbumFunctions(selectedCount))
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

    if (importDocumentTreeDialog) {
        ImportDocumentDialog(rememberDocumentDialogState(album.value)) {
            importDocumentTreeDialog = false
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
                colors = LocalMiddleButtonConfig.current.colors.copy(
                    containerColor = LocalAppPalette.current.deleteButtonColor,
                    contentColor = Color.White
                ),
            )
        ) {
            AppThemeConfirmDialog(
                "确认删除$title",
                reversed = true,
                onPositive = null,
                onMiddleClick = {
                    viewModel.deleteItems(currentSelectedItem.values)
                    showConfirmDeleteDialog = false
                },
                onNegative = {
                    showConfirmDeleteDialog = false
                },
                onDismissRequest = {
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
        currentSelectedItem.keys.firstOrNull()?.let { firstKey ->
            (currentSelectedItem[firstKey])?.let { albumItemRelation ->
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
            filterConfig,
        ) {
            showLabelFilter = false
        }
    }
}
