package com.jingtian.composedemo.ui.widget

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.net.toFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.io.IOException

open class CommonWebView @JvmOverloads constructor(
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
    }

    open suspend fun suspendLoadUri(uri: Uri?) {
        try {
            if (uri != null) {
                withContext(Dispatchers.IO) {
                    FileInputStream(uri.toFile()).use {
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
                    }
                }
            } else {
                throw RuntimeException("没有找到地址")
            }

        } catch (e: IOException) {
            // 加载失败时显示错误信息
            loadData("<h1>加载失败</h1><p>原因：${e.message}</p>", "text/html", "UTF-8")
        }
    }
}