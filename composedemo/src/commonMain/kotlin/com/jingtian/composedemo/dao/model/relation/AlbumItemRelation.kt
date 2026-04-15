package com.jingtian.composedemo.dao.model.relation

import androidx.room.DatabaseView
import androidx.room.Embedded
import androidx.room.Relation
import com.jingtian.composedemo.dao.AlbumItemDao
import com.jingtian.composedemo.dao.model.AlbumItem
import com.jingtian.composedemo.dao.model.FileInfo
import com.jingtian.composedemo.dao.model.LabelInfo
import kotlinx.serialization.Serializable

@DatabaseView("select * from ${AlbumItemDao.TABLE_NAME}", "album_relation")
@Serializable
class AlbumItemRelation(
    @Embedded
    val albumItem: AlbumItem,

    @Relation(
        parentColumn = "itemId",
        entityColumn = "albumItemId",
        entity = LabelInfo::class,
    )
    val labelInfos: List<LabelInfo>,

    @Relation(
        parentColumn = "fileId",
        entityColumn = "id",
        entity = FileInfo::class
    )
    val fileInfo: FileInfo
)
