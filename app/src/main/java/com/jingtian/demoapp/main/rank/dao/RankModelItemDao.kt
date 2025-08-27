package com.jingtian.demoapp.main.rank.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.jingtian.demoapp.main.rank.model.ModelRankItem

@Dao
interface RankModelItemDao {
    companion object {
        const val TABLE_NAME = "rank_model_item_table"
    }
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(modelRank: ModelRankItem): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertAll(modelRank: List<ModelRankItem>): List<Long>

    @Update
    fun update(modelRank: ModelRankItem)


    @Delete
    fun delete(modelRank: ModelRankItem)

    @Query("SELECT * FROM ${RankModelItemDao.TABLE_NAME} WHERE rankName = :rankName ORDER BY score DESC")
    fun getAllRankItemByRankName(rankName : String) : List<ModelRankItem>


    @Query("SELECT * FROM ${RankModelItemDao.TABLE_NAME} WHERE (rankName = :rankName AND itemName = :itemName)")
    fun getRankItem(rankName : String, itemName: String) : List<ModelRankItem>


    @Query("SELECT * FROM ${RankModelItemDao.TABLE_NAME}")
    fun getAllRankItem() : List<ModelRankItem>
}