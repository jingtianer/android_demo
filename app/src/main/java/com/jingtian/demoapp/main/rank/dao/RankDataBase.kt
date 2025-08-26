package com.jingtian.demoapp.main.rank.dao

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.jingtian.demoapp.main.rank.model.DateTypeConverter
import com.jingtian.demoapp.main.rank.model.ModelItemComment
import com.jingtian.demoapp.main.rank.model.ModelRank
import com.jingtian.demoapp.main.rank.model.ModelRankItem
import com.jingtian.demoapp.main.rank.model.RankItemImageTypeConverter

@Database(
    entities = [
        ModelRank::class,
        ModelRankItem::class,
        ModelItemComment::class
    ], version = 1
)
@TypeConverters(DateTypeConverter::class, RankItemImageTypeConverter::class)
abstract class RankDatabase : RoomDatabase() {
    abstract fun rankListDao(): RankModelDao
    abstract fun rankItemDao(): RankModelItemDao
    abstract fun rankCommentDao(): RankModelItemCommentDao
}