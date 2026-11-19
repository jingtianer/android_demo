package com.jingtian.composedemo.web

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.viewinterop.AndroidView
import com.jingtian.composedemo.multiplatform.MultiplatformFile
import com.jingtian.composedemo.multiplatform.MultiplatformFileImpl
import com.jingtian.composedemo.ui.widget.CommonWebView
import com.jingtian.composedemo.utils.ViewUtils.dpValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CommonWebViewWrapper(val commonWebView: CommonWebView) : FrameLayout(commonWebView.context) {
    init {
        addView(commonWebView, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
    }
}

@Composable
actual fun CommonWebView(modifier: Modifier, file: MultiplatformFile?, enabled: Boolean, width: Dp?, height: Dp?, viewScope: ICommonWebViewScope.()->Unit) {
    val uri = suspend {
        withContext(Dispatchers.IO) {
            (file as? MultiplatformFileImpl)?.getUri()
        }
    }
    val scope = rememberCoroutineScope()
    fun CommonWebView.init(view: View) {
        scope.launch {
            suspendLoadUri(uri())
        }
        isEnabled = enabled
        viewScope(object : ICommonWebViewScope {
            override suspend fun tackSnapShot(): ImageBitmap {
                return this@init.takeSnapShot(view).asImageBitmap()
            }

            override fun initForSnapShot(width: Int?, height: Int?, enabled: Boolean) {
                this@init.initForSnapShot(width ?: ViewGroup.LayoutParams.MATCH_PARENT, height ?: ViewGroup.LayoutParams.MATCH_PARENT, enable = enabled)
            }
        })
    }
    AndroidView(factory = { context->
        val webView = CommonWebView(context)
        val wrapper = CommonWebViewWrapper(webView)
        webView.apply {
//            layoutParams = ViewGroup.LayoutParams(width?.dpValue?.toInt()?:ViewGroup.LayoutParams.MATCH_PARENT, height?.dpValue?.toInt()?:ViewGroup.LayoutParams.MATCH_PARENT)
            init(wrapper)
            initForSnapShot(width?.dpValue?.toInt()?: ViewGroup.LayoutParams.MATCH_PARENT, height?.dpValue?.toInt()?: ViewGroup.LayoutParams.MATCH_PARENT, enable = enabled)
        }
        wrapper
    }, modifier = modifier)
}