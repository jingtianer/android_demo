package com.jingtian.demoapp.main.base

import android.view.View
import androidx.recyclerview.widget.RecyclerView

abstract class BaseViewHolder<T>(itemView : View) : RecyclerView.ViewHolder(itemView) {
    var currentData: T? = null
    var currentAdapter: BaseAdapter<T>? = null
    var currentPosition: Int = 0
    abstract fun onBind(data: T, position: Int)

    open fun onAttach() {}

    open fun onDetach() {}
}