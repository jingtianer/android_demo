package com.jingtian.demoapp.main.base

import android.util.SparseIntArray
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jingtian.demoapp.R

open class BaseHeaderFooterAdapter<T> {
    private val headersAdapter = HeaderFooterAdapter()
    private val footersAdapter = HeaderFooterAdapter()
    private var innerAdapter: BaseAdapter<T>? = null

    private val adapter = ConcatAdapter()

    init {
        adapter.addAdapter(headersAdapter)
        adapter.addAdapter(footersAdapter)
    }

    fun bindRecyclerView(recyclerView: RecyclerView, adapter: BaseAdapter<T>) {
        val innerAdapter = innerAdapter
        if (innerAdapter != null) {
            this.adapter.removeAdapter(innerAdapter)
        }
        this.innerAdapter = adapter
        this.adapter.addAdapter(1, adapter)
        recyclerView.adapter = this.adapter
    }

    fun addHeader(view: View) {
        headersAdapter.addView(view)
    }

    fun addFooter(view: View) {
        footersAdapter.addView(view)
    }

    fun removeHeader(view: View) {
        headersAdapter.removeView(view)
    }

    fun removeFooter(view: View) {
        footersAdapter.removeView(view)
    }


    class HeaderFooterHolder(itemView: View, val viewType: Int) : RecyclerView.ViewHolder(itemView)

    inner class HeaderFooterAdapter : RecyclerView.Adapter<HeaderFooterHolder>() {
        private var lastViewType = 0
        private val viewList = arrayListOf<HeaderFooterHolder>()
        private val viewType2Position = SparseIntArray()
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HeaderFooterHolder {
            return viewList[viewType2Position[viewType]]
        }

        override fun getItemCount(): Int {
            return viewList.size
        }

        override fun onBindViewHolder(holder: HeaderFooterHolder, position: Int) {
        }

        override fun getItemViewType(position: Int): Int {
            val view = viewList[position]
            return view.viewType
        }

        fun addView(view: View) {
            val insertPos = viewList.size
            viewType2Position.put(lastViewType, insertPos)
            viewList.add(HeaderFooterHolder(view, lastViewType))
            lastViewType++
            notifyItemInserted(insertPos)
        }

        fun removeView(view: View) {
            val index = viewList.withIndex().find { it.value.itemView == view }
            if (index != null) {
                viewList.removeAt(index.index)
                for (i in index.index until viewList.size) {
                    viewType2Position.put(viewList[i].viewType, i)
                }
                notifyItemRemoved(index.index)
            }
        }

    }
}