package com.jingtian.composedemo.ui.preview

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.MutableLiveData
import com.jingtian.composedemo.FilterPanel
import com.jingtian.composedemo.LabelCheckInfo
import com.jingtian.composedemo.dao.model.FileType
import com.jingtian.composedemo.dao.model.ItemRank
import kotlinx.coroutines.launch
import kotlin.random.Random


@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun Preview() {
//    val scope = rememberCoroutineScope()
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
//    scope.launch {
//        state.expand()
//    }
    FilterPanel(
        state,
        FileType.entries.map { LabelCheckInfo(it, it.name, MutableLiveData(Random.nextBoolean())) },
        {},
        ItemRank.entries.map { LabelCheckInfo(it, it.name, MutableLiveData(Random.nextBoolean())) },
        {},
        (0 until 20).map { "标签-$it" }.map { LabelCheckInfo(it, it, MutableLiveData(Random.nextBoolean()))  },
        {},
    ) {
//        scope.launch {
//            state.hide()
//        }
    }
}