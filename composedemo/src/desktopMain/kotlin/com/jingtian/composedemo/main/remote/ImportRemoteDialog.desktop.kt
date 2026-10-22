package com.jingtian.composedemo.main.remote

import com.jingtian.composedemo.utils.Base64Utils
import com.jingtian.composedemo.viewmodels.AuthParams
import java.nio.charset.Charset

actual fun onServerEditAuthSuccess(cipher: AuthParams, sftpServer: SftpServerStateHolder) {
    sftpServer.password = Base64Utils.decrypt(sftpServer.password)
}