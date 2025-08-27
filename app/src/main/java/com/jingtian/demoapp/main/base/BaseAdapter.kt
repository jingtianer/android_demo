package com.jingtian.demoapp.main.base

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

abstract class BaseAdapter<T> : RecyclerView.Adapter<BaseViewHolder<T>>() {
    private var dataList: MutableList<T> = mutableListOf()

    override fun getItemCount(): Int = dataList.size

    abstract override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<T>

    override fun onBindViewHolder(holder: BaseViewHolder<T>, position: Int) {
        val data = dataList[position]
        holder.currentData = data
        holder.currentAdapter = this
        holder.currentPosition = position
        holder.onBind(data, position)
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

    open fun setData(data: T, pos: Int) {
        if (pos >= 0 && pos < dataList.size) {
            dataList.set(pos, data)
            notifyItemChanged(pos)
        }
    }

    open fun append(data : T) {
        val insertedPosition = this.dataList.size
        this.dataList.add(data)
        notifyItemInserted(insertedPosition)
    }

    open fun appendAll(data : List<T>) {
        val insertedPosition = this.dataList.size
        val insertedSize = data.size
        this.dataList.addAll(data)
        notifyItemRangeInserted(insertedPosition, insertedSize)
        notifyItemRangeChanged(insertedPosition, insertedSize)
    }

    open fun getDataList(): List<T> {
        return dataList
    }

    open fun remove(pos: Int) {
        if (pos >= 0 && pos < dataList.size) {
            dataList.removeAt(pos)
            notifyItemRemoved(pos)
            notifyItemRangeChanged(pos, dataList.size - pos)
        }
    }

    open fun insert(pos: Int, data: T) {
        if (pos >= 0 && pos <= dataList.size) {
            dataList.add(pos, data)
            notifyItemInserted(pos)
            notifyItemRangeChanged(pos, dataList.size - pos)
        }
    }
}