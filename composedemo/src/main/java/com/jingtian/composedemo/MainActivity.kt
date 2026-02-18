package com.jingtian.composedemo

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerState
import androidx.compose.runtime.Composable
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
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
import com.jingtian.composedemo.dao.model.Label
import com.jingtian.composedemo.dao.model.LabelInfo
import com.jingtian.composedemo.dao.model.relation.AlbumItemRelation
import com.jingtian.composedemo.ui.theme.LocalAppPalette
import com.jingtian.composedemo.ui.theme.LocalAppUIConstants
import com.jingtian.composedemo.utils.BitMapCachePool
import com.jingtian.composedemo.utils.CoroutineUtils
import com.jingtian.composedemo.utils.FileStorageUtils
import com.jingtian.composedemo.utils.UserStorage
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
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Open)
    val viewModel: AlbumViewModel = viewModel()
    var menuItemsEntity by remember { mutableStateOf(emptyList<Album>()) }
//    val rememberScope = rememberCoroutineScope()
    var currentSelectedAlbum by remember { mutableStateOf<IndexedValue<Album>?>(null) }
    var dataChanged by remember { mutableStateOf(false) }

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

    LaunchedEffect(drawerState.isOpen) {
        if (drawerState.isOpen) {
            withContext(Dispatchers.IO) {
                viewModel.menuItemsFlow.collect { value ->
                    val nextSelectedAlbum = updateSelectedValue(value)
                    withContext(Dispatchers.Main) {
                        menuItemsEntity = value
                        currentSelectedAlbum = nextSelectedAlbum
                    }
                }
            }
        }
    }

    LaunchedEffect(dataChanged) {
        if (drawerState.isOpen && dataChanged) {
            withContext(Dispatchers.IO) {
                viewModel.menuItemsFlow.collect { value ->
                    val nextSelectedAlbum = updateSelectedValue(value)
                    withContext(Dispatchers.Main) {
                        menuItemsEntity = value
                        dataChanged = false
                        currentSelectedAlbum = nextSelectedAlbum
                    }
                }
            }
        }
    }

    viewModel.albumListChange.composeObserve {
        dataChanged = true
    }

    ModalNavigationDrawer(
        {
            MainDrawer(drawerState, menuItemsEntity) { index, album ->
                currentSelectedAlbum = IndexedValue(index, album)
            }
        },
        Modifier.fillMaxSize(),
        drawerState = drawerState
    ) {
        Gallery(currentSelectedAlbum)
    }
}

@Composable
fun Gallery(album: IndexedValue<Album>?) {
    if (album == null) {
        return
    }

    var addImageDialogState by remember { mutableStateOf(false) }
    var itemList by remember { mutableStateOf(emptyList<AlbumItemRelation>()) }
    val viewModel: AlbumViewModel = viewModel()

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
                .wrapContentHeight()) {
            AppThemeText(album.value.albumName, Modifier.align(Alignment.CenterStart))
            Image(
                painter = painterResource(R.drawable.add),
                contentDescription = "添加图片",
                modifier = Modifier
                    .size(32.dp)
                    .align(Alignment.CenterEnd)
                    .clickable { addImageDialogState = true }
            )
        }
        LazyColumn(Modifier.fillMaxSize().weight(1f)) {
            items(itemList.size, key = { index: Int ->  itemList[index].albumItem.itemId ?: DataBase.INVALID_ID } ) { index: Int ->
                AlbumItemView(itemList[index])
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
fun AlbumItemView(albumItemRelation: AlbumItemRelation) {
    val size = 130.dp
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val (_, bitmap) = BitMapCachePool.loadImage(albumItemRelation.fileInfo ?: return@withContext, size.value.toInt(), size.value.toInt())
            withContext(Dispatchers.Main) {
                imageBitmap = bitmap?.asImageBitmap()
            }
        }
    }
    Column {
        val currentImage: ImageBitmap? = imageBitmap
        if (currentImage == null) {
            Image(
                painter = painterResource(R.drawable.load_failed),
                contentDescription = "图片加载失败",
                Modifier.size(size)
            )
        } else {
            Image(
                bitmap = currentImage,
                contentDescription = "图片加载失败",
                Modifier.size(size)
            )
        }
    }
}

@Composable
fun AddImageDialog(album: Album, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = { onDismiss() }) {
        Column(
            Modifier
                .fillMaxWidth(LocalAppUIConstants.current.dialogaxPercent)
                .verticalScroll(rememberScrollState())
                .background(LocalAppPalette.current.dialogBg)
                .padding(12.dp)
                .wrapContentHeight()
        ) {
            var pickedImage by remember { mutableStateOf<ImageBitmap?>(null) }
            var itemName by remember { mutableStateOf("") }
            var itemRank by remember { mutableStateOf(ItemRank.NONE) }
            var itemScore by remember { mutableStateOf(0.0f) }
            var itemLabel by remember { mutableStateOf<List<Label>>(listOf()) }

            var selectedUri by remember { mutableStateOf<Uri?>(null) }
            val scope = rememberCoroutineScope()
            var imageResource by remember { mutableStateOf(R.drawable.upload_to_cloud) }

            fun saveItem() {
                val albumId = album.albumId ?: return
                val uri = selectedUri ?: return
                CoroutineUtils.runIOTask({
                    val nextId = when(FileStorageUtils.getMediaType(uri)) {
                        FileStorageUtils.MediaType.IMAGE -> {
                            val imageStorage = FileStorageUtils.getStorage(FileType.IMAGE) ?: return@runIOTask
                            imageStorage.asyncStore(uri)
                        }
                        FileStorageUtils.MediaType.VIDEO -> {
                            val imageStorage = FileStorageUtils.getStorage(FileType.VIDEO) ?: return@runIOTask
                            imageStorage.asyncStore(uri)
                        }
                        FileStorageUtils.MediaType.UNKNOWN -> {
                            val imageStorage = FileStorageUtils.getStorage(FileType.RegularFile) ?: return@runIOTask
                            imageStorage.asyncStore(uri)
                        }
                    }
                    val file = FileInfo(uri = uri, storageId = nextId, fileType = FileType.IMAGE)
                    val fileId = DataBase.dbImpl.getFileInfoDao().insertFileInfo(file)
                    val albumItem = AlbumItem(itemName = itemName, rank = itemRank, score = itemScore, albumId = albumId, fileId = fileId)
                    val albumItemId = DataBase.dbImpl.getAlbumItemDao().insertAlbumItem(albumItem)
                    val label = itemLabel.map { LabelInfo(albumItemId = albumItemId, label = it) }
                    DataBase.dbImpl.getLabelInfoDao().insertAllLabel(label)
                }) {
                }
            }

            val multipleImagePickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.PickVisualMedia(),
                onResult = { uri: Uri? ->
                    uri ?: return@rememberLauncherForActivityResult
                    when(FileStorageUtils.getMediaType(uri)) {
                        FileStorageUtils.MediaType.IMAGE -> {
                            scope.launch(Dispatchers.IO) {
                                val bitmap = BitMapCachePool.toBitMap(uri).second?.asImageBitmap()
                                withContext(Dispatchers.Main) {
                                    pickedImage = bitmap
                                }
                            }
                        }
                        FileStorageUtils.MediaType.VIDEO -> {
                            FileStorageUtils.getVideoThumbnail(scope, uri) { bitmap: Bitmap? ->
                                pickedImage = bitmap?.asImageBitmap()
                            }
                        }
                        FileStorageUtils.MediaType.UNKNOWN -> {
                            imageResource = R.drawable.file
                        }
                    }
                    selectedUri = uri
                }
            )

            fun pickImage() {
                multipleImagePickerLauncher.launch(PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                    .build())
            }

            val currentPickedImage = pickedImage
            if (currentPickedImage == null) {
                Image(
                    painter = painterResource(imageResource),
                    contentDescription = "上传照片",
                    Modifier.clickable {
                        pickImage()
                    }.fillMaxWidth().wrapContentHeight(),
                    contentScale = ContentScale.Fit
                )
            } else {
                Image(
                    bitmap = currentPickedImage,
                    contentDescription = "上传照片",
                    Modifier.clickable {
                        pickImage()
                    }.fillMaxWidth().wrapContentHeight(),
                    contentScale = ContentScale.Fit
                )
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
                    150.dp.value.toInt(),
                    150.dp.value.toInt()
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