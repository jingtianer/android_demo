package com.jingtian.composedemo.ui.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jingtian.composedemo.ui.theme.DemoAppTheme
import com.jingtian.composedemo.ui.theme.appBackground
import com.jingtian.composedemo.ui.theme.drawerBackground
import com.jingtian.composedemo.utils.ViewUtils.dpValue

@Preview
@Composable
fun BackgroundPreview() {
    Row (
        Modifier
            .fillMaxSize()
            .background(Color.Red)) {
        DemoAppTheme {
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .fillMaxHeight()
                    .appBackground()
            )
//            Spacer(modifier = Modifier.size(8.dp))
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .fillMaxHeight()
                    .appBackground()
            )
//            Spacer(modifier = Modifier.size(8.dp))
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .fillMaxHeight()
                    .appBackground()
            )
//            Spacer(modifier = Modifier.size(8.dp))
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .fillMaxHeight()
                    .drawerBackground()
            )
        }
    }
}