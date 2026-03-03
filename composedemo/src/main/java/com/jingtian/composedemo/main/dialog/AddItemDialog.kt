package com.jingtian.composedemo.main.dialog

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.staggeredgrid.LazyHorizontalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jingtian.composedemo.R
import com.jingtian.composedemo.base.AppThemeDialog
import com.jingtian.composedemo.base.AppThemeText
import com.jingtian.composedemo.dao.DataBase
import com.jingtian.composedemo.dao.model.Album
import com.jingtian.composedemo.dao.model.FileType
import com.jingtian.composedemo.dao.model.ItemRank
import com.jingtian.composedemo.main.LabelCheckInfo
import com.jingtian.composedemo.main.drawer.ImmutableDrawerMenuItem
import com.jingtian.composedemo.main.labels.CheckableLabelView
import com.jingtian.composedemo.main.labels.EditableLabelView
import com.jingtian.composedemo.ui.theme.LocalAppPalette
import com.jingtian.composedemo.ui.theme.LocalAppUIConstants
import com.jingtian.composedemo.ui.widget.RankTypeChooser
import com.jingtian.composedemo.ui.widget.StarRateView
import com.jingtian.composedemo.utils.BitMapCachePool
import com.jingtian.composedemo.utils.FileStorageUtils
import com.jingtian.composedemo.utils.FileStorageUtils.isHidden
import com.jingtian.composedemo.utils.ViewUtils.commonEditableConfig
import com.jingtian.composedemo.utils.ViewUtils.dpValue
import com.jingtian.composedemo.utils.splitByWhiteSpace
import com.jingtian.composedemo.viewmodels.AlbumViewModel
import com.jingtian.composedemo.web.CommonWebView
import kotlinx.coroutines.launch
import kotlin.math.min

@Composable
fun AddItemDialog(album: Album, totalLabelList: List<String>, albumData: List<Album>, onDismiss: () -> Unit) {

    var pickedImage by remember { mutableStateOf<ImageBitmap?>(null) }
    var itemName by remember { mutableStateOf("") }
    var itemDesc by remember { mutableStateOf("") }
    var itemRank by remember { mutableStateOf(ItemRank.NONE) }
    val filteredTotalLabelList = remember { mutableStateMapOf(*totalLabelList.map { it to it }.toTypedArray()) }
    val selectedTotalLabelList = remember { mutableStateMapOf<String, String>() }
    var itemScore by remember { mutableFloatStateOf(0.0f) }
    val itemLabel = remember { mutableStateListOf<String>() }
    val itemLabelSet = remember { mutableStateMapOf<String, String>() }
    var webSnapShotTaker: (suspend ()-> Bitmap)? by remember { mutableStateOf(null) }

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    val scope = rememberCoroutineScope()
    var imageResource by remember { mutableStateOf(R.drawable.upload_to_cloud) }
    val viewModel: AlbumViewModel = viewModel()
    val imageWidth = min(LocalConfiguration.current.let {
        min(
            it.screenWidthDp,
            it.screenHeightDp
        ) / 2
    }, 180).dp
    var currentSelectedAlbum by remember { mutableStateOf(album) }
    var selectedFileType by remember { mutableStateOf<FileType?>(null) }
    fun saveItem(context: Context) {
        if (selectedUri == null || itemName.isBlank()) {
            Toast.makeText(context, "数据不合法，缺少文件或标题", Toast.LENGTH_SHORT).show()
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

    val multipleImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? ->
            uri?.takeIf { !it.isHidden() } ?: return@rememberLauncherForActivityResult
            when (FileStorageUtils.getMediaType(uri)) {
                FileType.IMAGE -> {
                    BitMapCachePool.toBitMap(
                        scope,
                        uri,
                        maxWidth = imageWidth.dpValue.toInt()
                    ) { _, bitmap ->
                        pickedImage = bitmap?.asImageBitmap()
                    }
                }

                FileType.VIDEO -> {
                    FileStorageUtils.getThumbnail(
                        FileType.VIDEO,
                        scope,
                        uri,
                        maxWidth = imageWidth.dpValue.toInt()
                    ) { bitmap: Bitmap? ->
                        pickedImage = bitmap?.asImageBitmap()
                    }
                }

                FileType.AUDIO -> {
                    imageResource = R.drawable.music
                    pickedImage = null
                    FileStorageUtils.getThumbnail(
                        FileType.AUDIO,
                        scope,
                        uri,
                        maxWidth = imageWidth.dpValue.toInt()
                    ) { bitmap: Bitmap? ->
                        pickedImage = bitmap?.asImageBitmap()
                    }
                }

                FileType.HTML -> {
                    imageResource = R.drawable.chrome
                    pickedImage = null
                    FileStorageUtils.getThumbnail(
                        FileType.HTML,
                        scope,
                        uri,
                        maxWidth = imageWidth.dpValue.toInt()
                    ) { bitmap: Bitmap? ->
                        pickedImage = bitmap?.asImageBitmap()
                    }
                }

                FileType.RegularFile -> {
                    imageResource = R.drawable.file
                    pickedImage = null
                }
            }
            if (itemName.isNullOrBlank()) {
                itemName = FileStorageUtils.getFileNameFromUri(uri) ?: ""
            }
            selectedUri = uri
            selectedUri?.let { selectedUri ->
                selectedFileType = FileStorageUtils.getMediaType(selectedUri)
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
                    OutlinedTextField(itemName, { value ->
                        itemName = value
                    }, modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp), label = {
                        AppThemeText("文件名称")
                    }, maxLines = Int.MAX_VALUE
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
                                if (filteredTotalLabelList.containsKey(item)) {
                                    selectedTotalLabelList.remove(item)
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