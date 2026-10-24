package com.jingtian.composedemo.base

import kotlinx.atomicfu.atomic

object JumpOnPurposeMarkHolder {
    val isJumpOnPurpose = atomic(false)
}