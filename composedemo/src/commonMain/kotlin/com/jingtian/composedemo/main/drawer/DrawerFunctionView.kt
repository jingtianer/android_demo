package com.jingtian.composedemo.main.drawer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jingtian.composedemo.base.AppThemeText
import androidx.compose.ui.graphics.painter.Painter
import com.jingtian.composedemo.main.gallery.GalleryFunctions

@Composable
fun DrawerFunctionView(galleryFunctions: GalleryFunctions, onClick: () -> Unit) {
    DrawerFunctionView(onClick = onClick, painter = galleryFunctions.iconProvider(), text = galleryFunctions.functionName)
}

@Composable
fun DrawerFunctionView(modifier: Modifier = Modifier, onClick: () -> Unit = {}, painter: Painter, text: String) {
    Row(
        modifier
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 8.dp)
    ) {
        Icon(
            painter = painter,
            contentDescription = "leadingIcon",
            Modifier
                .size(26.dp)
                .align(Alignment.CenterVertically),
        )
        AppThemeText(
            text,
            Modifier
                .align(Alignment.CenterVertically)
                .padding(horizontal = 6.dp),
            style = LocalTextStyle.current.copy(fontSize = 16.sp)
        )
    }
}