package com.jingtian.composedemo.main.viewer

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import kotlinx.coroutines.withContext

class FeedState(albumList: List<AlbumItemRelation>) {
    val shuffledAlbumList = albumList.filter { it.fileInfo.fileType == FileType.VIDEO || it.fileInfo.fileType == FileType.IMAGE }.shuffled()
}

@Composable
fun Feed(albumItemList: List<AlbumItemRelation>) {
    val feedState = remember(albumItemList) { FeedState(albumItemList) }
    val pagerState = rememberPagerState(initialPage = 0) { feedState.shuffledAlbumList.size }
    VerticalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize().clipDeviceRoundCorner().appBackground(),
        beyondViewportPageCount = 10,
    ) { index->
        val item = feedState.shuffledAlbumList[index]
        when(item.fileInfo.fileType) {
            FileType.IMAGE -> {
                ImageFeed(item, index == pagerState.currentPage)
            }
            FileType.VIDEO -> {
                VideoFeed(item, index == pagerState.currentPage)
            }
            else -> {

            }
        }
    }
}

@Composable
fun ImageFeed(albumItemRelation: AlbumItemRelation, isSelected: Boolean) {
    var uri: MultiplatformFile? by remember { mutableStateOf(null) }
    LaunchedEffect(albumItemRelation) {
        uri = withContext(Dispatchers.IO) {
            albumItemRelation.fileInfo.getFileUri()
        }
    }
    val uriSnapshot = uri ?: return
    ImagePlayer(modifier = Modifier.fillMaxSize(), uri = uriSnapshot)
}

@Composable
fun VideoFeed(albumItemRelation: AlbumItemRelation, isSelected: Boolean) {
    var uri: MultiplatformFile? by remember { mutableStateOf(null) }
    LaunchedEffect(albumItemRelation) {
        uri = withContext(Dispatchers.IO) {
            albumItemRelation.fileInfo.getFileUri()
        }
    }
    val uriSnapshot = uri ?: return
    val playerState = rememberUIVideoPlayerState(initialPlaying = false)
    LaunchedEffect(Unit, isSelected) {
        if (isSelected) {
            playerState.playerState.play()
        } else {
            playerState.playerState.pause()
        }
    }
    VideoPlayer(uiVideoPlayerState = playerState, modifier = Modifier.fillMaxSize(), uri = uriSnapshot)
}