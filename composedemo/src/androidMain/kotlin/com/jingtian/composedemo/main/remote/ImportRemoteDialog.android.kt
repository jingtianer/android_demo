package com.jingtian.composedemo.main.remote

import com.jingtian.composedemo.utils.Base64Utils
import com.jingtian.composedemo.viewmodels.AuthParams
import java.nio.charset.Charset

actual fun onServerEditAuthSuccess(cipher: AuthParams, sftpServer: SftpServerStateHolder) {
    cipher.cipher?.doFinal(
        Base64Utils.decryptAsByteArray(sftpServer.password)
    )?.toString(Charset.defaultCharset())?.let { password ->
        sftpServer.password = password
    }
}