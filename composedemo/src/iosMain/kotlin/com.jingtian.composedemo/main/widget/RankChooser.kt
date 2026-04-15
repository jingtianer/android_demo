package com.jingtian.composedemo.main.widget

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import com.jingtian.composedemo.dao.model.ItemRank

@Composable
actual fun ColumnScope.RankChooser(itemRank: ItemRank, onRankUpdate: (ItemRank)->Unit) {

}