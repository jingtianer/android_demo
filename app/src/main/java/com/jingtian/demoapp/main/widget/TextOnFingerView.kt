package com.jingtian.demoapp.main.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.annotation.ColorInt
import com.jingtian.demoapp.main.dp

class TextOnFingerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : View(context, attrs, defStyleAttr, defStyleRes) {
    private val paint = Paint()
    private var textColor = Color.RED
    private var path = Path()
    private var text = "HelloWorld!"

    private var textWidth = ArrayList<Lazy<Float>>(text.length)

    init {
        paint.textSize = 15f.dp
        paint.color = textColor
        initTextWidth()
    }

    private var mLastX = 0f
    private var mLastY = 0f
    private var x = 0f
    private var y = 0f
    private var drawInfo = DrawInfo()

    private val bitMap by lazy {
        Bitmap.createBitmap(measuredWidth, measuredHeight, Bitmap.Config.ARGB_8888)
    }
    private val cacheCanvas by lazy {
        Canvas(bitMap)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        bitMap.reconfigure(measuredWidth, measuredHeight, Bitmap.Config.ARGB_8888)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        x = event.x
        y = event.y
        parent?.requestDisallowInterceptTouchEvent(true)
        when(event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                path.reset()
                path.moveTo(event.x, event.y)
                drawInfo = DrawInfo()
            }
            MotionEvent.ACTION_MOVE -> {
                path.lineTo(x, y)
                drawInfo.cal()
                invalidate()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                cacheCanvas.drawCurrentData()
            }
        }
        mLastX = x
        mLastY = y
        super.onTouchEvent(event)
        return true
    }

    fun initTextWidth() {
        textWidth.clear()
        for (i in text.indices) {
            textWidth.add(lazy(LazyThreadSafetyMode.NONE) {
                paint.measureText(text[i].toString()) + if (i > 0) textWidth[i - 1].value else 0f
            })
        }
    }

    fun setColor(@ColorInt color: Int) {
        paint.color = color
        invalidate()
    }

    fun getColor(): Int {
        return paint.color
    }

    fun setTextSize(textSize: Float) {
        paint.textSize = textSize
        resetState()
        invalidate()
    }

    fun setDrawText(text: String) {
        this.text = text
        resetState()
        invalidate()
    }

    private fun resetState() {
        initTextWidth()
        drawInfo = DrawInfo()
        drawInfo.cal()
    }

    private fun String.rotate(N: Int): String {
        return substring(N, length) + substring(0, N)
    }

    inner class DrawInfo(private val offset: Int = 0) {
        private var repeatedString = ""
        private var currentRepeatCnt = 0
        private var remainText = 0
        private val rotatedText = text.rotate(offset)
        private var doubleText = rotatedText + rotatedText
        val drawText: String
            get() {
                if (offset > repeatedString.length) {
                    return doubleText.substring(0, remainText)
                }
                return repeatedString + doubleText.substring(0, remainText)
            }

        fun cal() {
            if (rotatedText.isEmpty()) {
                return
            }
            val pathMeasure = PathMeasure(path, false)
            val pathLength = pathMeasure.measure()
            val repeatCnt = (pathLength / textWidth.last().value).toInt()
            repeatedString += rotatedText.repeat(repeatCnt - currentRepeatCnt)
            currentRepeatCnt = repeatCnt
            val remainedLength = pathLength - repeatCnt * textWidth.last().value
            remainText = textWidth.binarySearch {
                if (it.value == remainedLength) {
                    0
                } else if (it.value > remainedLength) {
                    1
                } else {
                    -1
                }
            }
            if (remainText < 0) {
                remainText = -remainText - 1
            } else if (remainText < text.length){
                remainText + 1
            }
            if (repeatCnt >= 10) {
                cacheCanvas.drawCurrentData()
                path.reset()
                path.moveTo(x, y)
                drawInfo = DrawInfo((remainText + offset) % text.length)
            }
        }
    }


    private fun PathMeasure.measure(): Float {
        var totalLength = 0f
        do {
            val segmentLength = length
            totalLength += segmentLength
        } while (nextContour())

        return totalLength
    }

    private fun Canvas.drawCurrentData() {
        drawTextOnPath(drawInfo.drawText, path, 0f, 0f, paint)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawBitmap(bitMap, 0f, 0f, null)
        canvas.drawCurrentData()
    }
}