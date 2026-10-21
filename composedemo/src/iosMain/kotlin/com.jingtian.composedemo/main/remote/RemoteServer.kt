package com.jingtian.composedemo.main.remote

import com.jingtian.composedemo.dao.model.Album
import com.jingtian.composedemo.viewmodels.AlbumViewModel
import com.jingtian.composedemo.viewmodels.AuthParams
import com.jingtian.composedemo.viewmodels.AuthSuccess
import kotlin.random.Random
import kotlin.random.nextULong


enum class ServerType(val type: Int) {
    SFTP(0)
}

@kotlinx.serialization.Serializable
abstract class RemoteServer(
    var serverName: String = "新建: ${Random.nextULong()}",
    var serverId: String = "",
) {
    abstract suspend fun deleteFiles(files: List<RemoteSftpFileImpl>)
}

actual fun createSftpServer(serverName: String, serverId: String, userName: String, port: Int, ip: String, password: String, passwordIv: String?, path: String,
) : SftpServer {
    return SftpServerImpl(serverName, serverId, userName, port, ip, password, passwordIv, path)
}

@kotlinx.serialization.Serializable
class SftpServerImpl : SftpServer {
    constructor(
        serverName: String = "新建: ${Random.nextULong()}",
        serverId: String = "",
        userName: String = "",
        port: Int = 22,
        ip: String = "localhost",
        password: String = "",
        passwordIv: String? = null,
        path: String = "/",
    ) : super(serverName, serverId) {
        this.userName = userName
        this.port = port
        this.ip = ip
        this.password = password
        this.passwordIv = passwordIv
        this.path = path
    }

    override suspend fun importFiles(viewModel: AlbumViewModel, album: Album) {
        TODO("Not yet implemented")
    }

    override fun getInitializedCipher(): AuthParams {
        TODO("Not yet implemented")
    }

    override fun onUpdateAuthSuccess(authResult: AuthSuccess) {
        TODO("Not yet implemented")
    }

    override fun onAddSftpServerAuthSuccess(authResult: AuthSuccess) {
        TODO("Not yet implemented")
    }

    override fun getDecryptCipher(server: SftpServer): AuthParams {
        TODO("Not yet implemented")
    }

    override suspend fun deleteFiles(files: List<RemoteSftpFile>) {
        TODO("Not yet implemented")
    }
}

