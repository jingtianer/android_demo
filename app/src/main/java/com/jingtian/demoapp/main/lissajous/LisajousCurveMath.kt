package com.jingtian.demoapp.main.lissajous

import com.ezylang.evalex.Expression

object LisajousCurveMath {
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

    // a,b,x,y
    fun evalX(expr: LisajousCurve, t: List<Float>): List<Float> {
        return eval(expr, t) {
            this.xExprStr
        }
    }

    fun evalY(expr: LisajousCurve, t: List<Float>): List<Float> {
        return eval(expr, t) {
            this.yExprStr
        }
    }

    private inline fun eval(expr: LisajousCurve, t: List<Float>, exprStr: LisajousCurve.()->String): List<Float> {
        val exp = ExpressionPool.get(expr.exprStr())
        return try {
            t.map {
                exp.copy()
                    .with("a", expr.a)
                    .with("b", expr.b)
                    .with("x", expr.x)
                    .with("t", it)
                    .evaluate()
                    .numberValue
                    .toFloat()
            }
        } catch (e: Exception) { listOf() }
    }
}