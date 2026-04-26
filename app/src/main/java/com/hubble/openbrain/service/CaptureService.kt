package com.hubble.openbrain.service

import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.content.getSystemService
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.hubble.openbrain.audio.AudioRecorder
import com.hubble.openbrain.audio.chunkToFloatWindows
import com.hubble.openbrain.data.prefs.SettingsStore
import com.hubble.openbrain.data.repo.ThoughtRepository
import com.hubble.openbrain.transcribe.ModelState
import com.hubble.openbrain.transcribe.WhisperModelRepository
import com.hubble.openbrain.transcribe.WhisperTranscriber
import com.hubble.openbrain.widget.CaptureWidgetProvider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class CaptureService : LifecycleService() {

    @Inject lateinit var audioRecorder: AudioRecorder
    @Inject lateinit var transcriber: WhisperTranscriber
    @Inject lateinit var modelRepo: WhisperModelRepository
    @Inject lateinit var thoughts: ThoughtRepository
    @Inject lateinit var state: CaptureStateHolder
    @Inject lateinit var settings: SettingsStore

    private var wakeLock: PowerManager.WakeLock? = null
    private var pipelineJob: Job? = null
    private var tickerJob: Job? = null
    private var drainJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        CaptureNotifications.ensureChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_STOP -> {
                Log.i(TAG, "ACTION_STOP received")
                beginDrainAndStop()
                return START_NOT_STICKY
            }
            else -> startCapture()
        }
        return START_STICKY
    }

    private fun startCapture() {
        if (pipelineJob?.isActive == true) return
        val startedAt = System.currentTimeMillis()
        startForegroundWithType(contentText = "Listening…")
        state.startSession(startedAt)
        CaptureWidgetProvider.refresh(this)
        acquireWake()

        tickerJob = lifecycleScope.launch {
            while (true) {
                delay(1000)
                state.tickDuration(System.currentTimeMillis() - startedAt)
            }
        }

        pipelineJob = lifecycleScope.launch(Dispatchers.IO) {
            runCatching {
                val model = settings.whisperModel.first()
                val modelFile = modelRepo.modelFile(model)
                check(modelFile.exists()) { "Model file missing: ${modelFile.name}" }
                Log.i(TAG, "Loading whisper model ${model.displayName}")
                transcriber.ensureLoaded(modelFile)

                audioRecorder.stream()
                    .chunkToFloatWindows()
                    .onStart { Log.i(TAG, "Audio pipeline started") }
                    .collect { window ->
                        val rms = rms(window)
                        if (rms < RMS_SILENCE_THRESHOLD) {
                            Log.i(TAG, "Skipping window · rms=${"%.4f".format(rms)} (silent)")
                            return@collect
                        }
                        val t0 = System.currentTimeMillis()
                        val result = runCatching { transcriber.transcribe(window) }
                            .getOrElse { err ->
                                Log.e(TAG, "Transcription failed", err)
                                return@collect
                            }
                        val clean = result.text.trim()
                        if (clean.isBlank() || clean in IGNORED_TEXTS) return@collect
                        if (result.maxNoSpeechProb > NO_SPEECH_THRESHOLD) {
                            Log.i(TAG, "Dropping '$clean' · no_speech_prob=${"%.2f".format(result.maxNoSpeechProb)}")
                            return@collect
                        }
                        Log.i(TAG, "Transcribed ${clean.length} chars · rms=${"%.4f".format(rms)} · nsp=${"%.2f".format(result.maxNoSpeechProb)} · ${System.currentTimeMillis() - t0}ms")
                        thoughts.insertPending(
                            text = clean,
                            createdAt = System.currentTimeMillis(),
                            durationMs = window.size * 1000L / AudioRecorder.SAMPLE_RATE,
                        )
                        state.recordTranscription(clean, System.currentTimeMillis())
                    }
            }.onFailure { err ->
                Log.e(TAG, "Capture pipeline failed", err)
                state.recordError(err.message ?: "pipeline failed")
            }
        }
    }

    /**
     * Graceful shutdown: tell AudioRecorder to complete its flow so the chunker can flush its
     * partial tail and any in-flight Whisper transcription finishes and inserts. Only after
     * the pipeline completes do we release the wake lock and stop the foreground service.
     */
    private fun beginDrainAndStop() {
        if (drainJob?.isActive == true) return
        val job = pipelineJob
        pipelineJob = null
        tickerJob?.cancel(); tickerJob = null
        if (job == null) {
            releaseWake()
            state.stopSession()
            stopSelf()
            return
        }
        startForegroundWithType(contentText = "Finishing…")
        state.beginDraining()
        CaptureWidgetProvider.refresh(this)
        audioRecorder.requestStop()
        drainJob = lifecycleScope.launch {
            withContext(NonCancellable) {
                runCatching { job.join() }
                runCatching { transcriber.release() }
            }
            releaseWake()
            state.stopSession()
            CaptureWidgetProvider.refresh(this@CaptureService)
            stopSelf()
        }
    }

    override fun onDestroy() {
        // If destroyed without a clean Stop (e.g. swipe-away), tear everything down hard.
        if (drainJob == null) {
            pipelineJob?.cancel(); pipelineJob = null
            tickerJob?.cancel(); tickerJob = null
            releaseWake()
            state.stopSession()
            lifecycleScope.launch { runCatching { transcriber.release() } }
        }
        super.onDestroy()
    }

    private fun startForegroundWithType(contentText: String) {
        val notification = CaptureNotifications.build(this, contentText).build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                CaptureNotifications.NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE,
            )
        } else {
            startForeground(CaptureNotifications.NOTIFICATION_ID, notification)
        }
    }

    private fun acquireWake() {
        val pm = getSystemService<PowerManager>() ?: return
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "OpenBrain:capture").apply {
            setReferenceCounted(false)
            acquire(30 * 60 * 1000L)
        }
    }

    private fun releaseWake() {
        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = null
    }

    companion object {
        private const val TAG = "CaptureService"
        const val ACTION_STOP = "com.hubble.openbrain.action.STOP_CAPTURE"
        private const val RMS_SILENCE_THRESHOLD = 0.005f
        private const val NO_SPEECH_THRESHOLD = 0.6f

        private fun rms(window: FloatArray): Float {
            if (window.isEmpty()) return 0f
            var sumSq = 0.0
            for (s in window) sumSq += s * s
            return kotlin.math.sqrt(sumSq / window.size).toFloat()
        }

        private val IGNORED_TEXTS = setOf(
            "[silence]", "[BLANK_AUDIO]", "[ Silence ]", "[Pause]", "[pause]",
            "[MUSIC]", "[music]", "[Music]", "[noise]", "[NOISE]", ".", "...",
        )

        fun start(context: Context) {
            val intent = Intent(context, CaptureService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, CaptureService::class.java).setAction(ACTION_STOP),
            )
        }
    }
}
