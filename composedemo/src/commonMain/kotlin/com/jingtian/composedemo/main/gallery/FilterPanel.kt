package com.jingtian.composedemo.main.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.staggeredgrid.LazyHorizontalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jingtian.composedemo.base.AppThemeBasicTextField
import com.jingtian.composedemo.base.AppThemeText
import com.jingtian.composedemo.base.resources.DrawableIcon
import com.jingtian.composedemo.base.resources.getPainter
import com.jingtian.composedemo.multiplatform.logD
import com.jingtian.composedemo.ui.theme.LocalAppPalette
import com.jingtian.composedemo.ui.theme.LocalAppUIConstants
import com.jingtian.composedemo.ui.theme.goldenRatio
import com.jingtian.composedemo.utils.splitBy
import com.jingtian.composedemo.viewmodels.AlbumViewModel
import com.jingtian.composedemo.viewmodels.AlbumViewModel.Companion.notifyChange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.sqrt

private val checkButtonText = listOf("or", "and")

internal fun calcSearchScore(searchWord: String, target: String): Double {
    if (searchWord.isBlank() || target.isBlank()) {
        return 0.0
    }
    val splitSearchWord = searchWord.splitBy { it.isWhitespace() }
        .filter { it.isNotBlank() }
    if (splitSearchWord.isEmpty()) {
        return 0.0
    }
    return splitSearchWord.map { subWord ->
        stringSim(subWord, target)
    }.withIndex().maxOf { (index, value) ->
        value
    }
}

internal fun calcSearchSortedList(searchWord: String, candidateList: List<String>): List<String> {
    return candidateList
        .map { candidate -> candidate to calcSearchScore(searchWord, candidate) }
        .sortedByDescending { it.second }
        .map { it.first }
}

private fun stringSim(a: String, b: String): Double {
    var strA = a
    var strB = b
    // 保证 b 更短，减少空间
    if (strA.length < strB.length) {
        val temp = strA
        strA = strB
        strB = temp
    }

    val lenA = strA.length
    val lenB = strB.length
    var prev = IntArray(lenB + 1) { it }

    for (i in 1..lenA) {
        val curr = IntArray(lenB + 1)
        curr[0] = i
        for (j in 1..lenB) {
            val cost = if (strA[i - 1] == strB[j - 1]) 0 else 1
            curr[j] = minOf(prev[j] + 1, curr[j - 1] + 1, prev[j - 1] + cost)
        }
        prev = curr
    }
    return 1 - prev[lenB].toDouble() / max(a.length, b.length)
}

private fun CharSequence.charMap(): Map<Char, Int> {
    val ret = mutableMapOf<Char, Int>()
    for (c in this) {
        ret[c] = ret.getOrElse(c) { 0 } + 1
    }
    return ret
}

class FilterConfig(
    val labelOr: MutableState<Boolean> = mutableStateOf(true),
    val isInSearch: MutableState<Boolean> = mutableStateOf(false)
) {
    val labelChecked: Int get() = if (labelOr.value) 0 else 1
    val searchWord: MutableState<String> = mutableStateOf("")

    val searchWordList = mutableStateListOf<String>()

    val defaultStaggerLabelListScrollState = LazyStaggeredGridState(
        0,
        0
    )

    val searchStaggerLabelListScrollState by lazy {
        LazyStaggeredGridState(
            0,
            0
        )
    }

    val staggerLabelListScrollState get() = if (isInSearch.value) searchStaggerLabelListScrollState else defaultStaggerLabelListScrollState

    suspend fun updateSearchWord(searchWord: String, labelList: List<String>) {
        this.searchWord.value = searchWord
        val searchWordList = withContext(Dispatchers.Default) {
            calcSearchSortedList(searchWord, labelList)
        }
        logD("updateSearchWord") {
            "searchWordList=${searchWordList.toTypedArray().contentDeepToString()}"
        }
        this.searchWordList.clear()
        this.searchWordList.addAll(searchWordList)
    }
}

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
    filterConfig: MutableState<FilterConfig> = mutableStateOf(FilterConfig()),
    onDismiss: () -> Unit,
) {
    val horizontalPadding = 6.dp
    val verticalPadding = 6.dp
    val horizontalInnerPadding = LocalAppUIConstants.current.filterLabelPaddings[2]
    val viewModel: AlbumViewModel = viewModel(factory = AlbumViewModel.viewModelFactory)
    LaunchedEffect(labelList) {
        filterConfig.value.updateSearchWord(filterConfig.value.searchWord.value, labelList)
    }
    Box(Modifier.fillMaxSize()) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(sqrt(goldenRatio))
                .align(Alignment.BottomCenter),
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

                        Box(
                            modifier = Modifier.padding(
                                horizontal = horizontalInnerPadding,
                                vertical = verticalPadding
                            )
                        ) {
                            AppThemeText(
                                text = "标签筛选",
                                style = LocalTextStyle.current.copy(
                                    fontWeight = FontWeight(600),
                                    fontSize = 16.sp
                                ),
                                modifier = Modifier.align(Alignment.CenterStart)
                            )
                            RadioGroup(
                                Modifier.align(Alignment.CenterEnd),
                                checkButtonText,
                                filterConfig.value.labelChecked
                            ) {
                                filterConfig.value.labelOr.value = it == 0
                                viewModel.filterCheckChanged.notifyChange()
                            }
                        }
                    }
                    item(span = { GridItemSpan(this.maxLineSpan) }) {

                        val labelItemHeight =
                            LocalAppUIConstants.current.filterLabelHeight + (LocalAppUIConstants.current.filterLabelPaddings[1] + LocalAppUIConstants.current.filterLabelPaddings[3]) * 2
                        LazyHorizontalStaggeredGrid(
//            StaggeredGridCells.Adaptive(labelItemHeight),
                            StaggeredGridCells.Fixed(3),
                            Modifier
                                .height(labelItemHeight * 3)
                                .fillMaxWidth()
                            ,
                            state = filterConfig.value.staggerLabelListScrollState,
                            horizontalItemSpacing = horizontalInnerPadding,
                        ) {
                            val finalLabelList = if (filterConfig.value.isInSearch.value) {
                                filterConfig.value.searchWordList
                            } else {
                                labelList
                            }
                            items(
                                finalLabelList.size,
                                { index -> finalLabelList[index] }) { index ->
                                val item = finalLabelList[index]
                                RoundRectCheckableLabel(
                                    item,
                                    labelCheckStateList[item] ?: false,
                                    labelCheckStateList,
                                    true,
                                )
                            }
                        }
                    }

                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(
                            modifier = Modifier.padding(
                                horizontal = horizontalInnerPadding,
                                vertical = verticalPadding
                            )
                        ) {
                            Row(Modifier.align(Alignment.CenterEnd)) {
                                if (filterConfig.value.isInSearch.value) {
                                    BasicTextField(
                                        value = filterConfig.value.searchWord.value,
                                        onValueChange = {
                                            scope.launch {
                                                filterConfig.value.searchStaggerLabelListScrollState.scrollToItem(0, 0)
                                                filterConfig.value.updateSearchWord(it, labelList)
                                            }
                                        },
                                        modifier = Modifier
                                            .align(Alignment.CenterVertically)
                                            .fillMaxWidth()
                                            .background(
                                                color = LocalAppPalette.current.galleryCardBg,
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .padding(start = 12.dp, end = 6.dp, top = 8.dp, bottom = 8.dp)
                                            .weight(1f),
                                        singleLine = true,
                                        textStyle = LocalTextStyle.current.copy(fontSize = 16.sp, fontWeight = FontWeight(400), color = LocalTextStyle.current.color),
                                        cursorBrush = SolidColor(LocalTextStyle.current.color)
                                    )
                                }
                                Icon(
                                    painter = getPainter(DrawableIcon.DrawableSearch),
                                    contentDescription = "搜索",
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                        .size(LocalAppUIConstants.current.filterLabelHeight + 4.dp)
                                        .background(LocalAppPalette.current.labelUnChecked, shape = CircleShape)
                                        .clickable {
                                            filterConfig.value.isInSearch.value =
                                                !filterConfig.value.isInSearch.value

                                            scope.launch {
                                                filterConfig.value.searchStaggerLabelListScrollState.scrollToItem(0, 0)
                                            }
                                        }
                                        .padding(8.dp)
                                        .align(Alignment.CenterVertically)
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

                                val finalLabelList = if (filterConfig.value.isInSearch.value) {
                                    filterConfig.value.searchWordList
                                } else {
                                    labelList
                                }
                                fileTypeCheckStateList.reverseList(fileTypeList)
                                itemRankCheckStateList.reverseList(itemRankList)
                                labelCheckStateList?.reverseList(finalLabelList)
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
}

@Composable
fun RadioGroup(
    modifier: Modifier,
    textString: List<String>,
    checked: Int,
    onCheckChange: (Int) -> Unit
) {
    Box(modifier) {
        // 记录当前选中项 ID
        var selectedId by remember(checked) { mutableStateOf(checked) }

        // 选项列表
        val options = textString.withIndex()

        Row(modifier = Modifier.align(Alignment.Center)) {
            options.forEach { option ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedId == option.index,
                        onClick = {
                            selectedId = option.index
                            onCheckChange(option.index)
                        },
                    )
                    Text(
                        text = option.value,
                        modifier = Modifier
                    )
                }
            }
        }
    }
}
