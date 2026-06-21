package com.jingtian.demoapp.main.lissajous

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewConfiguration
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.jingtian.demoapp.BuildConfig
import com.jingtian.demoapp.main.rank.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.round

class CurveView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0, defStyleRes: Int = 0
) : View(context, attrs, defStyleAttr, defStyleRes) {

    private var lifecycleOwner: LifecycleOwner? = context as? LifecycleOwner

    private var DEFAULT_SCALE = -1f

    // ===================== 双指缩放 新增 =====================
    private val scaleGestureDetector = ScaleGestureDetector(context, ScaleListener())
    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scale *= detector.scaleFactor
            return true
        }
    }

    private val gestureDetector: GestureDetector by lazy(LazyThreadSafetyMode.NONE) {
        val detector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {})
        detector.setOnDoubleTapListener(object : GestureDetector.OnDoubleTapListener {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                return false
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                scale = DEFAULT_SCALE
                canvasOffsetX = 0f
                canvasOffsetY = 0f
                return true
            }

            override fun onDoubleTapEvent(e: MotionEvent): Boolean {
                return false
            }
        })
        detector
    }

    // 曲线配置
    var lisajousCurve: LisajousCurve = LisajousCurve()
        set(value) {
            field = value
            updateAll()
        }

    // ===================== 【预编译 Path】 =====================
    private val pathOrigin = Path()   // 原始曲线，提前编译好

    // ===================== 画笔 =====================
    private val paintOrigin = Paint().apply {
        color = Color.BLUE
        strokeWidth = 3f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }
    // 新增 坐标轴画笔
    private val paintAxis = Paint().apply {
        color = 0xFF999999.toInt()
        strokeWidth = 1.5f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }
    // 刻度文字画笔
    private val paintAxisText = Paint().apply {
        color = 0xFF666666.toInt()
        textSize = 24f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }

    // 坐标变换
    private var scale: Float = DEFAULT_SCALE.toFloat()
        set(value) {
            field = value
            // 缩放后只需要重新刷新路径坐标，不需要重算数学
            redrawAll()
        }

    private var screenOffsetX = 0f
    private var screenOffsetY = 0f

    private val offsetX get() = screenOffsetX + canvasOffsetX
    private val offsetY get() = screenOffsetY + canvasOffsetY

    private fun toScreenX(x: Float) = offsetX + x * scale
    private fun toScreenY(y: Float) = offsetY - y * scale

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w <= 0 || h <= 0) {
            return
        }
        if (floor(offsetX) != floor(w / 2f) || floor(offsetY) != floor(h / 2f)) {
            screenOffsetX = w / 2f
            screenOffsetY = h / 2f
            DEFAULT_SCALE = min(w, h) / 2 * 0.618f
            if (scale <= 0) {
                scale = DEFAULT_SCALE
            }
            redrawAll()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (floor(offsetX) != floor(measuredWidth / 2f) || floor(offsetY) != floor(measuredHeight / 2f)) {
            screenOffsetX = measuredWidth / 2f
            screenOffsetY = measuredHeight / 2f
            DEFAULT_SCALE = min(measuredWidth, measuredHeight) / 2 * 0.618f
            if (scale <= 0) {
                scale = DEFAULT_SCALE
            }
            redrawAll()
        }
    }

    // ===================== 全部提前计算 + 提前编译 Path =====================
    private fun updateAll() {
        originPathTask.run()
    }

    private fun redrawAll() {
        originPathTask.scheduleRedraw()
        axisJob.scheduleRedraw()
    }

    private inline fun <T> runTask(crossinline task: suspend ()->T, crossinline callback: (T)->Unit = {}, crossinline onError: (Throwable)->Unit = {}): Job {
        val scope = (lifecycleOwner?.lifecycleScope ?: Utils.CoroutineUtils.globalScope)
        return scope.launch(Dispatchers.Default) {
            runCatching {
                task()
            }.onSuccess {
                withContext(Dispatchers.Main) {
                    callback(it)
                }
            }.onFailure {
                withContext(Dispatchers.Main) {
                    onError(it)
                }
            }
        }
    }

    private fun createAxisInfo() = AxisInfo(offsetX, offsetY, measuredWidth, measuredHeight, scale, paintAxisText)

    private val axisJob = CanvasJob("axisJob", {}, {
        this.axisInfo = createAxisInfo()
    }, {
        this.axisInfo = createAxisInfo()
    })

    private val originPathTask = CanvasJob("originPathTask", {
        calcOriginPath()
    }, {
        updateOriginPath(it)
    }, {
        clearOriginPath()
    })

    /** 预编译：原始曲线 Path */
    private fun calcOriginPath(): Pair<List<Float>, List<Float>> {
        val step = (lisajousCurve.tMax - lisajousCurve.tMin) / lisajousCurve.stepCount.toFloat()
        val xPoint = LisajousCurveMath.evalX(lisajousCurve, (0..lisajousCurve.stepCount).map { lisajousCurve.tMin + step * it })
        val yPoint = LisajousCurveMath.evalY(lisajousCurve, (0..lisajousCurve.stepCount).map { lisajousCurve.tMin + step * it })
        return xPoint to yPoint
    }

    private fun updateOriginPath(path: Pair<List<Float>, List<Float>>) {
        pathOrigin.reset()
        var first = true
        val (xPoint, yPoint) = path
        for (i in 0 until min(xPoint.size, yPoint.size)) {
            val x = toScreenX(xPoint[i])
            val y = toScreenY(yPoint[i])
            if (first) {
                pathOrigin.moveTo(x, y)
                first = false
            } else {
                pathOrigin.lineTo(x, y)
            }
        }
    }

    private fun clearOriginPath() {
        pathOrigin.reset()
    }

    private var axisInfo: AxisInfo = createAxisInfo()

    // ===================== 【最极致轻量 onDraw】 =====================
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        Utils.timeCost("onDraw->drawAxis") {
            axisInfo.drawAxis(canvas)
        }

        // 只绘制，无任何计算、无循环、无Path创建
        Utils.timeCost("onDraw->pathOrigin") {
            canvas.drawPath(pathOrigin, paintOrigin)
        }
    }

    class AxisInfo(offsetX: Float, offsetY: Float, measuredWidth: Int, measuredHeight: Int, scale: Float, paintAxisText: Paint) {
        val originScreenX = offsetX
        val originScreenY = offsetY

        fun Float.print(): String {
            return if (abs(this) < 0.01 || abs(this) > 1e4) {
                String.format("%.2e", this)
            } else {
                String.format("%.3f", this)
            }
        }

        // 单位1长度: scale
        // 单位1的个数
        val xUnitOneCount = ceil(measuredWidth.toFloat() / 2f / scale).toInt()
        val yUnitOneCount = ceil(measuredHeight.toFloat() / 2f / scale).toInt()

        val screenAspectRatio = if (measuredHeight > 0) (measuredWidth.toFloat() / measuredHeight.toFloat()) else 1f

        val xBarCount = 3f
        val xStepValue: Float
        val xStepCount: Int

        val yBarCount = round(xBarCount / screenAspectRatio)
        val yStepValue: Float
        val yStepCount: Int

        init {
            val (xStepValue, xStepCount) = if (xUnitOneCount <= 1) {
                val xUnitOneCount = measuredWidth.toFloat() / 2f / scale
                xUnitOneCount / xBarCount to ceil(xUnitOneCount / (xUnitOneCount / xBarCount)).toInt()
            } else if (xUnitOneCount <= xBarCount) {
                1f to xUnitOneCount
            } else {
                xUnitOneCount / xBarCount to ceil(xUnitOneCount / (xUnitOneCount / xBarCount)).toInt()
            }
            this.xStepValue = xStepValue
            this.xStepCount = xStepCount
            val (yStepValue, yStepCount) = if (xUnitOneCount <= 1) {
                val yUnitOneCount = measuredHeight.toFloat() / 2f / scale
                yUnitOneCount / yBarCount to ceil(yUnitOneCount / (yUnitOneCount / yBarCount)).toInt()
            } else if (yUnitOneCount <= yBarCount) {
                1f to yUnitOneCount
            } else {
                yUnitOneCount / yBarCount to ceil(yUnitOneCount / (yUnitOneCount / yBarCount)).toInt()
            }
            this.yStepValue = yStepValue
            this.yStepCount = yStepCount
        }

        val xBarHeight = xStepValue * scale

        val yBarHeight = yStepValue * scale

        // X轴正方向刻度
        val xPos = originScreenX + xBarHeight
        val yPos = originScreenY + yBarHeight

        val xPosInfo = (0 until xStepCount).map {
            ((it + 1) * xStepValue).print()
        }

        val xNegInfo = (0 until xStepCount).map {
            (-(it + 1) * xStepValue).print()
        }

        val yNegInfo = (0 until  yStepCount).map {
            (-(it + 1) * yStepValue).print().let { text->
                text to (paintAxisText.measureText(text) / 2f + 12f)
            }
        }

        val yPosInfo = (0 until  yStepCount).map {
            ((it + 1) * yStepValue).print().let { text->
                text to (paintAxisText.measureText(text) / 2f + 12f)
            }
        }

        init {
            if (BuildConfig.IS_DEBUG) {
                Log.d("jingtian", "drawInfo: $this")
            }
        }

        override fun toString(): String {
            return "${xStepValue}, ${xStepCount}, ${yStepValue}, ${yStepCount}"
        }
    }

    /** 绘制XY坐标轴 + 简易刻度 */
    private fun AxisInfo.drawAxis(canvas: Canvas) {
        // 原点屏幕坐标
        val originScreenX = offsetX
        val originScreenY = offsetY

        // X轴 水平
        canvas.drawLine(0f, originScreenY, width.toFloat(), originScreenY, paintAxis)
        // Y轴 垂直
        canvas.drawLine(originScreenX, 0f, originScreenX, height.toFloat(), paintAxis)

        // 绘制原点文字
        canvas.drawText("O", originScreenX - 15f, originScreenY + 30f, paintAxisText)

        var xPos = this.xPos
        xPosInfo.forEach { text->
            canvas.drawLine(xPos, originScreenY - 8f, xPos, originScreenY + 8f, paintAxis)
            canvas.drawText(text, xPos, originScreenY + 35f, paintAxisText)
            xPos += xBarHeight
        }

        // X轴负方向刻度
        xPos = originScreenX - xBarHeight
        xNegInfo.forEach { text->
            canvas.drawLine(xPos, originScreenY - 8f, xPos, originScreenY + 8f, paintAxis)
            canvas.drawText(text, xPos, originScreenY + 35f, paintAxisText)
            xPos -= xBarHeight
        }

        // y轴负方向刻度
        var yPos = this.yPos
        yNegInfo.forEach { (text, textWidth)->
            canvas.drawLine(originScreenX - 8f, yPos, originScreenX + 8f, yPos, paintAxis)
            canvas.drawText(text, originScreenX + textWidth, yPos + 8f, paintAxisText)
            yPos += yBarHeight
        }

        // y轴正方向刻度
        yPos = originScreenY - yBarHeight
        yPosInfo.forEach { (text, textWidth)->
            canvas.drawLine(originScreenX - 8f, yPos, originScreenX + 8f, yPos, paintAxis)
            canvas.drawText(text, originScreenX + textWidth, yPos + 8f, paintAxisText)
            yPos -= yBarHeight
        }
    }


    // ===================== 拖拽 =====================
    private var isDraggingCanvas = false
    private var canvasOffsetX = 0f
    private var canvasOffsetY = 0f
    private var canvasStartX = 0f
    private var canvasStartY = 0f
    private var canvasOffsetPointerId = 0
    override fun onTouchEvent(event: MotionEvent): Boolean {
        parent?.requestDisallowInterceptTouchEvent(true)
        scaleGestureDetector.onTouchEvent(event)
        if (gestureDetector.onTouchEvent(event)) {
            return true
        }
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isDraggingCanvas = true
                canvasStartX = event.getX(0)
                canvasStartY = event.getY(0)
                canvasOffsetPointerId = event.getPointerId(0)
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDraggingCanvas) {
                    val lastPointerIndex = event.findPointerIndex(canvasOffsetPointerId)
                    val pointerIndex = if (lastPointerIndex in 0 until event.pointerCount) {
                        if (event.pointerCount == 1) {
                            canvasOffsetX += event.getX(lastPointerIndex) - canvasStartX
                            canvasOffsetY += event.getY(lastPointerIndex) - canvasStartY
                        }
                        lastPointerIndex
                    } else {
                        canvasOffsetPointerId = event.getPointerId(0)
                        0
                    }
                    canvasStartX = event.getX(pointerIndex)
                    canvasStartY = event.getY(pointerIndex)
                    redrawAll()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDraggingCanvas = false
            }
        }
        super.onTouchEvent(event)
        return true
    }

    inner class CanvasJob<T>(private val tag: String, private val task: ()->T, private val callback: (T)->Unit = {}, private val onError: (Throwable) -> Unit) {
        private var job: Job? = null
        private var scheduleJob: Job? = null
        private var scheduleRedrawJob: Job? = null
        private var result: T? = null
        fun run(redraw: Boolean = false) {
            val result = result
            job?.cancel()
            scheduleJob?.cancel()
            scheduleRedrawJob?.cancel()
            job = runTask({
                if (redraw && result != null) {
                    result
                } else {
                    task.invoke()
                }
            }, { result->
                Utils.timeCost(tag) {
                    callback.invoke(result)
                }
                this.result = result
                job = null
                invalidate()
            }, onError = {
                job = null
                this.result = null
                onError.invoke(it)
                invalidate()
            })
        }

        fun schedule() {
            scheduleJob?.cancel()
            scheduleRedrawJob?.cancel()
            scheduleJob = runTask({
                job?.join()
                run()
            }, callback = {
                scheduleJob = null
            }, onError = {
                scheduleJob = null
            })
        }

        fun scheduleRedraw() {
            scheduleRedrawJob?.cancel()
            scheduleRedrawJob = runTask({
                job?.join()
                scheduleJob?.join()
                run(true)
            }, callback = {
                scheduleRedrawJob = null
            }, onError = {
                scheduleRedrawJob = null
            })
        }
    }
}