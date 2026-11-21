package com.jingtian.composedemo.main.viewer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.jingtian.composedemo.multiplatform.MultiplatformFile
import com.jvziyaoyao.scale.image.sampling.rememberSamplingDecoder
import com.jvziyaoyao.scale.image.sampling.samplingProcessorPair
import com.jvziyaoyao.scale.image.viewer.ImageViewer
import com.jvziyaoyao.scale.image.viewer.ModelProcessor
import com.jvziyaoyao.scale.zoomable.zoomable.ZoomableGestureScope
import com.jvziyaoyao.scale.zoomable.zoomable.rememberZoomableState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.io.buffered
import kotlinx.io.readByteArray

@Composable
fun ImagePlayer(modifier: Modifier, uri: MultiplatformFile) {
    val bytes = remember { mutableStateOf<ByteArray?>(null) }
    LaunchedEffect(uri) {
        bytes.value = withContext(Dispatchers.IO) {
            uri.inputStream()?.buffered()?.readByteArray()
        }
    }
    val (samplingDecoder) = rememberSamplingDecoder(bytes.value)
    val scope = rememberCoroutineScope()
    if (samplingDecoder != null) {
        val state = rememberZoomableState(
            contentSize = samplingDecoder.intrinsicSize
        )
        ImageViewer(
            modifier = modifier,
            model = samplingDecoder,
            state = state,
            processor = ModelProcessor(samplingProcessorPair),
            detectGesture = ZoomableGestureScope(onDoubleTap = {
                scope.launch {
                    state.toggleScale(it)
                }
            })
        )
    }
}