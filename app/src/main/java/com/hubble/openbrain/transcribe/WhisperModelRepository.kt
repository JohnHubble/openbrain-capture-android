package com.hubble.openbrain.transcribe

import android.content.Context
import android.util.Log
import com.hubble.openbrain.data.prefs.SettingsStore
import com.hubble.openbrain.data.prefs.WhisperModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

sealed class ModelState {
    data object Checking : ModelState()
    data class NotPresent(val model: WhisperModel) : ModelState()
    data class Downloading(val model: WhisperModel, val bytes: Long, val total: Long) : ModelState()
    data class Ready(val model: WhisperModel, val file: File) : ModelState()
    data class Failed(val model: WhisperModel, val message: String) : ModelState()
}

@Singleton
class WhisperModelRepository @Inject constructor(
    @ApplicationContext context: Context,
    private val settings: SettingsStore,
    private val http: OkHttpClient,
) {
    private val modelsDir = File(context.filesDir, "models").apply { mkdirs() }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _state = MutableStateFlow<ModelState>(ModelState.Checking)
    val state: StateFlow<ModelState> = _state.asStateFlow()

    private var downloadJob: Job? = null

    init {
        scope.launch {
            settings.whisperModel.collect { model ->
                // Setting changes (incl. first emit at startup): cancel any in-flight download
                // for the previous model and re-evaluate. If the new model isn't on disk, state
                // flips to NotPresent and MainActivity shows the setup screen.
                downloadJob?.cancel()
                downloadJob = null
                val file = modelFile(model)
                _state.value = if (file.exists() && file.length() > 1_000_000L) {
                    ModelState.Ready(model, file)
                } else {
                    ModelState.NotPresent(model)
                }
            }
        }
    }

    fun modelFile(model: WhisperModel): File = File(modelsDir, model.fileName)

    suspend fun refresh() {
        val model = settings.whisperModel.first()
        val file = modelFile(model)
        _state.value = if (file.exists() && file.length() > 1_000_000L) {
            ModelState.Ready(model, file)
        } else {
            ModelState.NotPresent(model)
        }
    }

    fun startDownload() {
        if (downloadJob?.isActive == true) return
        downloadJob = scope.launch {
            val model = settings.whisperModel.first()
            runCatching { download(model) }
                .onFailure {
                    Log.e(TAG, "Model download failed", it)
                    _state.value = ModelState.Failed(model, it.message ?: "Unknown error")
                }
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
    }

    private suspend fun download(model: WhisperModel) = withContext(Dispatchers.IO) {
        val url = urlFor(model)
        Log.i(TAG, "Downloading whisper model $model from $url")
        _state.value = ModelState.Downloading(model, 0, 0)
        val partFile = File(modelsDir, "${model.fileName}.part")
        val finalFile = modelFile(model)
        ModelDownloader.downloadAndVerify(
            http = http,
            url = url,
            partFile = partFile,
            finalFile = finalFile,
            expectedSha256 = model.expectedSha256,
        ) { downloaded, total ->
            _state.value = ModelState.Downloading(model, downloaded, total)
        }
        Log.i(TAG, "Model download complete: ${finalFile.length()} bytes (sha256 verified)")
        _state.value = ModelState.Ready(model, finalFile)
    }

    companion object {
        private const val TAG = "WhisperModelRepository"
        private const val HF_BASE = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/"

        fun urlFor(model: WhisperModel): String = HF_BASE + model.fileName
    }
}
