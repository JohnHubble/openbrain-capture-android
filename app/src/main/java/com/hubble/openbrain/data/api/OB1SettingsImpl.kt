package com.hubble.openbrain.data.api

import com.hubble.openbrain.data.prefs.SecureKeyStore
import com.hubble.openbrain.data.prefs.SettingsStore
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OB1SettingsImpl @Inject constructor(
    settings: SettingsStore,
    secure: SecureKeyStore,
) : OB1Settings {
    override val endpoint: Flow<String> = settings.endpoint
    override val accessKey: Flow<String> = secure.accessKey
}
