package com.jingtian.demoapp.main.rank.holder

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.jingtian.demoapp.databinding.ItemRankListBinding
import com.jingtian.demoapp.main.base.BaseViewHolder
import com.jingtian.demoapp.main.rank.adapter.RankItemAdapter
import com.jingtian.demoapp.main.rank.model.ModelRank

class RankListHolder private constructor(private val binding: ItemRankListBinding) : BaseViewHolder<ModelRank>(binding.root) {
    companion object {
        fun create(parent: ViewGroup): RankListHolder {
            return RankListHolder(ItemRankListBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }
    }

    private val rankItemAdapter = RankItemAdapter()

    override fun onBind(data: ModelRank, position: Int) {
        with(binding.title) {
            text = data.name
        }
        with(binding.recyclerView) {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = rankItemAdapter
            rankItemAdapter.setDataList(data.list)
        }
    }

}