package com.hubble.openbrain.data.db

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromStatus(status: ThoughtStatus): String = status.name

    @TypeConverter
    fun toStatus(value: String): ThoughtStatus = ThoughtStatus.valueOf(value)
}
