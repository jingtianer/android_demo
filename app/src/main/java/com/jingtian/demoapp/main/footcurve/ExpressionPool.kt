package com.jingtian.demoapp.main.footcurve

import androidx.collection.LruCache
import com.ezylang.evalex.Expression

object ExpressionPool {
    private const val EXPRESSION_POOL_SIZE = 30
    private val expressions = object : LruCache<String, Expression>(EXPRESSION_POOL_SIZE) {
        override fun create(key: String): Expression {
            return Expression(key)
        }
    }

    fun get(expr: String): Expression {
        return expressions[expr]?.copy() ?: Expression(expr)
    }
}