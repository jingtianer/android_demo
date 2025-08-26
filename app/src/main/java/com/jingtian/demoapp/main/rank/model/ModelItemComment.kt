package com.jingtian.demoapp.main.rank.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.ProvidedTypeConverter
import androidx.room.Relation
import androidx.room.TypeConverter
import com.jingtian.demoapp.main.rank.dao.RankModelItemCommentDao
import java.util.Date

@Entity(
    tableName = RankModelItemCommentDao.TABLE_NAME,
    foreignKeys = [ForeignKey(
        entity = ModelRankItem::class,
        parentColumns = arrayOf("itemName"),
        childColumns = arrayOf("itemName"),
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("itemName")]
)

data class ModelItemComment(
    val itemName: String,
    @PrimaryKey
    var id: Long = -1,
    var comment: String = "",
    var creationDate: Date = Date(),
    var lastModifyDate: Date = Date(),
)

@ProvidedTypeConverter
class DateTypeConverter {
    @TypeConverter
    fun toDate(time: Long): Date {
        return Date(time)
    }

    @TypeConverter
    fun toString(date: Date): Long {
        return date.time
    }
}

class RelationRankItemAndComment(
    @Embedded
    val rank: ModelRankItem,

    @Relation(
        parentColumn = "itemName",
        entityColumn = "id",
        entity = ModelItemComment::class,
    )
    val list: List<ModelItemComment>
)