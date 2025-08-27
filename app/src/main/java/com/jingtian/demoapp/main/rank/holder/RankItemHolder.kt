package com.jingtian.demoapp.main.rank.holder

import android.app.Dialog
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import com.jingtian.demoapp.R
import com.jingtian.demoapp.databinding.ItemRankItemBinding
import com.jingtian.demoapp.main.base.BaseViewHolder
import com.jingtian.demoapp.main.dp
import com.jingtian.demoapp.main.rank.Utils
import com.jingtian.demoapp.main.rank.activity.RankItemActivity
import com.jingtian.demoapp.main.rank.dialog.AddRankItemDialog
import com.jingtian.demoapp.main.rank.model.ModelRankItem
import com.jingtian.demoapp.main.widget.RankTypeChooser

class RankItemHolder private constructor(private val binding: ItemRankItemBinding) :
    BaseViewHolder<ModelRankItem>(binding.root), AddRankItemDialog.Companion.Callback {
    companion object {
        fun create(parent: ViewGroup): RankItemHolder {
            return RankItemHolder(
                ItemRankItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        }
    }

    override fun onBind(data: ModelRankItem, position: Int) {
        with(binding.starRate) {
            updateStarConfig(
                false,
                5,
                3f.dp,
                ResourcesCompat.getDrawable(resources, R.drawable.star_high_lighted, null),
                ResourcesCompat.getDrawable(resources, R.drawable.star, null),
            )
            setScore(data.score)
        }
        with(binding.root) {
            setOnClickListener {
                RankItemActivity.startActivity(context, data.rankName, data.itemName)
            }
            setOnLongClickListener {
                AddRankItemDialog(context, data.rankName, this@RankItemHolder, data).show()
                false
            }
        }
        with(binding.rankType) {
            val bg = RankTypeChooser.createBg(data.rankType)
            binding.rankType.layoutParams.apply {
                width = bg.getWidth().toInt()
                height = bg.getHeight().toInt()
            }
            background = bg
        }
        with(binding.title) {
            text = data.itemName
        }
        with(binding.image) {
            setImageURI(data.image.image)
        }
    }

    override fun onPositiveClick(dialog: Dialog, modelRank: ModelRankItem) {
        dialog.dismiss()
//        val oldImageId = currentData?.image?.id ?: -1
        currentData = modelRank
        Utils.CoroutineUtils.run({
            Utils.DataHolder.rankDB.rankItemDao().update(modelRank)
        }) {
            currentAdapter?.let { adapter->
                adapter.setData(modelRank, currentPosition)
            }
        }
    }

    override fun onNegativeClick(dialog: Dialog) {
        dialog.dismiss()
    }

    override fun onDeleteClick(dialog: Dialog) {
        dialog.dismiss()
        currentAdapter?.let { adapter->
            adapter.remove(currentPosition)
        }
        currentData?.let {
            Utils.CoroutineUtils.run({
                Utils.DataHolder.rankDB.rankItemDao().delete(it)
            }) { }
        }
    }
}