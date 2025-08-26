package com.jingtian.demoapp.main.rank.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.jingtian.demoapp.main.rank.dao.RankModelItemCommentDao.Companion
import com.jingtian.demoapp.main.rank.model.ModelRank
import com.jingtian.demoapp.main.rank.model.ModelRankItem

@Dao
interface RankModelItemDao {
    companion object {
        const val TABLE_NAME = "rank_model_item_table"
    }
    @Insert
    fun insert(modelRank: ModelRankItem)

    @Insert
    fun insertAll(modelRank: List<ModelRankItem>)

    @Update
    fun update(modelRank: ModelRankItem)


    @Delete
    fun delete(modelRank: ModelRankItem)

    @Query("SELECT * FROM ${RankModelItemDao.TABLE_NAME} WHERE rankName = :rankName")
    fun getAllRankModel(rankName : String) : List<ModelRankItem>
}