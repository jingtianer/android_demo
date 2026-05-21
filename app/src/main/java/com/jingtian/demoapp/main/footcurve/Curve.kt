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