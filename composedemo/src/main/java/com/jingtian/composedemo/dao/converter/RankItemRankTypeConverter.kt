package com.jingtian.composedemo.dao.converter

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import com.jingtian.composedemo.dao.model.ItemRank

@ProvidedTypeConverter
class ItemRankConverter {
    @TypeConverter
    fun toRankItemImage(id: Int): ItemRank {
        return when(id) {
            0 -> {
                ItemRank.夯
            }
            1 -> {
                ItemRank.顶尖
            }
            2 -> {
                ItemRank.人上人
            }
            3 -> {
                ItemRank.NPC
            }
            4 -> {
                ItemRank.史
            }
            else -> {
                ItemRank.NONE
            }
        }
    }

    @TypeConverter
    fun fromRankItemImage(rankItemImage: ItemRank): Int {
        return rankItemImage.ordinal
    }
}