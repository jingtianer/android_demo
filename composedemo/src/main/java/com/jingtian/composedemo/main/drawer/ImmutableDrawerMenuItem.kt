package com.jingtian.composedemo.main.drawer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jingtian.composedemo.base.AppThemeText
import com.jingtian.composedemo.dao.model.Album

@Composable
fun ImmutableDrawerMenuItem(
    item: Album,
    modifier: Modifier,
    onItemClick: () -> Unit
) {
    Box(
        Modifier
            .wrapContentSize()
            .clickable { onItemClick() }) {
        AppThemeText(item.albumName, modifier)
    }
}