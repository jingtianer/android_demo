package com.jingtian.demoapp.main.base

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

abstract class BaseAdapter<T> : RecyclerView.Adapter<BaseViewHolder<T>>() {
    private var dataList: MutableList<T> = mutableListOf()

    override fun getItemCount(): Int = dataList.size

    abstract override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<T>

    override fun onBindViewHolder(holder: BaseViewHolder<T>, position: Int) {
        holder.onBind(dataList[position], position)
    }

    override fun onViewAttachedToWindow(holder: BaseViewHolder<T>) {
        super.onViewAttachedToWindow(holder)
        holder.onAttach()
    }

    override fun onViewDetachedFromWindow(holder: BaseViewHolder<T>) {
        super.onViewDetachedFromWindow(holder)
        holder.onDetach()
    }

    open fun setDataList(list: List<T>) {
        this.dataList = list.toMutableList()
        notifyDataSetChanged()
    }

    open fun append(data : T) {
        val insertedPosition = this.dataList.size
        this.dataList.add(data)
        notifyItemInserted(insertedPosition)
    }

    open fun getDataList(): List<T> {
        return dataList
    }
}