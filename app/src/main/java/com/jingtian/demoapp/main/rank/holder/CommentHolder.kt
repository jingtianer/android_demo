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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date

class CommentHolder private constructor(private val binding: ItemRankItemCommentBinding):
    BaseViewHolder<ModelItemComment>(binding.root), AddCommentDialog.Companion.Callback {

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

    override fun onBind(data: ModelItemComment, position: Int) {
        lifecycleScope.launch {
            val user = withContext(Dispatchers.IO) {
                ModelRankUser.getUserInfo(data.userName)
            }
            user.loadUserImage(binding.avatar, 30f.dp.toInt(), 30f.dp.toInt())
            binding.userName.text = user.getUserNameOrDefault()
        }
        with(binding.logTime) {
            text = dataTimeFormat.format(data.lastModifyDate)
        }
        with(binding.logContent) {
            text = data.comment
        }
        with(binding.root) {
            setOnLongClickListener { v->
                AddCommentDialog(context, this@CommentHolder).show()
                false
            }
        }
    }

    override fun onPositiveClick(dialog: Dialog, comment: String) {
        dialog.dismiss()
        val currentData = currentData?.apply {
            this.comment = comment
            this.lastModifyDate = Date()
        } ?: return
        currentAdapter?.setData(currentData, currentPosition)
        Utils.CoroutineUtils.runIOTask({
            Utils.DataHolder.rankDB.rankCommentDao().update(currentData)
        }) {}
    }

    override fun onNegativeClick(dialog: Dialog) {
        dialog.dismiss()
    }

}