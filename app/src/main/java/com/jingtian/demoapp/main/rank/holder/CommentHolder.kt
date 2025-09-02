package com.jingtian.demoapp.main.rank.holder

import android.app.Dialog
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.jingtian.demoapp.databinding.ItemRankItemCommentBinding
import com.jingtian.demoapp.main.base.BaseViewHolder
import com.jingtian.demoapp.main.dp
import com.jingtian.demoapp.main.rank.Utils
import com.jingtian.demoapp.main.rank.dialog.AddCommentDialog
import com.jingtian.demoapp.main.rank.model.ModelItemComment
import com.jingtian.demoapp.main.rank.model.ModelRankUser
import com.jingtian.demoapp.main.rank.model.ModelRankUser.Companion.getUserNameOrDefault
import com.jingtian.demoapp.main.rank.model.ModelRankUser.Companion.loadUserImage
import com.jingtian.demoapp.main.rank.model.RelationUserAndComment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date

class CommentHolder private constructor(private val binding: ItemRankItemCommentBinding):
    BaseViewHolder<RelationUserAndComment>(binding.root), AddCommentDialog.Companion.Callback {

    companion object {
        fun create(parent: ViewGroup): CommentHolder {
            return CommentHolder(
                ItemRankItemCommentBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        }
        private val dataTimeFormat = SimpleDateFormat.getDateTimeInstance()
    }

    override fun onBind(data: RelationUserAndComment, position: Int) {
        data.user.loadUserImage(binding.avatar, 30f.dp.toInt(), 30f.dp.toInt())
        binding.userName.text = "@${data.user.getUserNameOrDefault()}"
        with(binding.logTime) {
            text = dataTimeFormat.format(data.comment.lastModifyDate)
        }
        with(binding.logContent) {
            text = data.comment.comment
        }
        with(binding.root) {
            setOnLongClickListener { v->
                AddCommentDialog(context, this@CommentHolder, true).apply {
                    setComment(data.comment.comment)
                    setHint("修改评论")
                    show()
                }
                false
            }
        }
    }

    override fun onPositiveClick(dialog: Dialog, comment: String) {
        dialog.dismiss()
        val currentData = currentData?.apply {
            this.comment.comment = comment
            this.comment.lastModifyDate = Date()
        } ?: return
        currentAdapter?.setData(currentData, currentPosition)
        Utils.CoroutineUtils.runIOTask({
            Utils.DataHolder.rankDB.rankCommentDao().update(currentData.comment)
        }) {}
    }

    override fun onNegativeClick(dialog: Dialog) {
        dialog.dismiss()
    }

    override fun onDeleteClick(dialog: Dialog) {
        dialog.dismiss()
        val currentData = currentData ?: return
        Utils.CoroutineUtils.runIOTask({
            Utils.DataHolder.rankDB.rankCommentDao().delete(currentData.comment)
        }) {
            lifecycleScope.launch {
                currentAdapter?.remove(currentPosition)
            }
        }
    }

}