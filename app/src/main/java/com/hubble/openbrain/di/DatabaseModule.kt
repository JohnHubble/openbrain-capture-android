package com.hubble.openbrain.di

import android.content.Context
import androidx.room.Room
import com.hubble.openbrain.data.db.ThoughtDao
import com.hubble.openbrain.data.db.ThoughtDb
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDb(@ApplicationContext ctx: Context): ThoughtDb =
        Room.databaseBuilder(ctx, ThoughtDb::class.java, "thoughts.db")
            .build()

    @Provides
    fun provideDao(db: ThoughtDb): ThoughtDao = db.thoughtDao()
}
