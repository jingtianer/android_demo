package com.jingtian.demoapp.main.rank.model

import android.net.Uri
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
    var id: Long = -1,
    var image: Uri = Uri.EMPTY
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

@Entity(
    tableName = RankModelItemDao.TABLE_NAME,
    foreignKeys = [ForeignKey(
        entity = ModelRank::class,
        parentColumns = arrayOf("rankName"),
        childColumns = arrayOf("rankName"),
        onDelete = ForeignKey.CASCADE
    )],
    indices = [androidx.room.Index("rankName")]
)
data class ModelRankItem(
    @PrimaryKey
    var itemName: String = "",
    var rankName: String = "",
    var score: Float = 0f,
    var desc: String = "",
    var image: RankItemImage = RankItemImage(),
) {
    companion object {
        fun ModelRankItem.isValid(): Boolean {
            return itemName.isNotEmpty()
        }
    }
}
class RelationRankAndItem(
    @Embedded
    val rank: ModelRank,

    @Relation(
        parentColumn = "rankName",
        entityColumn = "itemName",
        entity = ModelRankItem::class,
    )
    val list: List<ModelRankItem>
)