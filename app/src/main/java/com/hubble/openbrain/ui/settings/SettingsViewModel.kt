package com.hubble.openbrain.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hubble.openbrain.data.api.OB1Client
import com.hubble.openbrain.data.prefs.SettingsStore
import com.hubble.openbrain.data.prefs.WhisperModel
import com.hubble.openbrain.data.repo.ThoughtRepository
import com.hubble.openbrain.ui.theme.ThemeId
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

data class SettingsUiState(
    val themeId: ThemeId = ThemeId.MaterialDefault,
    val endpoint: String = SettingsStore.DEFAULT_ENDPOINT,
    val accessKey: String = "",
    val whisperModel: WhisperModel = WhisperModel.BASE,
    val audioRetention: Boolean = false,
)

sealed interface TestState {
    data object Idle : TestState
    data object Running : TestState
    data class Success(val responseText: String) : TestState
    data class Failure(val message: String) : TestState
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val store: SettingsStore,
    private val thoughts: ThoughtRepository,
    private val client: OB1Client,
) : ViewModel() {

    private val _testState = MutableStateFlow<TestState>(TestState.Idle)
    val testState: StateFlow<TestState> = _testState.asStateFlow()

    val state: StateFlow<SettingsUiState> = combine(
        store.themeId,
        store.endpoint,
        store.accessKey,
        store.whisperModel,
        store.audioRetention,
    ) { theme, endpoint, key, model, retention ->
        SettingsUiState(theme, endpoint, key, model, retention)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SettingsUiState())

    private val _storageBytes = MutableStateFlow(0L to 0L)
    val storageBytes: StateFlow<Pair<Long, Long>> = _storageBytes.asStateFlow()

    init {
        refreshStorage()
    }

    fun refreshStorage() {
        viewModelScope.launch {
            val bytes = withContext(Dispatchers.IO) {
                val dbSize = context.getDatabasePath("thoughts.db").let {
                    if (it.exists()) it.length() else 0L
                }
                val modelsDir = File(context.filesDir, "models")
                val modelSize = modelsDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
                dbSize + modelSize to 4_500_000_000L
            }
            _storageBytes.value = bytes
        }
    }

    fun setEndpoint(value: String) = viewModelScope.launch { store.setEndpoint(value) }
    fun setAccessKey(value: String) = viewModelScope.launch { store.setAccessKey(value) }
    fun setWhisperModel(model: WhisperModel) = viewModelScope.launch { store.setWhisperModel(model) }
    fun setAudioRetention(enabled: Boolean) = viewModelScope.launch { store.setAudioRetention(enabled) }

    fun clearQueue() = viewModelScope.launch {
        thoughts.clearUnsent()
        refreshStorage()
    }

    fun resetAll() = viewModelScope.launch {
        store.reset()
        thoughts.clearAll()
        refreshStorage()
    }

    fun testConnection() {
        if (_testState.value is TestState.Running) return
        _testState.value = TestState.Running
        viewModelScope.launch {
            val probe = "Open Brain Android connection test at ${java.time.Instant.now()}"
            val result = client.captureThought(probe)
            _testState.value = when (result) {
                is OB1Client.CaptureResult.Success -> TestState.Success(result.text)
                is OB1Client.CaptureResult.Failure -> TestState.Failure(result.message)
            }
        }
    }

    fun dismissTest() {
        _testState.value = TestState.Idle
    }
}
