package com.jingtian.composedemo.web

import android.view.MotionEvent
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

class CommonWebViewWrapper(commonWebView: CommonWebView, var isEnable: Boolean = true) : FrameLayout(commonWebView.context) {
    init {
        addView(commonWebView, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return isEnable && super.onTouchEvent(event)
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        return isEnable && super.dispatchTouchEvent(ev)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        return isEnable && super.onInterceptTouchEvent(ev)
    }
}

@Composable
actual fun CommonWebView(modifier: Modifier, file: MultiplatformFile?, enabled: Boolean, width: Int?, height: Int?, viewScope: ICommonWebViewScope.()->Unit) {
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
            override suspend fun takeSnapshot(): ImageBitmap {
                return this@init.takeSnapShot(view).asImageBitmap()
            }

            override fun initForSnapshot(width: Int?, height: Int?, enabled: Boolean) {
                this@init.initForSnapShot(width ?: ViewGroup.LayoutParams.MATCH_PARENT, height ?: ViewGroup.LayoutParams.MATCH_PARENT, enable = enabled)
            }
        })
    }
    AndroidView(factory = { context->
        val webView = CommonWebView(context)
        val wrapper = CommonWebViewWrapper(webView, isEnable = enabled)
        webView.apply {
//            layoutParams = ViewGroup.LayoutParams(width?.dpValue?.toInt()?:ViewGroup.LayoutParams.MATCH_PARENT, height?.dpValue?.toInt()?:ViewGroup.LayoutParams.MATCH_PARENT)
            init(wrapper)
            initForSnapShot(width?.toInt()?: ViewGroup.LayoutParams.MATCH_PARENT, height?.toInt()?: ViewGroup.LayoutParams.MATCH_PARENT, enable = enabled)
        }
        wrapper
    }, modifier = modifier)
}