package com.jingtian.demoapp.main.rank.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.jingtian.demoapp.main.rank.dao.RankModelDao

@Entity(
    tableName = RankModelDao.TABLE_NAME,
)
data class ModelRank(
    @PrimaryKey
    val rankName: String = "",
) {
    companion object {
        fun ModelRank.isValid(): Boolean {
            return rankName.isNotEmpty()
        }
    }
}

