package com.jingtian.composedemo.main.gallery

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier

fun LazyListScope.roundRectTabFilter(modifier: Modifier, checkedList: SnapshotStateMap<String, Boolean>, filerList: List<String>) {
    items(filerList.size, key = { index-> filerList[index] }) { index->
        val item = filerList[index]
        RoundRectCheckableLabel(
            item,
            checkedList[item] ?: false,
            checkedList,
            true,
            modifier
        )
    }
}