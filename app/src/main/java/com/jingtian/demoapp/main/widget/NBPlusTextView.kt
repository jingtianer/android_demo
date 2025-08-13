package com.jingtian.demoapp.main.widget

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Region
import android.text.TextPaint
import android.util.AttributeSet
import android.util.Property
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.Keep
import androidx.annotation.Px
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin


class NBPlusTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : View(context, attrs, defStyleAttr, defStyleRes) {
    companion object {
        private fun getMeasureSize(textSize: Int, measureSpec: Int, minSize: Int): Int {
            val mode = MeasureSpec.getMode(measureSpec)
            val size = MeasureSpec.getSize(measureSpec)
            val measuredSize = when(mode) {
                MeasureSpec.UNSPECIFIED -> {
                    textSize
                }
                MeasureSpec.EXACTLY -> {
                    size
                }
                MeasureSpec.AT_MOST -> {
                    min(textSize, size)
                }
                else -> {
                    max(textSize, size)
                }
            }
            return max(minSize, measuredSize)
        }
        fun Paint.getBaseline(): Float {
            return (fontMetrics.descent - fontMetrics.ascent) / 2 - fontMetrics.descent
        }
    }

    private var text = ""
    private var textSize = 0f
    private var animProgress = 0f
    private val mPaint = TextPaint()
    private val mPaintForeground = TextPaint()
    private val mPaintForeground1 = TextPaint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.DARKEN)
    }
    private var textWidth = 0f
    private var textHeight = 0f
    private val clipPath = Path()

    private val animator = ObjectAnimator.ofFloat(this, "progress", 0f, 1f).apply {
        repeatMode = ObjectAnimator.REVERSE
        repeatCount = ObjectAnimator.INFINITE
        duration = 300L
    }

    fun setDuration(duration: Long) {
        animator.duration = duration
    }

    fun setText(text: String) {
        this.text = text
        invalidate()
    }

    fun setTextSize(@Px textSize : Float) {
        this.textSize = textSize
        mPaint.textSize = textSize
        mPaintForeground.textSize = textSize
        mPaintForeground1.textSize = textSize
        invalidate()
    }

    fun setTextColor(@ColorInt textColor: Int) {
        mPaint.color = textColor
        mPaintForeground.color = Color.argb(
            Color.alpha(textColor),
            255 - Color.red(textColor),
            255 - Color.green(textColor),
            255 - Color.blue(textColor),
        )
        mPaintForeground1.color = Color.argb(
            Color.alpha(textColor),
            Color.red(textColor),
            255 - Color.green(textColor),
            Color.blue(textColor),
        )
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        textWidth = mPaint.measureText(text)
        textHeight = (mPaint.fontMetrics.bottom - mPaint.fontMetrics.top)
        setMeasuredDimension(
            getMeasureSize(textWidth.toInt(), widthMeasureSpec, suggestedMinimumWidth),
            getMeasureSize(textHeight.toInt(), heightMeasureSpec, suggestedMinimumHeight),
        )
    }


    private fun getTextBaseline(): Float {
        val fontMetrics = mPaint.fontMetrics
        return measuredHeight - fontMetrics.descent
    }

    fun start() {
        animator.start()
    }

    @Keep
    fun getProgress(): Float {
        return this.animProgress
    }

    @Keep
    fun setProgress(progress : Float) {
        this.animProgress = progress
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = textWidth * animProgress
        val cy = textHeight / 2f * (1 + 1f * sin(2 * PI * animProgress * 4).toFloat())
        val radius = textHeight / 2f


        val cx1 = textWidth * (1 - animProgress)
        val cy1 = textHeight / 2f * (1 + 1f * sin(2 * PI * (0.8f - animProgress) * 4).toFloat())
        val radius1 = textHeight / 2f

        val textBaseLine = measuredHeight - mPaint.getBaseline()

        clipPath.reset()
        clipPath.addCircle(cx, cy, radius, Path.Direction.CW)
        clipPath.addCircle(cx1, cy1, radius1, Path.Direction.CW)


        var storeCount: Int
        storeCount = canvas.save()
        canvas.clipPath(clipPath, Region.Op.DIFFERENCE)
        canvas.drawText(text, 0f, textBaseLine, mPaint)
        canvas.restoreToCount(storeCount)

        clipPath.reset()
        clipPath.addCircle(cx, cy, radius, Path.Direction.CW)

        val saveLayer = canvas.saveLayer(0f, 0f, measuredWidth.toFloat(), measuredHeight.toFloat(), null, Canvas.ALL_SAVE_FLAG)
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        storeCount = canvas.save()
        canvas.clipPath(clipPath, Region.Op.INTERSECT)
        canvas.drawText(text, 0f, textBaseLine, mPaintForeground)
        canvas.restoreToCount(storeCount)

        clipPath.reset()
        clipPath.addCircle(cx1, cy1, radius1, Path.Direction.CW)

        storeCount = canvas.save()
        canvas.clipPath(clipPath, Region.Op.INTERSECT)
        canvas.drawText(text, 0f, textBaseLine, mPaintForeground1)
        canvas.restoreToCount(storeCount)
        canvas.restoreToCount(saveLayer)
    }
}