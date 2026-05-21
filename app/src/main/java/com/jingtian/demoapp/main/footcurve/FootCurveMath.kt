package com.jingtian.demoapp.main.footcurve

import android.util.Log
import com.ezylang.evalex.Expression
import com.ezylang.evalex.config.ExpressionConfiguration
import kotlin.math.abs

object FootCurveMath {
    private const val DX_EPS = 1e-4f
    private const val INF_THRESHOLD = 1e10f

    fun eval(paramName: String, expr: String, t: Float): Float {
        return try {
            val value = Expression(expr).with(paramName, t).evaluate().numberValue.toFloat()
            value
        } catch (e: Exception) { 0f }
    }

    fun eval(paramName: String, expr: String, t: List<Float>): List<Float> {
        val exp = Expression(expr)
        return try {
            t.map { exp.copy().with(paramName, it).evaluate().numberValue.toFloat() }
        } catch (e: Exception) { listOf() }
    }

    fun Expression.eval(paramName: String, t: Float): Float {
        return this.copy().with(paramName, t).evaluate().numberValue.toFloat()
    }

    fun Expression.diff(paramName: String, t: Float): Float {
        val h = 0.0001f
        return (this.eval(paramName, t+h) - this.eval(paramName, t-h)) / (2*h)
    }

    fun calcFoot(inXExp: Expression, inYExp: Expression, curve: Curve, t: Float, px: Float, py: Float): FloatArray {
        val xExp = inXExp
        val yExp = inYExp
        val cx = xExp.eval(curve.paramName, t)
        val cy = yExp.eval(curve.paramName, t)
        val dx = xExp.diff(curve.paramName, t)
        val dy = yExp.diff(curve.paramName, t)

        val A = dy
        val B = -dx
        val C = dx*cy - dy*cx
        var den = A*A + B*B
        if (den < 1e-6f) den = 1e-6f

        var fx = px - A*(A*px + B*py + C)/den
        var fy = py - B*(A*px + B*py + C)/den

        if (fx.isInfinite() || fy.isInfinite() || fx.isNaN() || fy.isNaN()) {
            fx = cx; fy = cy
        }
        return floatArrayOf(cx, cy, fx, fy, dx, dy)
    }

    fun calcFoot(curve: Curve, t: Float, px: Float, py: Float): FloatArray {
        val xExp = Expression(curve.xExprStr)
        val yExp = Expression(curve.yExprStr)
        return calcFoot(xExp, yExp, curve, t, px, py)
    }


    fun calcFoot(curve: Curve, tList: List<Float>, px: Float, py: Float): List<FloatArray> {
        val xExp = Expression(curve.xExprStr)
        val yExp = Expression(curve.yExprStr)
        return tList.map { t->
            return@map calcFoot(xExp, yExp, curve, t, px, py)
        }
    }

    // 表达式求值
    fun evalExpr(exprStr: String, varName: String, value: Float): Float {
        return try {
            Expression(exprStr)
                .with(varName, value.toDouble())
                .evaluate().numberValue.toFloat().apply {
                    Log.d("jingtian", "evalExpr: $this")
                }
        } catch (e: Exception) {
            0f
        }
    }

    // 数值求导 dy/dt
    fun diffExpr(exprStr: String, varName: String, t: Float): Float {
        val y1 = evalExpr(exprStr, varName, t - DX_EPS)
        val y2 = evalExpr(exprStr, varName, t + DX_EPS)
        return (y2 - y1) / (2 * DX_EPS)
    }

    // 判断是否无穷/异常
    fun isInvalid(v: Float): Boolean {
        return abs(v) > INF_THRESHOLD || v.isNaN()
    }

    /**
     * 计算切点、垂足
     * @return 切点(x,y) 垂足(x,y) dx dy
     */
    fun calcFootPoint(
        t: Float,
        curve: Curve,
        px: Float, py: Float
    ): FloatArray {
        val tName = curve.paramName
        // 切点坐标
        val cx = evalExpr(curve.xExprStr, tName, t)
        val cy = evalExpr(curve.yExprStr, tName, t)
        // 切线导数
        val dx = diffExpr(curve.xExprStr, tName, t)
        val dy = diffExpr(curve.yExprStr, tName, t)

        // 切线直线: A x + B y + C = 0
        val A = dy
        val B = -dx
        val C = dx * cy - dy * cx

        var denom = A * A + B * B
        if (denom < 1e-6f) denom = 1e-6f

        // 点到直线垂足公式
        var fx = px - A * (A * px + B * py + C) / denom
        var fy = py - B * (A * px + B * py + C) / denom

        // 异常兜底
        if (isInvalid(fx) || isInvalid(fy)) {
            fx = cx
            fy = cy
        }
        return floatArrayOf(cx, cy, fx, fy, dx, dy)
    }

    // 批量生成整条垂足曲线
    fun genFootCurvePoints(
        curve: Curve,
        px: Float, py: Float,
        count: Int = 300
    ): List<Pair<Float, Float>> {
        val list = mutableListOf<Pair<Float, Float>>()
        val step = (curve.tMax - curve.tMin) / count
        for (i in 0..count) {
            val t = curve.tMin + step * i
            val res = calcFootPoint(t, curve, px, py)
            list.add(res[2] to res[3])
        }
        return list
    }
}