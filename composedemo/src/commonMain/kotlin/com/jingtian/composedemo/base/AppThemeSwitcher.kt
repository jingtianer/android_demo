package com.jingtian.composedemo.base

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jingtian.composedemo.ui.theme.LocalAppPalette
import com.jingtian.composedemo.utils.AppTheme
import com.jingtian.composedemo.viewmodels.AppThemeViewModel
import com.jingtian.composedemo.base.resources.getPainter
import com.jingtian.composedemo.base.resources.DrawableIcon


@Composable
fun AppThemeSwitcher() {
    val viewModel: AppThemeViewModel = viewModel(factory = AppThemeViewModel.viewModelFactory)
    var currentTheme by remember { viewModel.currentAppTheme }
    @Composable
    fun Modifier.modifier(appTheme: AppTheme): Modifier {
        val modifier = this
        return if (appTheme == currentTheme) {
            modifier.background(
                color = LocalAppPalette.current.labelChecked,
                shape = RoundedCornerShape(100)
            )
        } else {
            modifier.background(
                color = LocalAppPalette.current.labelUnChecked,
                shape = RoundedCornerShape(100)
            )
        }
            .clip(RoundedCornerShape(100))
            .clickable {
                currentTheme = appTheme
            }
            .padding(8.dp)
    }

    val size = 42.dp
    val autoWidth = size * 1.6464f

    Box(
        Modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .fillMaxWidth()
            .wrapContentHeight()) {
//        AppThemeText(
//            "主题颜色",
//            Modifier
//                .wrapContentSize()
//                .align(Alignment.CenterStart),
//            style = LocalTextStyle.current.copy(fontSize = 16.sp)
//        )
        Row(
            Modifier
                .wrapContentSize()
                .align(Alignment.CenterEnd)
        ) {
            Image(
                painter = getPainter(DrawableIcon.DrawableAppThemeAuto),
                contentDescription = "自动",
                Modifier
                    .size(width = autoWidth, size)
                    .modifier(AppTheme.AUTO)
            )
            Spacer(Modifier.width(8.dp))
            Image(
                painter = getPainter(DrawableIcon.DrawableAppThemeLight),
                contentDescription = "浅色",
                Modifier
                    .size(size)
                    .modifier(AppTheme.Lite)
            )
            Spacer(Modifier.width(8.dp))
            Image(
                painter = getPainter(DrawableIcon.DrawableAppThemeNight),
                contentDescription = "深色",
                Modifier
                    .size(size)
                    .modifier(AppTheme.Dark)
            )
        }
    }
}

