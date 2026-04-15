package com.jingtian.composedemo.main.remote

import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toLowerCase
import com.jingtian.composedemo.multiplatform.getLongStorage
import com.jingtian.composedemo.multiplatform.getJsonStorage
import com.jingtian.composedemo.multiplatform.getRawStorage
import com.jingtian.composedemo.utils.SharedPreferenceUtils
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object ServerStorage {

    private val serverIdStorage = getLongStorage("server_id_storage")

    class Storage<T : RemoteServer>(private val serverType: ServerType, kSerializer: KSerializer<T>) {
        private val storageObject by lazy {
            getRawStorage<T>("server_storage_${serverType.type}", kSerializer)
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
            return storageObject.editor().getValue(serverId)
        }

        fun updateServer(serverId: String, sftpServer: T) {
            storageObject.editor().setValue(serverId, sftpServer)
        }

        fun allocateServerId(server: RemoteServer) {
            val nextId = storageId++
            val serverId = "${serverType.name.toLowerCase(Locale.current)}serve_$nextId"
            server.serverId = serverId
        }

        fun addServer(server: T) {
            storageObject.editor().setValue(server.serverId, server)
        }

        fun deleteServer(serverId: String) {
            storageObject.editor().delete(serverId)
        }
    }

    private val sftpServerStorage = Storage(ServerType.SFTP, SftpServer.serializer())

    fun <T: RemoteServer> getStorage(serverType: ServerType): Storage<T> {
        return when(serverType) {
            ServerType.SFTP -> {
                sftpServerStorage as Storage<T>
            }
        }
    }
}