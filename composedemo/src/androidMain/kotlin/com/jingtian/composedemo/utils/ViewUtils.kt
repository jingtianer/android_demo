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
import kotlin.math.abs

object ViewUtils {
    class FixNestedScroll(
        private val view: View,
        private val orientation: Int
    ) : View.OnTouchListener {
        private var initX = 0f
        private var initY = 0f
        private var scrolled = false
        private var touchSlop = ViewConfiguration.get(view.context).scaledTouchSlop

        private val Float.intSign: Int
            get() = when {
                this > 0 -> {
                    1
                }

                this < 0 -> {
                    -1
                }

                else -> {
                    0
                }
            }

        override fun onTouch(v: View, event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    initX = event.x
                    initY = event.y
                    scrolled = false
                    v.parent?.requestDisallowInterceptTouchEvent(true)
                }

                MotionEvent.ACTION_MOVE -> {
                    if (!scrolled) {
                        val dx = initX - event.x
                        val dy = initY - event.y
                        val absDx = abs(dx)
                        val absDy = abs(dy)
                        if (orientation == RecyclerView.HORIZONTAL && absDx > absDy && view.canScrollHorizontally(
                                dx.intSign
                            )
                        ) {
                            scrolled = true
                        } else if (orientation == RecyclerView.VERTICAL && absDy > absDx && view.canScrollVertically(
                                dy.intSign
                            )
                        ) {
                            scrolled = true
                        } else if (absDy > touchSlop && absDy > touchSlop) {
                            v.parent?.requestDisallowInterceptTouchEvent(false)
                        }
                    }
                }
            }
            return false
        }

    }

    fun TextPaint.measure(text: String, outArray: FloatArray) {
        val textWidth = measureText(text)
        val textHeight = (fontMetrics.bottom - fontMetrics.top)
        if (outArray.size >= 2) {
            outArray[0] = textWidth
            outArray[1] = textHeight
        }
    }

    fun StarRateView.commonConfig(isDark: Boolean): StarRateView {
        updateStarConfig(
            false,
            5,
            3f.dp.value,
            ResourcesCompat.getDrawable(resources, if (isDark) R.drawable.star_high_lighted_night else R.drawable.star_high_lighted, null),
            ResourcesCompat.getDrawable(resources, if (isDark) R.drawable.star_night else R.drawable.star, null),
        )
        return this
    }

    fun StarRateView.commonEditableConfig(isDark: Boolean): StarRateView {
        updateStarConfig(
            true,
            5,
            3f.dp.value,
            ResourcesCompat.getDrawable(resources, if (isDark) R.drawable.star_high_lighted_night else R.drawable.star_high_lighted, null),
            ResourcesCompat.getDrawable(resources, if (isDark) R.drawable.star_night else R.drawable.star, null),
        )
        return this
    }

    val Dp.dpValue: Float
        get() = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            this.value,
            app.resources.displayMetrics
        )
}