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
import com.jingtian.composedemo.ui.widget.StarRateView
import com.jingtian.composedemo.utils.AppTheme
import com.jingtian.composedemo.utils.ViewUtils.commonEditableConfig
import com.jingtian.composedemo.viewmodels.AppThemeViewModel

@Composable
actual fun ColumnScope.ScoreChooser(itemScore: Float, onScoreUpdate: (Float)->Unit) {
    val viewModel: AppThemeViewModel = viewModel(factory = AppThemeViewModel.viewModelFactory)
    val appTheme by remember { viewModel.currentAppTheme }
    val isSystemDark = isSystemInDarkTheme()
    val isDark = AppTheme.isDark(appTheme, isSystemDark)
    AndroidView({ context ->
        StarRateView(context).commonEditableConfig(isDark).apply {
            onScoreChange = StarRateView.Companion.OnScoreChange { score: Float ->
                onScoreUpdate(score)
            }
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    },
        Modifier
            .wrapContentWidth()
            .height(30.dp)
            .align(Alignment.CenterHorizontally),
        update = {
            it.setScore(itemScore)
            it.commonEditableConfig(isDark)
        })
}