package com.jingtian.composedemo.demonstration.gradient

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.jingtian.composedemo.demonstration.gradient.SameStoryAggregationBackgroundConstants.BACKGROUND_HEIGHT_DP
import com.jingtian.composedemo.demonstration.gradient.SameStoryAggregationBackgroundConstants.END_ALPHA
import com.jingtian.composedemo.demonstration.gradient.SameStoryAggregationBackgroundConstants.GRADIENT_END
import com.jingtian.composedemo.demonstration.gradient.SameStoryAggregationBackgroundConstants.GRADIENT_START
import com.jingtian.composedemo.demonstration.gradient.SameStoryAggregationBackgroundConstants.START_ALPHA

object SameStoryAggregationBackgroundConstants {
    var BACKGROUND_ALPHA by mutableFloatStateOf(0.3f)

    const val GRADIENT_START = 0.15f // [0-1] 垂直渐变开始位置
    const val GRADIENT_END = 1f // [0-1] 垂直渐变开始位置
    val START_ALPHA by derivedStateOf {
        1f * BACKGROUND_ALPHA
    }
    val END_ALPHA by derivedStateOf {
        0f * BACKGROUND_ALPHA
    }
    const val BACKGROUND_HEIGHT_DP = 300
}

private fun Modifier.verticalAlphaGradient(
    color: Color,
    startY: Dp,
    endY: Dp,
    topAlpha: Float,
    bottomAlpha: Float,
): Modifier = this.drawWithCache {
    val brush = Brush.verticalGradient(
        colors = listOf(
            color.copy(alpha = topAlpha.coerceIn(0f, 1f)),
            color.copy(alpha = bottomAlpha.coerceIn(0f, 1f)),
        ),
        startY = startY.toPx(),
        endY = endY.toPx(),
    )
    onDrawBehind {
        drawRect(brush = brush)
    }
}


private fun Modifier.addHorizontalEdgeMask(
    leftShadowWidth: Float,
    rightShadowWidth: Float,
): Modifier {
    val leftShadow = if (leftShadowWidth > 0) {
        Brush.horizontalGradient(
            colors = listOf(
                Color.Black.copy(alpha = 0f),
                Color.Black.copy(alpha = 1f),
            ),
            startX = 0f,
            endX = leftShadowWidth,
        )
    } else {
        null
    }

    return this.graphicsLayer {
        compositingStrategy = CompositingStrategy.Offscreen
    }.drawWithContent {
        drawContent()
        if (leftShadow != null) {
            drawRect(
                brush = leftShadow,
                blendMode = BlendMode.DstIn,
            )
        }
        if (rightShadowWidth > 0) {
            val rightShadow = Brush.horizontalGradient(
                colors = listOf(
                    Color.Black.copy(alpha = 1f),
                    Color.Black.copy(alpha = 0f),
                ),
                startX = size.width - rightShadowWidth,
                endX = size.width,
            )
            drawRect(
                brush = rightShadow,
                blendMode = BlendMode.DstIn,
            )
        }
    }
}

@Composable
fun GradientWrapper(
    mainColor: Color,
    offsetY: Dp = 0.dp,
    leftShadow: Dp = 0.dp,
    rightShadow: Dp = 0.dp,
    leftShadowContent: @Composable () -> Unit = {},
    rightShadowContent: @Composable () -> Unit = {},
    content: @Composable () -> Unit = {},
) {
    val density = LocalDensity.current
    val totalHeight = BACKGROUND_HEIGHT_DP.dp
    val startOffsetY = totalHeight * GRADIENT_START
    val endOffsetY = totalHeight * GRADIENT_END

    val startY = startOffsetY - offsetY
    val endY = endOffsetY - offsetY
    val leftShadowWidth = with(density) {
        leftShadow.roundToPx().toFloat()
    }
    val rightShadowWidth = with(density) {
        rightShadow.roundToPx().toFloat()
    }
    Row(
        modifier = Modifier.fillMaxWidth().background(Color.White).verticalAlphaGradient(
            color = mainColor,
            startY = startY,
            endY = endY,
            topAlpha = START_ALPHA,
            bottomAlpha = END_ALPHA
        )
    ) {
        Box(
            modifier = Modifier.wrapContentWidth()
                .background(Color.White)
                .verticalAlphaGradient(
                    color = mainColor,
                    startY = startY,
                    endY = endY,
                    topAlpha = START_ALPHA,
                    bottomAlpha = END_ALPHA
                )
        ) {
            leftShadowContent()
        }
        Box(
            modifier = Modifier.addHorizontalEdgeMask(
                leftShadowWidth = leftShadowWidth,
                rightShadowWidth = rightShadowWidth
            ).weight(1f)
        ) {
            content()
        }
        Box(
            modifier = Modifier.wrapContentWidth()
                .background(Color.White)
                .verticalAlphaGradient(
                    color = mainColor,
                    startY = startY,
                    endY = endY,
                    topAlpha = START_ALPHA,
                    bottomAlpha = END_ALPHA
                )
        ) {
            rightShadowContent()
        }
    }
}