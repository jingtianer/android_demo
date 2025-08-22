package com.jingtian.demoapp.main.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Region
import android.util.AttributeSet
import android.view.View
import android.view.ViewOutlineProvider
import com.jingtian.demoapp.main.ReflectClass
import com.jingtian.demoapp.main.ReflectObject
import com.jingtian.demoapp.main.dp

class OverDrawView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0,
): View(context, attributeSet, defStyleAttr, defStyleRes) {
    private val offset = 100f.dp
    private val radius = 10f.dp
    private val correctShadowRadius = 800f.dp
    private val realDx = 1f.dp
    private val realDy = 4f.dp
    private val shadowColor = Color.GREEN
    private val fillColor = Color.RED
    private val fillColor1 = Color.YELLOW
    private val strokeWidth = 10f.dp

    init {
        clipToOutline = false
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline?) {
                outline?.setRoundRect(offset.toInt(), offset.toInt(), (100f.dp - offset).toInt(), (100f.dp - offset).toInt(), radius)
            }
        }
    }

    private val paint = Paint().apply {
        color = fillColor
        isAntiAlias = true
        setShadowLayer(
            correctShadowRadius,
            realDx,
            realDy,
            shadowColor
        )
    }

    override fun setClipBounds(clipBounds: Rect?) {
        val bounds = clipBounds ?: Rect()
        val offset = offset.toInt()
        bounds.set(
            -offset,
            -offset,
            offset + width,
            offset + height,
        )
        super.setClipBounds(bounds)
    }

    private val boundPaint = Paint().apply {
        color = fillColor1
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = this@OverDrawView.strokeWidth
    }

    private val instrumentedClipRect : Canvas.(Float, Float, Float, Float, Int) -> Boolean = run {
        val method = try {
            ReflectClass(Canvas::class.java).method("clipRect", arrayOf(
                Float::class.javaPrimitiveType!!,
                Float::class.javaPrimitiveType!!,
                Float::class.javaPrimitiveType!!,
                Float::class.javaPrimitiveType!!,
                Int::class.javaPrimitiveType!!,
            ))
        } catch (ignore : Exception) {
            null
        }
        val returnFunction : Canvas.(Float, Float, Float, Float, Int) -> Boolean = { l, t, r, b, op ->
            method?.obj = this
            method?.call<Boolean>(arrayOf(
                l, t, r, b, op
            )) ?: false
        }
        returnFunction
    }

    private val nClipRect : Canvas.(Float, Float, Float, Float, Int) -> Boolean = run {
        val method = try {
            ReflectClass(Canvas::class.java).method("nClipRect", arrayOf(
                Long::class.javaPrimitiveType!!,
                Float::class.javaPrimitiveType!!,
                Float::class.javaPrimitiveType!!,
                Float::class.javaPrimitiveType!!,
                Float::class.javaPrimitiveType!!,
                Int::class.javaPrimitiveType!!,
            ))
        } catch (ignore : Exception) {
            null
        }
        val getNativeCanvasWrapper = ReflectClass(Canvas::class.java).method("getNativeCanvasWrapper", arrayOf())
        val returnFunction : Canvas.(Float, Float, Float, Float, Int) -> Boolean = { l, t, r, b, op ->
            method?.obj = this
            getNativeCanvasWrapper.obj = this
            val canvasId = getNativeCanvasWrapper.call<Long>(arrayOf())!!
            method?.call<Boolean>(arrayOf(
                canvasId, l, t, r, b, op
            )) ?: false
        }
        returnFunction
    }

    private val instrumentedViewClipRect : Canvas.(Float, Float, Float, Float, Int) -> Boolean = run {
        val method = try {
            ReflectObject(this).method("clipRect", arrayOf(
                Canvas::class.java,
                Float::class.javaPrimitiveType!!,
                Float::class.javaPrimitiveType!!,
                Float::class.javaPrimitiveType!!,
                Float::class.javaPrimitiveType!!,
                Int::class.javaPrimitiveType!!,
            ))
        } catch (ignore : Exception) {
            throw ignore
        }
        try {
            ReflectClass(Canvas::class.java).method("nClipRect", arrayOf(
                Long::class.javaPrimitiveType!!,
                Float::class.javaPrimitiveType!!,
                Float::class.javaPrimitiveType!!,
                Float::class.javaPrimitiveType!!,
                Float::class.javaPrimitiveType!!,
                Int::class.javaPrimitiveType!!,
            ))
        } catch (ignore : Exception) {
            throw ignore
        }
        val returnFunction : Canvas.(Float, Float, Float, Float, Int) -> Boolean = { l, t, r, b, op ->
            method?.call<Boolean>(arrayOf(
                this, l, t, r, b, op
            )) ?: false
        }
        returnFunction
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val savedCount = canvas.save()
        canvas.nClipRect(-offset, -offset, width + offset, height + offset, 5)
        val bounds = 0f
        canvas.drawRoundRect(
            bounds,
            bounds,
            width.toFloat() - bounds,
            height.toFloat() - bounds,
            radius,
            radius,
            paint,
        )
        canvas.drawRect(
            strokeWidth / 2,
            strokeWidth / 2,
            width.toFloat() - strokeWidth / 2,
            height.toFloat() - strokeWidth / 2,
            boundPaint
        )
        canvas.restoreToCount(savedCount)
    }
}