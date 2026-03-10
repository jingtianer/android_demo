package com.jingtian.composedemo.web

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.jingtian.composedemo.multiplatform.MultiplatformFile
import java.awt.Desktop

@Composable
actual fun CommonWebView(modifier: Modifier, file: MultiplatformFile?, enabled: Boolean, width: Dp?, height: Dp?, viewScope: ICommonWebViewScope.()->Unit) {
    Desktop.getDesktop().browse(file?.file?.toURI())
}