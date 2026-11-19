package com.jingtian.composedemo.main.dialog

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.staggeredgrid.LazyHorizontalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.jingtian.composedemo.base.AppThemeDialog
import com.jingtian.composedemo.base.AppThemeText
import com.jingtian.composedemo.base.screenHeight
import com.jingtian.composedemo.base.screenWidth
import com.jingtian.composedemo.dao.DataBase
import com.jingtian.composedemo.dao.model.Album
import com.jingtian.composedemo.dao.model.FileInfo
import com.jingtian.composedemo.dao.model.FileType
import com.jingtian.composedemo.dao.model.relation.AlbumItemRelation
import com.jingtian.composedemo.main.drawer.ImmutableDrawerMenuItem
import com.jingtian.composedemo.main.labels.CheckableLabelView
import com.jingtian.composedemo.main.labels.EditableLabelView
import com.jingtian.composedemo.main.widget.RankChooser
import com.jingtian.composedemo.main.widget.ScoreChooser
import com.jingtian.composedemo.multiplatform.MultiplatformFile
import com.jingtian.composedemo.navigation.rememberDocumentPicker
import com.jingtian.composedemo.ui.theme.LocalAppPalette
import com.jingtian.composedemo.ui.theme.LocalMiddleButtonConfig
import com.jingtian.composedemo.utils.BitMapCachePool
import com.jingtian.composedemo.utils.FileStorageUtils
import com.jingtian.composedemo.utils.dpValue
import com.jingtian.composedemo.utils.splitByWhiteSpace
import com.jingtian.composedemo.viewmodels.AlbumViewModel
import com.jingtian.composedemo.web.CommonWebView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.min
import com.jingtian.composedemo.base.resources.getPainter
import com.jingtian.composedemo.base.resources.DrawableIcon
import com.jingtian.composedemo.viewmodels.albumViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.IO

private class EditDialogState(val albumItemRelation: AlbumItemRelation, relatedAlbum: Album, rawTotalLabelList: List<String>) {
    val album = albumItemRelation.albumItem
    val viewModel : AlbumViewModel = albumViewModel()

    var pickedImage by mutableStateOf<ImageBitmap?>(null)
    var itemName by mutableStateOf(album.itemName)
    var itemDesc by mutableStateOf(album.desc)
    var itemRank by mutableStateOf(album.rank)
    var itemScore by mutableStateOf(album.score)
    val itemLabel = mutableStateListOf(*albumItemRelation.labelInfos.map { it.label }.toTypedArray())
    val itemLabelSet = mutableStateMapOf(*albumItemRelation.labelInfos.map { it.label to it.label }.toTypedArray())

    var selectedUri by mutableStateOf<MultiplatformFile?>(null)
    var selectedFileType by mutableStateOf(albumItemRelation.fileInfo?.fileType)
    var imageResource by mutableStateOf(DrawableIcon.DrawableUploadToCloud)

    val totalLabelList = mutableStateMapOf(*(rawTotalLabelList.map { it to it }).toTypedArray())
    val filteredTotalLabelList = mutableStateListOf(*((totalLabelList.keys - albumItemRelation.labelInfos.map { it.label }
        .toSet()).toTypedArray()))
    val selectedTotalLabelList = mutableStateMapOf<String, String>()

    var webSnapShotTaker: (suspend ()-> ImageBitmap)? by mutableStateOf(null)

    var currentSelectedAlbum by mutableStateOf(relatedAlbum)

    fun saveItem(scope: CoroutineScope, onDismiss: () -> Unit) {
        val selectedUri = selectedUri
        val selectedFileType = selectedFileType
        if (selectedUri == null || selectedFileType == null || itemName.isNullOrBlank()) {
            viewModel.sendMessage("数据不合法，缺少文件或标题")
            return
        }
        scope.launch {
            val webSnapShot = if (selectedFileType == FileType.HTML) {
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

    suspend fun updateImage(scope: CoroutineScope, uri: MultiplatformFile, fileType: FileType, fileInfo: FileInfo?, imageDpSize: Float) {
        withContext(Dispatchers.IO) {
            when (fileType) {
                FileType.IMAGE -> {
                    val bitmap = if (fileInfo != null) {
                        BitMapCachePool.loadImage(
                            fileInfo,
                            maxWidth = imageDpSize.toInt()
                        ).second
                    } else {
                        BitMapCachePool.toBitMap(
                            uri,
                            maxWidth = imageDpSize.toInt()
                        ).second
                    }
                    withContext(Dispatchers.Main) {
                        pickedImage = bitmap
                    }
                }

                FileType.VIDEO -> {
                    if (fileInfo != null) {
                        FileStorageUtils.getThumbnail(
                            fileInfo,
                            scope,
                            uri,
                            maxWidth = imageDpSize.toInt()
                        ) { bitmap ->
                            withContext(Dispatchers.Main) {
                                pickedImage = bitmap
                            }
                        }
                    } else {
                        FileStorageUtils.getThumbnail(
                            FileType.VIDEO,
                            scope,
                            uri,
                            maxWidth = imageDpSize.toInt()
                        ) { bitmap ->
                            withContext(Dispatchers.Main) {
                                pickedImage = bitmap
                            }
                        }
                    }
                }

                FileType.AUDIO -> {
                    imageResource = DrawableIcon.DrawableMusic
                    if (fileInfo != null) {
                        FileStorageUtils.getThumbnail(
                            fileInfo,
                            scope,
                            uri,
                            maxWidth = imageDpSize.toInt()
                        ) { bitmap ->
                            withContext(Dispatchers.Main) {
                                pickedImage = bitmap
                            }
                        }
                    } else {
                        FileStorageUtils.getThumbnail(
                            FileType.AUDIO,
                            scope,
                            uri,
                            maxWidth = imageDpSize.toInt()
                        ) { bitmap ->
                            withContext(Dispatchers.Main) {
                                pickedImage = bitmap
                            }
                        }
                    }
                }

                FileType.HTML -> {
                    withContext(Dispatchers.Main) {
                        imageResource = DrawableIcon.DrawableChrome
                        pickedImage = null
                    }
                    if (fileInfo != null) {
                        FileStorageUtils.getThumbnail(
                            fileInfo,
                            scope,
                            uri,
                            maxWidth = imageDpSize.toInt()
                        ) { bitmap ->
                            withContext(Dispatchers.Main) {
                                pickedImage = bitmap
                            }
                        }
                    } else {
                        FileStorageUtils.getThumbnail(
                            FileType.HTML,
                            scope,
                            uri,
                            maxWidth = imageDpSize.toInt()
                        ) { bitmap ->
                            withContext(Dispatchers.Main) {
                                pickedImage = bitmap
                            }
                        }
                    }
                }

                FileType.RegularFile -> {
                    withContext(Dispatchers.Main) {
                        imageResource = DrawableIcon.DrawableFile
                        pickedImage = null
                    }
                }
            }
        }
    }

    suspend fun onSelectedUriChange(scope: CoroutineScope, imageDpSize: Float) {
        val uri = selectedUri
        val fileType = selectedFileType
        if (uri != null && fileType != null) {
            updateImage(scope, uri, fileType, null, imageDpSize)
        } else {
            imageResource = DrawableIcon.DrawableLoadFailed
        }
        if (itemName.isBlank() && uri != null) {
            itemName = uri.fileName() ?: ""
        }
    }
}

@Composable
fun EditDialog(albumItemRelation: AlbumItemRelation, relatedAlbum: Album, albumData: List<Album>, totalLabelList: List<String>, onDismiss: ()->Unit) {
    val editDialogState = remember(albumItemRelation, relatedAlbum, totalLabelList) { EditDialogState(albumItemRelation, relatedAlbum, totalLabelList) }
    editDialogState.EditDialog(albumData, onDismiss)
}

@Composable
private fun EditDialogState.EditDialog(albumData: List<Album>, onDismiss: ()->Unit) {
    val scope = rememberCoroutineScope()

    val imageWidth = min(
        min(
            screenWidth(),
            screenHeight()
        ) / 2, 180
    ).dp
    val imageDpSize = imageWidth.dpValue()

    LaunchedEffect(Unit) {
        selectedUri = albumItemRelation.fileInfo?.getFileUri()
        val uri = selectedUri
        val fileType = selectedFileType
        if (uri != null && fileType != null) {
            updateImage(scope, uri, fileType, albumItemRelation.fileInfo, imageDpSize)
        } else {
            imageResource = DrawableIcon.DrawableLoadFailed
        }
        if (itemName.isNullOrBlank() && uri != null) {
            itemName = uri.fileName() ?: ""
        }
    }

    val multipleImagePickerLauncher = rememberDocumentPicker { uri: MultiplatformFile? ->
        scope.launch {
            uri?.takeIf { !it.isHidden() } ?: return@launch
            selectedFileType = uri.mediaType()
            selectedUri = uri
            onSelectedUriChange(scope, imageDpSize)
        }
    }

    fun pickImage() {
        multipleImagePickerLauncher.launch(FileType.mimes)
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
                saveItem(scope, onDismiss)
            },
            onDismissRequest = {},
            properties = DialogProperties(usePlatformDefaultWidth = false)
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
                        OutlinedTextField(itemName, { value ->
                            itemName = value
                        }, modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp), label = {
                            AppThemeText("文件名称")
                        }, maxLines = Int.MAX_VALUE, enabled = false
                        )

                        OutlinedTextField(itemDesc, { value ->
                            itemDesc = value
                        }, modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp), label = {
                            AppThemeText("文件描述")
                        }, maxLines = Int.MAX_VALUE
                        )

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
                            key = { index: Int ->
                                albumData[index].let {
                                    it.albumId ?: DataBase.INVALID_ID to it.albumName
                                }
                            }
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
                            ScoreChooser(itemScore) { value->
                                itemScore = value
                            }
                            Spacer(Modifier.height(4.dp))

                            RankChooser(itemRank) { value->
                                itemRank = value
                            }
                            Spacer(Modifier.height(4.dp))
                        }
                    }

                    item {
                        LazyHorizontalStaggeredGrid(
                            rows = StaggeredGridCells.FixedSize(30.dp),
                            Modifier
                                .padding(horizontal = 6.dp)
                                .height(90.dp)
                                .fillMaxWidth()
                        ) {
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
                                }) { value ->
                                    addItemValue = value
                                }
                            }
                            items(
                                filteredTotalLabelList.size,
                                key = { index -> filteredTotalLabelList[index] }) { index ->
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
                        LazyHorizontalStaggeredGrid(
                            rows = StaggeredGridCells.FixedSize(30.dp),
                            Modifier
                                .padding(horizontal = 6.dp)
                                .height(30.dp)
                                .fillMaxWidth()
                        ) {
                            items(itemLabel.size, key = { index: Int ->
                                itemLabel[index]
                            }) { index: Int ->
                                val item = itemLabel[index]
                                EditableLabelView(item, enableEdit = false, onRemove = {
                                    itemLabel.remove(item)
                                    itemLabelSet.remove(item)
                                    selectedTotalLabelList.remove(item)
                                    if (totalLabelList.containsKey(item) && !filteredTotalLabelList.contains(item)) {
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
                                .fillMaxWidth()
                        ) {
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
                } else if (selectedFileType == FileType.HTML && selectedUri != null) {
                    val density = LocalDensity.current
                    val imageWidthPx = with(density) {
                        imageWidth.roundToPx()
                    }
                    CommonWebView(
                        Modifier
                            .size(imageWidth)
                            .clickable {
                                pickImage()
                            }
                            .clip(RoundedCornerShape(12.dp))
                            .align(Alignment.TopCenter),
                        selectedUri,
                        false,
                        width = imageWidthPx,
                        height = imageWidthPx,
                    ) {
                        initForSnapshot(imageWidthPx, imageWidthPx, false)
                        webSnapShotTaker = suspend {
                            this.takeSnapshot()
                        }
                    }
                } else {
                    Image(
                        painter = getPainter(imageResource),
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