package com.jingtian.composedemo.main.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jingtian.composedemo.dao.model.ItemRank
import com.jingtian.composedemo.viewmodels.appThemeViewModel

@Composable
fun Modifier.textStroke(
    strokeColor: Color,
    strokeWidth: Dp,
    textLayoutResult: TextLayoutResult?,
): Modifier {
    if (textLayoutResult == null) {
        return this
    }
    return this.drawWithCache {
        onDrawBehind {
            drawText(
                textLayoutResult = textLayoutResult,
                color = strokeColor,
                drawStyle = Stroke(width = strokeWidth.roundToPx().toFloat())
            )
        }
    }
}

@Composable
fun RankLabel(
    modifier: Modifier = Modifier,
    itemRank: ItemRank,
    isSelected: Boolean = true,
    padding: Dp = 4.dp,
    textHeight: Dp = 30.dp,
    shape: Shape = RoundedCornerShape(bottomStart = padding * 2)
) {
    if (itemRank == ItemRank.NONE) {
        return
    }
    Box(
        modifier = modifier
            .wrapContentSize()
            .clip(shape)
            .background(color = if (isSelected) itemRank.color() else Color.Transparent)
            .padding(padding),
    ) {
        val isNight by appThemeViewModel().rememberIsDark()
        val textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.ExtraBold)
        var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
        Text(
            text = itemRank.name,
            modifier = Modifier.textStroke(
                strokeColor = if (isNight) Color.Black else Color.White,
                strokeWidth = padding,
                textLayoutResult = textLayoutResult
            ).height(textHeight),
            color = if (isNight) Color.White else Color.Black,
            style = textStyle,
            autoSize = TextAutoSize.StepBased(),
            onTextLayout = {
                textLayoutResult = it
            }
        )
    }
}