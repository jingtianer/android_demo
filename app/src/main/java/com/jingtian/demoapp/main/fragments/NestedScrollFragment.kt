package com.jingtian.demoapp.main.fragments

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jingtian.demoapp.databinding.FragmentNestedScrollBinding
import com.jingtian.demoapp.main.ScreenUtils.screenHeight
import com.jingtian.demoapp.main.dp
import kotlin.random.Random

object NestedScrollFragment : BaseFragment() {
    private lateinit var binding: FragmentNestedScrollBinding
    private val adapter = ColorBlockAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentNestedScrollBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding.recyclerView) {
            adapter = this@NestedScrollFragment.adapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            isNestedScrollingEnabled = true
            layoutParams?.height = context.screenHeight
            binding.nestedScrollView.setInnerRecyclerView(this)
        }
        for (i in 0 until 200) {
            adapter.addData()
        }
    }

    class ColorBlockHolder(context: Context) :
        RecyclerView.ViewHolder(View(context)) {
        fun onBind(color: Int, height: Int) {
            itemView.layoutParams = if (itemView.layoutParams != null) {
                itemView.layoutParams.height = height
                itemView.layoutParams
            } else {
                RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, height)
            }
            itemView.setBackgroundColor(color)
        }
    }

    class ColorBlockAdapter : RecyclerView.Adapter<ColorBlockHolder>() {
        private val random = Random
        private val dataList = mutableListOf<Pair<Int, Int>>()
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorBlockHolder {
            return ColorBlockHolder(parent.context)
        }

        override fun getItemCount(): Int {
            return dataList.size
        }

        override fun onBindViewHolder(holder: ColorBlockHolder, position: Int) {
            val (color, height) = dataList[position]
            holder.onBind(color, height)
        }

        fun addData() {
            dataList.add(
                Color.argb(
                    255,
                    random.nextInt(0, 255),
                    random.nextInt(0, 255),
                    random.nextInt(0, 255)
                ) to random.nextInt(50, 400).toFloat().dp.toInt()
            )
        }
    }
}