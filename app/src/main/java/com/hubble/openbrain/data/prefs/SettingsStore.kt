package com.hubble.openbrain.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.hubble.openbrain.ui.theme.ThemeId
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class WhisperModel(val displayName: String, val sizeMb: Int, val fileName: String) {
    TINY("Tiny", 39, "ggml-tiny.bin"),
    BASE("Base", 142, "ggml-base.bin"),
    SMALL("Small", 466, "ggml-small.bin");
}

private val Context.dataStore by preferencesDataStore(name = "openbrain_settings")

@Singleton
class SettingsStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val themeIdKey = stringPreferencesKey("theme_id")
    private val endpointKey = stringPreferencesKey("ob1_endpoint")
    private val accessKeyKey = stringPreferencesKey("ob1_access_key")
    private val whisperModelKey = stringPreferencesKey("whisper_model")
    private val audioRetentionKey = booleanPreferencesKey("audio_retention")

    val themeId: Flow<ThemeId> = context.dataStore.data.map { prefs ->
        prefs[themeIdKey]?.let { runCatching { ThemeId.valueOf(it) }.getOrNull() }
            ?: ThemeId.MaterialDefault
    }

    val endpoint: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[endpointKey] ?: DEFAULT_ENDPOINT
    }

    val accessKey: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[accessKeyKey] ?: ""
    }

    val whisperModel: Flow<WhisperModel> = context.dataStore.data.map { prefs ->
        prefs[whisperModelKey]?.let { runCatching { WhisperModel.valueOf(it) }.getOrNull() }
            ?: WhisperModel.BASE
    }

    val audioRetention: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[audioRetentionKey] ?: false
    }

    suspend fun setThemeId(id: ThemeId) {
        context.dataStore.edit { it[themeIdKey] = id.name }
    }

    suspend fun setEndpoint(value: String) {
        context.dataStore.edit { it[endpointKey] = value }
    }

    suspend fun setAccessKey(value: String) {
        context.dataStore.edit { it[accessKeyKey] = value }
    }

    suspend fun setWhisperModel(model: WhisperModel) {
        context.dataStore.edit { it[whisperModelKey] = model.name }
    }

    suspend fun setAudioRetention(enabled: Boolean) {
        context.dataStore.edit { it[audioRetentionKey] = enabled }
    }

    suspend fun reset() {
        context.dataStore.edit { it.clear() }
    }

    companion object {
        const val DEFAULT_ENDPOINT = "https://YOUR_PROJECT.supabase.co/functions/v1/open-brain-mcp"
    }
}
