package com.jingtian.demoapp.main.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.text.TextPaint
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.annotation.Px
import kotlin.math.max
import kotlin.math.min


class StrokeTextDrawable @JvmOverloads constructor(color: Int = 0) : ColorDrawable(color) {
    companion object {
        fun Paint.getBaseline(): Float {
            return (fontMetrics.descent - fontMetrics.ascent) / 2 - fontMetrics.descent
        }
    }

    private var text = ""
    private var textSize = 0f

    private val mPaint = Paint().apply {
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


    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        val textBaseLine = bounds.height() - mPaint.getBaseline()
        val xOffset = (bounds.width() - measuredWidth) / 2f
        canvas.drawText(text, xOffset, textBaseLine, mTextStrokePaint)
        canvas.drawText(text, xOffset, textBaseLine, mPaint)
    }
}