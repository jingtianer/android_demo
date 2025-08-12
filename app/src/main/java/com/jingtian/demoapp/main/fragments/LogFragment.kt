package com.jingtian.demoapp.main.fragments

import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jingtian.demoapp.databinding.FragmentLogBinding
import com.jingtian.demoapp.databinding.ItemLogBinding
import java.util.Collections
import java.util.Date

open class LogFragment(private val title: String): BaseFragment() {
    protected lateinit var binding: FragmentLogBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLogBinding.inflate(inflater, container, false)
        return binding.root
    }

    private val adapter = LogAdapter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding.recyclerView) {
            adapter = this@LogFragment.adapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, true).also {
                it.setStackFromEnd(true)
            }
        }
        binding.titleText.text = title
        binding.fab.setOnClickListener {
            binding.titleText.text = title
            clearLog()
        }
    }

    class LogViewHolder private constructor(private val binding: ItemLogBinding): RecyclerView.ViewHolder(binding.root) {
        companion object {
            fun newInstance(parent: ViewGroup): LogViewHolder {
                return LogViewHolder(
                    ItemLogBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
            }
        }

        private val dateFormat = SimpleDateFormat.getDateTimeInstance()

        fun bindData(data: Pair<String, Date>, index: Int) {
            binding.logTime.text = dateFormat.format(data.second)
            binding.logContent.text = data.first
            binding.index.text = "${index + 1}"
        }

    }

    fun addLog(log: String) {
        adapter.add(log)
    }

    fun asyncAddLog(log: String) {
        adapter.asyncAdd(log)
    }

    fun clearLog() {
        adapter.clear()
    }

    inner class LogAdapter : RecyclerView.Adapter<LogViewHolder>() {
        private val dataList = Collections.synchronizedList(ArrayList<Pair<String, Date>>())
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
            return LogViewHolder.newInstance(parent)
        }

        override fun getItemCount(): Int {
            return dataList.size
        }

        fun add(log: String) {
            val size = dataList.size
            dataList.add(log to Date())
            notifyItemInserted(size)
            notifyItemRangeChanged(size, 1)
        }

        fun asyncAdd(log: String) {
            val oldSize = dataList.size
            dataList.add(log to Date())
            binding.recyclerView.post {
                val size = dataList.size
                notifyItemInserted(size)
                notifyItemRangeChanged(oldSize, size - oldSize)
            }
        }

        fun clear() {
            dataList.clear()
            notifyDataSetChanged()
        }

        override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
            holder.bindData(dataList[position], position)
        }

    }
}