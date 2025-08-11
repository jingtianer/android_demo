package com.jingtian.demoapp.main.widget
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.StyleRes

class ChildShadowFrameLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {

    init {
        setWillNotDraw(false)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (i in 0 until childCount) {
            val child = getChildAt(i) ?: continue
            val lp = child.layoutParams as? LayoutParams ?: continue
            lp.draw(
                canvas,
                child.left.toFloat(),
                child.top.toFloat(),
                child.right.toFloat(),
                child.bottom.toFloat(),
            )
        }
    }

    class LayoutParams : FrameLayout.LayoutParams {
        private var params: Params
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private var outerRadius = 0F

        private var desiredLeftPadding = 0F
        private var desiredRightPadding = 0F
        private var desiredTopPadding = 0F
        private var desiredBottomPadding = 0F

        private var realDx = 0F
        private var realDy = 0F
        private var correctShadowRadius = 0F

        constructor(width: Int, height: Int, gravity: Int) : super(width, height, gravity) {
            this.params = emptyParams()
        }

        constructor(width: Int, height: Int) : super(width, height) {
            this.params = emptyParams()
        }

        constructor(layoutParams: LayoutParams) : super(layoutParams) {
            this.params = layoutParams.params
            applyParam(this.params)
        }

        constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
            this.params = emptyParams()
        }

        constructor(source: ViewGroup.LayoutParams) : super(source) {
            this.params = emptyParams()
        }

        constructor(source: MarginLayoutParams) : super(source) {
            this.params = emptyParams()
        }

        private fun emptyParams() = Params()

        fun setParam(params: Params) {
            this.params = params
            applyParam(params)
        }

        fun getParam(): Params {
            return params
        }

        private fun applyParam(params: Params) {
            outerRadius = params.outerRadius
            val shadowRadius = params.shadowRadius
            correctShadowRadius = shadowRadius.toFloat()
            val dx = params.dx.toFloat()
            val dy = params.dy.toFloat()

            val alpha = Color.alpha(params.shadowColor)
            val alphaToFloat = alpha.toFloat() / 255

            // 先假定dx、dy都为0；然后按照 Figma 的参数，先算出 UI 同学期望的阴影 padding 值。
            desiredLeftPadding = shadowRadius.toFloat()
            desiredRightPadding = shadowRadius.toFloat()
            desiredTopPadding = shadowRadius.toFloat()
            desiredBottomPadding = shadowRadius.toFloat()
            if (params.excludeDx) {
                desiredLeftPadding = 0F
                desiredRightPadding = 0F
            }
            if (params.excludeLeft) {
                desiredLeftPadding = 0F
            }
            if (params.excludeRight) {
                desiredRightPadding = 0F
            }
            if (params.excludeTop) {
                desiredTopPadding = 0F
            }
            if (params.excludeBottom) {
                desiredBottomPadding = 0F
            }

            realDx = dx
            realDy = dy

            if (params.dx != 0 && !params.excludeDx) {
                desiredLeftPadding -= dx
                desiredRightPadding += dx

                val outerShadowDx = alphaToFloat * dx
                correctShadowRadius = shadowRadius - (outerShadowDx / 2)
                realDx = dx - outerShadowDx + (outerShadowDx / 2)
            }

            if (params.dy != 0) {
                desiredTopPadding -= dy
                desiredBottomPadding += dy

                val outerShadowDy = alphaToFloat * dy
                correctShadowRadius = shadowRadius - (outerShadowDy / 2)
                realDy = dy - outerShadowDy + (outerShadowDy / 2)
            }

            paint.isAntiAlias = true
            paint.color = params.shapeColor
            paint.setShadowLayer(
                correctShadowRadius,
                realDx,
                realDy,
                params.shadowColor
            )
        }

        fun draw(canvas: Canvas, left: Float, top: Float, right: Float, bottom: Float) {
            canvas.drawRoundRect(
                left,
                top,
                right,
                bottom,
                outerRadius,
                outerRadius,
                paint
            )
        }
    }

    class Params(
        val outerRadius: Float = 0f,
        @ColorInt val shapeColor: Int = 0,
        val dx: Int = 0,
        val dy: Int = 0,
        val shadowRadius: Int = 0,
        val excludeDx: Boolean = false,
        @ColorInt val shadowColor: Int = 0,
        val excludeLeft: Boolean = false,
        val excludeTop: Boolean = false,
        val excludeRight: Boolean = false,
        val excludeBottom: Boolean = false,
    )
}