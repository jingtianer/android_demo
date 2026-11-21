package com.jingtian.composedemo.main.viewer

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.jingtian.composedemo.dao.model.Album
import com.jingtian.composedemo.dao.model.FileType
import com.jingtian.composedemo.dao.model.relation.AlbumItemRelation
import com.jingtian.composedemo.dao.model.relation.AlbumRelation
import com.jingtian.composedemo.main.dialog.EditDialog
import com.jingtian.composedemo.multiplatform.MultiplatformFile
import com.jingtian.composedemo.ui.theme.appBackground
import com.jingtian.composedemo.ui.theme.clipDeviceRoundCorner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FeedState(val albumList: List<AlbumItemRelation>, val album: Album, val totalLabelList: List<String>) {
    // todo: fix albumList顺序变，会导致VerticalPager顺序变
    val shuffleIndex = IntArray(albumList.size) { it }.filter { albumList[it].fileInfo.fileType == FileType.VIDEO || albumList[it].fileInfo.fileType == FileType.IMAGE }.shuffled()
    val pagerState = PagerState(0) {
        shuffleIndex.size
    }
}

@Composable
fun Feed(albumItemList: List<AlbumItemRelation>, album: Album, totalLabelList: List<String>) {
    val feedState = remember(albumItemList, album, totalLabelList) { FeedState(albumItemList, album, totalLabelList) }
    val scope = rememberCoroutineScope()
    with(feedState) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize().clipDeviceRoundCorner().appBackground(),
            beyondViewportPageCount = 10,
            key = { index-> albumList[shuffleIndex[index]].albumItem.itemId ?: -1 }
        ) { index->
            val item = albumList[shuffleIndex[index]]
            when(item.fileInfo.fileType) {
                FileType.IMAGE -> {
                    ImageFeed(item, index == pagerState.currentPage) {
                        scope.launch {
                            pagerState.animateScrollToPage((index + 1) % shuffleIndex.size)
                        }
                    }
                }
                FileType.VIDEO -> {
                    VideoFeed(item, index == pagerState.currentPage) {
                        scope.launch {
                            pagerState.animateScrollToPage((index + 1) % shuffleIndex.size)
                        }
                    }
                }
                else -> {

                }
            }
        }
    }
}

@Composable
fun FeedState.ImageFeed(albumItemRelation: AlbumItemRelation, isSelected: Boolean, onPlayFinish: () -> Unit) {
    val imagePlayerState = rememberImagePlayerState()
    var isEditing by remember { mutableStateOf(false) }
    LaunchedEffect(isSelected, imagePlayerState.maskVisible, isEditing) {
        if (!imagePlayerState.maskVisible && isSelected && !isEditing) {
            delay(10000L)
            onPlayFinish()
        }
    }
    ImagePlayer(state = imagePlayerState, modifier = Modifier.fillMaxSize(), uri = albumItemRelation) {
        isEditing = true
    }

    if (isEditing) {
        EditDialog(albumItemRelation, album, listOf(album), totalLabelList) {
            isEditing = false
        }
    }
}

@Composable
fun FeedState.VideoFeed(albumItemRelation: AlbumItemRelation, isSelected: Boolean, onPlayFinish: () -> Unit) {
    val playerState = rememberUIVideoPlayerState(initialPlaying = false)
    var isEditing by remember { mutableStateOf(false) }
    LaunchedEffect(Unit, isSelected) {
        if (isSelected) {
            playerState.playerState.play()
        } else {
            playerState.playerState.pause()
        }
    }
    LaunchedEffect(playerState.playerState.sliderPos, isSelected, playerState.maskVisible, isEditing) {
        if (isSelected && !isEditing && playerState.playerState.isPlaying && !playerState.maskVisible && playerState.playerState.sliderPos >= 1000f) {
            onPlayFinish()
            playerState.playerState.stop()
        }
    }
    VideoPlayer(uiVideoPlayerState = playerState, modifier = Modifier.fillMaxSize(), uri = albumItemRelation) {
        isEditing = true
    }

    if (isEditing) {
        EditDialog(albumItemRelation, album, listOf(album), totalLabelList) {
            isEditing = false
        }
    }
}