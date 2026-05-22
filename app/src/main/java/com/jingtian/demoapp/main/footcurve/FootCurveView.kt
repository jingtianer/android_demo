package com.jingtian.demoapp.main.footcurve

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
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

class FootCurveView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0, defStyleRes: Int = 0
) : View(context, attrs, defStyleAttr, defStyleRes) {

    companion object {
        private const val DEFAULT_SCALE = 200f
    }

    var lifecycleOwner: LifecycleOwner? = context as? LifecycleOwner

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
    var curve: Curve = Curve()
        set(value) {
            field = value
            updateAll()
        }

    // 当前切点参数 t
    var currentT: Float = curve.initT
        get() = curve.initT
        set(value) {
            field = value
            curve.initT = value
            tangentJob.run()
        }

    private fun calcTangentInfo(): FloatArray {
        return FootCurveMath.calcFoot(curve, currentT, px, py)
    }


    // 可拖动定点 P
    var px: Float = curve.initPx
        get() = curve.initPx
        set(value) {
            field = value
            curve.initPx = value
            footPathJob.schedule()
            tangentJob.schedule()
            invalidate()
        }
    var py: Float = curve.initPy
        get() = curve.initPy
        set(value) {
            field = value
            curve.initPy = value
            footPathJob.schedule()
            tangentJob.schedule()
            invalidate()
        }

    val pxPyChangeListener = mutableListOf<(Float, Float)->Unit>()

    fun dispatchPxPyChange() {
        pxPyChangeListener.forEach { it.invoke(px, py) }
    }

    // ===================== 【预编译 Path】 =====================
    private val pathOrigin = Path()   // 原始曲线，提前编译好
    private val pathFoot = Path()     // 垂足曲线，提前编译好

    // 切线、垂线 两点坐标（仅存坐标，不计算）
    private var lineTan = FloatArray(4)
    private var linePerp = FloatArray(4)

    // 绘制点坐标
    private var pointP = FloatArray(2) // 定点
    private var pointTan = FloatArray(2) // 切点
    private var pointFoot = FloatArray(2) // 垂足

    // ===================== 画笔 =====================
    private val paintOrigin = Paint().apply {
        color = Color.BLUE
        strokeWidth = 3f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }
    private val paintFoot = Paint().apply {
        color = Color.MAGENTA
        strokeWidth = 3f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }
    private val paintTangent = Paint().apply {
        color = Color.GRAY
        strokeWidth = 2f
        pathEffect = DashPathEffect(floatArrayOf(10f,10f), 0f)
    }
    private val paintPerp = Paint().apply {
        color = Color.BLACK
        strokeWidth = 2f
    }
    private val paintPoint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
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
    private var scale = DEFAULT_SCALE
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
            Log.d("jingtian", "onSizeChanged: redraw, w=$w, h=$h, $measuredWidth, $measuredHeight")
            screenOffsetX = w / 2f
            screenOffsetY = h / 2f
            redrawAll()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (floor(offsetX) != floor(measuredWidth / 2f) || floor(offsetY) != floor(measuredHeight / 2f)) {
            Log.d("jingtian", "onMeasure: redraw, $measuredWidth, $measuredHeight")
            screenOffsetX = measuredWidth / 2f
            screenOffsetY = measuredHeight / 2f
            redrawAll()
        }
    }

    // ===================== 全部提前计算 + 提前编译 Path =====================
    private fun updateAll() {
        originPathTask.run()
        footPathJob.run()
        tangentJob.run()
    }

    private fun redrawAll() {
        originPathTask.scheduleRedraw()
        footPathJob.scheduleRedraw()
        tangentJob.scheduleRedraw()
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
        val step = (curve.tMax - curve.tMin) / 300f
        val xPoint = FootCurveMath.eval(curve.paramName, curve.xExprStr, (0..300).map { curve.tMin + step * it })
        val yPoint = FootCurveMath.eval(curve.paramName, curve.yExprStr, (0..300).map { curve.tMin + step * it })
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

    private val footPathJob = CanvasJob("footPathJob", {
        calcFootPath()
    }, {
        updateFootPath(it)
    }, {
        clearFootPath()
    })

    private fun calcFootPath(): List<FloatArray> {
        val step = (curve.tMax - curve.tMin) / 300f
        val fList = FootCurveMath.calcFoot(curve, (0..300).map { curve.tMin + step * it }, px, py)
        return fList
    }

    /** 预编译：垂足曲线 Path */
    private fun updateFootPath(fList: List<FloatArray>) {
        pathFoot.reset()
        var first = true
        for (f in fList) {
            val sx = toScreenX(f[2])
            val sy = toScreenY(f[3])
            if (first) {
                pathFoot.moveTo(sx, sy)
                first = false
            } else {
                pathFoot.lineTo(sx, sy)
            }
        }
    }

    private fun clearFootPath() {
        pathFoot.reset()
    }

    private val tangentJob = CanvasJob("tangentJob", {
        calcTangentInfo()
    }, {
        updateTangentInfo(it)
    }, {
        clearTangentInfo()
    })

    private fun clearTangentInfo() {
        lineTan.fill(0f)
        linePerp.fill(0f)
        pointP.fill(0f)
        pointTan.fill(0f)
        pointFoot.fill(0f)
    }

    /** 预计算：切线、垂线、点坐标 */
    private fun updateTangentInfo(f: FloatArray) {
        val cx = f[0]; val cy = f[1]
        val fx = f[2]; val fy = f[3]
        val dx = f[4]; val dy = f[5]

        // 切线线段
        val L = max(hypot(cx - px, cy - py), 1f) + 1f
        val d = hypot(dx, dy)
        val x1 = cx - dx/d*L
        val y1 = cy - dy/d*L
        val x2 = cx + dx/d*L
        val y2 = cy + dy/d*L
        lineTan[0] = toScreenX(x1)
        lineTan[1] = toScreenY(y1)
        lineTan[2] = toScreenX(x2)
        lineTan[3] = toScreenY(y2)

        // 垂线
        linePerp[0] = toScreenX(px)
        linePerp[1] = toScreenY(py)
        linePerp[2] = toScreenX(fx)
        linePerp[3] = toScreenY(fy)

        // 点坐标
        pointP[0] = linePerp[0]
        pointP[1] = linePerp[1]
        pointTan[0] = toScreenX(cx)
        pointTan[1] = toScreenY(cy)
        pointFoot[0] = linePerp[2]
        pointFoot[1] = linePerp[3]
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
        Utils.timeCost("onDraw->pathFoot") {
            canvas.drawPath(pathFoot, paintFoot)
        }
        Utils.timeCost("onDraw->lineTan") {
            canvas.drawLines(lineTan, paintTangent)
        }
        Utils.timeCost("onDraw->linePerp") {
            canvas.drawLines(linePerp, paintPerp)
        }
        Utils.timeCost("onDraw->drawDots") {
            paintPoint.color = 0xFFFF9800.toInt()
            canvas.drawCircle(pointP[0], pointP[1], 8f, paintPoint)

            paintPoint.color = Color.RED
            canvas.drawCircle(pointTan[0], pointTan[1], 8f, paintPoint)

            paintPoint.color = Color.GREEN
            canvas.drawCircle(pointFoot[0], pointFoot[1], 8f, paintPoint)
        }
    }

    class AxisInfo(offsetX: Float, offsetY: Float, measuredWidth: Int, measuredHeight: Int, scale: Float, paintAxisText: Paint) {
        val originScreenX = offsetX
        val originScreenY = offsetY

        fun Float.print(): String {
            return if (this < 0.01 || this > 1e4) {
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
    private var isDraggingFixPoint = false
    private var isDraggingCanvas = false
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
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
                val d = hypot(event.x-toScreenX(px), event.y - toScreenY(py))
                isDraggingFixPoint = d <= touchSlop*3
                if (!isDraggingFixPoint) {
                    isDraggingCanvas = true
                    canvasStartX = event.getX(0)
                    canvasStartY = event.getY(0)
                    canvasOffsetPointerId = event.getPointerId(0)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDraggingFixPoint) {
                    this.px = (event.x - offsetX) / scale
                    this.py = (offsetY - event.y) / scale
                    dispatchPxPyChange()
                } else if (isDraggingCanvas) {
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
                isDraggingFixPoint = false
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