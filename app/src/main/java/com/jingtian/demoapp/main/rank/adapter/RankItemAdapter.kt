package com.jingtian.demoapp.main.rank.adapter

import android.view.ViewGroup
import com.jingtian.demoapp.main.base.BaseAdapter
import com.jingtian.demoapp.main.base.BaseViewHolder
import com.jingtian.demoapp.main.rank.holder.RankItemHolder
import com.jingtian.demoapp.main.rank.model.ModelRankItem

class RankItemAdapter : BaseAdapter<ModelRankItem>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<ModelRankItem> {
        return RankItemHolder.create(parent)
    }
}