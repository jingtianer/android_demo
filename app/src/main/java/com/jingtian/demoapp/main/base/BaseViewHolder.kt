package com.jingtian.demoapp.main.base

import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.State
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.recyclerview.widget.RecyclerView
import kotlin.properties.Delegates

abstract class BaseViewHolder<T>(itemView : View) : RecyclerView.ViewHolder(itemView), LifecycleOwner {
    private var lifecycleRegistry: LifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle
        get() = lifecycleRegistry
    var currentData: T? = null
    var currentAdapter: BaseAdapter<T>? = null
    var currentPosition: Int = 0

    fun realOnBind(data: T, position: Int) {
        if (lifecycleRegistry.currentState == State.DESTROYED) {
            lifecycleRegistry = LifecycleRegistry(this)
        }
        lifecycleRegistry.currentState = State.STARTED
        onBind(data, position)
    }

    abstract fun onBind(data: T, position: Int)

    fun realOnAttach() {
        onAttach()
    }

    open fun onAttach() {

    }

    fun realOnDetach() {
        onDetach()
    }

    open fun onDetach() {

    }

    fun realOnRecycled() {
        lifecycleRegistry.currentState = State.DESTROYED
        onRecycled()
    }

    open fun onRecycled() {

    }
}