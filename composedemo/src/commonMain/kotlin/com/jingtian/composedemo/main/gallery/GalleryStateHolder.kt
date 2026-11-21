package com.jingtian.composedemo.main.gallery

import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.unit.dp
import com.jingtian.composedemo.dao.model.Album
import com.jingtian.composedemo.dao.model.FileType
import com.jingtian.composedemo.dao.model.ItemRank
import com.jingtian.composedemo.dao.model.relation.AlbumItemRelation
import com.jingtian.composedemo.main.albumItem.AlbumItemViewStateHolder
import com.jingtian.composedemo.main.remote.ImportRemoteDialogStateHolder
import com.jingtian.composedemo.main.widget.SimpleNavHostController
import com.jingtian.composedemo.multiplatform.SoftRef
import com.jingtian.composedemo.viewmodels.AlbumViewModel
import com.jingtian.composedemo.viewmodels.notifyChange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max

class GalleryDpConstants(
    val scrollBarWidthDp: Float,
    val scrollBarHeightDp: Float,
    val scrollAreaWidthDp : Float
)

interface IPlatformExtra

expect fun getPlatformExtra(): IPlatformExtra

class GalleryStateHolder(val album: IndexedValue<Album>, val currentSelectedAlbum: State<Album>, val albumList: List<Album>, val viewModel: AlbumViewModel, val galleryDpConstants: GalleryDpConstants, val navController : SimpleNavHostController) {
    var addImageDialogState by mutableStateOf(false)
    var itemList by mutableStateOf(emptyList<AlbumItemRelation>())
    var showLabelFilter by mutableStateOf(false)
    var totalLabelListState = mutableStateOf(emptyList<String>())
    var totalLabelList by totalLabelListState
    val totalFileTypeList = FileType.entries.map { it.typeName }
    val totalItemRankList = ItemRank.entries.map { it.name }
    val labelFilterCheckedInfo = mutableStateMapOf<String, Boolean>()
    val fileTypeCheckState = mutableStateMapOf<String, Boolean>()
    val itemRankTypeCheckState = mutableStateMapOf<String, Boolean>()
    var albumName by mutableStateOf(album.value.albumName)
    val currentSelectedItem = mutableStateMapOf<Long, AlbumItemRelation>()
    val itemSelectStateChangeState = mutableLongStateOf(0L)
    var itemSelectStateChange by itemSelectStateChangeState
    val currentFunctions = mutableStateListOf<GalleryFunctions>()
    val platformFunctions = mutableStateListOf<GalleryFunctions>()
    var headerHeight by mutableFloatStateOf(0f)
    val enterEditModeState = mutableStateOf(false)
    var enterEditMode by enterEditModeState
    var editAlbumDialogState by mutableStateOf(false)
    var importDocumentTreeDialog by mutableStateOf(false)
    var showEditDialogStateOnLongClickState = mutableStateOf<AlbumItemRelation?>(null)
    var showEditDialogOnLongClick by showEditDialogStateOnLongClickState

    var selectAll by mutableStateOf(false)
    var selectNone by mutableStateOf(false)
    val size = 160.dp
    val galleryItemPadding = 4.dp
    var defaultScrollOffsetY by mutableFloatStateOf(0f)
    val searchScrollOffsetY by lazy {
        mutableFloatStateOf(0f)
    }
    val scrollBarSize = mutableStateListOf(galleryDpConstants.scrollBarWidthDp, galleryDpConstants.scrollBarHeightDp)
    val scrollAreaWidth = 28.dp
    val scrollBarOffset = mutableStateListOf(galleryDpConstants.scrollAreaWidthDp - scrollBarSize[0], 0f)
    val defaultGalleryScrollState = LazyStaggeredGridState(0, 0)
    val searchGalleryScrollState by lazy { LazyStaggeredGridState(0, 0) }
    val galleryScrollState get() = if (gallerySearchEnabled) searchGalleryScrollState else defaultGalleryScrollState

    var showEditDialog by mutableStateOf(false)
    var showConfirmDeleteDialog by mutableStateOf(false)
    var showMoveToDialog by mutableStateOf(false)

    var showImportRemoteDialog = mutableStateOf(false)
    var showDeleteRemoteFileConfirmDialog = mutableStateOf(false)
    var showImportRemoteDialogStateHolder = mutableStateOf(ImportRemoteDialogStateHolder())

    val albumViewMap = mutableStateMapOf<Triple<Long, Long, Long>, SoftRef<AlbumItemViewStateHolder>>()

    val filterConfig = mutableStateOf(FilterConfig())
    var gallerySearchWord by mutableStateOf("")
    var gallerySearchEnabled by mutableStateOf(false)

    var scrollOffsetY = if (gallerySearchEnabled) searchScrollOffsetY.value else defaultScrollOffsetY
        get() = if (gallerySearchEnabled) searchScrollOffsetY.value else defaultScrollOffsetY
        set(value) {
            field = value
            if (gallerySearchEnabled) {
                searchScrollOffsetY.value = value
            } else {
                defaultScrollOffsetY = value
            }
        }

    fun updateGallerySearchWord(searchWord: String) {
        gallerySearchWord = searchWord
    }

    fun AlbumItemRelation.filter(): Boolean {
        val item = this
        val labelFilterCheckedInfo = labelFilterCheckedInfo
        val labelCheck = labelFilterCheckedInfo.isNullOrEmpty() || item.labelInfos.map { it.label }.toSet().let {
            if (filterConfig.value.labelOr.value) {
                it.intersect(labelFilterCheckedInfo.keys).isNotEmpty()
            } else {
//                        logD("updateFilterList") {
//                            "checked: ${labelFilterCheckedInfo.keys.toTypedArray().contentDeepToString()}, it = ${it.toTypedArray().contentDeepToString()} minus=${labelFilterCheckedInfo.keys.minus(it)}"
//                        }
                labelFilterCheckedInfo.keys.minus(it).isEmpty()
            }
        }
        val fileTypeCheck = fileTypeCheckState.isEmpty() || fileTypeCheckState.get(item.fileInfo.fileType.typeName) == true
        val itemRankCheck = itemRankTypeCheckState.isEmpty() || itemRankTypeCheckState.get(item.albumItem.rank.name) == true
        return labelCheck && fileTypeCheck && itemRankCheck
    }

    private val _filteredItemList = mutableStateListOf<AlbumItemRelation>()

    val filteredItemListState = derivedStateOf {
        itemList.filter { it.filter() }.let {
            if (gallerySearchEnabled && gallerySearchWord.isNotBlank()) {
                it.sortedByDescending { item ->
                    calcSearchScore(gallerySearchWord, item.albumItem.itemName)
                }
            } else {
                it
            }
        }
    }

    val filteredItemList by filteredItemListState


    suspend fun updateScrollItem() {
        val y = scrollOffsetY
        val totalHeight = galleryScrollState.layoutInfo.viewportSize.height.toFloat()
        val totalItemCount = galleryScrollState.layoutInfo.totalItemsCount
        val scrollPercent = (y / totalHeight) * totalItemCount
        val targetItem = scrollPercent
            .toInt()
            .coerceIn(0, totalItemCount)
//        Log.d("jingtian", "updateScrollOffset: $y, $totalHeight, $scrollPercent, $targetItem")
        galleryScrollState.scrollToItem(targetItem)
    }

    fun updateScrollOffset() {
        val y = scrollOffsetY - scrollBarSize[1] / 2
//        Log.d("jingtian", "updateScrollOffset: $y")
//        println("updateScrollOffset ${galleryScrollState.layoutInfo.viewportSize.height.toFloat() - scrollBarSize[1]}, ${galleryScrollState.layoutInfo.viewportSize.height.toFloat()}, ${scrollBarSize[1]}")
        scrollBarOffset[1] = y.coerceIn(0f, max(0f, galleryScrollState.layoutInfo.viewportSize.height.toFloat() - scrollBarSize[1]))
    }

    init {
        viewModel.viewModelScope.launch {
            initAlbumInfo()
        }
    }

    private suspend fun initAlbumInfo() {
        albumName = album.value.albumName
        onAlbumDataChanged()
    }

    private fun <T> SnapshotStateList<T>. safeReplace(list: List<T>) {
        val oldSize = this.size
        this.addAll(list)
        this.removeRange(0, oldSize)
    }

    suspend fun onAlbumDataChanged() {
        withContext(Dispatchers.IO) {
            val albumItems = viewModel.getAllAlbumItem(album.value)
            val labelList = viewModel.getLabelList(album.value)
            withContext(Dispatchers.Main) {
                albumViewMap.clear()
                currentSelectedItem.clear()
                itemList = albumItems
                totalLabelList = labelList
//            viewModel.filterCheckChanged.notifyChange()
                albumName = album.value.albumName
                itemSelectStateChange++
            }
        }
    }

    suspend fun onAlbumNameChanged() {
        withContext(Dispatchers.IO) {
            val newAlbum = viewModel.getAlbumName(album.value.albumId ?: return@withContext)
            album.value.albumName = newAlbum.albumName
            withContext(Dispatchers.Main) {
                albumName = newAlbum.albumName
            }
        }
    }

    val backEnable: Boolean get() = if (enterEditMode) {
        true
    } else if (gallerySearchEnabled) {
        true
    } else {
        false
    }

    fun onBackPressed(): Boolean {
        return if (enterEditMode) {
            enterEditMode = false
            true
        } else if (gallerySearchEnabled) {
            gallerySearchEnabled = false
            true
        } else {
            false
        }
    }
}
