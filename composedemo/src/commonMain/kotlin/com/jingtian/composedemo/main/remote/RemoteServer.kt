package com.jingtian.composedemo.main.remote


import com.jingtian.composedemo.dao.model.Album
import com.jingtian.composedemo.multiplatform.MultiplatformFile

import com.jingtian.composedemo.viewmodels.AlbumViewModel
import com.jingtian.composedemo.viewmodels.AuthParams
import com.jingtian.composedemo.viewmodels.AuthSuccess
import kotlin.random.Random
import kotlin.random.nextULong


enum class ServerType(val type: Int) {
    SFTP(0)
}

interface RemoteSftpFile: MultiplatformFile

@kotlinx.serialization.Serializable
abstract class RemoteServer(
    var serverName: String = "新建: ${Random.nextULong()}",
    var serverId: String = "",
) {
    abstract suspend fun deleteFiles(files: List<RemoteSftpFile>)
}

expect fun createSftpServer(
        serverName: String = "新建: ${Random.nextULong()}",
        serverId: String = "",
        userName: String = "",
        port: Int = 22,
        ip: String = "localhost",
        password: String = "",
      passwordIv: String? = null,
        path: String = "/",
) : SftpServer

@kotlinx.serialization.Serializable
abstract class SftpServer : RemoteServer {
    var userName: String
    var port: Int
    var ip: String
    var password: String
    var passwordIv: String?
    var path: String
    constructor(serverName: String = "新建: ${Random.nextULong()}",
                serverId: String = "",
                userName: String = "",
                port: Int = 22,
                ip: String = "localhost",
                password: String = "",
                passwordIv: String? = null,
                path: String = "/"
    ) : super(serverName, serverId) {
        this.userName = userName
        this.port = port
        this.ip = ip
        this.password = password
        this.passwordIv = passwordIv
        this.path = path
    }
    abstract suspend fun importFiles(viewModel: AlbumViewModel, album: Album)

    abstract fun getInitializedCipher(): AuthParams

    abstract fun onUpdateAuthSuccess(authResult: AuthSuccess)

    abstract fun onAddSftpServerAuthSuccess(authResult: AuthSuccess)

    abstract fun getDecryptCipher(server: SftpServer): AuthParams

    abstract override suspend fun deleteFiles(files: List<RemoteSftpFile>)
}

