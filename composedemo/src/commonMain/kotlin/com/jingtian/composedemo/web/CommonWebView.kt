package com.jingtian.composedemo.web

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.Dp
import com.jingtian.composedemo.multiplatform.MultiplatformFile

interface ICommonWebViewScope {
    suspend fun tackSnapShot(): ImageBitmap
    fun initForSnapShot(width: Dp?, height: Dp?, enabled: Boolean)
}

@Composable
expect fun CommonWebView(modifier: Modifier = Modifier, file: MultiplatformFile?, enabled: Boolean = true, width: Dp? = null, height: Dp? = null, viewScope: ICommonWebViewScope.()->Unit = {})