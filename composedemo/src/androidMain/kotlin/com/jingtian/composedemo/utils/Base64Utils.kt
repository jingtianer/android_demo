package com.jingtian.composedemo.utils

import android.util.Base64
import java.nio.charset.Charset

object Base64Utils {
    fun encrypt(str: String): String {
        return Base64.encode(str.toByteArray(Charset.defaultCharset()), Base64.DEFAULT).toString(Charset.defaultCharset())
    }

    fun encrypt(str: ByteArray): String {
        return Base64.encode(str, Base64.DEFAULT).toString(Charset.defaultCharset())
    }

    fun decrypt(str: String): String {
        return Base64.decode(str.toByteArray(Charset.defaultCharset()), Base64.DEFAULT).toString(Charset.defaultCharset())
    }

    fun decryptAsByteArray(str: String): ByteArray {
        return Base64.decode(str.toByteArray(Charset.defaultCharset()), Base64.DEFAULT)
    }
}