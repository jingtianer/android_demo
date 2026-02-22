package com.jingtian.composedemo.ui.widget

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.StateListDrawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.HorizontalScrollView
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import com.jingtian.composedemo.R
import com.jingtian.composedemo.base.app
import com.jingtian.composedemo.dao.model.ItemRank
import com.jingtian.composedemo.databinding.WidgetRankChooserBinding
import com.jingtian.composedemo.utils.ViewUtils.dpValue
import kotlin.math.max

class RankTypeChooser @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0,
): HorizontalScrollView(context, attributeSet, defStyleAttr, defStyleRes)  {
    private val binding = WidgetRankChooserBinding.inflate(LayoutInflater.from(context), this, true)
    var onRankChange = OnRankTypeChange {  }
        set(value) {
            field = value
            binding.root.setOnCheckedChangeListener { group, checkedId ->
                value.onRankTypeChange(getRankType(checkedId))
            }
        }
    init {
        val apply : View.(ItemRank) -> Unit = { itemRank ->
            val selected = createBg(itemRank)
            val unselected = createUnselectedBg(itemRank)

            background = StateListDrawable().apply {
                addState(intArrayOf(android.R.attr.state_checked), selected)
                addState(intArrayOf(-android.R.attr.state_checked), unselected)
            }

            layoutParams.width = max(selected.getWidth(), unselected.getWidth()).toInt() + paddingLeft + paddingRight
        }
        binding.rank1.apply(ItemRank.夯)
        binding.rank2.apply(ItemRank.顶尖)
        binding.rank3.apply(ItemRank.人上人)
        binding.rank4.apply(ItemRank.NPC)
        binding.rank5.apply(ItemRank.史)
        binding.root.setOnCheckedChangeListener { group, checkedId ->
            this.onRankChange.onRankTypeChange(getRankType(checkedId))
        }
    }
    companion object {

        fun interface OnRankTypeChange {
            fun onRankTypeChange(rankType: ItemRank)
        }

        fun createBg(rankType: ItemRank) : StrokeTextDrawable {
            return StrokeTextDrawable(Color.argb(
                rankType.a, rankType.r, rankType.g, rankType.b
            )).apply {
                setText(rankType.name)
                setStrokeColor(ResourcesCompat.getColor(app.resources, R.color.rank_text_stroke_color, null), 2.dp.dpValue)
                setTextSize(24.dp.dpValue)
                setTextColor(ResourcesCompat.getColor(app.resources, R.color.rank_text_color, null))
            }
        }
        fun createUnselectedBg(rankType: ItemRank) : StrokeTextDrawable {
            return StrokeTextDrawable(Color.TRANSPARENT).apply {
                setText(rankType.name)
                setStrokeColor(Color.TRANSPARENT, 0f.dp.dpValue)
                setTextSize(24.dp.dpValue)
                setTextColor(ResourcesCompat.getColor(app.resources, R.color.rank_text_color, null))
            }
        }
    }


    fun getRankType(checkedId:Int = binding.root.checkedRadioButtonId): ItemRank {
        return when(checkedId) {
            binding.rank1.id -> {
                ItemRank.夯
            }
            binding.rank2.id -> {
                ItemRank.顶尖
            }
            binding.rank3.id -> {
                ItemRank.人上人
            }
            binding.rank4.id -> {
                ItemRank.NPC
            }
            binding.rank5.id -> {
                ItemRank.史
            }
            else -> {
                ItemRank.NONE
            }
        }
    }

    fun setRankType(rankType: ItemRank) {
        when(rankType) {
            ItemRank.NONE -> {
                binding.root.clearCheck()
            }
            ItemRank.夯 -> {
                binding.root.check(binding.rank1.id)
            }
            ItemRank.顶尖 -> {
                binding.root.check(binding.rank2.id)
            }
            ItemRank.人上人 -> {
                binding.root.check(binding.rank3.id)
            }
            ItemRank.NPC -> {
                binding.root.check(binding.rank4.id)
            }
            ItemRank.史 -> {
                binding.root.check(binding.rank5.id)
            }
        }
    }
}