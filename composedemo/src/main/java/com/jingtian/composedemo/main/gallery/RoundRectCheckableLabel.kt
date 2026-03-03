package com.jingtian.composedemo.main.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jingtian.composedemo.base.AppThemeText
import com.jingtian.composedemo.ui.theme.LocalAppPalette
import com.jingtian.composedemo.ui.theme.LocalAppUIConstants
import com.jingtian.composedemo.viewmodels.AlbumViewModel

@Composable
fun RoundRectCheckableLabel(item: String, isChecked: Boolean, checkedList: SnapshotStateMap<String, Boolean>, wrapContent: Boolean) {
    val paddings = LocalAppUIConstants.current.filterLabelPaddings
    val paddingInnerHorizontal = paddings[0]
    val paddingInnerVertical = paddings[1]
    val paddingOuterHorizontal = paddings[2]
    val paddingOuterVertical = paddings[3]
    val viewModel: AlbumViewModel = viewModel()
    val modifier = if (wrapContent) {
        Modifier
            .padding(horizontal = paddingOuterHorizontal, vertical = paddingOuterVertical)
            .wrapContentWidth()
            .height(LocalAppUIConstants.current.filterLabelHeight)
            .background(
                color = if (isChecked) LocalAppPalette.current.labelChecked else LocalAppPalette.current.labelUnChecked,
                shape = RoundedCornerShape(100)
            )
            .widthIn(LocalAppUIConstants.current.filterLabelHeight * LocalAppUIConstants.current.filterLabelAspectRatio)
            .clip(
                RoundedCornerShape(100),
            )
    } else {
        Modifier
            .padding(horizontal = paddingOuterHorizontal, vertical = paddingOuterVertical)
            .fillMaxWidth()
            .height(LocalAppUIConstants.current.filterLabelHeight)
            .background(
                color = if (isChecked) LocalAppPalette.current.labelChecked else LocalAppPalette.current.labelUnChecked,
                shape = RoundedCornerShape(100)
            )
            .clip(
                RoundedCornerShape(100),
            )
    }
    Box(
        modifier
            .clickable {
                checkedList[item] = !isChecked
                if (isChecked) {
                    checkedList.remove(item)
                } else {
                    checkedList[item] = true
                }
                viewModel.filterCheckChanged.value = (viewModel.filterCheckChanged.value ?: 0) + 1
            }) {
        AppThemeText(
            text = item,
            Modifier
                .wrapContentSize()
                .align(Alignment.Center)
                .padding(horizontal = paddingInnerHorizontal, vertical = paddingInnerVertical)
        )
    }
}