package com.jingtian.composedemo.multiplatform

expect fun newReentrantLock(): IMultiplatformReentrantLock

interface IMultiplatformReentrantLock {
    fun <R> use(block: ()->R): R
}