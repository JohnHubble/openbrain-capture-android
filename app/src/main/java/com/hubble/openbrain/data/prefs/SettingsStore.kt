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

/**
 * SHA-256 hashes are taken from Hugging Face's `x-linked-etag` for each LFS file at
 * https://huggingface.co/ggerganov/whisper.cpp (resolve/main). If upstream rotates a
 * model the app will refuse to load it until this constant is bumped explicitly.
 */
enum class WhisperModel(
    val displayName: String,
    val sizeMb: Int,
    val fileName: String,
    val expectedSha256: String,
) {
    TINY("Tiny", 39, "ggml-tiny.bin", "be07e048e1e599ad46341c8d2a135645097a538221678b7acdd1b1919c6e1b21"),
    BASE("Base", 142, "ggml-base.bin", "60ed5bc3dd14eea856493d334349b405782ddcaf0028d4b5df4088345fba2efe"),
    SMALL("Small", 466, "ggml-small.bin", "1be3a9b2063867b937e64e2ec7483364a79917e157fa98c5d94b5c1fffea987b");
}

private val Context.dataStore by preferencesDataStore(name = "openbrain_settings")

@Singleton
class SettingsStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val themeIdKey = stringPreferencesKey("theme_id")
    private val endpointKey = stringPreferencesKey("ob1_endpoint")
    private val whisperModelKey = stringPreferencesKey("whisper_model")
    private val previewBeforeSaveKey = booleanPreferencesKey("preview_before_save")

    val themeId: Flow<ThemeId> = context.dataStore.data.map { prefs ->
        prefs[themeIdKey]?.let { runCatching { ThemeId.valueOf(it) }.getOrNull() }
            ?: ThemeId.MaterialDefault
    }

    val endpoint: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[endpointKey] ?: DEFAULT_ENDPOINT
    }

    val whisperModel: Flow<WhisperModel> = context.dataStore.data.map { prefs ->
        prefs[whisperModelKey]?.let { runCatching { WhisperModel.valueOf(it) }.getOrNull() }
            ?: WhisperModel.BASE
    }

    val previewBeforeSave: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[previewBeforeSaveKey] ?: false
    }

    suspend fun setThemeId(id: ThemeId) {
        context.dataStore.edit { it[themeIdKey] = id.name }
    }

    suspend fun setEndpoint(value: String) {
        context.dataStore.edit { it[endpointKey] = value }
    }

    suspend fun setWhisperModel(model: WhisperModel) {
        context.dataStore.edit { it[whisperModelKey] = model.name }
    }

    suspend fun setPreviewBeforeSave(enabled: Boolean) {
        context.dataStore.edit { it[previewBeforeSaveKey] = enabled }
    }

    suspend fun reset() {
        context.dataStore.edit { it.clear() }
    }

    /**
     * Reads and clears the legacy plaintext `ob1_access_key` value if present, returning it.
     * Used once at upgrade time to migrate the bearer key into encrypted storage.
     */
    suspend fun consumeLegacyAccessKey(): String {
        var value = ""
        context.dataStore.edit { prefs ->
            value = prefs[LEGACY_ACCESS_KEY] ?: ""
            if (value.isNotEmpty()) prefs.remove(LEGACY_ACCESS_KEY)
        }
        return value
    }

    companion object {
        const val DEFAULT_ENDPOINT = "https://YOUR_PROJECT.supabase.co/functions/v1/open-brain-mcp"
        private val LEGACY_ACCESS_KEY = stringPreferencesKey("ob1_access_key")
    }
}
