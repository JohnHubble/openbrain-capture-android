package com.hubble.openbrain.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [Thought::class], version = 1, exportSchema = true)
@TypeConverters(Converters::class)
abstract class ThoughtDb : RoomDatabase() {
    abstract fun thoughtDao(): ThoughtDao
}
