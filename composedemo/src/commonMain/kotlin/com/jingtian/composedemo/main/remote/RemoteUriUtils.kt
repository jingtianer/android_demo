package com.jingtian.composedemo.main.remote

expect fun serverTypeAndId(file: RemoteSftpFile): Pair<ServerType, String>?