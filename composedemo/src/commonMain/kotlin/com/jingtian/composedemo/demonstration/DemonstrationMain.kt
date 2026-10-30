package com.jingtian.composedemo.demonstration

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jingtian.composedemo.demonstration.DemonstrationMainTabsHolder.DEFAULT_SELECTED
import com.jingtian.composedemo.demonstration.DemonstrationMainTabsHolder.tabs
import kotlinx.coroutines.launch

enum class DemonstrationMainTab(val info: DemonstrationMainTabInfo) {
    GradientWrapper(
        DemonstrationMainTabInfo(
            "GradientWrapper",
        ) {
            com.jingtian.composedemo.demonstration.gradient.FilterPreview()
        }
    )
}

data class DemonstrationMainTabInfo(
    val tabName: String,
    val content: @Composable () -> Unit
)

object DemonstrationMainTabsHolder {
    val tabs = DemonstrationMainTab.values()
    val DEFAULT_SELECTED = DemonstrationMainTab.GradientWrapper
}

@Composable
fun DemonstrationMain() {
    val initialPage = DEFAULT_SELECTED.ordinal
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { tabs.size }
    )

    val scope = rememberCoroutineScope()

    // 选中索引统一由 pagerState.currentPage 驱动，不再单独维护 selectedDestination
    val selectedPage = pagerState.currentPage

    val statusBarHeight = with(LocalDensity.current) {
        WindowInsets.statusBars.getTop(this).toDp()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        PrimaryTabRow(
            selectedTabIndex = selectedPage,
            modifier = Modifier.fillMaxWidth()
        ) {
            tabs.forEachIndexed { index, demonstrationMainTab ->
                Tab(
                    modifier = Modifier.padding(top = statusBarHeight),
                    selected = selectedPage == index,
                    onClick = {
                        scope.launch {
                            // 点击Tab → 滚动Pager
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    text = {
                        Text(
                            text = demonstrationMainTab.info.tabName,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { index ->
            tabs[index].info.content()
        }
    }
}
