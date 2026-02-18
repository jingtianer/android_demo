package com.jingtian.composedemo.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow

@Composable
fun <T> Flow<T>.collectAsStateWithLifecycle(
    initialValue: T,
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED
): State<T> {
    // 1. 利用 produceState 管理 Compose 状态
    val state = produceState(initialValue, this, lifecycleOwner, minActiveState) {
        // 2. 绑定生命周期：仅在生命周期 >= minActiveState 时收集数据
        lifecycleOwner.lifecycle.repeatOnLifecycle(minActiveState) {
            this@collectAsStateWithLifecycle.collect { value ->
                // 3. 更新 Compose 状态
                this@produceState.value = value
            }
        }
    }
    // 4. 记忆状态，避免重组时重复创建
    return remember { state }
}