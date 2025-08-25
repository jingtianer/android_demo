package com.jingtian.demoapp.main.base

import android.view.View
import androidx.recyclerview.widget.RecyclerView

abstract class BaseViewHolder<T>(itemView : View) : RecyclerView.ViewHolder(itemView) {

    abstract fun onBind(data: T, position: Int)

    open fun onAttach() {}

    open fun onDetach() {}
}