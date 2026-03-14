package com.jingtian.composedemo.main.remote

import kotlin.random.Random
import kotlin.random.nextULong

enum class ServerType(val type: Int) {
    SFTP(0)
}

abstract class RemoteServer(
    var serverName: String = "新建: ${Random.nextULong()}",
    var serverId: String = "",
)

class SftpServer(
    serverName: String = "新建: ${Random.nextULong()}",
    serverId: String = "",
    var userName: String = "",
    var port: Int = 22,
    var ip: String = "",
    var password: String = "",
    var path: String = "/",
) : RemoteServer(serverName, serverId)