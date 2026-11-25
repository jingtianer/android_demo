package com.jingtian.composedemo.main.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.max
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.jingtian.composedemo.dao.model.Album
import com.jingtian.composedemo.dao.model.FileType
import com.jingtian.composedemo.dao.model.relation.AlbumItemRelation
import com.jingtian.composedemo.main.dialog.EditDialog
import com.jingtian.composedemo.main.http.api.api
import com.jingtian.composedemo.main.http.dao.HttpServerEntity
import com.jingtian.composedemo.main.http.file.HttpMultiplatformFile
import com.jingtian.composedemo.main.remote.SftpServer
import com.jingtian.composedemo.multiplatform.MultiplatformFile
import com.jingtian.composedemo.multiplatform.logD
import com.jingtian.composedemo.ui.theme.DarkWrapper
import com.jingtian.composedemo.ui.theme.clipDeviceRoundCorner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.internal.SynchronizedObject
import kotlinx.coroutines.internal.synchronized
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.random.Random

private const val PARALLEL_DOWNLOAD_COUNT = 4

abstract class FeedState {
    @Composable
    open fun Install() {}
    abstract val fileList: List<FlowFile>
    abstract val pagerState: PagerState
    abstract val album: Album
    abstract val totalLabelList: State<List<String>>
}

class FlowFeedState(
    private val itemList: State<List<AlbumItemRelation>>,
    override val album: Album,
    override val totalLabelList: State<List<String>>
): FeedState() {
    @OptIn(InternalCoroutinesApi::class)
    private val loadingLock = SynchronizedObject()
    private var candidateItemMap: MutableMap<Long?, AlbumItemRelation> by mutableStateOf(hashMapOf())
    override var fileList by mutableStateOf(mutableStateListOf<FlowFile>())
    private var loadingItemCount = 0
    override val pagerState = PagerState(0) {
        fileList.size
    }

    private fun AlbumItemRelation.key() = this.albumItem.itemId.takeIf { it != -1L }
    @OptIn(InternalCoroutinesApi::class)
    private suspend fun loadMore(scope: CoroutineScope) {
//        logD("jingtian") {
//            "candidate: fileList=${fileList.size}, candidateItemMap=${candidateItemMap.keys.size}, pagerState=${pagerState.currentPage}, loadingItemCount=${loadingItemCount}"
//        }
        val loadingItems = synchronized(loadingLock) {
            val itemMap = candidateItemMap
            if (itemMap.keys.isEmpty() || loadingItemCount >= PARALLEL_DOWNLOAD_COUNT) {
                return
            }
            val shuffledKeys = itemMap.keys.let {
                it.shuffled().subList(0, min(it.size, kotlin.math.max(0, PARALLEL_DOWNLOAD_COUNT - loadingItemCount)))
            }
//            logD("jingtian") {
//                "candidate: fileList=${fileList.size}, candidateItemMap=${candidateItemMap.keys.size}, pagerState=${pagerState.currentPage}, shuffledKeys=${shuffledKeys.toTypedArray().contentDeepToString()}, loadingItemCount=${loadingItemCount}"
//            }
            val loadingItems = buildList {
                for (key in shuffledKeys) {
                    itemMap[key]?.let {
                        add(it)
                    }
                    itemMap.remove(key)
                }
            }
            loadingItemCount += loadingItems.size
            loadingItems
        }
        for (newItem in loadingItems) {
            val newItemKey = newItem.key() ?: continue
            val uri = newItem.fileInfo.getFileUri() ?: continue
            scope.launch {
//                logD("jingtian") {
//                    "candidate: fileList=${fileList.size}, candidateItemMap=${candidateItemMap.keys.size}, pagerState=${pagerState.currentPage}, download:${newItem.albumItem.itemName}, loadingItemCount=${loadingItemCount}"
//                }

                val file = withContext(Dispatchers.Main) {
                    val file = FlowFile(
                        fileType = newItem.fileInfo.fileType,
                        key = newItemKey,
                        multiplatformFile = uri,
                        albumItemRelation = newItem,
                        isloading = true,
                    )
                    fileList.add(file)
                    file
                }
                uri.file()
                withContext(Dispatchers.Main) {
                    file.isloading = false
                }
//                logD("jingtian") {
//                    "candidate: finish fileList=${fileList.size}, candidateItemMap=${candidateItemMap.keys.size}, pagerState=${pagerState.currentPage}, download:${newItem.albumItem.itemName}, loadingItemCount=${loadingItemCount}"
//                }
                synchronized(loadingLock) {
                    loadingItemCount--
                }
            }
        }
    }

    @OptIn(InternalCoroutinesApi::class)
    @Composable
    override fun Install() {
        val scope = rememberCoroutineScope()
        LaunchedEffect(Unit, itemList.value) {
            withContext(Dispatchers.Default) {
                val currentPageId = fileList.getOrNull(pagerState.currentPage)?.albumItemRelation?.key()
                var nextPageIndex = 0
                val itemMap = itemList.value
                    .filter { it.key() != -1L && (it.fileInfo.fileType == FileType.IMAGE || it.fileInfo.fileType == FileType.VIDEO) }
                    .associateByTo(hashMapOf()) { item -> item.key() }
//                logD("jingtian") {
//                    "init: candidateItemMap=${itemMap.keys.size}"
//                }
                val nextList = mutableStateListOf<FlowFile>()
                for (idx in fileList.indices) {
                    val item = fileList[idx]
                    val newItemKey = item.albumItemRelation.key() ?: continue
                    val newItem = itemMap[newItemKey] ?: continue
                    val newFile = FlowFile(
                        fileType = newItem.fileInfo.fileType,
                        key = newItemKey,
                        multiplatformFile = newItem.fileInfo.getFileUri() ?: continue,
                        albumItemRelation = newItem,
                        isloading = item.isloading
                    )
                    if (item.isloading) {

                        scope.launch {
                            synchronized(loadingLock) {
                                loadingItemCount++
                            }
                            newItem.fileInfo.getFileUri()?.file()
                            withContext(Dispatchers.Main) {
                                newFile.isloading = false
                            }
                            synchronized(loadingLock) {
                                loadingItemCount--
                            }
                        }
                    }
                    nextList.add(newFile)
                    if (newItemKey == currentPageId) {
                        nextPageIndex = nextList.size - 1
                    }
                    itemMap.remove(newItemKey)
                }
                withContext(Dispatchers.Main) {
                    fileList = nextList
                    candidateItemMap = itemMap
//                    logD("jingtian") {
//                        "init: fileList=${fileList.size}, candidateItemMap=${candidateItemMap.keys.size}"
//                    }
                    if (fileList.isNotEmpty()) {
                        pagerState.scrollToPage(nextPageIndex)
                    }
                }
            }
        }
        LaunchedEffect(candidateItemMap, fileList) {
            scope.launch {
                loadMore(scope)
            }
        }
        LaunchedEffect(pagerState.currentPage) {
            scope.launch {
                loadMore(scope)
            }
        }
    }
}

class FlowFile(
    val fileType: FileType,
    val key: Long,
    val multiplatformFile: MultiplatformFile,
    val albumItemRelation: AlbumItemRelation,
    isloading: Boolean = true,
) {
    var isloading: Boolean by mutableStateOf(isloading)
}

class HttpFeedState(val httpServerEntity: HttpServerEntity, override val album: Album): FeedState() {
    @OptIn(InternalCoroutinesApi::class)
    private val loadingLock = SynchronizedObject()
    private val loadPageLock = Mutex()
    val itemList = mutableStateListOf<AlbumItemRelation>()
    private var candidateItemMap: MutableMap<Long?, AlbumItemRelation> by mutableStateOf(hashMapOf())
    override var fileList by mutableStateOf(mutableStateListOf<FlowFile>())
    private var loadingItemCount = 0
    override val pagerState = PagerState(0) {
        fileList.size
    }

    val api = httpServerEntity.api()

    var currentOffset = 0L

    override val totalLabelList: State<List<String>> = mutableStateOf(emptyList())

    private fun AlbumItemRelation.key() = this.albumItem.itemId.takeIf { it != -1L }
    @OptIn(InternalCoroutinesApi::class)
    private suspend fun loadMore(scope: CoroutineScope) {
//        logD("jingtian") {
//            "candidate: fileList=${fileList.size}, candidateItemMap=${candidateItemMap.keys.size}, pagerState=${pagerState.currentPage}, loadingItemCount=${loadingItemCount}"
//        }
        val loadingItems = synchronized(loadingLock) {
            val itemMap = candidateItemMap
            if (itemMap.keys.isEmpty() || loadingItemCount >= PARALLEL_DOWNLOAD_COUNT) {
                return
            }
            val shuffledKeys = itemMap.keys.let {
                it.shuffled().subList(0, min(it.size, kotlin.math.max(0, PARALLEL_DOWNLOAD_COUNT - loadingItemCount)))
            }
//            logD("jingtian") {
//                "candidate: fileList=${fileList.size}, candidateItemMap=${candidateItemMap.keys.size}, pagerState=${pagerState.currentPage}, shuffledKeys=${shuffledKeys.toTypedArray().contentDeepToString()}, loadingItemCount=${loadingItemCount}"
//            }
            val loadingItems = buildList {
                for (key in shuffledKeys) {
                    itemMap[key]?.let {
                        add(it)
                    }
                    itemMap.remove(key)
                }
            }
            loadingItemCount += loadingItems.size
            loadingItems
        }
        for (newItem in loadingItems) {
            val newItemKey = newItem.key() ?: continue
            val uri = HttpMultiplatformFile(httpServerEntity, newItem, api)
            scope.launch {
//                logD("jingtian") {
//                    "candidate: fileList=${fileList.size}, candidateItemMap=${candidateItemMap.keys.size}, pagerState=${pagerState.currentPage}, download:${newItem.albumItem.itemName}, loadingItemCount=${loadingItemCount}"
//                }

                val file = withContext(Dispatchers.Main) {
                    val file = FlowFile(
                        fileType = newItem.fileInfo.fileType,
                        key = newItemKey,
                        multiplatformFile = uri,
                        albumItemRelation = newItem,
                        isloading = true,
                    )
                    fileList.add(file)
                    file
                }
                uri.file()
                withContext(Dispatchers.Main) {
                    file.isloading = false
                }
//                logD("jingtian") {
//                    "candidate: finish fileList=${fileList.size}, candidateItemMap=${candidateItemMap.keys.size}, pagerState=${pagerState.currentPage}, download:${newItem.albumItem.itemName}, loadingItemCount=${loadingItemCount}"
//                }
                synchronized(loadingLock) {
                    loadingItemCount--
                }
            }
        }
        loadMoreList()

    }

    @OptIn(InternalCoroutinesApi::class)
    private suspend fun loadMoreList() {
        withContext(Dispatchers.IO) {
            loadPageLock.withLock {
                val albumId = album.albumId
                if (albumId != null && albumId != -1L) {
                    val loadMoreList = api.listAlbum(albumId, currentOffset)
                    withContext(Dispatchers.Main) {
                        currentOffset += loadMoreList.size
                        itemList.addAll(loadMoreList)
                    }
                }
            }
        }
    }

    @OptIn(InternalCoroutinesApi::class)
    @Composable
    override fun Install() {
        val scope = rememberCoroutineScope()
        LaunchedEffect(Unit) {
            loadMoreList()
        }
        LaunchedEffect(Unit, itemList.size) {
            withContext(Dispatchers.Default) {
                val currentPageId = fileList.getOrNull(pagerState.currentPage)?.albumItemRelation?.key()
                var nextPageIndex = 0
                val itemMap = itemList
                    .filter { it.key() != -1L && (it.fileInfo.fileType == FileType.IMAGE || it.fileInfo.fileType == FileType.VIDEO) }
                    .associateByTo(hashMapOf()) { item -> item.key() }
//                logD("jingtian") {
//                    "init: candidateItemMap=${itemMap.keys.size}"
//                }
                val nextList = mutableStateListOf<FlowFile>()
                for (idx in fileList.indices) {
                    val item = fileList[idx]
                    val newItemKey = item.albumItemRelation.key() ?: continue
                    val newItem = itemMap[newItemKey] ?: continue
                    val multiplatformFile = HttpMultiplatformFile(httpServerEntity, newItem, api)
                    val newFile = FlowFile(
                        fileType = newItem.fileInfo.fileType,
                        key = newItemKey,
                        multiplatformFile = multiplatformFile,
                        albumItemRelation = newItem,
                        isloading = item.isloading
                    )
                    if (item.isloading) {
                        scope.launch {
                            synchronized(loadingLock) {
                                loadingItemCount++
                            }
                            multiplatformFile.file()
                            withContext(Dispatchers.Main) {
                                newFile.isloading = false
                            }
                            synchronized(loadingLock) {
                                loadingItemCount--
                            }
                        }
                    }
                    nextList.add(newFile)
                    if (newItemKey == currentPageId) {
                        nextPageIndex = nextList.size - 1
                    }
                    itemMap.remove(newItemKey)
                }
                withContext(Dispatchers.Main) {
                    fileList = nextList
                    candidateItemMap = itemMap
//                    logD("jingtian") {
//                        "init: fileList=${fileList.size}, candidateItemMap=${candidateItemMap.keys.size}"
//                    }
                    if (fileList.isNotEmpty()) {
                        pagerState.scrollToPage(nextPageIndex)
                    }
                }
            }
        }
        LaunchedEffect(candidateItemMap, fileList) {
            scope.launch {
                loadMore(scope)
            }
        }
        LaunchedEffect(pagerState.currentPage) {
            scope.launch {
                loadMore(scope)
            }
        }
    }
}

@Composable
fun HttpFeed(
    httpServerEntity: HttpServerEntity,
    album: Album,
) {
    val httpFeedState = remember(httpServerEntity, album) { HttpFeedState(httpServerEntity, album) }
    FlowFeed(httpFeedState, enableEdit = false)
}

@Composable
fun FlowFeed(
    itemList: State<List<AlbumItemRelation>>,
    album: Album,
    totalLabelList: State<List<String>>
) {
    val feedState = remember(itemList, album, totalLabelList) { FlowFeedState(itemList, album, totalLabelList) }
    FlowFeed(feedState)
}

@Composable
private fun FlowFeed(
    feedState: FeedState, enableEdit: Boolean = true
) {

    with(feedState) {
        Install()

        suspend fun scrollToNextPage(index: Int) {
            withFrameMillis { }
            if (index + 1 < fileList.size) {
                pagerState.animateScrollToPage(index + 1)
            }
        }
        if (fileList.isEmpty()) {
            DarkWrapper {
                Box(
                    modifier = Modifier.fillMaxSize().clipDeviceRoundCorner().background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Text("loading...")
                }
            }
            return
        }
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize().clipDeviceRoundCorner().background(Color.Black),
            beyondViewportPageCount = 10,
            key = { index -> fileList[index].key }
        ) { index ->
            val item = fileList[index]
            if (!item.isloading) {
                when (item.fileType) {
                    FileType.IMAGE -> {
                        feedState.ImageFeed(item, index, enableEdit) {
                            scrollToNextPage(index)
                        }
                    }

                    FileType.VIDEO -> {
                        feedState.VideoFeed(item, index, enableEdit) {
                            scrollToNextPage(index)
                        }
                    }

                    else -> {
                        Box(
                            modifier = Modifier.fillMaxSize().background(color = Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            Column {
                                DarkWrapper {
                                    Text("error unknown filetype=${item.fileType.name}")
                                }
                            }
                        }
                        LaunchedEffect(item, index) {
                            delay(10000L)
                            scrollToNextPage(index)
                        }
                    }
                }
            } else {
                DarkWrapper {
                    Box(
                        modifier = Modifier.fillMaxSize().clipDeviceRoundCorner().background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("loading...\n${item.albumItemRelation.albumItem.itemName}")
                    }
                }
            }
        }
    }
}

@Composable
fun FeedState.ImageFeed(flowFile: FlowFile, index: Int, enableEdit: Boolean = true, onPlayFinish: suspend () -> Unit) {
    val imagePlayerState = rememberImagePlayerState(initialShowMask = false)
    var isEditing by remember { mutableStateOf(false) }
    val isSelected = index == pagerState.currentPage
    LaunchedEffect(
        isSelected,
        imagePlayerState.maskVisible,
        isEditing,
        imagePlayerState.isLoading,
        index
    ) {
        if (!imagePlayerState.maskVisible && isSelected && !isEditing && !imagePlayerState.isLoading) {
            delay(10000L)
            onPlayFinish()
        }
    }
//    LaunchedEffect(Unit, isSelected, imagePlayerState.isLoading) {
//        if (isSelected && !imagePlayerState.isLoading) {
//            imagePlayerState.showMask = true
//        }
//    }
    ImagePlayer(
        state = imagePlayerState,
        modifier = Modifier.fillMaxSize(),
        uri = flowFile.multiplatformFile,
        onEditRequest = {
            isEditing = true
        },
        maskContent = {
            DarkWrapper {
                FileExtraInfoView(albumItemRelation = flowFile.albumItemRelation)
            }
        })

    if (isEditing && enableEdit) {
        EditDialog(flowFile.albumItemRelation, album, listOf(album), totalLabelList.value) {
            isEditing = false
        }
    }
}

@Composable
fun FeedState.VideoFeed(flowFile: FlowFile, index: Int, enableEdit: Boolean = true, onPlayFinish: suspend () -> Unit) {
    val playerState = rememberUIVideoPlayerState(initialPlaying = false, initialShowMask = false)
    var isEditing by remember { mutableStateOf(false) }
    var isAlive by remember { mutableStateOf(true) }
    val isSelected = index == pagerState.currentPage
    LaunchedEffect(Unit, isSelected, playerState.isLoading, isAlive) {
        if (isSelected && !playerState.isLoading && isAlive) {
            playerState.playerState.play()
//            playerState.showMask = true
        } else {
            playerState.playerState.pause()
        }
    }
    LaunchedEffect(
        playerState.playerState.sliderPos,
        isSelected,
        playerState.maskVisible,
        isEditing
    ) {
        if (isSelected && !isEditing && playerState.playerState.isPlaying && !playerState.maskVisible && playerState.playerState.sliderPos >= 1000f) {
            onPlayFinish()
            playerState.playerState.stop()
        }
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(Unit) {
        val observer = object : DefaultLifecycleObserver {
            override fun onPause(owner: LifecycleOwner) {
                isAlive = false
            }

            override fun onResume(owner: LifecycleOwner) {
                isAlive = true
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    VideoPlayer(
        uiVideoPlayerState = playerState,
        modifier = Modifier.fillMaxSize(),
        uri = flowFile.multiplatformFile,
        onEditRequest = {
            isEditing = true
        },
        maskContent = {
            DarkWrapper {
                FileExtraInfoView(albumItemRelation = flowFile.albumItemRelation)
            }
        })

    if (isEditing && enableEdit) {
        EditDialog(flowFile.albumItemRelation, album, listOf(album), totalLabelList.value) {
            isEditing = false
        }
    }
}