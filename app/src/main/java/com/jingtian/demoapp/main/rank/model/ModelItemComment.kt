package com.jingtian.demoapp.main.rank.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.ProvidedTypeConverter
import androidx.room.Relation
import androidx.room.TypeConverter
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import com.jingtian.demoapp.main.rank.Utils
import com.jingtian.demoapp.main.rank.dao.RankModelItemCommentDao
import com.jingtian.demoapp.main.rank.model.ModelRankUser.Companion.getCurrentUserName
import java.util.Date

@Entity(
    tableName = RankModelItemCommentDao.TABLE_NAME,
    foreignKeys = [ForeignKey(
        entity = ModelRankItem::class,
        parentColumns = arrayOf("itemName"),
        childColumns = arrayOf("itemName"),
        onDelete = ForeignKey.CASCADE
    ),
    ForeignKey(
        entity = ModelRankUser::class,
        parentColumns = arrayOf("userName"),
        childColumns = arrayOf("userName"),
        onDelete = ForeignKey.RESTRICT
    )],
    indices = [Index("itemName")]
)

data class ModelItemComment(
    val itemName: String,
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    var userName: String = getCurrentUserName(),
    var comment: String = "",
    var creationDate: Date = Date(),
    var lastModifyDate: Date = Date(),
)

@ProvidedTypeConverter
class DateTypeConverter : TypeAdapter<Date>() {
    @TypeConverter
    fun toDate(time: Long): Date {
        return Date(time)
    }

    @TypeConverter
    fun toString(date: Date): Long {
        return date.time
    }

    override fun write(out: JsonWriter, value: Date) {
        out.value(value.time)
    }

    override fun read(`in`: JsonReader): Date {
        return Date(`in`.nextLong())
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