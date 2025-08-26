package com.jingtian.demoapp.main.rank.holder

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import com.jingtian.demoapp.R
import com.jingtian.demoapp.databinding.ItemRankItemBinding
import com.jingtian.demoapp.main.base.BaseViewHolder
import com.jingtian.demoapp.main.dp
import com.jingtian.demoapp.main.rank.model.ModelRankItem
import com.jingtian.demoapp.main.widget.RankTypeChooser

class RankItemHolder private constructor(private val binding: ItemRankItemBinding) : BaseViewHolder<ModelRankItem>(binding.root) {
    companion object {
        fun create(parent: ViewGroup) : RankItemHolder {
            return RankItemHolder(ItemRankItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
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
}