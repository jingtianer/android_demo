package com.jingtian.composedemo.utils

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

object Base64Utils {
    @OptIn(ExperimentalEncodingApi::class)
    fun encrypt(str: String): String {
        return Base64.encode(str.encodeToByteArray())
    }

    @OptIn(ExperimentalEncodingApi::class)
    fun encrypt(str: ByteArray): String {
        return Base64.encode(str)
    }

    @OptIn(ExperimentalEncodingApi::class)
    fun decrypt(str: String): String {
        return Base64.decode(str.encodeToByteArray()).decodeToString()
    }

    @OptIn(ExperimentalEncodingApi::class)
    fun decryptAsByteArray(str: String): ByteArray {
        return Base64.decode(str.encodeToByteArray())
    }
}