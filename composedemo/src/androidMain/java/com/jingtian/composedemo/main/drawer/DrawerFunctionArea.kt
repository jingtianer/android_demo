package com.jingtian.composedemo.main.drawer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jingtian.composedemo.R
import com.jingtian.composedemo.main.dialog.AddOrEditAlbumDialog

@Composable
fun DrawerFunctionArea() {
    var dialogState by remember { mutableStateOf(false) }
    DrawerFunctionView(
        onClick = {
            dialogState = true
        },
        drawableId = R.drawable.add,
        text = "添加合集",
    )

    if (dialogState) {
        AddOrEditAlbumDialog {
            dialogState = false
        }
    }
}