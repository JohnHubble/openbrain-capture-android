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
import okhttp3.Request
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
        scope.launch { refresh() }
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

        val request = Request.Builder().url(url).build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw RuntimeException("HTTP ${response.code}")
            val body = response.body ?: throw RuntimeException("Empty response body")
            val total = body.contentLength().takeIf { it > 0 } ?: -1L
            val partFile = File(modelsDir, "${model.fileName}.part")
            partFile.outputStream().use { out ->
                body.byteStream().use { input ->
                    val buf = ByteArray(64 * 1024)
                    var downloaded = 0L
                    var lastReport = 0L
                    while (true) {
                        val n = input.read(buf)
                        if (n <= 0) break
                        out.write(buf, 0, n)
                        downloaded += n
                        if (downloaded - lastReport > 256 * 1024) {
                            _state.value = ModelState.Downloading(model, downloaded, total)
                            lastReport = downloaded
                        }
                    }
                }
            }
            val finalFile = modelFile(model)
            if (finalFile.exists()) finalFile.delete()
            if (!partFile.renameTo(finalFile)) {
                throw RuntimeException("Rename ${partFile.name} -> ${finalFile.name} failed")
            }
            Log.i(TAG, "Model download complete: ${finalFile.length()} bytes")
            _state.value = ModelState.Ready(model, finalFile)
        }
    }

    companion object {
        private const val TAG = "WhisperModelRepository"
        private const val HF_BASE = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/"

        fun urlFor(model: WhisperModel): String = HF_BASE + model.fileName
    }
}
