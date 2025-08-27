package com.jingtian.demoapp.main.rank.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.jingtian.demoapp.main.rank.model.ModelItemComment
import com.jingtian.demoapp.main.rank.model.ModelRank
import com.jingtian.demoapp.main.rank.model.ModelRankItem

@Dao
interface RankModelItemCommentDao {

    companion object {
        const val TABLE_NAME = "rank_model_item_comment_table"
    }
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(modelRank: ModelItemComment): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertAll(modelRank: List<ModelItemComment>): List<Long>

    @Update
    fun update(modelRank: ModelItemComment)


    @Delete
    fun delete(modelRank: ModelItemComment)

    @Query("SELECT * FROM $TABLE_NAME WHERE itemName = :itemName")
    fun getAllComment(itemName : String) : List<ModelItemComment>
}