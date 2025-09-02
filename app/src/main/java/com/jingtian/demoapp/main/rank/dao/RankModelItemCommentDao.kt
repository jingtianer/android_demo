package com.jingtian.demoapp.main.rank.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.jingtian.demoapp.main.rank.model.ModelItemComment
import com.jingtian.demoapp.main.rank.model.RelationUserAndComment

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

    @Query("SELECT A.userName AS comment_userName, A.itemName AS comment_itemName, A.comment AS comment_comment, A.creationDate AS comment_creationDate, A.lastModifyDate AS comment_lastModifyDate, A.id AS comment_id, " +
            "B.userName AS user_userName, B.image AS user_image " +
            "FROM $TABLE_NAME A " +
            "INNER JOIN ${RankUserModelDao.TABLE_NAME} B " +
            "ON A.userName = B.userName " +
            "WHERE A.itemName = :itemName ")
    fun getAllUserWithComments(itemName: String): List<RelationUserAndComment>
}