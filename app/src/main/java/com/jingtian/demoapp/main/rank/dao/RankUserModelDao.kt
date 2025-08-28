package com.jingtian.demoapp.main.rank.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.jingtian.demoapp.main.rank.model.ModelRank
import com.jingtian.demoapp.main.rank.model.ModelRankUser

@Dao
interface RankUserModelDao {
    companion object {
        const val TABLE_NAME = "rank_user_model_table"
    }
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(modelRank: ModelRankUser): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertAll(modelRank: List<ModelRankUser>) : List<Long>

    @Update
    fun update(modelRank: ModelRankUser)


    @Delete
    fun delete(modelRank: ModelRankUser): Int

    @Query("SELECT * FROM $TABLE_NAME")
    fun getAllUser() : List<ModelRankUser>

    @Query("SELECT * FROM $TABLE_NAME WHERE userName = :userName")
    fun getUser(userName: String): ModelRankUser
}