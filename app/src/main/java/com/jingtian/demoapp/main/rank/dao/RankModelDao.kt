package com.jingtian.demoapp.main.rank.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.jingtian.demoapp.main.rank.model.ModelRank

@Dao
interface RankModelDao {
    companion object {
        const val TABLE_NAME = "rank_model_table"
    }
    @Insert
    fun insert(modelRank: ModelRank)

    @Insert
    fun insertAll(modelRank: List<ModelRank>)

    @Update
    fun update(modelRank: ModelRank)


    @Delete
    fun delete(modelRank: ModelRank)

    @Query("SELECT * FROM $TABLE_NAME")
    fun getAllRankModel() : List<ModelRank>
}