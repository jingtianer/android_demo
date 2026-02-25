package com.jingtian.composedemo.ui.widget

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class CommonWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : WebView(context, attrs, defStyleAttr, defStyleRes) {
    init {
        settings.apply {
            javaScriptEnabled = true
            defaultTextEncodingName = "UTF-8"
            loadWithOverviewMode = true
            useWideViewPort = true

            allowFileAccess = true
            allowFileAccessFromFileURLs = true
            allowUniversalAccessFromFileURLs = true

            blockNetworkImage = false
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }
        webViewClient = WebViewClient()
        setLayerType(View.LAYER_TYPE_HARDWARE, null)
    }

    fun initForSnapShot(width: Int, height: Int, enable: Boolean = false) {
        layoutParams = ViewGroup.LayoutParams(width, height)
        isHorizontalFadingEdgeEnabled = false
        isVerticalFadingEdgeEnabled = false
        settings.apply {
            useWideViewPort = true
            loadWithOverviewMode = true
            setSupportZoom(enable)
            builtInZoomControls = enable
            displayZoomControls = false
            loadsImagesAutomatically = true
        }
    }

    suspend fun suspendLoadUri(uri: Uri?) {
        try {
            if (uri != null) {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use {
                        val htmlContent = it.bufferedReader().readText()
                        withContext(Dispatchers.Main) {
                            loadDataWithBaseURL(
                                uri.toString(),
                                htmlContent,
                                "text/html",
                                "UTF-8",
                                null
                            )
                        }
                    } ?: throw RuntimeException("无法打开uri $uri")
                }
            } else {
                throw RuntimeException("没有找到地址")
            }

        } catch (e: IOException) {
            // 加载失败时显示错误信息
            loadData("<h1>加载失败</h1><p>原因：${e.message}</p>", "text/html", "UTF-8")
        }
    }

    suspend fun takeSnapShot(view: View = this): Bitmap {
        val bitmap = Bitmap.createBitmap(measuredWidth, measuredHeight, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        withContext(Dispatchers.Main) {
            view.draw(canvas)
        }
        return bitmap
    }
}