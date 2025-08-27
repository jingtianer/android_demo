package com.jingtian.demoapp.main.rank.adapter

import android.view.ViewGroup
import com.jingtian.demoapp.main.base.BaseAdapter
import com.jingtian.demoapp.main.base.BaseViewHolder
import com.jingtian.demoapp.main.rank.holder.CommentHolder
import com.jingtian.demoapp.main.rank.model.ModelItemComment

class CommentListAdapter : BaseAdapter<ModelItemComment>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): BaseViewHolder<ModelItemComment> {
        return CommentHolder.create(parent)
    }
}