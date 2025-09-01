package com.jingtian.demoapp.main.rank.holder

import android.app.Dialog
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import com.jingtian.demoapp.R
import com.jingtian.demoapp.databinding.ItemRankItemBinding
import com.jingtian.demoapp.main.base.BaseViewHolder
import com.jingtian.demoapp.main.dp
import com.jingtian.demoapp.main.rank.Utils
import com.jingtian.demoapp.main.rank.activity.RankItemActivity
import com.jingtian.demoapp.main.rank.dialog.AddRankItemDialog
import com.jingtian.demoapp.main.rank.model.ModelRankItem
import com.jingtian.demoapp.main.rank.model.ModelRankItem.Companion.isValid
import com.jingtian.demoapp.main.widget.RankTypeChooser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
        private const val IMAGE_WIDTH = 200f
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
                width = bg.getWidth().toInt() + 4f.dp.toInt()
                height = bg.getHeight().toInt()
            }
            background = bg
        }
        with(binding.title) {
            text = data.itemName
        }
        with(binding.image) {
            if (data.image.isValid()) {
                scaleType = ImageView.ScaleType.CENTER_CROP
                data.image.loadImage(this, maxWidth = IMAGE_WIDTH.dp.toInt(), maxHeight = -1)
            } else {
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                setImageResource(R.drawable.load_failed)
            }
        }
    }

    override fun onPositiveClick(dialog: Dialog, modelRank: ModelRankItem) {
        dialog.dismiss()
//        val oldImageId = currentData?.image?.id ?: -1
        val currentData = currentData
        Utils.CoroutineUtils.runIOTask({
            if (currentData != null) {
                Utils.DataHolder.rankDB.updateRankItem(currentData, modelRank)
            } else {
                Utils.DataHolder.rankDB.rankItemDao().insert(modelRank)
            }
        }) {}
        this.currentData = modelRank
        lifecycleScope.launch {
            val currentAdapter = currentAdapter ?: return@launch
            currentAdapter.remove(currentPosition)
            val searchPos = currentAdapter.getDataList().binarySearch {
                if (it.score > modelRank.score) {
                    -1
                } else if (it.score < modelRank.score) {
                    1
                } else {
                    0
                }
            }
            val insertPos = if (searchPos < 0) {
                - searchPos - 1
            } else {
                searchPos
            }
            currentAdapter.insert(insertPos, modelRank)
        }
    }

    override fun onNegativeClick(dialog: Dialog) {
        dialog.dismiss()
    }

    override fun onDeleteClick(dialog: Dialog) {
        dialog.dismiss()
        currentAdapter?.remove(currentPosition)
        currentData?.let {
            Utils.CoroutineUtils.runIOTask({
                Utils.DataHolder.rankDB.deleteRankItem(it)
            }) { }
        }
    }
}