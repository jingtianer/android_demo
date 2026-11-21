package com.jingtian.composedemo.main.viewer

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.jingtian.composedemo.dao.model.relation.AlbumItemRelation
import com.jingtian.composedemo.main.widget.AppScrim
import com.jingtian.composedemo.ui.theme.DarkWrapper
import com.jvziyaoyao.scale.image.sampling.rememberSamplingDecoder
import com.jvziyaoyao.scale.image.sampling.samplingProcessorPair
import com.jvziyaoyao.scale.image.viewer.ImageViewer
import com.jvziyaoyao.scale.image.viewer.ModelProcessor
import com.jvziyaoyao.scale.zoomable.zoomable.ZoomableGestureScope
import com.jvziyaoyao.scale.zoomable.zoomable.rememberZoomableState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.io.buffered
import kotlinx.io.readByteArray

class ImagePlayerState {
    var bytes by mutableStateOf<ByteArray?>(null)
    var showMask by mutableStateOf(false)
    var maskVisible by mutableStateOf(false)
    val showMaskAnim = Animatable(initialValue = 0f)

    @Composable
    fun ToggleMask() {
        LaunchedEffect(showMask) {
            if (showMask) {
                maskVisible = true
                showMaskAnim.animateTo(1f)
                delay(2000)
                showMask = false
            } else {
                maskVisible = true
                showMaskAnim.animateTo(0f)
                maskVisible = false
            }
        }
    }
}

@Composable
fun rememberImagePlayerState(): ImagePlayerState {
    return remember { ImagePlayerState() }
}

@Composable
fun ImagePlayer(
    state: ImagePlayerState = rememberImagePlayerState(),
    modifier: Modifier,
    uri: AlbumItemRelation
) {
    with(state) {
        ToggleMask()
        LaunchedEffect(uri) {
            bytes = withContext(Dispatchers.IO) {
                uri.fileInfo.getFileUri()?.inputStream()?.buffered()?.readByteArray()
            }
        }
        val (samplingDecoder) = rememberSamplingDecoder(bytes)
        val scope = rememberCoroutineScope()
        if (samplingDecoder != null) {
            val state = rememberZoomableState(
                contentSize = samplingDecoder.intrinsicSize
            )
            Box(modifier = modifier.background(Color.Black).clickable(interactionSource = null, indication = null) {
                showMask = !showMask
            }) {
                ImageViewer(
                    modifier = Modifier.fillMaxSize(),
                    model = samplingDecoder,
                    state = state,
                    processor = ModelProcessor(samplingProcessorPair),
                    detectGesture = ZoomableGestureScope(
                        onDoubleTap = {
                            scope.launch {
                                state.toggleScale(it)
                            }
                        },
                        onTap = {
                            showMask = !showMask
                        }
                    )
                )

                if (maskVisible) {
                    Box(Modifier.fillMaxSize().graphicsLayer { alpha = showMaskAnim.value }) {
                        AppScrim(alpha = 0.8f)

                        Column(
                            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter)
                                .windowInsetsPadding(
                                    WindowInsets.systemGestures
                                ).padding(horizontal = 16.dp)
                        ) {
                            DarkWrapper {
                                FileExtraInfoView(albumItemRelation = uri)
                            }
                        }
                    }
                }
            }
        }
    }
}