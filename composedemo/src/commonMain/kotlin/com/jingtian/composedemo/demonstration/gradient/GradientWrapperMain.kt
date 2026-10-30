package com.jingtian.composedemo.demonstration.gradient

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import com.jingtian.composedemo.base.screenWidth

/**
 * HSL 转 Compose Color
 * @param hue 色相 [0,360]
 * @param saturation 饱和度 [0,1]
 * @param lightness 明度 [0,1]
 * @param alpha 透明度 [0,1]
 */
fun Color.Companion.hsl(
    hue: Float,
    saturation: Float,
    lightness: Float,
    alpha: Float = 1f
): Color {
    val h = (hue % 360f + 360f) % 360f / 60f
    val s = saturation.coerceIn(0f, 1f)
    val l = lightness.coerceIn(0f, 1f)

    val r: Float
    val g: Float
    val b: Float

    if (s == 0f) {
        r = l
        g = l
        b = l
    } else {
        val q = if (l < 0.5f) l * (1f + s) else l + s - l * s
        val p = 2f * l - q

        fun hue2rgb(t: Float): Float {
            val tt = when {
                t < 0f -> t + 1f
                t > 1f -> t - 1f
                else -> t
            }
            return when {
                tt < 1f / 6f -> p + (q - p) * 6f * tt
                tt < 1f / 2f -> q
                tt < 2f / 3f -> p + (q - p) * (2f / 3f - tt) * 6f
                else -> p
            }
        }

        r = hue2rgb(h + 1f / 3f)
        g = hue2rgb(h)
        b = hue2rgb(h - 1f / 3f)
    }
    return Color(r, g, b, alpha.coerceIn(0f, 1f))
}

private val shadowSize = 20.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterPreview() {
    var verticalColorH by remember { mutableFloatStateOf(0f) }
    var verticalColorS by remember { mutableFloatStateOf(0.4f) }
    var verticalColorL by remember { mutableFloatStateOf(0.75f) }
    var verticalColor by remember { mutableStateOf(Color.hsl(verticalColorH, verticalColorS, verticalColorL)) }
    val hState = rememberSliderState(value = 0f, valueRange = 0f..1f)
    val sState = rememberSliderState(value = 0.4f, valueRange = 0f..1f)
    val lState = rememberSliderState(value = 0.75f, valueRange = 0f..1f)
    val alphaState = rememberSliderState(value = 0.3f, valueRange = 0f..1f)
    LaunchedEffect(hState.value, sState.value, lState.value, alphaState.value) {
        verticalColorH = hState.value * 360
        verticalColorS = sState.value
        verticalColorL = lState.value
        SameStoryAggregationBackgroundConstants.BACKGROUND_ALPHA = alphaState.value
        verticalColor = Color.hsl(verticalColorH, verticalColorS, verticalColorL)
    }
    val density = LocalDensity.current
    var offsetY by remember { mutableFloatStateOf(100f) }
    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        val demoHeight = 350
        val textHeight = 100
        GradientWrapper(
            mainColor = verticalColor
        ) {
            Column {
                Spacer(Modifier.height(offsetY.dp))
                GradientWrapper(
                    leftShadow = shadowSize,
                    rightShadow = shadowSize,
                    offsetY = offsetY.dp,
                    mainColor = verticalColor
                ) {
                    val scrollState =
                        rememberScrollState(initial = with(LocalDensity.current) { shadowSize.roundToPx() })
                    Row(modifier = Modifier.fillMaxWidth().height(textHeight.dp)
                        .horizontalScroll(state = scrollState)
                        .pointerInput(Unit) {
                            detectDragGestures(onDrag = { change: PointerInputChange, dragAmount: Offset ->
                                scrollState.dispatchRawDelta(-dragAmount.x)
                                with(density) {
                                    offsetY = (offsetY + dragAmount.y.toDp().value).coerceIn(
                                        0f,
                                        (demoHeight - textHeight).dp.value
                                    )
                                }
                                change.consume()
                            })
                        }) {
                        Spacer(Modifier.width(shadowSize))
                        Text(
                            "这是一段超级长长长长长的文字",
                            color = Color.Black,
                            style = TextStyle.Default.copy(fontSize = 38.sp),
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                        Spacer(Modifier.width(shadowSize))
                    }
                }
                Spacer(Modifier.height((demoHeight - offsetY - textHeight).dp))
            }
            Text(
                text = "假装搜索框",
                color = Color.Black,
                textAlign = TextAlign.Center,
                style = TextStyle.Default.copy(fontSize = 20.sp),
                modifier = Modifier.padding(top = 55.dp, start = 20.dp, end = 20.dp).height(36.dp)
                    .fillMaxWidth().clip(RoundedCornerShape(100)).background(Color.White)
            )
        }
        Column(modifier = Modifier.fillMaxWidth().wrapContentHeight().padding(horizontal = 10.dp)) {
            Row {
                Slider(
                    state = alphaState,
                    modifier = Modifier.width(screenWidth().dp - 20.dp - 80.dp)
                )
                Text(
                    text = "a = ${((SameStoryAggregationBackgroundConstants.BACKGROUND_ALPHA) * 100).toInt() / 100f}",
                    color = Color.Black,
                    modifier = Modifier.weight(1f).align(Alignment.CenterVertically)
                )
            }
            Row {
                Slider(
                    state = hState,
                    modifier = Modifier.width(screenWidth().dp - 20.dp - 80.dp)
                )
                Text(
                    text = "h = ${((verticalColorH) * 100).toInt() / 100f}",
                    color = Color.Black,
                    modifier = Modifier.weight(1f).align(Alignment.CenterVertically)
                )
            }
            Row {
                Slider(
                    state = sState,
                    modifier = Modifier.width(screenWidth().dp - 20.dp - 80.dp)
                )
                Text(
                    text = "s = ${((verticalColorS) * 100).toInt() / 100f}",
                    color = Color.Black,
                    modifier = Modifier.weight(1f).align(Alignment.CenterVertically)
                )
            }
            Row {
                Slider(
                    state = lState,
                    modifier = Modifier.width(screenWidth().dp - 20.dp - 80.dp)
                )
                Text(
                    text = "l = ${((verticalColorL) * 100).toInt() / 100f}",
                    color = Color.Black,
                    modifier = Modifier.weight(1f).align(Alignment.CenterVertically),
                )
            }
            Text(
                text = "r=${verticalColor.red}, g=${verticalColor.green}, b=${verticalColor.blue}",
                color = Color.Black
            )
        }
    }
}
