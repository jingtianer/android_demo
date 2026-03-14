package com.jingtian.composedemo.main.remote

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.jingtian.composedemo.multiplatform.getLongStorage
import com.jingtian.composedemo.multiplatform.getRawStorage
import com.jingtian.composedemo.utils.SharedPreferenceUtils

object ServerStorage {

    private val serverIdStorage = getLongStorage("server_id_storage")

    class Storage<T : RemoteServer>(private val serverType: ServerType) {
        private val storageObject by lazy {
            getRawStorage(
                "server_storage_${serverType.type}",
                GsonBuilder().create(),
                TypeToken.getParameterized(Map::class.java, String::class.java, SftpServer::class.java) as TypeToken<Map<String, SftpServer>>
            )
        }
        private var storageId by SharedPreferenceUtils.StorageLong(
            serverIdStorage,
            "server_type_${serverType.type}",
            0
        )
        fun allServer(): List<T> {
            return storageObject.all().values.toList() as List<T>
        }

        fun getServer(serverId: String): T? {
            return storageObject.editor<T?>().getValue(serverId, null)
        }

        fun updateServer(serverId: String, sftpServer: T) {
            storageObject.editor<T>().setValue(serverId, sftpServer)
        }

        fun addServer(server: T) {
            val nextId = storageId++
            val serverId = "${serverType.name.lowercase()}serve_$nextId"
            server.serverId = serverId
            storageObject.editor<T>().setValue(serverId, server)
        }

        fun deleteServer(serverId: String) {
            storageObject.editor<T>().delete(serverId)
        }
    }

    private val sftpServerStorage = Storage<SftpServer>(ServerType.SFTP)


    fun <T: RemoteServer> getStorage(serverType: ServerType): Storage<T> {
        return when(serverType) {
            ServerType.SFTP -> {
                sftpServerStorage as Storage<T>
            }
        }
    }
}