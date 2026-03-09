package com.jingtian.composedemo.utils

import android.text.TextPaint
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.jingtian.composedemo.R
import com.jingtian.composedemo.base.app
import com.jingtian.composedemo.ui.widget.StarRateView
import com.jingtian.composedemo.utils.ViewUtils.dpValue
import kotlin.math.abs

actual fun Dp.dpValue(): Float = this.dpValue
