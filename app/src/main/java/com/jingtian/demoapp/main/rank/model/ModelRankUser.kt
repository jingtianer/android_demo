package com.jingtian.demoapp.main.rank.model

import android.net.Uri
import android.widget.ImageView
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jingtian.demoapp.R
import com.jingtian.demoapp.main.rank.Utils
import com.jingtian.demoapp.main.rank.dao.RankUserModelDao

@Entity(
    tableName = RankUserModelDao.TABLE_NAME,
)
data class ModelRankUser(
    @PrimaryKey
    var userName: String = DEFAULT_USER_NAME,
    var image: RankItemImage = RankItemImage()
) {
    companion object {
        private const val DEFAULT_USER_NAME = "默认用户"

        fun getUserInfo(userName: String? = Utils.DataHolder.userName): ModelRankUser? {
            if (userName != null) {
                val user = Utils.DataHolder.rankDB.rankUserDao().getUser(userName)
                return user
            }
            return null
        }

        fun getCurrentUserName(): String {
            val name = Utils.DataHolder.userName
            if (name != null) {
                return name
            }
            return DEFAULT_USER_NAME
        }

        fun ModelRankUser?.loadUserImage(
            imageView: ImageView, maxWidth: Int = -1, maxHeight: Int = -1,
        ) {
            if (this != null) {
                val rankImage = image
                if (rankImage.image != Uri.EMPTY) {
                    rankImage.loadImage(imageView, maxWidth, maxHeight)
                    return
                }
            }
            imageView.setImageResource(R.drawable.user)
        }

        fun ModelRankUser?.getUserNameOrDefault(): String {
            if (this != null) {
                return userName
            }
            return DEFAULT_USER_NAME
        }
    }

}