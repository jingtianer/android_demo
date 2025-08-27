package com.jingtian.demoapp.main.rank.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.jingtian.demoapp.main.rank.model.ModelRank

@Dao
interface RankModelDao {
    companion object {
        const val TABLE_NAME = "rank_model_table"
    }
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(modelRank: ModelRank): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertAll(modelRank: List<ModelRank>) : List<Long>

    @Update
    fun update(modelRank: ModelRank)


    @Delete
    fun delete(modelRank: ModelRank)

    @Query("SELECT * FROM $TABLE_NAME")
    fun getAllRankModel() : List<ModelRank>
}