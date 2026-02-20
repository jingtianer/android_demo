package com.jingtian.composedemo

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.staggeredgrid.LazyHorizontalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerState
import androidx.compose.runtime.Composable
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jingtian.composedemo.base.AppThemeBasicTextField
import com.jingtian.composedemo.base.AppThemeClickEditableText
import com.jingtian.composedemo.base.AppThemeHorizontalDivider
import com.jingtian.composedemo.base.AppThemeText
import com.jingtian.composedemo.base.BaseActivity
import com.jingtian.composedemo.dao.DataBase
import com.jingtian.composedemo.dao.model.Album
import com.jingtian.composedemo.dao.model.DEFAULT_DESC
import com.jingtian.composedemo.dao.model.DEFAULT_USER_NAME
import com.jingtian.composedemo.dao.model.FileType
import com.jingtian.composedemo.dao.model.ItemRank
import com.jingtian.composedemo.dao.model.LabelInfo
import com.jingtian.composedemo.dao.model.relation.AlbumItemRelation
import com.jingtian.composedemo.ui.theme.LocalAppPalette
import com.jingtian.composedemo.ui.theme.LocalAppUIConstants
import com.jingtian.composedemo.ui.theme.LocalSecondaryTextStyle
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
        Log.d("jingtian", "Main: $snackBarMessage")
    }

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
    ) { innerPadding->
        ModalNavigationDrawer(
            {
                MainDrawer(drawerState, menuItemsEntity) { index, album ->
                    currentSelectedAlbum = IndexedValue(index, album)
                }
            },
            Modifier
                .fillMaxSize(),
            drawerState = drawerState,
            gesturesEnabled = menuItemsEntity.isNotEmpty()
        ) {
            Gallery(currentSelectedAlbum)
        }
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

    val size = 28.dp
    val padding = 4.dp
    fun notifyCheckChanged() {
        onCheckStateChange(labelList.mapNotNull { checkInfo -> checkInfo.label.takeIf { checkInfo.isChecked } })
    }
    LazyHorizontalGrid(rows = GridCells.Fixed(2),
        Modifier
            .height((size + padding * 2) * 2)
            .fillMaxWidth()) {
        items(labelList.size) { index ->
            val item = labelList[index]
            var checked by remember { mutableStateOf(item.isChecked) }
            CheckableLabelView(label = item.label, isChecked = checked, onCheckStateChange = { checked = it })
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
    val albumItemDataChange by viewModel.albumItemListChange.observeAsState()

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
    val importDirLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) {uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        viewModel.importFiles(album.value, uri)
    }

    Column(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .wrapContentHeight()
        ) {
            AppThemeText(album.value.albumName, Modifier.align(Alignment.CenterStart), style = LocalTextStyle.current.copy(fontSize = 24.sp, fontWeight = FontWeight(600)))

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
                    painter = painterResource(R.drawable.resource_import),
                    contentDescription = "批量导入",
                    modifier = Modifier
                        .size(32.dp)
                        .clickable {
                            importDirLauncher.launch(null)
                        }
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
            items(finalItemList.size, key = { index-> finalItemList[index].hashCode() }) { index: Int ->
                AlbumItemView(finalItemList[index], size, padding)
            }
        }
    }

    if (addImageDialogState) {
        AddItemDialog(album.value) {
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
    var itemLabel by remember { mutableStateOf(albumItemRelation.labelInfos.toMutableList()) }
    var itemLabelSize by remember { mutableStateOf(albumItemRelation.labelInfos.size) }

    val scope = rememberCoroutineScope()
    var imageResource by remember { mutableStateOf(R.drawable.load_failed) }
    imageResource = R.drawable.load_failed
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
                    }
                }
                FileType.VIDEO -> {
                    getVideoThumbnail(scope, uri) { bitmap->
                        imageBitmap = bitmap?.asImageBitmap()
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

    Column(
        Modifier
            .width(size)
            .padding(padding)
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = {
                        showEditDialog = true
                    }
                )
            }
            .background(
                color = LocalAppPalette.current.dialogBg,
                shape = RoundedCornerShape(padding)
            )) {


        AppThemeText(itemName, modifier = Modifier.fillMaxWidth(), maxLines = Int.MAX_VALUE, style = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 16.sp))

        val currentPickedImage = imageBitmap
        Box {
            if (currentPickedImage == null) {
                Image(
                    painter = painterResource(imageResource),
                    contentDescription = "上传照片",
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
                            if (playIntent != null) {
                                context.startActivity(playIntent)
                            }
                        }
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .align(Alignment.Center),
                    contentScale = ContentScale.FillWidth
                )
            }

            if (itemRank != null && itemRank != ItemRank.NONE) {
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
        }

        if (!itemDesc.isNullOrBlank()) {
            OutlinedTextField(itemDesc, { value->
                itemDesc = value
            }, modifier = Modifier.fillMaxWidth(), label = {
                AppThemeText("评论")
            }, maxLines = Int.MAX_VALUE, enabled = false)
        }

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

        if (itemLabel.isNotEmpty()) {
            LazyHorizontalStaggeredGrid(rows = StaggeredGridCells.Adaptive(24.dp),
                Modifier
                    .fillMaxWidth()
                    .height(48.dp)) {
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

    if (showEditDialog) {
        EditDialog(albumItemRelation) {
            showEditDialog = false
        }
    }
}

@Composable
fun EditDialog(albumItemRelation: AlbumItemRelation, onDismiss: ()->Unit) {
    val album = albumItemRelation.albumItem
    val viewModel : AlbumViewModel = viewModel()
    Dialog(onDismissRequest = { onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .fillMaxWidth(LocalAppUIConstants.current.dialogPercent)
                .verticalScroll(rememberScrollState())
                .background(LocalAppPalette.current.dialogBg)
                .padding(12.dp)
                .clip(RectangleShape)
                .wrapContentHeight()
        ) {
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
                )
            }

            fun updateImage(uri: Uri, fileType: FileType) {
                when (fileType) {
                    FileType.IMAGE -> {
                        scope.launch(Dispatchers.IO) {
                            val bitmap = BitMapCachePool.toBitMap(uri).second?.asImageBitmap()
                            withContext(Dispatchers.Main) {
                                pickedImage = bitmap
                            }
                        }
                    }

                    FileType.VIDEO -> {
                        getVideoThumbnail(scope, uri) { bitmap: Bitmap? ->
                            pickedImage = bitmap?.asImageBitmap()
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

            LaunchedEffect(selectedUri) {
                val uri = selectedUri
                val fileType = selectedFileType
                if (uri != null && fileType != null) {
                    updateImage(uri, fileType)
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
                }
            )

            fun pickImage() {
                multipleImagePickerLauncher.launch(FileType.mimes)
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

            OutlinedTextField(itemName, { value->
                itemName = value
            }, modifier = Modifier.fillMaxWidth(), label = {
                AppThemeText("文件名称")
            }, maxLines = Int.MAX_VALUE)

            OutlinedTextField(itemDesc, { value->
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
            Column {
                EditLabelView {
                    itemLabel.add(0, LabelInfo(label = it))
                    itemLabelSize = itemLabel.size
                }
                LazyRow {
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

            val context = LocalContext.current

            Row {
                Button({
                    onDismiss()
                }) {
                    Text("取消")
                }
                Button({
                    deleteItem()
                    onDismiss()
                }) {
                    Text("删除")
                }
                Button({
                    saveItem(context)
                }) {
                    Text("确认")
                }
            }
        }
    }
}

@Composable
fun AddItemDialog(album: Album, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = { onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .fillMaxWidth(LocalAppUIConstants.current.dialogPercent)
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

            OutlinedTextField(itemName, { value->
                itemName = value
            }, modifier = Modifier.fillMaxWidth(), label = {
                AppThemeText("文件名称")
            }, maxLines = Int.MAX_VALUE)

            OutlinedTextField(itemDesc, { value->
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

            Column {
                EditLabelView {
                    itemLabel.add(0, LabelInfo(label = it))
                    itemLabelSize = itemLabel.size
                }
                LazyRow {
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
            val context = LocalContext.current
            Row {
                Button({
                    onDismiss()
                }) {
                    Text("取消")
                }
                Button({
                    saveItem(context)
                }) {
                    Text("确认")
                }
            }
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
                .padding(horizontal = 3.dp, vertical = 2.dp)
                .wrapContentSize(), style = LocalTextStyle.current.copy(fontSize = 14.sp))
    }
}

@Composable
fun LabelView(label: LabelInfo, editable: Boolean = true, onRemove: ()->Unit) {
    if (editable) {
        Row(Modifier.wrapContentSize(), verticalAlignment = Alignment.CenterVertically) {
            AppThemeText(label.label, Modifier.wrapContentSize(), style = LocalTextStyle.current.copy(fontSize = 16.sp))
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
                    color = LocalAppPalette.current.dialogBg,
                    shape = RoundedCornerShape(4.dp)
                )
                .wrapContentSize(), contentAlignment = Alignment.Center) {
            AppThemeText(label.label,
                Modifier
                    .padding(horizontal = 3.dp, vertical = 2.dp)
                    .wrapContentSize(), style = LocalTextStyle.current.copy(fontSize = 14.sp))
        }
    }
}

@Composable
fun EditLabelView(onAddLabel: (String)->Unit) {
    var labelText by remember { mutableStateOf("") }
    Row(Modifier.wrapContentSize(), verticalAlignment = Alignment.CenterVertically) {
        CompositionLocalProvider(LocalTextStyle provides LocalTextStyle.current.copy(fontSize = 24.sp)) {
            AppThemeBasicTextField(labelText, {value-> labelText = value},
                Modifier
                    .wrapContentWidth()
                    .wrapContentHeight(), hint = "添加标签")
        }
        Spacer(Modifier.padding(2.dp))
        Image(
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
fun DrawerHeader(drawerState: DrawerState) {
    if (drawerState.isClosed) {
        return
    }
    var userName by remember { mutableStateOf(DEFAULT_USER_NAME) }
    var userDesc by remember { mutableStateOf(DEFAULT_DESC) }
    var userAvatarImage by remember { mutableStateOf<ImageBitmap?>(null) }

    val avatarSize = 150.dp

    val scope = rememberCoroutineScope()
    var editUserInfoJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(drawerState.isClosed, drawerState.isOpen) {
        if (!drawerState.isClosed || drawerState.isOpen) {
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
                    BitMapCachePool.invalid(currentImageId)
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

    Column(
        Modifier.fillMaxWidth()
    ) {
        val currentUserAvatarImage = userAvatarImage

        val imageModifier = Modifier
            .padding(end = 8.dp)
            .size(avatarSize, avatarSize)
            .clip(CircleShape)
            .border(4.dp, LocalAppPalette.current.strokeColor, CircleShape)
            .clickable {
                multipleImagePickerLauncher.launch(
                    PickVisualMediaRequest
                        .Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        .build()
                )
            }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 6.dp, top = 6.dp, end = 6.dp)
                .background(
                    color = LocalAppPalette.current.cardBg,
                    shape = RoundedCornerShape(4.dp)
                )
                .align(Alignment.CenterHorizontally)
                .padding(vertical = 6.dp, horizontal = 6.dp)
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
            val editSize = 14.dp
            Column(Modifier.align(Alignment.CenterVertically)) {
                CompositionLocalProvider(LocalTextStyle provides LocalTextStyle.current.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight(600),
                )) {
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
                        modifier = Modifier.fillMaxWidth(),
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 2,
                        hint = "输入用户名",
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                CompositionLocalProvider(LocalTextStyle provides LocalSecondaryTextStyle.current.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight(600),
                )) {
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
                        modifier = Modifier.fillMaxWidth(),
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 2,
                        hint = "输入个性签名",
                    )
                }
            }
        }
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
            .padding(vertical = 8.dp, horizontal = 8.dp)
    ) {
        Image(
            painter = painterResource(R.drawable.add),
            contentDescription = "添加按钮",
            Modifier
                .size(26.dp)
                .align(Alignment.CenterVertically),
        )
        AppThemeText("添加相册",
            Modifier
                .align(Alignment.CenterVertically)
                .padding(horizontal = 6.dp),
            style = LocalTextStyle.current.copy(fontSize = 16.sp)
        )
    }

    if (dialogState) {
        val viewModel: AlbumViewModel = viewModel()
        var albumName by remember { mutableStateOf("") }
        val focusRequester = remember { FocusRequester() }
        Dialog(
            onDismissRequest = { dialogState = false },
        ) {
            Column(
                Modifier
                    .background(LocalAppPalette.current.dialogBg)
                    .fillMaxWidth(0.82f)
                    .padding(8.dp)
            ) {

                OutlinedTextField(albumName, { value ->
                    albumName = value
                }, label = {
                    AppThemeText("相册名称")
                }, modifier = Modifier.focusRequester(focusRequester))
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
        LaunchedEffect(dialogState) {
            if (dialogState) {
                focusRequester.requestFocus()
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
        AppThemeHorizontalDivider(modifier = Modifier
            .height(1.dp)
            .fillMaxWidth(0.95f)
            .align(Alignment.CenterHorizontally))
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
    var deleteConfirmDialogState by remember { mutableStateOf(false) }
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
            Box(
                Modifier
                    .align(Alignment.CenterVertically)
                    .fillMaxHeight()
                    .fillMaxWidth()
                    .clickable { onItemClick() }
                    .background(LocalAppPalette.current.drawerBg)
                    .padding(vertical = 8.dp, horizontal = 8.dp),
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
                )
            }
        }
    }

    if (deleteConfirmDialogState) {
        ConfirmDialog("确认删除相册: ${item.albumName}", properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false), onNegative = {
            deleteConfirmDialogState = false
        }, onPositive = {
            deleteConfirmDialogState = false
            viewModel.deleteAlbum(item)
        }, content = {})
    }
}

@Composable
fun ConfirmDialog(title: String, titleTextStyle: TextStyle = LocalTextStyle.current.copy(
    fontSize = 22.sp,
    fontWeight = FontWeight(600)
), properties: DialogProperties = DialogProperties(), onNegative: () -> Unit, onPositive: () -> Unit, content: @Composable ()->Unit) {
    Dialog(onDismissRequest = onNegative, properties = properties) {
        Column(
            Modifier
                .fillMaxWidth(0.9f)
                .background(LocalAppPalette.current.dialogBg)
                .padding(horizontal = 8.dp)) {
            Spacer(modifier = Modifier.height(8.dp))
            AppThemeText(title, style = titleTextStyle)
            Spacer(modifier = Modifier.height(2.dp))
            AppThemeHorizontalDivider(
                modifier = Modifier
                    .height(1.dp)
                    .align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(8.dp))
            content()
            Spacer(modifier = Modifier.height(8.dp))
            Row(Modifier.align(Alignment.End)) {
                Button(onClick = onNegative) {
                    AppThemeText("取消")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onPositive) {
                    AppThemeText("确认")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}