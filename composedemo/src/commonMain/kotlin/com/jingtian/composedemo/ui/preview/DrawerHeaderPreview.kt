//package com.jingtian.composedemo.ui.preview
//
//import android.graphics.drawable.shapes.RoundRectShape
//import androidx.compose.foundation.background
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.BoxWithConstraints
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.IntrinsicSize
//import androidx.compose.foundation.layout.Row
//import androidx.compose.foundation.layout.aspectRatio
//import androidx.compose.foundation.layout.fillMaxHeight
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.height
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.layout.requiredWidth
//import androidx.compose.foundation.layout.size
//import androidx.compose.foundation.layout.sizeIn
//import androidx.compose.foundation.layout.width
//import androidx.compose.foundation.layout.widthIn
//import androidx.compose.foundation.layout.wrapContentHeight
//import androidx.compose.foundation.layout.wrapContentSize
//import androidx.compose.foundation.layout.wrapContentWidth
//import androidx.compose.foundation.lazy.LazyRow
//import androidx.compose.foundation.lazy.grid.GridCells
//import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
//import androidx.compose.foundation.lazy.staggeredgrid.LazyHorizontalStaggeredGrid
//import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
//import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material3.Surface
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.geometry.RoundRect
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.layout.AlignmentLine
//import androidx.compose.ui.layout.FirstBaseline
//import androidx.compose.ui.layout.Layout
//import androidx.compose.ui.layout.MeasurePolicy
//import androidx.compose.ui.layout.MeasureResult
//import androidx.compose.ui.tooling.preview.Preview
//import androidx.compose.ui.unit.Dp
//import androidx.compose.ui.unit.dp
//import com.jingtian.composedemo.ui.theme.LocalAppPalette
//import com.jingtian.composedemo.ui.theme.LocalAppUIConstants
//import com.jingtian.composedemo.ui.theme.goldenRatio
//import kotlin.random.Random
//
////@Preview
////@Composable
////fun PreviewDrawerHead() = DrawerHeader()
//
//@Composable
//fun AspectRatioComponent(paddings: List<Dp>, wrapContent: Boolean, isChecked: Boolean = Random.nextBoolean()) {
//    // 核心Modifier配置：wrapContentWidth + aspectRatio(0.31)
//    val paddingInnerHorizontal = paddings[0]
//    val paddingInnerVertical = paddings[1]
//    val paddingOuterHorizontal = paddings[2]
//    val paddingOuterVertical = paddings[3]
//    val modifier = if (wrapContent) {
//        Modifier
//            .padding(horizontal = paddingOuterHorizontal, vertical = paddingOuterVertical)
//            .height(LocalAppUIConstants.current.filterLabelHeight)
//            .background(color = if (isChecked) LocalAppPalette.current.labelChecked else LocalAppPalette.current.labelUnChecked, shape = RoundedCornerShape(100))
//            .widthIn(LocalAppUIConstants.current.filterLabelHeight * LocalAppUIConstants.current.filterLabelAspectRatio)
//            .clip(
//                RoundedCornerShape(100),
//            )
//    } else {
//        Modifier
//            .padding(horizontal = paddingOuterHorizontal, vertical = paddingOuterVertical)
//            .wrapContentWidth()
//            .height(LocalAppUIConstants.current.filterLabelHeight)
//            .widthIn(min = LocalAppUIConstants.current.filterLabelHeight * LocalAppUIConstants.current.filterLabelAspectRatio, max = Dp.Infinity)
//            .background(color = if (isChecked) LocalAppPalette.current.labelChecked else LocalAppPalette.current.labelUnChecked, shape = RoundedCornerShape(100))
//            .clip(
//                RoundedCornerShape(100),
//            )
//    }
//    Box(modifier) {
//        Text(
//            text = "字".repeat(Random.nextInt(2, 15)),
//            modifier = Modifier
//                .padding(horizontal = paddingInnerHorizontal, vertical = paddingInnerVertical)
//                .wrapContentWidth()
//                .align(Alignment.Center)
//        )
//    }
//}
//
//// 预览函数，可直接在IDE中查看效果
//@Preview(showBackground = true)
//@Composable
//fun AspectRatioComponentPreview() {
//    Column {
//        LazyRow(Modifier.wrapContentHeight().fillMaxWidth()) {
//            items(10) {
//                AspectRatioComponent(LocalAppUIConstants.current.filterLabelPaddings, true)
//            }
//        }
//        LazyRow(Modifier.wrapContentHeight().fillMaxWidth()) {
//            items(10) {
//                AspectRatioComponent(LocalAppUIConstants.current.filterLabelPaddings, true)
//            }
//        }
//        LazyRow(Modifier.wrapContentHeight().fillMaxWidth()) {
//            items(10) {
//                AspectRatioComponent(LocalAppUIConstants.current.filterLabelPaddings, true)
//            }
//        }
//        LazyRow(Modifier.wrapContentHeight().fillMaxWidth()) {
//            items(10) {
//                AspectRatioComponent(LocalAppUIConstants.current.filterLabelPaddings, true)
//            }
//        }
//        LazyRow(Modifier.wrapContentHeight().fillMaxWidth()) {
//            items(10) {
//                AspectRatioComponent(LocalAppUIConstants.current.filterLabelPaddings, true)
//            }
//        }
//        LazyRow(Modifier.wrapContentHeight().fillMaxWidth()) {
//            items(10) {
//                AspectRatioComponent(LocalAppUIConstants.current.filterLabelPaddings, true)
//            }
//        }
//        LazyRow(Modifier.wrapContentHeight().fillMaxWidth()) {
//            items(10) {
//                AspectRatioComponent(LocalAppUIConstants.current.filterLabelPaddings, true)
//            }
//        }
//        LazyRow(Modifier.wrapContentHeight().fillMaxWidth()) {
//            items(10) {
//                AspectRatioComponent(LocalAppUIConstants.current.filterLabelPaddings, true)
//            }
//        }
//        LazyRow(Modifier.wrapContentHeight().fillMaxWidth()) {
//            items(10) {
//                AspectRatioComponent(LocalAppUIConstants.current.filterLabelPaddings, true)
//            }
//        }
//        LazyRow(Modifier.wrapContentHeight().fillMaxWidth()) {
//            items(10) {
//                AspectRatioComponent(LocalAppUIConstants.current.filterLabelPaddings, true)
//            }
//        }
//        LazyRow(Modifier.wrapContentHeight().fillMaxWidth()) {
//            items(10) {
//                AspectRatioComponent(LocalAppUIConstants.current.filterLabelPaddings, true)
//            }
//        }
//        LazyVerticalGrid(columns = GridCells.Adaptive(LocalAppUIConstants.current.filterLabelHeight * LocalAppUIConstants.current.filterLabelAspectRatio), Modifier.wrapContentHeight().fillMaxWidth()) {
//            items(10) {
//                AspectRatioComponent(LocalAppUIConstants.current.filterLabelPaddings, false)
//            }
//        }
//        LazyHorizontalStaggeredGrid(
//            StaggeredGridCells.Adaptive(LocalAppUIConstants.current.filterLabelHeight + LocalAppUIConstants.current.filterLabelPaddings[3]),
//            Modifier.fillMaxHeight().fillMaxWidth().weight(1f)
//        ) {
//            items(20) {
//                AspectRatioComponent(LocalAppUIConstants.current.filterLabelPaddings, true)
//            }
//        }
//    }
////    AspectRatioComponent()
//
//}