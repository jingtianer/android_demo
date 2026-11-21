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
import com.jingtian.composedemo.multiplatform.MultiplatformFile
import com.jingtian.composedemo.ui.theme.appBackground
import com.jingtian.composedemo.ui.theme.clipDeviceRoundCorner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FeedState(albumList: List<AlbumItemRelation>) {
    val shuffledAlbumList = albumList.filter { it.fileInfo.fileType == FileType.VIDEO || it.fileInfo.fileType == FileType.IMAGE }.shuffled()
    val pagerState = PagerState(0) {
        shuffledAlbumList.size
    }
}

@Composable
fun Feed(albumItemList: List<AlbumItemRelation>) {
    val feedState = remember(albumItemList) { FeedState(albumItemList) }
    val scope = rememberCoroutineScope()
    with(feedState) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize().clipDeviceRoundCorner().appBackground(),
            beyondViewportPageCount = 10,
        ) { index->
            val item = shuffledAlbumList[index]
            when(item.fileInfo.fileType) {
                FileType.IMAGE -> {
                    ImageFeed(item, index == pagerState.currentPage) {
                        if (index + 1 < shuffledAlbumList.size) {
                            scope.launch {
                                pagerState.scrollToPage(index + 1)
                            }
                        }
                    }
                }
                FileType.VIDEO -> {
                    VideoFeed(item, index == pagerState.currentPage) {
                        if (index + 1 < shuffledAlbumList.size) {
                            scope.launch {
                                pagerState.scrollToPage(index + 1)
                            }
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
fun ImageFeed(albumItemRelation: AlbumItemRelation, isSelected: Boolean, onPlayFinish: () -> Unit) {
    val imagePlayerState = rememberImagePlayerState()
    LaunchedEffect(isSelected, imagePlayerState.maskVisible) {
        if (!imagePlayerState.maskVisible && isSelected) {
            delay(10000L)
            onPlayFinish()
        }
    }
    ImagePlayer(state = imagePlayerState, modifier = Modifier.fillMaxSize(), uri = albumItemRelation)
}

@Composable
fun VideoFeed(albumItemRelation: AlbumItemRelation, isSelected: Boolean, onPlayFinish: () -> Unit) {
    val playerState = rememberUIVideoPlayerState(initialPlaying = false)
    LaunchedEffect(Unit, isSelected) {
        if (isSelected) {
            playerState.playerState.play()
        } else {
            playerState.playerState.pause()
        }
    }
    LaunchedEffect(playerState.playerState.sliderPos, isSelected, playerState.maskVisible) {
        if (isSelected && playerState.playerState.isPlaying && !playerState.maskVisible && playerState.playerState.sliderPos >= 1000f) {
            onPlayFinish()
            playerState.playerState.stop()
        }
    }
    VideoPlayer(uiVideoPlayerState = playerState, modifier = Modifier.fillMaxSize(), uri = albumItemRelation)
}