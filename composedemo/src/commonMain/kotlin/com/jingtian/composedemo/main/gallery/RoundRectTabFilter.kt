package com.jingtian.composedemo.main.gallery

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
import dev.chrisbanes.haze.HazeState

fun LazyListScope.roundRectTabFilter(hazeState: HazeState?, checkedList: SnapshotStateMap<String, Boolean>, filerList: List<String>) {
    items(filerList.size, key = { index-> filerList[index] }) { index->
        val item = filerList[index]
        RoundRectCheckableLabel(
            item,
            checkedList[item] ?: false,
            checkedList,
            true,
            hazeState,
            enableHazeState = true
        )
    }
}