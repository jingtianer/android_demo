package com.jingtian.composedemo.main.widget

import android.view.ViewGroup
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jingtian.composedemo.ui.widget.StarRateView
import com.jingtian.composedemo.utils.AppTheme
import com.jingtian.composedemo.utils.ViewUtils.commonConfig
import com.jingtian.composedemo.viewmodels.AppThemeViewModel

@Composable
fun ColumnScope.ScoreStar(itemScore: Float, padding: Dp) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .wrapContentHeight()
            .align(Alignment.CenterHorizontally)
    ) {
        val viewModel: AppThemeViewModel = viewModel()
        val appTheme by remember { viewModel.currentAppTheme }
        val isSystemDark = isSystemInDarkTheme()
        val isDark = AppTheme.isDark(appTheme, isSystemDark)
        AndroidView({ context ->
            StarRateView(context).commonConfig(isDark).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
            Modifier
                .wrapContentWidth()
                .height(30.dp)
                .align(Alignment.Center)
                .padding(bottom = 4.dp, start = padding, end = padding),
            update = {
                it.commonConfig(isDark)
                it.setScore(itemScore)
            })
    }
}