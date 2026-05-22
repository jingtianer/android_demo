package com.jingtian.demoapp.main.footcurve

import com.ezylang.evalex.Expression

object FootCurveMath {
    fun eval(expr: String): Float {
        return try {
            ExpressionPool.get(expr).evaluate().numberValue.toFloat()
        } catch (e: Exception) { 0f }
    }

    fun evalInt(expr: String): Int {
        return try {
            ExpressionPool.get(expr).evaluate().numberValue.toInt()
        } catch (e: Exception) { 0 }
    }

    fun eval(paramName: String, expr: String, t: List<Float>): List<Float> {
        val exp = ExpressionPool.get(expr)
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
        val xExp = ExpressionPool.get(curve.xExprStr)
        val yExp = ExpressionPool.get(curve.yExprStr)
        return calcFoot(xExp, yExp, curve, t, px, py)
    }


    fun calcFoot(curve: Curve, tList: List<Float>, px: Float, py: Float): List<FloatArray> {
        val xExp = ExpressionPool.get(curve.xExprStr)
        val yExp = ExpressionPool.get(curve.yExprStr)
        return tList.map { t->
            return@map calcFoot(xExp, yExp, curve, t, px, py)
        }
    }
}