package com.jingtian.demoapp.main.rank.dao

import android.text.TextUtils
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.jingtian.demoapp.main.rank.Utils
import com.jingtian.demoapp.main.rank.model.DateTypeConverter
import com.jingtian.demoapp.main.rank.model.ModelItemComment
import com.jingtian.demoapp.main.rank.model.ModelRank
import com.jingtian.demoapp.main.rank.model.ModelRankItem
import com.jingtian.demoapp.main.rank.model.ModelRankUser
import com.jingtian.demoapp.main.rank.model.RankItemImageTypeConverter
import com.jingtian.demoapp.main.rank.model.RankItemRankTypeConverter
import java.util.concurrent.Callable

@Database(
    entities = [
        ModelRank::class,
        ModelRankItem::class,
        ModelItemComment::class,
        ModelRankUser::class,
    ], version = 1
)
@TypeConverters(DateTypeConverter::class, RankItemImageTypeConverter::class, RankItemRankTypeConverter::class)
abstract class RankDatabase : RoomDatabase() {
    abstract fun rankListDao(): RankModelDao
    abstract fun rankItemDao(): RankModelItemDao
    abstract fun rankCommentDao(): RankModelItemCommentDao
    abstract fun rankUserDao(): RankUserModelDao

    fun deleteRankItem(rankItem: ModelRankItem) {
        runInTransaction {
            try {
                Utils.DataHolder.ImageStorage.delete(rankItem.image.id)
            } catch (ignore :Exception) {

            }
            rankItemDao().delete(rankItem)
        }
    }

    fun deleteRank(modelRank: ModelRank) {
        runInTransaction {
            val rankItems = rankItemDao().getAllRankItemByRankName(modelRank.rankName)
            for (rankItem in rankItems) {
                try {
                    Utils.DataHolder.ImageStorage.delete(rankItem.image.id)
                } catch (ignore : Exception) {

                }
            }
            rankListDao().delete(modelRank)
        }
    }

    fun tryDeleteUser(modelRankUser: ModelRankUser): Int {
        return try {
            runInTransaction(Callable {
                Utils.DataHolder.ImageStorage.delete(modelRankUser.image.id)
                rankUserDao().delete(modelRankUser)
            })
        } catch (ignore : Exception) {
            0
        }
    }

    fun updateUser(oldUser: ModelRankUser, modelRankUser: ModelRankUser) {
        runInTransaction {
            val dao = rankUserDao()
            if (TextUtils.equals(oldUser.userName, modelRankUser.userName)) {
                dao.update(modelRankUser)
            } else {
                dao.updateUserName(oldUser.userName, modelRankUser.userName)
                dao.update(modelRankUser)
            }
        }
    }

    fun updateRankItem(oldRankItem: ModelRankItem, rankItem: ModelRankItem) {
        runInTransaction {
            val dao = rankItemDao()
            if (TextUtils.equals(oldRankItem.itemName, rankItem.itemName)) {
                dao.update(rankItem)
            } else {
                dao.updateItemName(oldRankItem.itemName, rankItem.itemName)
                dao.update(rankItem)
            }
        }
    }
}