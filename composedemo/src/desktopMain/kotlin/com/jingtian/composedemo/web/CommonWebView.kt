package com.jingtian.composedemo.web

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.jingtian.composedemo.multiplatform.MultiplatformFile

@Composable
actual fun CommonWebView(modifier: Modifier, file: MultiplatformFile?, enabled: Boolean, width: Dp?, height: Dp?, viewScope: ICommonWebViewScope.()->Unit) {

}