package com.jingtian.composedemo.utils

import java.nio.charset.Charset
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

object Base64Utils {
    @OptIn(ExperimentalEncodingApi::class)
    fun encrypt(str: String): String {
        return Base64.encode(str.toByteArray(Charset.defaultCharset()))
    }

    @OptIn(ExperimentalEncodingApi::class)
    fun encrypt(str: ByteArray): String {
        return Base64.encode(str)
    }

    @OptIn(ExperimentalEncodingApi::class)
    fun decrypt(str: String): String {
        return Base64.decode(str.toByteArray(Charset.defaultCharset())).toString(Charset.defaultCharset())
    }

    @OptIn(ExperimentalEncodingApi::class)
    fun decryptAsByteArray(str: String): ByteArray {
        return Base64.decode(str.toByteArray(Charset.defaultCharset()))
    }
}