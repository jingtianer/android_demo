package com.jingtian.composedemo.main.widget

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.jingtian.composedemo.dao.model.ItemRank

@Composable
fun RankChooser(itemRank: ItemRank, onRankUpdate: (ItemRank) -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ItemRank.visibleRanks.forEach {
            RankLabel(
                modifier = Modifier.clickable {
                    if (it === itemRank) {
                        onRankUpdate(ItemRank.NONE)
                    } else {
                        onRankUpdate(it)
                    }
                },
                itemRank = it,
                isSelected = it === itemRank,
                shape = RectangleShape
            )
        }
    }
}