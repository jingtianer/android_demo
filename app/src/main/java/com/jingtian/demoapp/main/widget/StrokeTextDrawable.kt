package com.jingtian.demoapp.main.widget

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.text.TextPaint
import android.util.Log
import androidx.annotation.ColorInt
import androidx.annotation.Px
import com.jingtian.demoapp.main.TextUtils.measure
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min


class StrokeTextDrawable @JvmOverloads constructor(color: Int = 0) : ColorDrawable(color) {
    companion object {
        fun Paint.getBaseline(): Float {
            return (-fontMetrics.descent - fontMetrics.ascent)
        }
        fun TextPaint.fontHeight(): Float {
            return fontMetrics.bottom - fontMetrics.top
        }
    }

    private var text = ""
    private var textSize = 0f

    private var autoAdjust: Boolean = false
    private var autoPadding = 0f

    fun setAutoAdjust(autoAdjust: Boolean = this.autoAdjust, autoPadding: Float = this.autoPadding) {
        this.autoPadding = autoPadding
        this.autoAdjust = autoAdjust
        invalidateSelf()
    }

    private val mPaint = TextPaint().apply {
        typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true
    }

    private val mTextStrokePaint = TextPaint().apply {
        typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true
    }

    private var measuredWidth = 0f
    private var measuredHeight = 0f

    private fun measure() {
        val w1 = mTextStrokePaint.measureText(text)
        val w2 = mPaint.measureText(text)
        measuredWidth = max(w1, w2)
        measuredHeight = max(
            mPaint.fontMetrics.bottom - mPaint.fontMetrics.top,
            mTextStrokePaint.fontMetrics.bottom - mTextStrokePaint.fontMetrics.top
        )
    }

    fun setText(text: String) {
        this.text = text
        measure()
        invalidateSelf()
    }

    fun setTextSize(@Px textSize: Float) {
        this.textSize = textSize
        mPaint.textSize = textSize
        mTextStrokePaint.textSize = textSize
        measure()
        invalidateSelf()
    }

    fun setTextColor(@ColorInt textColor: Int) {
        mPaint.color = textColor
        invalidateSelf()
    }

    fun getWidth(): Float {
        return measuredWidth
    }

    fun getHeight(): Float {
        return measuredHeight
    }

    fun setStrokeColor(
        @ColorInt textStrokeColor: Int = mTextStrokePaint.color,
        textStrokeWidth: Float = mTextStrokePaint.strokeWidth
    ) {
        mTextStrokePaint.textSize = textSize
        mTextStrokePaint.isAntiAlias = true
        mTextStrokePaint.setColor(textStrokeColor)
        mTextStrokePaint.style = Paint.Style.STROKE
        mTextStrokePaint.strokeWidth = textStrokeWidth
        mTextStrokePaint.isDither = true
        invalidateSelf()
    }

    private var boundsWidth = 0f
    private var boundsHeight = 0f


    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        if (autoAdjust && (boundsWidth != bounds.width().toFloat() || boundsHeight != bounds.height().toFloat())) {
            boundsWidth = bounds.width().toFloat()
            boundsHeight = bounds.height().toFloat()
            val textSize  = autoAdjustTextSize(text, 1f, min(boundsWidth, boundsHeight), boundsWidth - 2 * autoPadding, boundsHeight)
            mPaint.textSize = textSize
            mTextStrokePaint.textSize = textSize
            measure()
        }
        val textBaseLine = -mTextStrokePaint.fontMetrics.descent + bounds.height() - (bounds.height() - measuredHeight) / 2
        val xOffset = (bounds.width() - measuredWidth) / 2f
        val yOffset = 0 // (bounds.height() - measuredHeight) / 2f
        canvas.drawText(text, xOffset, textBaseLine - yOffset, mTextStrokePaint)
        canvas.drawText(text, xOffset, textBaseLine - yOffset, mPaint)
    }

    private val measuredTextSize = FloatArray(2)

    private fun autoAdjustTextSize(text: String, left: Float, right: Float, width: Float, height: Float): Float {
        var l = left.toInt()
        var r = ceil(right.toDouble()).toInt()
        while(l <= r) {
            Log.d("TAG", "autoAdjustTextSize: l=$l, r=$r")
            val mid = ((r - l) / 2f  + l).toInt()
            mTextStrokePaint.textSize = mid.toFloat()
            mTextStrokePaint.measure(text, measuredTextSize)
            if (measuredTextSize[0] > width || measuredTextSize[1] > height) {
                r = mid - 1
            } else {
                l = mid + 1
            }
        }
        return r.toFloat()
    }
}