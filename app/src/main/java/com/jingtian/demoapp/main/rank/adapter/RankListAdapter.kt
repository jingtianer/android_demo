package com.jingtian.demoapp.main.rank.adapter

import android.view.ViewGroup
import com.jingtian.demoapp.main.base.BaseAdapter
import com.jingtian.demoapp.main.base.BaseViewHolder
import com.jingtian.demoapp.main.rank.holder.RankListHolder
import com.jingtian.demoapp.main.rank.model.ModelRank

class RankListAdapter : BaseAdapter<ModelRank>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<ModelRank> {
        return RankListHolder.create(parent)
    }
}