package com.jingtian.composedemo.main.widget

import android.view.ViewGroup
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jingtian.composedemo.dao.model.ItemRank
import com.jingtian.composedemo.ui.widget.RankTypeChooser
import com.jingtian.composedemo.utils.AppTheme
import com.jingtian.composedemo.viewmodels.AppThemeViewModel

@Composable
fun ColumnScope.RankChooser(itemRank: ItemRank, onRankUpdate: (ItemRank)->Unit) {
    val viewModel: AppThemeViewModel = viewModel()
    val appTheme by remember { viewModel.currentAppTheme }
    val isSystemDark = isSystemInDarkTheme()
    val isDark = AppTheme.isDark(appTheme, isSystemDark)
    AndroidView({ context ->
        RankTypeChooser(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            onRankChange = RankTypeChooser.Companion.OnRankTypeChange { value ->
                onRankUpdate(value)
            }
            init(isDark)
        }
    },
        Modifier
            .wrapContentWidth()
            .height(30.dp)
            .align(Alignment.CenterHorizontally),
        update = {
            it.setRankType(itemRank)
            it.init(isDark)
        })
}