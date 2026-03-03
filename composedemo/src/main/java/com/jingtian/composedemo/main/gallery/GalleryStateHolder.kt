package com.jingtian.composedemo.main.gallery

import android.util.Log
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.material3.DrawerState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import com.jingtian.composedemo.dao.model.Album
import com.jingtian.composedemo.dao.model.FileType
import com.jingtian.composedemo.dao.model.ItemRank
import com.jingtian.composedemo.dao.model.relation.AlbumItemRelation
import com.jingtian.composedemo.utils.ViewUtils.dpValue
import com.jingtian.composedemo.viewmodels.AlbumViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GalleryStateHolder(val album: IndexedValue<Album>, val albumList: List<Album>, val drawerState: DrawerState, val viewModel: AlbumViewModel) {
    var addImageDialogState by mutableStateOf(false)
    var itemList by mutableStateOf(emptyList<AlbumItemRelation>())
    val filteredItemList = mutableStateListOf<AlbumItemRelation>()
    var showLabelFilter by mutableStateOf(false)
    val totalLabelList = mutableStateListOf<String>()
    val totalFileTypeList = FileType.entries.map { it.typeName }
    val totalItemRankList = ItemRank.entries.map { it.name }
    val labelFilterCheckedInfo = mutableStateMapOf<String, Boolean>()
    val fileTypeCheckState = mutableStateMapOf<String, Boolean>()
    val itemRankTypeCheckState = mutableStateMapOf<String, Boolean>()
    var albumName by mutableStateOf(album.value.albumName)
    val currentSelectedItem = mutableStateMapOf<Long, AlbumItemRelation>()
    val itemSelectStateChangeState = mutableLongStateOf(0L)
    var itemSelectStateChange by itemSelectStateChangeState
    val currentFunctions = mutableStateMapOf<GalleryFunctions, GalleryFunctions>()
    val enterEditModeState = mutableStateOf(false)
    var enterEditMode by enterEditModeState
    var editAlbumDialogState by mutableStateOf(false)
    var showEditDialogStateOnLongClickState = mutableStateOf<AlbumItemRelation?>(null)
    var showEditDialogOnLongClick by showEditDialogStateOnLongClickState

    var selectAll by mutableStateOf(false)
    var selectNone by mutableStateOf(false)
    val size = 160.dp
    val galleryItemPadding = 4.dp
    var scrollOffsetY by mutableFloatStateOf(0f)
    val scrollBarSize = mutableStateListOf(6.dp.dpValue, 64.dp.dpValue)
    val scrollAreaWidth = 28.dp
    val scrollBarOffset = mutableStateListOf(scrollAreaWidth.dpValue - scrollBarSize[0], 0f)
    val galleryScrollState = LazyStaggeredGridState(0, 0)

    var showEditDialog by mutableStateOf(false)
    var showConfirmDeleteDialog by mutableStateOf(false)
    var showMoveToDialog by mutableStateOf(false)

    suspend fun updateFilterList() {
        withContext(Dispatchers.Default) {
            val labelFilterCheckedInfo = labelFilterCheckedInfo
            val filteredList = mutableListOf<AlbumItemRelation>()
            for (item in itemList) {
                val labelCheck = labelFilterCheckedInfo.isNullOrEmpty() || item.labelInfos.map { it.label }.intersect(labelFilterCheckedInfo.keys).isNotEmpty()
                val fileTypeCheck = fileTypeCheckState.isEmpty() || fileTypeCheckState.get(item.fileInfo.fileType.typeName) == true
                val itemRankCheck = itemRankTypeCheckState.isEmpty() || itemRankTypeCheckState.get(item.albumItem.rank.name) == true
                if (labelCheck && fileTypeCheck && itemRankCheck) {
                    filteredList.add(item)
                }
            }
            withContext(Dispatchers.Main) {
                filteredItemList.clear()
                filteredItemList.addAll(filteredList)
            }
        }
    }


    suspend fun updateScrollItem() {
        val y = scrollOffsetY
        val totalHeight = galleryScrollState.layoutInfo.viewportSize.height.toFloat()
        val totalItemCount = galleryScrollState.layoutInfo.totalItemsCount
        val scrollPercent = (y / totalHeight) * totalItemCount
        val targetItem = scrollPercent
            .toInt()
            .coerceIn(0, totalItemCount)
        Log.d("jingtian", "updateScrollOffset: $y, $totalHeight, $scrollPercent, $targetItem")
        galleryScrollState.scrollToItem(targetItem)
    }

    fun updateScrollOffset() {
        val y = scrollOffsetY - scrollBarSize[1] / 2
        Log.d("jingtian", "updateScrollOffset: $y")
        scrollBarOffset[1] = y.coerceIn(0f, galleryScrollState.layoutInfo.viewportSize.height.toFloat() - scrollBarSize[1])
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

    suspend fun onAlbumDataChanged() {
        withContext(Dispatchers.IO) {
            viewModel.getAllAlbumItem(album.value).collect {
                withContext(Dispatchers.Main) {
                    itemList = it
                }
            }
        }
        viewModel.getLabelList(album.value).collect { value->
//            val checkList = SnapshotStateMap<String, Boolean>()
//            labelFilterCheckedInfo.let {
//                for ((k, v) in it) {
//                    if (v) {
//                        checkList[k] = true
//                    }
//                }
//            }
            withContext(Dispatchers.Main) {
                albumName = album.value.albumName
//                labelFilterCheckedInfo = checkList
                totalLabelList.clear()
                totalLabelList.addAll(value)
            }
            updateFilterList()
            currentSelectedItem.clear()
            itemSelectStateChange++
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
}