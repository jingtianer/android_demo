package com.jingtian.demoapp.main.fragments

import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jingtian.demoapp.main.dp

class ConcatAdapterFragment : BaseFragment() {
    companion object {
        private val ADAPTER_SIZE = intArrayOf(12,19,27)
    }
    private lateinit var recyclerView: RecyclerView
    private val concatAdapter by lazy {
        ConcatAdapter()
    }

    private val adapterList = arrayListOf<RecyclerView.Adapter<RecyclerView.ViewHolder>>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        recyclerView = RecyclerView(inflater.context)
        return recyclerView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ADAPTER_SIZE.forEach {
            concatAdapter.addAdapter(InnerAdapter(it))
        }
        recyclerView.adapter = concatAdapter
        repeat(recyclerView.itemDecorationCount) {
            recyclerView.removeItemDecorationAt(0)
        }
        recyclerView.layoutManager = LinearLayoutManager(
            context,
            RecyclerView.VERTICAL,
            false,
        )
        recyclerView.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(
                outRect: Rect,
                view: View,
                parent: RecyclerView,
                state: RecyclerView.State
            ) {
                outRect.set(
                    8f.dp.toInt(),
                    8f.dp.toInt(),
                    8f.dp.toInt(),
                    8f.dp.toInt(),
                )
            }
        })
    }

    class SimpleViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView) {
        constructor(context: Context) : this(TextView(context))
    }

    class InnerAdapter(private val size: Int): RecyclerView.Adapter<SimpleViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SimpleViewHolder {
            return SimpleViewHolder(parent.context)
        }

        override fun getItemCount(): Int {
            return size
        }

        override fun onBindViewHolder(holder: SimpleViewHolder, position: Int) {
            holder.textView.text = """
                onBindViewHolder.position = $position
                adapterPosition = ${holder.adapterPosition}
                absoluteAdapterPosition = ${holder.absoluteAdapterPosition}
                bindingAdapterPosition = ${holder.bindingAdapterPosition}
                holder.position = ${holder.position}
                adapterPosition = ${holder.adapterPosition}
                oldPosition = ${holder.oldPosition}
                layoutPosition = ${holder.layoutPosition}
            """.trimIndent()
        }

    }
}