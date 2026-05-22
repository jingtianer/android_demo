package com.jingtian.demoapp.main.footcurve

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
        DefConfig(Curve(), "圆", ),
        DefConfig(Curve(
            xExprStr = "t",
            yExprStr = "t*t",
            tMin = -1f,
            tMax = 1f,
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
    )
}