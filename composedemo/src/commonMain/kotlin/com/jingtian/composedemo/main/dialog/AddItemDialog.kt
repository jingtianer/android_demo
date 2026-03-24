package com.jingtian.composedemo.main.dialog

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jingtian.composedemo.base.AppThemeDialog
import com.jingtian.composedemo.base.AppThemeText
import com.jingtian.composedemo.base.screenHeight
import com.jingtian.composedemo.base.screenWidth
import com.jingtian.composedemo.dao.DataBase
import com.jingtian.composedemo.dao.model.Album
import com.jingtian.composedemo.dao.model.FileType
import com.jingtian.composedemo.dao.model.ItemRank
import com.jingtian.composedemo.main.drawer.ImmutableDrawerMenuItem
import com.jingtian.composedemo.main.labels.CheckableLabelView
import com.jingtian.composedemo.main.labels.EditableLabelView
import com.jingtian.composedemo.main.widget.RankChooser
import com.jingtian.composedemo.main.widget.ScoreChooser
import com.jingtian.composedemo.multiplatform.MultiplatformFile
import com.jingtian.composedemo.navigation.rememberDocumentPicker
import com.jingtian.composedemo.ui.theme.LocalAppPalette
import com.jingtian.composedemo.ui.theme.LocalAppUIConstants
import com.jingtian.composedemo.utils.BitMapCachePool
import com.jingtian.composedemo.utils.FileStorageUtils
import com.jingtian.composedemo.utils.dpValue
import com.jingtian.composedemo.utils.splitByWhiteSpace
import com.jingtian.composedemo.viewmodels.AlbumViewModel
import com.jingtian.composedemo.web.CommonWebView
import kotlinx.coroutines.launch
import kotlin.math.min
import com.jingtian.composedemo.base.resources.getPainter
import com.jingtian.composedemo.base.resources.DrawableIcon

@Composable
fun AddItemDialog(album: Album, totalLabelList: List<String>, albumData: List<Album>, onDismiss: () -> Unit) {

    var pickedImage by remember { mutableStateOf<ImageBitmap?>(null) }
    var itemName by remember { mutableStateOf("") }
    var itemDesc by remember { mutableStateOf("") }
    var itemRank by remember { mutableStateOf(ItemRank.NONE) }
    val selectedTotalLabelList = remember { mutableStateMapOf<String, String>() }
    var itemScore by remember { mutableFloatStateOf(0.0f) }
    val itemLabel = remember { mutableStateListOf<String>() }
    val itemLabelSet = remember { mutableStateMapOf<String, String>() }
    var webSnapShotTaker: (suspend ()-> ImageBitmap)? by remember { mutableStateOf(null) }

    var selectedUri by remember { mutableStateOf<MultiplatformFile?>(null) }
    val scope = rememberCoroutineScope()
    var imageResource by remember { mutableStateOf(DrawableIcon.DrawableUploadToCloud) }
    val viewModel: AlbumViewModel = viewModel(factory = AlbumViewModel.viewModelFactory)
    val imageWidth = min(
        min(
            screenWidth(),
            screenHeight()
        ) / 2, 180
    ).dp
    var currentSelectedAlbum by remember { mutableStateOf(album) }
    var selectedFileType by remember { mutableStateOf<FileType?>(null) }
    fun saveItem() {
        if (selectedUri == null || itemName.isBlank()) {
            viewModel.sendMessage("数据不合法，缺少文件或标题")
            return
        }
        scope.launch {
            selectedUri?.let { selectedUri->
                val webSnapShot = if (selectedFileType == FileType.HTML) {
                    webSnapShotTaker?.invoke()
                } else {
                    null
                }
                onDismiss()
                viewModel.addItem(currentSelectedAlbum, selectedUri, itemName, itemRank, itemDesc, itemScore, itemLabelSet.keys, webSnapShot)
            }
        }
    }

    val imageDpSize = imageWidth.dpValue()
    val multipleImagePickerLauncher by rememberDocumentPicker(
        onResult = { uri: MultiplatformFile? ->
            uri?.takeIf { !it.isHidden } ?: return@rememberDocumentPicker
            when (uri.mediaType) {
                FileType.IMAGE -> {
                    BitMapCachePool.toBitMap(
                        scope,
                        uri,
                        maxWidth = imageDpSize.toInt()
                    ) { _, bitmap ->
                        pickedImage = bitmap
                    }
                }

                FileType.VIDEO -> {
                    FileStorageUtils.getThumbnail(
                        FileType.VIDEO,
                        scope,
                        uri,
                        maxWidth = imageDpSize.toInt()
                    ) { bitmap ->
                        pickedImage = bitmap
                    }
                }

                FileType.AUDIO -> {
                    imageResource = DrawableIcon.DrawableMusic
                    pickedImage = null
                    FileStorageUtils.getThumbnail(
                        FileType.AUDIO,
                        scope,
                        uri,
                        maxWidth = imageDpSize.toInt()
                    ) { bitmap ->
                        pickedImage = bitmap
                    }
                }

                FileType.HTML -> {
                    imageResource = DrawableIcon.DrawableChrome
                    pickedImage = null
                    FileStorageUtils.getThumbnail(
                        FileType.HTML,
                        scope,
                        uri,
                        maxWidth = imageDpSize.toInt()
                    ) { bitmap ->
                        pickedImage = bitmap
                    }
                }

                FileType.RegularFile -> {
                    imageResource = DrawableIcon.DrawableFile
                    pickedImage = null
                }
            }
            if (itemName.isNullOrBlank()) {
                itemName = uri.fileName ?: ""
            }
            selectedUri = uri
            selectedUri?.let { selectedUri ->
                selectedFileType = selectedUri.mediaType
            }
        }
    )

    fun pickImage() {
        multipleImagePickerLauncher.launch(FileType.mimes)
    }

    AppThemeDialog(
        Modifier
            .fillMaxWidth(LocalAppUIConstants.current.dialogPercent)
            .fillMaxHeight(LocalAppUIConstants.current.dialogPercent)
            .wrapContentHeight(),
        onDismissRequest = {},
        properties = DialogProperties(usePlatformDefaultWidth = false),
        onNegative = onDismiss,
        onPositive = { saveItem() }
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
                                (it.albumId ?: DataBase.INVALID_ID) to it.albumName
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
                        ScoreChooser(itemScore) { score->
                            itemScore = score
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
                            .height(60.dp)
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
                            totalLabelList.size,
                            key = { index -> totalLabelList[index] }) { index ->
                            val item = totalLabelList[index]
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
                            .height(60.dp)
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
                    width = imageWidth,
                    height = imageWidth,
                ) {
                    initForSnapShot(imageWidth, imageWidth, false)
                    webSnapShotTaker = suspend {
                        this.tackSnapShot()
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