package com.jingtian.demoapp.main.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.annotation.ColorInt
import com.jingtian.demoapp.main.MutableLazy
import com.jingtian.demoapp.main.dp
import kotlin.math.max

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

    private val rotateFunctions = run {
        val rotateMap = HashMap<Int, String>()
        val rotate: String.(Int) -> String = { N: Int ->
            rotateMap.getOrPut(N) {
                substring(N, length) + substring(0, N)
            }
        }
        val clearRotate = {
            rotateMap.clear()
        }
        rotate to clearRotate
    }

    private val rotate = rotateFunctions.first
    private val clearRotate = rotateFunctions.second


    private val repeatTextFunctions = run {
        val rotateMap = HashMap<Int, String>()
        val repeat: String.(Int) -> String = { N: Int ->
            rotateMap.getOrPut(N) {
                this.repeat(N)
            }
        }
        val resetRepeat: () -> Unit = {
            rotateMap.clear()
        }
        repeat to resetRepeat
    }

    private val repeatDrawText = repeatTextFunctions.first
    private val resetRepeatCache = repeatTextFunctions.second

    private var x = 0f
    private var y = 0f
    private var drawInfo = DrawInfo()

    private var bitMap by MutableLazy {
        Bitmap.createBitmap(measuredWidth, measuredHeight, Bitmap.Config.ARGB_8888)
    }
    private var cacheCanvas by MutableLazy {
        Canvas(bitMap)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (!(w >= bitMap.width || h >= bitMap.height)) {
            return
        }
        val oldBitmap = bitMap
        createNewCanvas(max(w, bitMap.width), max(h, bitMap.height))
        cacheCanvas.drawBitmap(oldBitmap, 0f, 0f, null)
        oldBitmap.recycle()
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        x = event.x
        y = event.y
        when(event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                path.reset()
                path.moveTo(event.x, event.y)
                drawInfo.reset()
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
        return super.onTouchEvent(event)
    }

    private fun initTextWidth() {
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
        resetRepeatCache()
        clearRotate()
        resetState()
        invalidate()
    }

    private fun resetState() {
        initTextWidth()
        drawInfo = DrawInfo()
        drawInfo.cal()
    }

    private fun createNewCanvas(width: Int = bitMap.width, height:Int = bitMap.height) {
        path.reset()
        drawInfo = DrawInfo()
        bitMap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        cacheCanvas = Canvas(bitMap)
    }

    fun clearCanvas(width: Int = bitMap.width, height:Int = bitMap.height) {
        val oldBitmap = bitMap
        createNewCanvas(width, height)
        oldBitmap.recycle()
        invalidate()
    }

    inner class DrawInfo(private var offset: Int = 0) {
        private var repeatedString = ""
        private var currentRepeatCnt = 0
        private var remainText = 0
        private var rotatedText = text.rotate(offset)
        private val pathMeasure = PathMeasure(path, false)
        val drawText: String
            get() {
                if (offset > repeatedString.length) {
                    return rotatedText.substring(0, remainText)
                }
                return repeatedString + rotatedText.substring(0, remainText)
            }

        fun reset(offset: Int = 0) {
            this.offset = offset
            rotatedText = text.rotate(offset)
            repeatedString = ""
            currentRepeatCnt = 0
            remainText = 0
        }

        fun cal() {
            if (rotatedText.isEmpty()) {
                return
            }
            pathMeasure.setPath(path, false)
            val pathLength = pathMeasure.length
            val repeatCnt = (pathLength / textWidth.last().value).toInt()
            repeatedString += text.repeatDrawText(repeatCnt - currentRepeatCnt)
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
                remainText += 1
            }
            if (repeatCnt >= 1) {
                remainText = 0
                val (x, y) = pathMeasure.moveBackward(remainedLength)

                cacheCanvas.drawCurrentData()
                path.reset()
                path.moveTo(x, y)
                drawInfo.reset((remainText + offset) % text.length)
            }
        }
    }

    private val moveBackward =  run {
        val pos = FloatArray(2)
        val inner: PathMeasure.(Float) -> FloatArray = { dis: Float ->
            getPosTan(length - dis, pos, null)
            pos
        }
        inner
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