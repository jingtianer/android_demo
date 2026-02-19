package com.jingtian.composedemo

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyHorizontalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DrawerState
import androidx.compose.runtime.Composable
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toFile
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jingtian.composedemo.base.AppThemeBasicTextField
import com.jingtian.composedemo.base.AppThemeClickEditableText
import com.jingtian.composedemo.base.AppThemeText
import com.jingtian.composedemo.base.AppThemeTextField
import com.jingtian.composedemo.base.BaseActivity
import com.jingtian.composedemo.dao.DataBase
import com.jingtian.composedemo.dao.model.Album
import com.jingtian.composedemo.dao.model.AlbumItem
import com.jingtian.composedemo.dao.model.DEFAULT_DESC
import com.jingtian.composedemo.dao.model.DEFAULT_USER_NAME
import com.jingtian.composedemo.dao.model.FileInfo
import com.jingtian.composedemo.dao.model.FileType
import com.jingtian.composedemo.dao.model.ItemRank
import com.jingtian.composedemo.dao.model.LabelInfo
import com.jingtian.composedemo.dao.model.relation.AlbumItemRelation
import com.jingtian.composedemo.ui.theme.LocalAppPalette
import com.jingtian.composedemo.ui.theme.LocalAppUIConstants
import com.jingtian.composedemo.ui.widget.RankTypeChooser
import com.jingtian.composedemo.ui.widget.RankTypeChooser.Companion.createBg
import com.jingtian.composedemo.ui.widget.StarRateView
import com.jingtian.composedemo.ui.widget.StarRateView.Companion.OnScoreChange
import com.jingtian.composedemo.utils.BitMapCachePool
import com.jingtian.composedemo.utils.CoroutineUtils
import com.jingtian.composedemo.utils.FileStorageUtils
import com.jingtian.composedemo.utils.FileStorageUtils.getMediaType
import com.jingtian.composedemo.utils.FileStorageUtils.getVideoThumbnail
import com.jingtian.composedemo.utils.FileStorageUtils.safeToFile
import com.jingtian.composedemo.utils.UserStorage
import com.jingtian.composedemo.utils.ViewUtils.commonConfig
import com.jingtian.composedemo.utils.ViewUtils.commonEditableConfig
import com.jingtian.composedemo.utils.ViewUtils.dpValue
import com.jingtian.composedemo.utils.composeObserve
import com.jingtian.composedemo.viewmodels.AlbumViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : BaseActivity() {
    @Composable
    override fun content() = Main()
}

@Preview
@Composable
fun Main() {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val viewModel: AlbumViewModel = viewModel()
    var menuItemsEntity by remember { mutableStateOf(emptyList<Album>()) }
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
                    if (value.isEmpty()) {
                        drawerState.snapTo(DrawerValue.Open)
                    }
                }
            }
        }
    }


    ModalNavigationDrawer(
        {
            MainDrawer(drawerState, menuItemsEntity) { index, album ->
                currentSelectedAlbum = IndexedValue(index, album)
            }
        },
        Modifier.fillMaxSize(),
        drawerState = drawerState,
        gesturesEnabled = menuItemsEntity.isNotEmpty()
    ) {
        Gallery(currentSelectedAlbum)
    }
}

@Composable
fun LabelFilter(showFilter: Boolean , album: Album, onCheckStateChange: (List<String>)->Unit) {
    class LabelCheckInfo(val label: String, var isChecked: Boolean = false)
    val viewModel: AlbumViewModel = viewModel()
    var labelList by remember { mutableStateOf<List<LabelCheckInfo>>(emptyList()) }
    LaunchedEffect(album) {
        withContext(Dispatchers.IO) {
            viewModel.getLabelList(album).collect { value->
                labelList = value.map { LabelCheckInfo(it, false) }
            }
        }
    }

    if (!showFilter) {
        return
    }

    val size = 36.dp
    val padding = 4.dp
    fun notifyCheckChanged() {
        onCheckStateChange(labelList.mapNotNull { checkInfo -> checkInfo.label.takeIf { checkInfo.isChecked } })
    }
    LazyHorizontalGrid(rows = GridCells.Fixed(2), Modifier.height((size + padding * 2) * 2).fillMaxWidth()) {
        items(labelList.size) { index ->
            val item = labelList[index]
            var checked by remember { mutableStateOf(item.isChecked) }
            Row(Modifier.height(size).fillMaxWidth().padding(padding), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked, onCheckedChange = { value: Boolean->
                    checked = value
                }, modifier = Modifier.size(size))
                AppThemeText(text = item.label,
                    Modifier
                        .wrapContentWidth()
                        .height(size)
                        .clickable {
                            checked = !checked
                        })
            }
            LaunchedEffect(checked) {
                item.isChecked = checked
                notifyCheckChanged()
            }
        }
    }
}

@Composable
fun Gallery(album: IndexedValue<Album>?) {
    if (album == null) {
        return
    }

    var addImageDialogState by remember { mutableStateOf(false) }
    var itemList by remember { mutableStateOf(emptyList<AlbumItemRelation>()) }
    var filteredItemList by remember { mutableStateOf(emptyList<AlbumItemRelation>()) }
    val viewModel: AlbumViewModel = viewModel()
    var filterLabels by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showLabelFilter by remember { mutableStateOf(false) }
    val coroutine = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            viewModel.getAllAlbumItem(album.value).collect {
                withContext(Dispatchers.Main) {
                    itemList = it
                }
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .wrapContentHeight()
        ) {
            AppThemeText(album.value.albumName, Modifier.align(Alignment.CenterStart))

            Row(
                Modifier
                    .align(Alignment.CenterEnd)
                    .wrapContentSize()) {
                Image(
                    painter = painterResource(R.drawable.add),
                    contentDescription = "添加图片",
                    modifier = Modifier
                        .size(32.dp)
                        .clickable { addImageDialogState = true }
                )
                Image(
                    painter = painterResource(R.drawable.filter),
                    contentDescription = "过滤器",
                    modifier = Modifier
                        .size(32.dp)
                        .clickable { showLabelFilter = !showLabelFilter }
                )
            }
        }
        LabelFilter(showLabelFilter, album.value) { checkInfo->
            val targetLabelSet = checkInfo.toSet()
            coroutine.launch(Dispatchers.Default) {
                val insersectList = itemList.filter { it.labelInfos.map { it.label }.toSet().intersect(targetLabelSet).isNotEmpty() }
                withContext(Dispatchers.Main) {
                    filterLabels = targetLabelSet
                    filteredItemList = insersectList
                }
            }
        }
        val size = 160.dp
        val padding = 4.dp
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Adaptive((size + padding*2)),
            Modifier
                .fillMaxSize()
                .weight(1f)) {
            val finalItemList = if (filterLabels.isEmpty()) {
                itemList
            } else {
                filteredItemList
            }
            items(
                finalItemList.size,
                key = { index: Int ->
                    finalItemList[index].albumItem.itemId ?: DataBase.INVALID_ID
                }) { index: Int ->
                AlbumItemView(finalItemList[index], size, padding)
            }
        }
    }

    if (addImageDialogState) {
        AddImageDialog(album.value) {
            addImageDialogState = false
        }
    }
}

@Composable
fun AlbumItemView(albumItemRelation: AlbumItemRelation, size: Dp, padding: Dp) {
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    var itemName by remember { mutableStateOf(albumItemRelation.albumItem.itemName) }
    var itemDesc by remember { mutableStateOf(albumItemRelation.albumItem.desc) }
    var itemRank by remember { mutableStateOf(albumItemRelation.albumItem.rank) }
    var itemScore by remember { mutableStateOf(albumItemRelation.albumItem.score) }
    val itemLabel by remember { mutableStateOf(albumItemRelation.labelInfos.toMutableList()) }
    var itemLabelSize by remember { mutableStateOf(albumItemRelation.labelInfos.size) }

    val scope = rememberCoroutineScope()
    var imageResource by remember { mutableStateOf(R.drawable.load_failed) }
    suspend fun fetchImage() {
        withContext(Dispatchers.IO) {
            val uri = albumItemRelation.fileInfo?.getFileUri() ?: return@withContext
            val fileType = albumItemRelation.fileInfo.fileType
            when(fileType) {
                FileType.IMAGE -> {
                    val (_, bitmap) = BitMapCachePool.loadImage(
                        albumItemRelation.fileInfo,
                        size.dpValue.toInt(),
                        size.dpValue.toInt()
                    )
                    withContext(Dispatchers.Main) {
                        imageBitmap = bitmap?.asImageBitmap()
                    }
                }
                FileType.VIDEO -> {
                    getVideoThumbnail(scope, uri) { bitmap->
                        imageBitmap = bitmap?.asImageBitmap()
                    }
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
        val originFileUri = fileInfo.getFileUri()?.safeToFile() ?: return@remember null
        val mediaUri: Uri =
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                originFileUri
            )
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

    Column(Modifier.width(size).padding(padding)) {
        val currentPickedImage = imageBitmap
        Box {
            if (currentPickedImage == null) {
                Image(
                    painter = painterResource(imageResource),
                    contentDescription = "上传照片",
                    Modifier
                        .clickable { scope.launch(Dispatchers.IO) { fetchImage() } }
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .align(Alignment.Center),
                    contentScale = ContentScale.Fit
                )
            } else {
                Image(
                    bitmap = currentPickedImage,
                    contentDescription = "上传照片",
                    Modifier
                        .clickable {
                            context.startActivity(playIntent)
                        }
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .align(Alignment.Center),
                    contentScale = ContentScale.Fit
                )
            }

            AndroidView({ context ->
                View(context).apply {
                    val bg = createBg(itemRank)
                    background = bg
                    layoutParams = ViewGroup.LayoutParams(bg.getWidth().toInt(), bg.getHeight().toInt())
                }
            },
                Modifier
                    .wrapContentSize()
                    .align(Alignment.TopEnd),
                update = {
                    val bg = createBg(itemRank)
                    it.background = bg
                    it.layoutParams = ViewGroup.LayoutParams(bg.getWidth().toInt(), bg.getHeight().toInt())
                })
        }

        OutlinedTextField(itemName, { value->
            itemName = value
        }, modifier = Modifier.fillMaxWidth(), label = {
            AppThemeText("文件名称")
        }, maxLines = Int.MAX_VALUE, enabled = false)

        OutlinedTextField(itemDesc, { value->
            itemDesc = value
        }, modifier = Modifier.fillMaxWidth(), label = {
            AppThemeText("文件描述")
        }, maxLines = Int.MAX_VALUE, enabled = false)

        AndroidView({ context ->
            StarRateView(context).commonConfig().apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
            Modifier
                .fillMaxWidth()
                .height(30.dp),
            update = {
                it.setScore(itemScore)
            })

        LazyRow {
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

@Composable
fun AddImageDialog(album: Album, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = { onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .fillMaxWidth(LocalAppUIConstants.current.dialogaxPercent)
                .verticalScroll(rememberScrollState())
                .background(LocalAppPalette.current.dialogBg)
                .padding(12.dp)
                .clip(RectangleShape)
                .wrapContentHeight()
        ) {
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

            fun saveItem() {
                val albumId = album.albumId ?: return
                val uri = selectedUri ?: return
                CoroutineUtils.runIOTask({
                    val mediaType = FileStorageUtils.getMediaType(uri)
                    val imageStorage =
                        FileStorageUtils.getStorage(mediaType) ?: return@runIOTask
                    val nextId = imageStorage.asyncStore(uri)
                    val file = FileInfo(uri = uri, storageId = nextId, fileType = mediaType)
                    val fileId = DataBase.dbImpl.getFileInfoDao().insertFileInfo(file)
                    val albumItem = AlbumItem(
                        itemName = itemName,
                        rank = itemRank,
                        desc = itemDesc,
                        score = itemScore,
                        albumId = albumId,
                        fileId = fileId
                    )
                    val albumItemId = DataBase.dbImpl.getAlbumItemDao().insertAlbumItem(albumItem)
                    itemLabel.forEach { it.albumItemId = albumItemId }
                    DataBase.dbImpl.getLabelInfoDao().insertAllLabel(itemLabel)
                }) {
                }
            }

            val multipleImagePickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.PickVisualMedia(),
                onResult = { uri: Uri? ->
                    uri ?: return@rememberLauncherForActivityResult
                    when (FileStorageUtils.getMediaType(uri)) {
                        FileType.IMAGE -> {
                            scope.launch(Dispatchers.IO) {
                                val bitmap = BitMapCachePool.toBitMap(uri).second?.asImageBitmap()
                                withContext(Dispatchers.Main) {
                                    pickedImage = bitmap
                                }
                            }
                        }

                        FileType.VIDEO -> {
                            FileStorageUtils.getVideoThumbnail(scope, uri) { bitmap: Bitmap? ->
                                pickedImage = bitmap?.asImageBitmap()
                            }
                        }

                        FileType.RegularFile -> {
                            imageResource = R.drawable.file
                        }
                    }
                    selectedUri = uri
                }
            )

            fun pickImage() {
                multipleImagePickerLauncher.launch(
                    PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                        .build()
                )
            }

            val currentPickedImage = pickedImage
            if (currentPickedImage == null) {
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

            AppThemeTextField(itemName, { value->
                itemName = value
            }, modifier = Modifier.fillMaxWidth(), label = {
                AppThemeText("文件名称")
            }, maxLines = Int.MAX_VALUE)

            AppThemeTextField(itemDesc, { value->
                itemDesc = value
            }, modifier = Modifier.fillMaxWidth(), label = {
                AppThemeText("文件描述")
            }, maxLines = Int.MAX_VALUE)

            AndroidView({ context ->
                StarRateView(context).commonEditableConfig().apply {
                    onScoreChange = StarRateView.Companion.OnScoreChange { score: Float ->
                        itemScore = score
                    }
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
                Modifier
                    .fillMaxWidth()
                    .height(30.dp),
                update = {
                    it.setScore(itemScore)
                })

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
                    .height(30.dp),
                update = {
                    it.setRankType(itemRank)
                })

            LazyRow {
                items(itemLabelSize + 1, key = { index: Int ->
                    if (index == 0) {
                        "null"
                    } else {
                        itemLabel[index - 1].label
                    }
                }, contentType = { index->
                    if (index == 0) {
                        0
                    } else {
                        1
                    }
                }) { index: Int ->
                    if (index == 0) {
                        EditLabelView {
                            itemLabel.add(LabelInfo(label = it))
                            itemLabelSize = itemLabel.size
                        }
                    } else {
                        LabelView(itemLabel[index - 1]) {
                            itemLabel.removeAt(index - 1)
                            itemLabelSize = itemLabel.size
                        }
                    }
                }
            }

            Row {
                Button({
                    onDismiss()
                }) {
                    Text("取消")
                }
                Button({
                    saveItem()
                    onDismiss()
                }) {
                    Text("确认")
                }
            }

        }
    }
}

@Composable
fun LabelView(label: LabelInfo, editable: Boolean = true, onRemove: ()->Unit) {
    if (editable) {
        Row(Modifier.wrapContentSize(), verticalAlignment = Alignment.CenterVertically) {
            AppThemeText(label.label, Modifier.wrapContentSize(), style = LocalTextStyle.current.copy(fontSize = 10.sp))
            Spacer(Modifier.padding(2.dp))
            Image(
                painter = painterResource(R.drawable.close),
                contentDescription = "删除标签",
                Modifier
                    .size(12.dp)
                    .clickable { onRemove() },
            )
        }
    } else {
        AppThemeText(label.label, Modifier.wrapContentSize(), style = LocalTextStyle.current.copy(fontSize = 10.sp))
    }
}

@Composable
fun EditLabelView(onAddLabel: (String)->Unit) {
    var labelText by remember { mutableStateOf("") }
    Row(Modifier.wrapContentSize(), verticalAlignment = Alignment.CenterVertically) {
        CompositionLocalProvider(LocalTextStyle provides LocalTextStyle.current.copy(fontSize = 10.sp)) {
            AppThemeBasicTextField(labelText, {value-> labelText = value},
                Modifier
                    .wrapContentWidth()
                    .height(13.dp), hint = "添加标签")
        }
        Spacer(Modifier.padding(2.dp))
        Image(
            painter = painterResource(R.drawable.add),
            contentDescription = "添加标签",
            Modifier
                .size(12.dp)
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
fun DrawerHeader(drawerState: DrawerState) {
    if (drawerState.isClosed) {
        return
    }
    var userName by remember { mutableStateOf(DEFAULT_USER_NAME) }
    var userDesc by remember { mutableStateOf(DEFAULT_DESC) }
    var userAvatarImage by remember { mutableStateOf<ImageBitmap?>(null) }

    val avatarSize = 150.dp

    val scope = rememberCoroutineScope()
    var editIconJob by remember { mutableStateOf<Job?>(null) }
    var imageValid by remember { mutableStateOf(false) }
    var editUserInfoJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(imageValid, drawerState.isClosed, drawerState.isOpen) {
        editIconJob?.cancel()
        if (!imageValid && (!drawerState.isClosed || drawerState.isOpen)) {
            editIconJob = scope.launch(Dispatchers.IO) {
                val innerUserInfo = UserStorage.userInstance
                withContext(Dispatchers.Main) {
                    userName = innerUserInfo.userName
                    userDesc = innerUserInfo.userDesc
                }
                val fileInfo = innerUserInfo.userAvatar
                val (_, image) = BitMapCachePool.loadImage(
                    fileInfo,
                    avatarSize.dpValue.toInt(),
                    avatarSize.dpValue.toInt()
                )
                withContext(Dispatchers.Main) {
                    userAvatarImage = image?.asImageBitmap()
                    imageValid = true
                }
            }
        }
    }

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val multipleImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri: Uri? ->
            selectedImageUri = uri
            uri ?: return@rememberLauncherForActivityResult
            scope.launch(Dispatchers.IO) {
                val imageStorage = FileStorageUtils.getStorage(FileType.IMAGE) ?: return@launch
                val currentUser = UserStorage.userInstance
                val currentImageId =
                    currentUser.userAvatar.storageId.takeIf { it != DataBase.INVALID_ID }
                val nextId = if (currentImageId != null) {
                    imageStorage.asyncStore(currentImageId, uri)
                } else {
                    imageStorage.asyncStore(uri)
                }
                currentUser.userAvatar.storageId = nextId
                currentUser.userAvatar.fileType = FileType.IMAGE
                currentUser.userAvatar.uri = uri
                UserStorage.userInstance = currentUser
                withContext(Dispatchers.Main) {
                    imageValid = false
                }
            }
        }
    )

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        val currentUserAvatarImage = userAvatarImage

        val imageModifier = Modifier
            .size(avatarSize, avatarSize)
            .clip(CircleShape)
            .clickable {
                multipleImagePickerLauncher.launch(
                    PickVisualMediaRequest
                        .Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        .build()
                )
            }
        Row(
            Modifier.fillMaxWidth()
        ) {
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
            Spacer(Modifier.padding(12.dp))
            val editSize = 14.dp
            Column(Modifier.align(Alignment.CenterVertically)) {
                AppThemeClickEditableText(
                    editSize = editSize,
                    userName,
                    { value, editable ->
                        val userInstance = UserStorage.userInstance
                        userInstance.userName = value
                        userName = value
                        editUserInfoJob?.cancel()
                        editUserInfoJob = CoroutineUtils.runIOTask({
                            UserStorage.userInstance = userInstance
                        })
                    },
                    overflow = TextOverflow.Ellipsis,
                    verticalOffset = 2 * editSize / 3
                )
                Spacer(Modifier.padding(4.dp))
                AppThemeClickEditableText(
                    editSize = editSize,
                    value = userDesc,
                    { value, editable ->
                        val userInstance = UserStorage.userInstance
                        userInstance.userDesc = value
                        userDesc = value
                        editUserInfoJob?.cancel()
                        editUserInfoJob = CoroutineUtils.runIOTask({
                            UserStorage.userInstance = userInstance
                        })
                    },
                    overflow = TextOverflow.Ellipsis,
                    verticalOffset = 2 * editSize / 3
                )
            }
        }
        Spacer(Modifier.padding(8.dp))
        DrawerFunctionArea()
    }
}

@Composable
fun DrawerFunctionArea() {
    var dialogState by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .clickable {
                dialogState = true
            }
    ) {
        Image(
            painter = painterResource(R.drawable.add),
            contentDescription = "添加按钮",
            Modifier
                .size(24.dp)
                .align(Alignment.CenterVertically),
        )
        Spacer(Modifier.padding(2.dp))
        AppThemeText("添加相册", Modifier.align(Alignment.CenterVertically))
    }

    if (dialogState) {
        val viewModel: AlbumViewModel = viewModel()
        var albumName by remember { mutableStateOf("") }
        Dialog(
            onDismissRequest = { dialogState = false },
        ) {
            Column(
                Modifier
                    .background(LocalAppPalette.current.dialogBg)
                    .fillMaxWidth(0.82f)
                    .padding(8.dp)
            ) {
                AppThemeTextField(albumName, { value ->
                    albumName = value
                }, label = {
                    AppThemeText("相册名称")
                })
                Row {
                    Button({
                        dialogState = false
                    }) {
                        AppThemeText("取消")
                    }
                    Button({
                        dialogState = false
                        viewModel.addAlbum(Album(albumName = albumName))
                    }) {
                        AppThemeText("确认")
                    }
                }
            }
        }
    }
}

@Composable
fun MainDrawer(
    drawerState: DrawerState,
    albumData: List<Album>,
    onAlbumSelected: (Int, Album) -> Unit
) {
    if (drawerState.isClosed) {
        return
    }
    val rememberScope = rememberCoroutineScope()
    Column(
        Modifier
            .fillMaxHeight()
            .fillMaxWidth(LocalAppUIConstants.current.drawerMaxPercent)
            .background(LocalAppPalette.current.drawerBg)
    ) {
        DrawerHeader(drawerState)
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(Modifier.fillMaxSize()) {
            items(
                albumData.size,
                key = { index: Int -> albumData[index].albumId ?: DataBase.INVALID_ID }) { index ->
                val item = albumData[index]
                DrawerMenuItem(item, drawerState) {
                    rememberScope.launch {
                        onAlbumSelected(index, item)
                        drawerState.close()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawerMenuItem(
    item: Album,
    drawerState: DrawerState,
    onItemClick: () -> Unit
) {
    var albumName by remember { mutableStateOf(item.albumName) }
    var changeNameJob by remember { mutableStateOf<Job?>(null) }
    val viewModel: AlbumViewModel = viewModel()
    val editSize = 14.dp
    val size = 36.dp
    val modifier = Modifier
        .fillMaxWidth()
        .clip(RectangleShape)
        .height(size)
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val swipeToDismissBoxState = rememberSwipeToDismissBoxState(confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    viewModel.deleteAlbum(item)
                    true
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
                            .background(Color.Red),
                    )
                }
            }) {
            Box(
                Modifier
                    .align(Alignment.CenterVertically)
                    .fillMaxHeight()
                    .fillMaxWidth()
                    .clickable { onItemClick() }
                    .background(LocalAppPalette.current.drawerBg),
            ) {
                AppThemeClickEditableText(
                    editSize = editSize,
                    value = albumName,
                    modifier = Modifier.align(Alignment.CenterStart),
                    onValueChange = { value, _ ->
                        albumName = value
                        item.albumName = value
                        changeNameJob?.cancel()
                        changeNameJob = CoroutineUtils.runIOTask({
                            DataBase.dbImpl.getAlbumDao().updateAlbum(item)
                        })
                    },
                    verticalOffset = 2 * editSize / 3
                )
            }
        }
    }
}