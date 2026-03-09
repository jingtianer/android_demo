package com.jingtian.composedemo.main.widget

import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jingtian.composedemo.dao.model.ItemRank
import com.jingtian.composedemo.ui.widget.RankTypeChooser
import com.jingtian.composedemo.utils.AppTheme
import com.jingtian.composedemo.utils.ViewUtils.dpValue
import com.jingtian.composedemo.viewmodels.AppThemeViewModel
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun BoxScope.RankLabel(itemRank: ItemRank, padding: Dp) {
    val appThemeViewModel: AppThemeViewModel = viewModel()
    val appTheme by remember { appThemeViewModel.currentAppTheme }
    val isSystemDark = isSystemInDarkTheme()
    val isNight = AppTheme.isDark(appTheme, isSystemDark)
    if (itemRank != ItemRank.NONE) {
        fun View.initRankView(isNight: Boolean): View {
            val bg = RankTypeChooser.createBg(itemRank, context, isNight)
            val paddingHorizontal = 4.dp.dpValue.roundToInt()
            val width = max(
                bg.getWidth(),
                bg.getHeight()
            ).roundToInt() + paddingHorizontal + paddingHorizontal
            layoutParams = ViewGroup.LayoutParams(width, bg.getHeight().roundToInt())
            setPadding(paddingHorizontal, 0, paddingHorizontal, 0)
            background = bg
            return this
        }
        AndroidView({ context ->
            View(context).initRankView(isNight)
        },
            Modifier
                .wrapContentSize()
                .align(Alignment.TopEnd)
                .clip(RoundedCornerShape(bottomStart = padding * 2)),
            update = {
                it.initRankView(isNight)
            })
    }
}