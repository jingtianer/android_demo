package com.jingtian.demoapp.main.footcurve

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class Curve(
    val paramName: String = "t",
    val xExprStr: String = "cosr(t)",
    val yExprStr: String = "sinr(t)",
    val tMin: Float = 0f,
    val tMax: Float = (2 * Math.PI).toFloat(),
    var initPx: Float = 0.5f,
    var initPy: Float = (sqrt(3.0) /2).toFloat(),
    var initT: Float = (7 * Math.PI/4).toFloat()
)

object DefaultConfigs {
    class DefConfig(val curve: Curve, val name: CharSequence, val desc: CharSequence = "")
    val defaultConfigs = listOf(
        DefConfig(Curve(), "圆"),
        DefConfig(Curve(
            xExprStr = "t",
            yExprStr = "t*t",
            tMin = -2f,
            tMax = 2f,
            initPx = 0f,
            initPy = 1/4f,
            initT = 1/2f
        ), "抛物线"),
        DefConfig(Curve(
            xExprStr = "cosr(t)^3",
            yExprStr = "sinr(t)^3",
            tMin = 0f,
            tMax = (2 * Math.PI).toFloat(),
            initPx = 0f,
            initPy = 0f,
            initT = (0.25 * Math.PI).toFloat()
        ), "星行线"),
        DefConfig(Curve(
            xExprStr = "t - sinr(t*2*pi)/2/pi",
            yExprStr = "1/2/pi - cosr(t*2*pi)/2/pi",
            tMin = 0f,
            tMax = 1f,
            initPx = (0.5 - sin(Math.PI)/2.0/Math.PI).toFloat(),
            initPy = (1.0/2.0/Math.PI - cos(Math.PI)/2.0/Math.PI).toFloat(),
            initT = 0f
        ), "摆线"),
        DefConfig(Curve(
            xExprStr = "cosr(2t)*cosr(t)",
            yExprStr = "cosr(2t)*sinr(t)",
            tMin = 0f,
            tMax = (2f*Math.PI).toFloat(),
            initPx = 0f,
            initPy = 0f,
            initT = 0f
        ), "四叶玫瑰线"),
        DefConfig(Curve(
            xExprStr = "secr(t)",
            yExprStr = "tanr(t)",
            tMin = 0f,
            tMax = (2f*Math.PI).toFloat(),
            initPx = 0f,
            initPy = 0f,
            initT = 0f
        ), "双曲线"),
    )
}