package com.jingtian.composedemo.web

import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.viewinterop.AndroidView
import com.jingtian.composedemo.multiplatform.MultiplatformFile
import com.jingtian.composedemo.multiplatform.MultiplatformFileImpl
import com.jingtian.composedemo.ui.widget.CommonWebView
import com.jingtian.composedemo.utils.ViewUtils.dpValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
actual fun CommonWebView(modifier: Modifier, file: MultiplatformFile?, enabled: Boolean, width: Dp?, height: Dp?, viewScope: ICommonWebViewScope.()->Unit) {
    val uri = suspend {
        withContext(Dispatchers.IO) {
            (file as? MultiplatformFileImpl)?.uri
        }
    }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    fun CommonWebView.init() {
        scope.launch {
            suspendLoadUri(uri())
        }
        isEnabled = enabled
        viewScope(object : ICommonWebViewScope {
            override suspend fun tackSnapShot(): ImageBitmap {
                return this@init.takeSnapShot().asImageBitmap()
            }

            override fun initForSnapShot(width: Dp?, height: Dp?, enabled: Boolean) {
                this@init.initForSnapShot(width?.dpValue?.toInt()?: ViewGroup.LayoutParams.MATCH_PARENT, height?.dpValue?.toInt()?: ViewGroup.LayoutParams.MATCH_PARENT, enable = enabled)
            }
        })
    }
    AndroidView(factory = {
        CommonWebView(context).apply {
//            layoutParams = ViewGroup.LayoutParams(width?.dpValue?.toInt()?:ViewGroup.LayoutParams.MATCH_PARENT, height?.dpValue?.toInt()?:ViewGroup.LayoutParams.MATCH_PARENT)
            init()
            initForSnapShot(width?.dpValue?.toInt()?: ViewGroup.LayoutParams.MATCH_PARENT, height?.dpValue?.toInt()?: ViewGroup.LayoutParams.MATCH_PARENT, enable = enabled)
        }
    }, modifier = modifier)
}