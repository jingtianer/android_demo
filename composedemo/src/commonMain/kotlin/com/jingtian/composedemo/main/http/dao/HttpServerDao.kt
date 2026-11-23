package com.jingtian.composedemo.main.http.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import com.jingtian.composedemo.dao.DataBase
import kotlinx.serialization.Serializable

@Dao
interface HttpServerDao {
    companion object {
        const val TABLE_NAME = "http_servers"
    }
    @Query("select * from ${TABLE_NAME}")
    suspend fun getAllHttpServer(): List<HttpServerEntity>

    @Insert
    suspend fun addHttpServer(server: HttpServerEntity): Long

    @Delete
    suspend fun removeHttpServer(server: HttpServerEntity)
}

@Entity(
    tableName = HttpServerDao.TABLE_NAME,
    indices = [Index(value = ["serverId"])],
)
@Serializable
class HttpServerEntity(
    @PrimaryKey(autoGenerate = true)
    var serverId: Long? = null,
    var serverName: String = "",
    var hostName: String = "",
    var port: String = "8000",
)
