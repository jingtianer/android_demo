package com.jingtian.demoapp.main.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import com.jingtian.demoapp.R
import com.jingtian.demoapp.main.base.ISharedElement
import com.jingtian.demoapp.main.base.SharedElementParcelable

class RoundRectImageView : AppCompatImageView, ISharedElement {

    val radii: FloatArray
    private val roundRectPath = Path()
    private var w = 0
    private var h = 0

    constructor(context: Context, radii: FloatArray) : super(context) {
        assert(radii.size == 8)
        this.radii = radii
    }

    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
    ) : super(context, attrs, defStyleAttr) {
        this.radii = FloatArray(8)
        attrs?.apply {
            val a = resources.obtainAttributes(attrs, R.styleable.RoundRectImageView)
            val radius = a.getDimension(R.styleable.RoundRectImageView_radius, 0f)
            val radiusLT = a.getDimension(R.styleable.RoundRectImageView_radiusLeftTop, radius)
            val radiusLB = a.getDimension(R.styleable.RoundRectImageView_radiusLeftBottom, radius)
            val radiusRT = a.getDimension(R.styleable.RoundRectImageView_radiusRightTop, radius)
            val radiusRB = a.getDimension(R.styleable.RoundRectImageView_radiusRightBottom, radius)
            a.recycle()
            updateRadii(radiusLT, radiusLB, radiusRT, radiusRB)
        }
    }

    fun setRadius(radius: Float) {
        updateRadii(radius, radius, radius, radius)
        invalidate()
    }

    fun updateRadii(
        radiusLT: Float = radii[0],
        radiusLB: Float = radii[2],
        radiusRT: Float = radii[4],
        radiusRB: Float = radii[6],
    ) {
        radii[0] = radiusLT
        radii[1] = radiusLT

        radii[2] = radiusRT
        radii[3] = radiusRT

        radii[4] = radiusRB
        radii[5] = radiusRB

        radii[6] = radiusLB
        radii[7] = radiusLB

        updateClipPath()
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        this.w = w
        this.h = h
        updateClipPath(w, h)
        invalidate()
    }

    private fun updateClipPath(width: Int = w, height: Int = h) {
        roundRectPath.reset()
        roundRectPath.addRoundRect(
            0f,
            0f,
            width.toFloat(),
            height.toFloat(),
            radii,
            Path.Direction.CW
        )
    }

    override fun draw(canvas: Canvas) {
        val savedCount = canvas.save()
        canvas.clipPath(roundRectPath)
        super.draw(canvas)
        canvas.restoreToCount(savedCount)
    }

    inner class SharedElementParcelableImpl : SharedElementParcelable() {
        init {
            bundle.putFloatArray("RoundRectImageView:radii", radii)
        }
        override fun createSnapShotView(
            context: Context
        ): View {
            return RoundRectImageView(context).apply {
                System.arraycopy(this.radii, 0, radii, 0, 8)
            }
        }
    }

    override fun onCaptureSharedElementSnapshot(): SharedElementParcelable {
        return SharedElementParcelableImpl()
    }

    var startRadii = FloatArray(8)
    var endRadii = FloatArray(8)

    override fun applySnapShot(snapshot: SharedElementParcelable?, isStart: Boolean): View {
        snapshot ?: return this
        if (isStart) {
            startRadii = radii
            endRadii = snapshot.bundle.getFloatArray("RoundRectImageView:radii") ?: FloatArray(8)
        } else {
            startRadii = snapshot.bundle.getFloatArray("RoundRectImageView:radii") ?: FloatArray(8)
            endRadii = radii
        }
        return this
    }
}