package com.jingtian.composedemo.main.http.api

import com.jingtian.composedemo.dao.model.Album
import com.jingtian.composedemo.dao.model.relation.AlbumItemRelation
import com.jingtian.composedemo.main.http.dao.HttpServerEntity
import io.ktor.client.*
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.utils.io.InternalAPI
import kotlinx.io.Source
import kotlinx.io.readByteArray
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

interface HttpServerApi {
    /* get */
    fun getFile(path: String): String

    /* get */
    suspend fun getFileSource(path: String): Source?

    /* get */
    suspend fun getAlbumList(): List<Album>

    /* get */
    suspend fun listAlbum(albumId: Long, offset: Long, limit: Long = 10): List<AlbumItemRelation>

    /* post */
    suspend fun updateItem(albumItemRelation: AlbumItemRelation /* body */)
}

expect fun platformKtorEngine(): HttpClientEngineFactory<HttpClientEngineConfig>

class HttpServerApiImpl(httpServerEntity: HttpServerEntity) : HttpServerApi {
    private val baseUrl = "http://${httpServerEntity.hostName}:${httpServerEntity.port}"
    val client = HttpClient(platformKtorEngine())
    val json = Json { ignoreUnknownKeys = true }
    override fun getFile(path: String): String {
        return "$baseUrl/getFile?path=$path"
    }

    @OptIn(InternalAPI::class)
    override suspend fun getFileSource(path: String): Source? {
        return runCatching {
            client.get(getFile(path)).rawContent.readBuffer
        }.getOrNull()
    }

    @OptIn(InternalAPI::class)
    override suspend fun getAlbumList(): List<Album> {
        return runCatching {
            json.decodeFromString<List<Album>>(client.get("$baseUrl/getAlbumList").rawContent.readBuffer.readByteArray().decodeToString())
        }.getOrDefault(listOf())
    }

    @OptIn(InternalAPI::class)
    override suspend fun listAlbum(
        albumId: Long,
        offset: Long,
        limit: Long
    ): List<AlbumItemRelation> {
        return runCatching { json.decodeFromString<List<AlbumItemRelation>>(client.get("$baseUrl/listAlbum") {
            parameter("albumId", albumId)
            parameter("offset", offset)
            parameter("limit", limit)
        }.rawContent.readBuffer.readByteArray().decodeToString()) }.getOrDefault(listOf())
    }

    override suspend fun updateItem(albumItemRelation: AlbumItemRelation) {
        runCatching {
            client.post("$baseUrl/updateItem") {
                setBody(json.encodeToString(albumItemRelation))
            }
        }
    }
}

fun HttpServerEntity.api(): HttpServerApi {
    return HttpServerApiImpl(this)
}