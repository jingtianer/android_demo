package com.jingtian.composedemo.dao.model.relation

import androidx.room.DatabaseView
import androidx.room.Embedded
import androidx.room.Relation
import com.jingtian.composedemo.dao.model.Album
import com.jingtian.composedemo.dao.model.AlbumItem
import com.jingtian.composedemo.dao.model.FileInfo
import com.jingtian.composedemo.dao.model.LabelInfo

@DatabaseView
class AlbumRelation(
    @Embedded
    val albumItem: Album,

    @Relation(
        parentColumn = "albumId",
        entityColumn = "albumId",
        entity = AlbumItem::class,
    )
    val albumItemList: List<AlbumItemRelation>,
)