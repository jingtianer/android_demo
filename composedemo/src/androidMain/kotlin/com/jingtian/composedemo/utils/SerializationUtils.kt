package com.jingtian.composedemo.utils

import android.net.Uri
import android.os.Parcel
import java.io.ByteArrayInputStream
import java.io.InputStream

object SerializationUtils {
    fun Uri.toInputStream(): InputStream {
        val parcel = Parcel.obtain()
        try {
            this.writeToParcel(parcel, 0)
            return ByteArrayInputStream(parcel.marshall())
        } finally {
            parcel.recycle()
        }
    }

    fun InputStream.readAsUri(): Uri {
        val parcel = Parcel.obtain()
        try {
            this.use { `in`->
                val byteArray = `in`.readBytes()
                parcel.unmarshall(byteArray, 0 , byteArray.size)
            }
            parcel.setDataPosition(0)
            return Uri.CREATOR.createFromParcel(parcel)
        } finally {
            parcel.recycle()
        }
    }
}