package com.jingtian.demoapp.main.rank.model

import android.graphics.Color
import android.net.Uri
import androidx.annotation.ColorInt
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.ProvidedTypeConverter
import androidx.room.Relation
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.jingtian.demoapp.main.rank.Utils
import com.jingtian.demoapp.main.rank.dao.RankModelItemDao

data class RankItemImage(
    var id: Long = -1, var image: Uri = Uri.EMPTY
)

@ProvidedTypeConverter
class RankItemImageTypeConverter {
    @TypeConverter
    fun toRankItemImage(id: Long): RankItemImage {
        return Utils.DataHolder.ImageStorage.getImage(id)?.let {
            RankItemImage(id, it)
        } ?: RankItemImage()
    }

    @TypeConverter
    fun toLong(rankItemImage: RankItemImage): Long {
        return rankItemImage.id
    }
}


enum class RankItemRankType(val r: Int, val g: Int, val b: Int, val a: Int = 255) {
    NONE(0, 0, 0), 夯(0xef, 0x2b, 0x03), 顶尖(0xff, 0xb6, 0x18), 人上人(0x39, 0x8d, 0xff), NPC(0xf9, 0xf5, 0xf2), 史(0xa1, 0xbd, 0xb1)
}

@ProvidedTypeConverter
class RankItemRankTypeConverter {
    @TypeConverter
    fun toRankItemImage(id: Int): RankItemRankType {
        return when(id) {
            RankItemRankType.夯.ordinal -> {
                RankItemRankType.夯
            }
            RankItemRankType.顶尖.ordinal -> {
                RankItemRankType.顶尖
            }
            RankItemRankType.人上人.ordinal -> {
                RankItemRankType.人上人
            }
            RankItemRankType.NPC.ordinal -> {
                RankItemRankType.NPC
            }
            RankItemRankType.史.ordinal -> {
                RankItemRankType.史
            }
            else -> {
                RankItemRankType.NONE
            }
        }
    }

    @TypeConverter
    fun toLong(rankItemImage: RankItemRankType): Int {
        return rankItemImage.ordinal
    }
}

@Entity(
    tableName = RankModelItemDao.TABLE_NAME, foreignKeys = [ForeignKey(
        entity = ModelRank::class,
        parentColumns = arrayOf("rankName"),
        childColumns = arrayOf("rankName"),
        onDelete = ForeignKey.CASCADE
    )], indices = [androidx.room.Index("rankName")]
)
data class ModelRankItem(
    @PrimaryKey var itemName: String = "",
    var rankName: String = "",
    var score: Float = 0f,
    var desc: String = "",
    var rankType: RankItemRankType = RankItemRankType.夯,
    var image: RankItemImage = RankItemImage(),
) {
    companion object {
        fun ModelRankItem.isValid(): Boolean {
            return itemName.isNotEmpty()
        }
    }
}

class RelationRankAndItem(
    @Embedded val rank: ModelRank,

    @Relation(
        parentColumn = "rankName",
        entityColumn = "itemName",
        entity = ModelRankItem::class,
    ) val list: List<ModelRankItem>
)