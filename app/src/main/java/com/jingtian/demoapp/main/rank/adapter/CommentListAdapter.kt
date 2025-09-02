package com.jingtian.demoapp.main.rank.adapter

import android.view.ViewGroup
import com.jingtian.demoapp.main.base.BaseAdapter
import com.jingtian.demoapp.main.base.BaseViewHolder
import com.jingtian.demoapp.main.rank.holder.CommentHolder
import com.jingtian.demoapp.main.rank.model.ModelItemComment
import com.jingtian.demoapp.main.rank.model.ModelRankUser
import com.jingtian.demoapp.main.rank.model.RelationUserAndComment

class CommentListAdapter : BaseAdapter<RelationUserAndComment>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): BaseViewHolder<RelationUserAndComment> {
        return CommentHolder.create(parent)
    }
}