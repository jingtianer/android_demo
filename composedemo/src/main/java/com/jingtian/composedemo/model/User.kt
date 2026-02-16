package com.jingtian.composedemo.model

import androidx.room.*

@Dao
interface UserInfoDao {
    companion object {
        const val TABLE_NAME = "TB_USER_INFO"
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertUser(user: User)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertAllUser(user: List<User>)

    @Query("SELECT max(userId) from $TABLE_NAME")
    fun getLastUserId(): Long

}

@Entity(
    tableName = UserInfoDao.TABLE_NAME,
)
class User(
    @PrimaryKey(autoGenerate = true)
    var userId: Long = INVALID_ID,
    var userName: String = "Unknown",
    var userDesc: String? = null,
    var userAvatar: FileInfo,
)