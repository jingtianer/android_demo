package com.jingtian.composedemo.demonstration.gradient

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.jingtian.composedemo.demonstration.gradient.SameStoryAggregationBackgroundConstants.BACKGROUND_HEIGHT_DP
import com.jingtian.composedemo.demonstration.gradient.SameStoryAggregationBackgroundConstants.END_ALPHA
import com.jingtian.composedemo.demonstration.gradient.SameStoryAggregationBackgroundConstants.GRADIENT_END
import com.jingtian.composedemo.demonstration.gradient.SameStoryAggregationBackgroundConstants.GRADIENT_START
import com.jingtian.composedemo.demonstration.gradient.SameStoryAggregationBackgroundConstants.START_ALPHA
import com.jingtian.composedemo.demonstration.gradient.SameStoryAggregationBackgroundConstants.defaultBgColor
import com.jingtian.composedemo.demonstration.gradient.SameStoryAggregationBackgroundConstants.defaultShadowColor

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
    val defaultBgColor = Color.White
    val defaultShadowColor = Color.White
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


private fun Modifier.twoAxisAlphaGradient(
    verticalColor: Color,
    horizontalColor: Color,
    leftAlpha: Float,
    rightAlpha: Float,
    startY: Dp,
    endY: Dp,
    topAlpha: Float,
    bottomAlpha: Float,
): Modifier = this.drawWithCache {
    val verticalBrush = Brush.verticalGradient(
        colors = listOf(
            verticalColor.copy(alpha = topAlpha.coerceIn(0f, 1f)),
            verticalColor.copy(alpha = bottomAlpha.coerceIn(0f, 1f)),
        ),
        startY = startY.toPx(),
        endY = endY.toPx(),
    )

    val horizontalBrush = Brush.horizontalGradient(
        colors = listOf(
            horizontalColor.copy(alpha = leftAlpha.coerceIn(0f, 1f)),
            horizontalColor.copy(alpha = rightAlpha.coerceIn(0f, 1f)),
        ),
        startX = 0f,
        endX = size.width,
    )
    onDrawBehind {
        drawRect(brush = horizontalBrush)
        drawRect(brush = verticalBrush, blendMode = BlendMode.Multiply)
    }
}

@Composable
fun GradientWrapper(
    mainColor: Color,
    offsetY: Dp = 0.dp,
    leftShadow: Dp = 0.dp,
    rightShadow: Dp = 0.dp,
    leftShadowContent: @Composable BoxScope.() -> Unit = {},
    rightShadowContent: @Composable BoxScope.() -> Unit = {},
    content: @Composable BoxScope.() -> Unit = {},
) {
    val density = LocalDensity.current
    var boxHeight by remember { mutableStateOf(0.dp) }
    var leftShadowContentWidth by remember { mutableStateOf(0.dp) }
    var rightShadowContentWidth by remember { mutableStateOf(0.dp) }
    val totalHeight = BACKGROUND_HEIGHT_DP.dp
    val startOffsetY = totalHeight * GRADIENT_START
    val endOffsetY = totalHeight * GRADIENT_END

    val startY = startOffsetY - offsetY
    val endY = endOffsetY - offsetY
    Box(
        modifier = Modifier.fillMaxWidth().wrapContentHeight().onSizeChanged {
            boxHeight = with(density) { it.height.toDp() }
        }.background(defaultBgColor)
            .verticalAlphaGradient(
                color = mainColor,
                startY = startY,
                endY = endY,
                topAlpha = START_ALPHA,
                bottomAlpha = END_ALPHA
            )
    ) {
        if (leftShadow > 0.dp) {
            Spacer(
                modifier = Modifier.align(Alignment.CenterStart).width(leftShadow + leftShadowContentWidth)
                    .height(boxHeight)
                    .background(defaultBgColor)
            )
        }
        if (rightShadow > 0.dp) {
            Spacer(
                modifier = Modifier.align(Alignment.CenterEnd).width(rightShadow + rightShadowContentWidth)
                    .height(boxHeight)
                    .background(defaultBgColor)
            )
        }
        content()
        if (leftShadow > 0.dp) {
            Row(modifier = Modifier.align(Alignment.CenterStart).wrapContentSize()) {
                Box(
                    modifier = Modifier.wrapContentWidth()
                        .height(boxHeight)
                        .onSizeChanged {
                            leftShadowContentWidth = with(density) {
                                it.width.toDp()
                            }
                        }
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
                Spacer(
                    modifier = Modifier.width(leftShadow)
                        .height(boxHeight)
                        .twoAxisAlphaGradient(
                            verticalColor = mainColor,
                            horizontalColor = defaultShadowColor,
                            leftAlpha = 1f,
                            rightAlpha = 0f,
                            startY = startY,
                            endY = endY,
                            topAlpha = START_ALPHA,
                            bottomAlpha = END_ALPHA
                        )
                )
            }
        }
        if (rightShadow > 0.dp) {
            Row(modifier = Modifier.align(Alignment.CenterEnd).wrapContentSize()) {
                Spacer(
                    modifier = Modifier.width(rightShadow)
                        .height(boxHeight)
                        .twoAxisAlphaGradient(
                            verticalColor = mainColor,
                            horizontalColor = defaultShadowColor,
                            leftAlpha = 0f,
                            rightAlpha = 1f,
                            startY = startY,
                            endY = endY,
                            topAlpha = START_ALPHA,
                            bottomAlpha = END_ALPHA
                        )
                )

                Box(
                    modifier = Modifier.wrapContentWidth()
                        .height(boxHeight)
                        .onSizeChanged {
                            rightShadowContentWidth = with(density) {
                                it.width.toDp()
                            }
                        }
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
    }
}