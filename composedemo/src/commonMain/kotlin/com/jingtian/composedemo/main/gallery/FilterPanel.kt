package com.jingtian.composedemo.main.gallery

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.staggeredgrid.LazyHorizontalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jingtian.composedemo.base.AppThemeText
import com.jingtian.composedemo.ui.theme.LocalAppPalette
import com.jingtian.composedemo.ui.theme.LocalAppUIConstants
import com.jingtian.composedemo.ui.theme.goldenRatio
import com.jingtian.composedemo.viewmodels.AlbumViewModel
import com.jingtian.composedemo.viewmodels.AlbumViewModel.Companion.notifyChange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterPanel(
    sheetState: SheetState,
    fileTypeCheckStateList: SnapshotStateMap<String, Boolean>,
    fileTypeList: List<String>,
    itemRankCheckStateList: SnapshotStateMap<String, Boolean>,
    itemRankList: List<String>,
    labelCheckStateList: SnapshotStateMap<String, Boolean>?,
    labelList: List<String>,
    onDismiss: () -> Unit,
) {
    val horizontalPadding = 6.dp
    val verticalPadding = 6.dp
    val horizontalInnerPadding = LocalAppUIConstants.current.filterLabelPaddings[2]
    val viewModel: AlbumViewModel = viewModel(factory = AlbumViewModel.viewModelFactory)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(sqrt(goldenRatio)),
        containerColor = LocalAppPalette.current.bottomSheetBackgroundColor,
        contentWindowInsets = { WindowInsets.navigationBars }
    ) {
        val scope = rememberCoroutineScope()

        LazyVerticalGrid(
            columns = GridCells.Adaptive(LocalAppUIConstants.current.filterLabelHeight * LocalAppUIConstants.current.filterLabelAspectRatio),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .weight(1f)
                .padding(horizontal = horizontalPadding)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                AppThemeText(
                    text = "类型筛选",
                    Modifier.padding(
                        horizontal = horizontalInnerPadding,
                        vertical = verticalPadding
                    ),
                    style = LocalTextStyle.current.copy(
                        fontWeight = FontWeight(600),
                        fontSize = 16.sp
                    )
                )
            }
            items(fileTypeList.size, key = { index -> fileTypeList[index] }) { index ->
                RoundRectCheckableLabel(
                    fileTypeList[index],
                    fileTypeCheckStateList[fileTypeList[index]] ?: false,
                    fileTypeCheckStateList,
                    false,
                )
            }
            item(span = { GridItemSpan(this.maxLineSpan) }) {
                AppThemeText(
                    text = "排行筛选",
                    Modifier.padding(
                        horizontal = horizontalInnerPadding,
                        vertical = verticalPadding
                    ),
                    style = LocalTextStyle.current.copy(
                        fontWeight = FontWeight(600),
                        fontSize = 16.sp
                    )
                )
            }
            items(itemRankList.size, key = { index -> itemRankList[index] }) { index ->
                RoundRectCheckableLabel(
                    itemRankList[index],
                    itemRankCheckStateList[itemRankList[index]] ?: false,
                    itemRankCheckStateList,
                    false,
                )
            }
            if (labelCheckStateList != null && labelList.isNotEmpty()) {
                item(span = { GridItemSpan(this.maxLineSpan) }) {
                    AppThemeText(
                        text = "标签筛选",
                        Modifier.padding(
                            horizontal = horizontalInnerPadding,
                            vertical = verticalPadding
                        ),
                        style = LocalTextStyle.current.copy(
                            fontWeight = FontWeight(600),
                            fontSize = 16.sp
                        )
                    )
                }
                item(span = { GridItemSpan(this.maxLineSpan) }) {

                    val labelItemHeight =
                        LocalAppUIConstants.current.filterLabelHeight + (LocalAppUIConstants.current.filterLabelPaddings[1] + LocalAppUIConstants.current.filterLabelPaddings[3]) * 2
                    LazyHorizontalStaggeredGrid(
//            StaggeredGridCells.Adaptive(labelItemHeight),
                        StaggeredGridCells.Fixed(3),
                        Modifier
                            .height(labelItemHeight * 3)
                            .fillMaxWidth(),
                        horizontalItemSpacing = horizontalInnerPadding,
                    ) {
                        items(labelList.size, { index -> labelList[index] }) { index ->
                            val item = labelList[index]
                            RoundRectCheckableLabel(
                                item,
                                labelCheckStateList[item] ?: false,
                                labelCheckStateList,
                                true,
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding)
        ) {
            Button(
                onClick = {
                    scope.launch {
                        withContext(Dispatchers.Default) {
                            fun SnapshotStateMap<String, Boolean>.reverseList(totalList: List<String>) {
                                val reveredList = totalList.toSet() - this.keys
                                this.clear()
                                this.putAll(reveredList.map { it to true })
                            }
                            fileTypeCheckStateList.reverseList(fileTypeList)
                            itemRankCheckStateList.reverseList(itemRankList)
                            labelCheckStateList?.reverseList(labelList)
                        }
                        viewModel.filterCheckChanged.notifyChange()
                    }
                },
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .weight(1f)
                    .padding(horizontal = horizontalInnerPadding)
            ) {
                AppThemeText(text = "反转")
            }
            Button(
                onClick = {
                    scope.launch {
                        withContext(Dispatchers.Default) {
                            fileTypeCheckStateList.clear()
                            itemRankCheckStateList.clear()
                            labelCheckStateList?.clear()
                        }
                        viewModel.filterCheckChanged.notifyChange()
                    }
                },
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .weight(1f)
                    .padding(horizontal = horizontalInnerPadding)
            ) {
                AppThemeText(text = "清空")
            }
            Button(
                onClick = {
                    onDismiss()
                },
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .weight(1f)
                    .padding(horizontal = horizontalInnerPadding)
            ) {
                AppThemeText(text = "确认")
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}