package com.hubble.openbrain.di

import com.hubble.openbrain.data.api.OB1Settings
import com.hubble.openbrain.data.api.OB1SettingsImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SettingsModule {
    @Binds
    @Singleton
    abstract fun bindOB1Settings(impl: OB1SettingsImpl): OB1Settings
}
