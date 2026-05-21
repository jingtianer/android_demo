package com.jingtian.demoapp.main.footcurve

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewConfiguration
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
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
    var lifecycleOwner: LifecycleOwner? = context as? LifecycleOwner

    // ===================== 双指缩放 新增 =====================
    private val scaleGestureDetector = ScaleGestureDetector(context, ScaleListener())
    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scale *= detector.scaleFactor
            // 缩放后只需要重新刷新路径坐标，不需要重算数学
            redrawAll()
            return true
        }
    }

    // 曲线配置
    var curve: Curve = Curve()
        set(value) {
            field = value
            updateAll()
        }

    // 当前切点参数 t
    var currentT: Float = curve.initT
        set(value) {
            field = value
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
    private var pointP = FloatArray(2)
    private var pointTan = FloatArray(2)
    private var pointFoot = FloatArray(2)

    // ===================== 画笔 =====================
    private val paintOrigin = Paint().apply {
        color = Color.BLUE
        strokeWidth = 10f
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
    private var scale = 200f
    private var offsetX = 0f
    private var offsetY = 0f

    private fun toScreenX(x: Float) = offsetX + x * scale
    private fun toScreenY(y: Float) = offsetY - y * scale

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (floor(offsetX) != floor(measuredWidth / 2f) || floor(offsetY) != floor(measuredHeight / 2f)) {
            offsetX = measuredWidth / 2f
            offsetY = measuredHeight / 2f
            redrawAll()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (floor(offsetX) != floor(measuredWidth / 2f) || floor(offsetY) != floor(measuredHeight / 2f)) {
            offsetX = measuredWidth / 2f
            offsetY = measuredHeight / 2f
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
    }

    fun <T> runTask(task: suspend ()->T, callback: (T)->Unit = {}, onError: (Throwable)->Unit = {}): Job {
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

    private val originPathTask = CanvasJob({
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

    private val footPathJob = CanvasJob({
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

    private val tangentJob = CanvasJob({
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
        val L = 1f + max(1f, hypot(cx - px, cy - py))
        val x1 = cx - dx*L
        val y1 = cy - dy*L
        val x2 = cx + dx*L
        val y2 = cy + dy*L
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

    // ===================== 【最极致轻量 onDraw】 =====================
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        drawAxis(canvas)

        // 只绘制，无任何计算、无循环、无Path创建
        canvas.drawPath(pathOrigin, paintOrigin)
        canvas.drawPath(pathFoot, paintFoot)
        canvas.drawLines(lineTan, paintTangent)
        canvas.drawLines(linePerp, paintPerp)

        paintPoint.color = 0xFFFF9800.toInt()
        canvas.drawCircle(pointP[0], pointP[1], 12f, paintPoint)

        paintPoint.color = Color.RED
        canvas.drawCircle(pointTan[0], pointTan[1], 8f, paintPoint)

        paintPoint.color = Color.GREEN
        canvas.drawCircle(pointFoot[0], pointFoot[1], 8f, paintPoint)
    }

    /** 绘制XY坐标轴 + 简易刻度 */
    private fun drawAxis(canvas: Canvas) {
        // 原点屏幕坐标
        val originScreenX = offsetX
        val originScreenY = offsetY

        // X轴 水平
        canvas.drawLine(0f, originScreenY, width.toFloat(), originScreenY, paintAxis)
        // Y轴 垂直
        canvas.drawLine(originScreenX, 0f, originScreenX, height.toFloat(), paintAxis)

        // 绘制原点文字
        canvas.drawText("O", originScreenX - 15f, originScreenY + 30f, paintAxisText)

        // 单位1长度: scale
        // 单位1的个数
        val xUnitOneCount = ceil(measuredWidth.toFloat() / 2f / scale).toInt()
        val yUnitOneCount = ceil(measuredHeight.toFloat() / 2f / scale).toInt()

        val screenAspectRatio = measuredWidth.toFloat() / measuredHeight.toFloat()

        val xStepValue = if (xUnitOneCount <= 1) {
            measuredWidth.toFloat() / 2f / scale / 5f
        } else if (xUnitOneCount <= 5) {
            1f
        } else {
            xUnitOneCount / 5f
        }

        val yBarCount = round(5f / screenAspectRatio)
        val yStepValue = if (xUnitOneCount <= 1) {
            measuredHeight.toFloat() / 2f / scale / yBarCount
        } else if (xUnitOneCount <= yBarCount) {
            1f
        } else {
            yUnitOneCount / yBarCount
        }

        val xStepCount = ceil(xUnitOneCount / xStepValue).toInt()
        val xBarHeight = xStepValue * scale

        val yStepCount = ceil(yUnitOneCount / yStepValue).toInt()
        val yBarHeight = yStepValue * scale

        // X轴正方向刻度
        var xPos = originScreenX + xBarHeight
        repeat(xStepCount) {
            canvas.drawLine(xPos, originScreenY - 8f, xPos, originScreenY + 8f, paintAxis)
            val value = (it + 1) * xStepValue
            canvas.drawText(String.format("%.2f", value), xPos, originScreenY + 35f, paintAxisText)
            xPos += xBarHeight
        }

        // X轴负方向刻度
        xPos = originScreenX - xBarHeight
        repeat(xStepCount) {
            canvas.drawLine(xPos, originScreenY - 8f, xPos, originScreenY + 8f, paintAxis)
            val value = -(it + 1) * xStepValue
            canvas.drawText(String.format("%.2f", value), xPos, originScreenY + 35f, paintAxisText)
            xPos -= xBarHeight
        }

        // y轴负方向刻度
        var yPos = originScreenY + yBarHeight
        repeat(yStepCount) {
            canvas.drawLine(originScreenX - 8f, yPos, originScreenX + 8f, yPos, paintAxis)
            val value = -(it + 1) * yStepValue
            val text = String.format("%.2f", value)
            canvas.drawText(text, originScreenX + paintAxisText.measureText(text) / 2f + 12f, yPos + 8f, paintAxisText)
            yPos += yBarHeight
        }

        // y轴正方向刻度
        yPos = originScreenY - yBarHeight
        repeat(yStepCount) {
            canvas.drawLine(originScreenX - 8f, yPos, originScreenX + 8f, yPos, paintAxis)
            val value = (it + 1) * yStepValue
            val text = String.format("%.2f", value)
            canvas.drawText(text, originScreenX + paintAxisText.measureText(text) / 2f + 12f, yPos + 8f, paintAxisText)
            yPos -= yBarHeight
        }
    }


    // ===================== 拖拽 =====================
    private var isDragging = false
    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleGestureDetector.onTouchEvent(event)
        parent?.requestDisallowInterceptTouchEvent(true)
        val x = (event.x - offsetX) / scale
        val y = (offsetY - event.y) / scale
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val d = hypot(x-px.toDouble(), y-py.toDouble())
                isDragging = d < 0.5
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    this.px = x
                    this.py = y
                    dispatchPxPyChange()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> isDragging = false
        }
        super.onTouchEvent(event)
        return true
    }

    inner class CanvasJob<T>(private val task: ()->T, private val callback: (T)->Unit = {}, private val onError: (Throwable) -> Unit) {
        private var job: Job? = null
        private var hasSchedule = false
        private val result: T? = null
        fun run(redraw: Boolean = false) {
            job?.cancel()
            val result = result
            if (redraw && result != null) {
                callback(result)
                job = null
            } else {
                job = runTask(task, {
                    callback.invoke(it)
                    job = null
                    invalidate()
                }, onError = {
                    onError.invoke(it)
                    job = null
                    invalidate()
                })
            }
        }

        fun schedule() {
            if (!hasSchedule) {
                hasSchedule = true
                runTask({
                    job?.join()
                    run()
                }, callback = {
                    hasSchedule = false
                }, onError = {
                    hasSchedule = false
                })
            }
        }

        fun scheduleRedraw() {
            if (!hasSchedule) {
                hasSchedule = true
                runTask({
                    job?.join()
                    run(true)
                }, callback = {
                    hasSchedule = false
                }, onError = {
                    hasSchedule = false
                })
            }
        }
    }
}