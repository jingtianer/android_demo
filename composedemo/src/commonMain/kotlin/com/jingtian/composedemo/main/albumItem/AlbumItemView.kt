package com.jingtian.composedemo.main.albumItem

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jingtian.composedemo.base.AppThemeText
import com.jingtian.composedemo.dao.DataBase
import com.jingtian.composedemo.dao.model.FileInfo
import com.jingtian.composedemo.dao.model.FileType
import com.jingtian.composedemo.dao.model.relation.AlbumItemRelation
import com.jingtian.composedemo.main.labels.LabelView
import com.jingtian.composedemo.main.widget.RankLabel
import com.jingtian.composedemo.main.widget.ScoreStar
import com.jingtian.composedemo.navigation.rememberAlbumLauncher
import com.jingtian.composedemo.ui.theme.LocalAppPalette
import com.jingtian.composedemo.ui.theme.LocalSecondaryTextStyle
import com.jingtian.composedemo.utils.BitMapCachePool
import com.jingtian.composedemo.utils.FileStorageUtils
import com.jingtian.composedemo.utils.dpValue
import com.jingtian.composedemo.utils.getOrPutRef
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.ref.SoftReference
import com.jingtian.composedemo.base.resources.getPainter
import com.jingtian.composedemo.base.resources.DrawableIcon

class AlbumItemViewStateHolder(
    val albumItemRelation: AlbumItemRelation,
    val size: Dp,
    val dpSize: Float,
    val padding: Dp,
    val currentSelectedItem: SnapshotStateMap<Long, AlbumItemRelation>,
    val enterEditState: MutableState<Boolean>,
    val itemSelectStateChangeState: MutableState<Long>,
    val showEditDialogState: MutableState<AlbumItemRelation?>
) {
    var imageBitmap by mutableStateOf<ImageBitmap?>(null)
    var itemName by mutableStateOf(albumItemRelation.albumItem.itemName)
    var itemDesc by mutableStateOf(albumItemRelation.albumItem.desc)
    var itemRank by mutableStateOf(albumItemRelation.albumItem.rank)
    var itemScore by mutableFloatStateOf(albumItemRelation.albumItem.score)
    var itemLabel by mutableStateOf(albumItemRelation.labelInfos)
    var imageResource by mutableStateOf(DrawableIcon.DrawableLoadFailed)
    var intrinsicRatio by mutableFloatStateOf(
        albumItemRelation.fileInfo.aspectRatio()
    )

    init {
//        Log.d("jingtian", "initHolder: ${albumItemRelation.fileInfo.fileType.name}, ${albumItemRelation.fileInfo.storageId}")
    }

    suspend fun initImage(scope: CoroutineScope) {
//        Log.d("jingtian", "initImage: ${albumItemRelation.fileInfo.fileType.name}, ${albumItemRelation.fileInfo.storageId}")
        if (imageBitmap == null) {
            fetchImage(scope)
        }
    }

    private fun FileInfo.aspectRatio(): Float {
        return (this.intrinsicWidth.toFloat() / this.intrinsicHeight.toFloat()).takeIf {
            this.intrinsicHeight > 0 && this.intrinsicWidth > 0
        } ?: 1f
    }

    private fun ImageBitmap.aspectRatio(): Float {
        return (this.width.toFloat() / this.height.toFloat()).takeIf {
            this.width > 0 && this.height > 0
        } ?: 1f
    }

    suspend fun fetchImage(scope: CoroutineScope) {
//        Log.d("jingtian", "fetchImage: ${albumItemRelation.fileInfo.fileType.name}, ${albumItemRelation.fileInfo.storageId}")
        withContext(Dispatchers.IO) {
            val uri = albumItemRelation.fileInfo.getFileUri() ?: return@withContext
            val fileType = albumItemRelation.fileInfo.fileType
            when (fileType) {
                FileType.IMAGE -> {
                    val (_, image) = BitMapCachePool.loadImage(
                        albumItemRelation.fileInfo,
                        dpSize.toInt(),
                    )
                    val bitmap = image
                    withContext(Dispatchers.Main) {
                        imageBitmap = bitmap
                        imageBitmap?.aspectRatio()?.let {
                            intrinsicRatio = it
                        }
                    }
                }

                FileType.VIDEO -> {
                    FileStorageUtils.getThumbnail(
                        albumItemRelation.fileInfo,
                        scope, uri,
                        maxWidth = dpSize.toInt(),
                    ) { bitmap ->
                        imageBitmap = bitmap
                        imageBitmap?.aspectRatio()?.let {
                            intrinsicRatio = it
                        }
                    }
                }

                FileType.AUDIO -> {
                    imageResource = DrawableIcon.DrawableMusic
                    FileStorageUtils.getThumbnail(
                        albumItemRelation.fileInfo,
                        scope, uri,
                        maxWidth = dpSize.toInt(),
                    ) { bitmap ->
                        imageBitmap = bitmap
                        imageBitmap?.aspectRatio()?.let {
                            intrinsicRatio = it
                        }
                    }
                }

                FileType.RegularFile -> {
                    intrinsicRatio = 1f
                    imageResource = DrawableIcon.DrawableFile
                    imageBitmap = null
                }

                FileType.HTML -> {
                    intrinsicRatio = 1f
                    imageResource = DrawableIcon.DrawableChrome
                    imageBitmap = null
                    FileStorageUtils.getThumbnail(
                        albumItemRelation.fileInfo,
                        scope, uri,
                        maxWidth = dpSize.toInt(),
                    ) { bitmap ->
                        imageBitmap = bitmap
                    }
                }
            }
        }
    }
}

@Composable
fun AlbumItemView(
    albumViewMap: SnapshotStateMap<Long, SoftReference<AlbumItemViewStateHolder>>,
    albumItemRelation: AlbumItemRelation,
    size: Dp,
    padding: Dp,
    currentSelectedItem: SnapshotStateMap<Long, AlbumItemRelation>,
    enterEditState: MutableState<Boolean>,
    itemSelectStateChangeState: MutableState<Long>,
    showEditDialogState: MutableState<AlbumItemRelation?>
) {
    val itemId = albumItemRelation.albumItem.itemId ?: return
    val dpSize = size.dpValue()
    val stateHolder by remember(itemId) {
        mutableStateOf(
            albumViewMap.getOrPutRef(itemId) {
//                Log.d("jingtian", "AlbumItemView: getOrPut ${itemId}, ${albumViewMap.hashCode()}")
                AlbumItemViewStateHolder(
                    albumItemRelation,
                    size,
                    dpSize,
                    padding,
                    currentSelectedItem,
                    enterEditState,
                    itemSelectStateChangeState,
                    showEditDialogState
                )
            }
        )
    }
    stateHolder.AlbumItemView()
}

@Composable
fun AlbumItemViewStateHolder.AlbumItemView() {
    val scope = rememberCoroutineScope()
    val itemId = albumItemRelation.albumItem.itemId ?: DataBase.INVALID_ID
    val isSelected = currentSelectedItem.containsKey(itemId)
    LaunchedEffect(this) {
        initImage(scope)
    }

    val fileTypeIcon by remember(albumItemRelation.fileInfo.fileType) {
        mutableStateOf(
            when (albumItemRelation.fileInfo.fileType) {
                FileType.IMAGE -> DrawableIcon.DrawablePicIcon
                FileType.VIDEO -> DrawableIcon.DrawableVideoIcon
                FileType.AUDIO -> DrawableIcon.DrawableMusicIcon
                FileType.HTML -> DrawableIcon.DrawableWebIcon
                FileType.RegularFile -> DrawableIcon.DrawableDocIcon
            }
        )
    }

    val albumItemLauncher by rememberAlbumLauncher { result->
        if (result.toInt() == -1) {
            scope.launch {
                fetchImage(scope)
            }
        }
    }

    Column(Modifier
        .width(size)
        .padding(padding)
        .pointerInput(this@AlbumItemView) {
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
                        albumItemLauncher.launch(albumItemRelation.fileInfo)
                        scope.launch(Dispatchers.IO) {
                            fetchImage(scope)
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

        Box(Modifier.fillMaxWidth()) {
            Icon(
                painter = getPainter(fileTypeIcon),
                contentDescription = "文件类型图标",
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .size(24.dp)
                    .align(Alignment.Center)
            )
            if (enterEditState.value) {
                Icon(painter = getPainter(DrawableIcon.DrawableCheck), contentDescription = "复选框",
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
                .fillMaxWidth()
        ) {
            val imageResource = imageResource
            if (currentPickedImage != null) {
                Image(
                    bitmap = currentPickedImage,
                    contentDescription = "文件缩略图",
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(intrinsicRatio)
                        .align(Alignment.Center),
                    contentScale = ContentScale.FillWidth
                )
            } else if (imageResource != null) {
                Image(
                    painter = getPainter(imageResource),
                    contentDescription = "文件缩略图",
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(intrinsicRatio)
                        .padding(6.dp)
                        .align(Alignment.Center),
                    contentScale = ContentScale.FillWidth
                )
            } else if (albumItemRelation.fileInfo.fileType == FileType.HTML) {
//                CommonWebView(Modifier
//                    .fillMaxWidth()
//                    .aspectRatioOrNull(intrinsicRatio)
//                    .align(Alignment.Center),
//                    uri = albumItemRelation.fileInfo.getFileUri(),
//                    enabled = false,
//                )
            }
            RankLabel(itemRank, padding)
        }

        AppThemeText(
            itemName,
            modifier = Modifier
                .wrapContentWidth()
                .padding(bottom = 4.dp, start = padding * 2, end = padding * 2, top = padding),
            maxLines = 2,
            style = LocalTextStyle.current.copy(
                textAlign = TextAlign.Start,
                fontSize = 16.sp,
                fontWeight = FontWeight(700)
            ),
            overflow = TextOverflow.Ellipsis
        )

        if (itemDesc.isNotBlank()) {
            OutlinedTextField(
                itemDesc,
                { value ->
                    itemDesc = value
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp, start = padding, end = padding),
                label = {
                    AppThemeText("评论")
                },
                maxLines = Int.MAX_VALUE,
                enabled = false,
                textStyle = LocalSecondaryTextStyle.current
            )
        }

        ScoreStar(itemScore, padding)

        if (itemLabel.isNotEmpty()) {
            LazyRow(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp, start = padding, end = padding)
            ) {
                items(itemLabel.size, key = { index: Int ->
                    itemLabel[index].label
                }) { index: Int ->
                    LabelView(itemLabel[index].label)
                }
            }
        }
    }
}