package com.jingtian.composedemo.dao.converter

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
//import java.util.Date

//@ProvidedTypeConverter
//class DateTypeConverter {
//    @TypeConverter
//    fun toDate(time: Long): Date {
//        return Date(time)
//    }
//
//    @TypeConverter
//    fun toString(date: Date): Long {
//        return date.time
//    }
//}

//object DateAsLongSerializer : KSerializer<Date> {
//    override val descriptor: SerialDescriptor =
//        PrimitiveSerialDescriptor("java.util.Date", PrimitiveKind.LONG)
//
//    override fun serialize(encoder: Encoder, value: Date) {
//        encoder.encodeLong(value.time)
//    }
//
//    override fun deserialize(decoder: Decoder): Date {
//        return Date(decoder.decodeLong())
//    }
//}
