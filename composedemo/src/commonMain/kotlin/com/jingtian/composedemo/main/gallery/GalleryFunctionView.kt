package com.jingtian.composedemo.main.gallery

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jingtian.composedemo.base.AppThemeText

@Composable
fun RowScope.GalleryFunctionView(func: GalleryFunctions, onClick: ()->Unit) {
    Column(
        Modifier
            .wrapContentSize()
            .padding(horizontal = 6.dp)
            .clickable { onClick() }) {
        Icon(
            painter = func.iconProvider(),
            contentDescription = "${func.functionName}功能",
            modifier = Modifier
                .size(32.dp)
                .align(Alignment.CenterHorizontally),
        )
        AppThemeText(
            func.functionName,
            Modifier.align(Alignment.CenterHorizontally),
            style = LocalTextStyle.current.copy(fontSize = 10.sp)
        )
    }
}