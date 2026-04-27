package com.hubble.openbrain.data.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stores the OB1 bearer key in `EncryptedSharedPreferences` (AES-256-GCM, key-wrapped by
 * Android Keystore). On first read, migrates any legacy plaintext value previously written
 * to DataStore by `SettingsStore`.
 */
@Singleton
class SecureKeyStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val legacy: SettingsStore,
) {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context, MasterKey.DEFAULT_MASTER_KEY_ALIAS)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREF_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    private val state = MutableStateFlow<String?>(null)
    private val mutex = Mutex()

    val accessKey: Flow<String> = state.filterNotNull().onStart { ensureLoaded() }

    suspend fun getAccessKey(): String {
        ensureLoaded()
        return state.value.orEmpty()
    }

    suspend fun setAccessKey(value: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            prefs.edit().putString(ACCESS_KEY, value).commit()
            state.value = value
        }
    }

    private suspend fun ensureLoaded() = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (state.value != null) return@withLock
            var value = prefs.getString(ACCESS_KEY, "").orEmpty()
            if (value.isBlank()) {
                val migrated = legacy.consumeLegacyAccessKey()
                if (migrated.isNotBlank()) {
                    prefs.edit().putString(ACCESS_KEY, migrated).commit()
                    value = migrated
                }
            }
            state.value = value
        }
    }

    companion object {
        private const val PREF_FILE = "openbrain_secure_keys"
        private const val ACCESS_KEY = "ob1_access_key"
    }
}
