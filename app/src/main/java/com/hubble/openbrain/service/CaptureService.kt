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
import com.hubble.openbrain.audio.SessionAudioBuffer
import com.hubble.openbrain.data.prefs.SettingsStore
import com.hubble.openbrain.data.repo.ThoughtRepository
import com.hubble.openbrain.transcribe.WhisperModelRepository
import com.hubble.openbrain.transcribe.WhisperTranscriber
import com.hubble.openbrain.widget.CaptureWidgetProvider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * One foreground service drives one capture session: tap-to-start records continuously
 * into [SessionAudioBuffer], tap-to-stop transcribes the full buffer in one Whisper pass,
 * then either previews the transcript (if the setting is on) or saves it as a single
 * [com.hubble.openbrain.data.db.Thought] PENDING row.
 *
 * No transcription happens during recording — that is the whole point of the rewrite.
 */
@AndroidEntryPoint
class CaptureService : LifecycleService() {

    @Inject lateinit var audioRecorder: AudioRecorder
    @Inject lateinit var transcriber: WhisperTranscriber
    @Inject lateinit var modelRepo: WhisperModelRepository
    @Inject lateinit var thoughts: ThoughtRepository
    @Inject lateinit var state: CaptureStateHolder
    @Inject lateinit var settings: SettingsStore

    private var wakeLock: PowerManager.WakeLock? = null
    private var collectorJob: Job? = null
    private var tickerJob: Job? = null
    private var finalizeJob: Job? = null

    private var sessionBuffer: SessionAudioBuffer? = null
    private var sessionStartedAtMs: Long = 0L

    override fun onCreate() {
        super.onCreate()
        CaptureNotifications.ensureChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_STOP -> {
                Log.i(TAG, "ACTION_STOP received")
                beginFinalize()
                return START_NOT_STICKY
            }
            ACTION_CONFIRM_SAVE -> {
                Log.i(TAG, "ACTION_CONFIRM_SAVE received")
                confirmSave()
                return START_NOT_STICKY
            }
            ACTION_DISCARD -> {
                Log.i(TAG, "ACTION_DISCARD received")
                discardPreview()
                return START_NOT_STICKY
            }
            else -> startCapture()
        }
        return START_STICKY
    }

    private fun startCapture() {
        if (collectorJob?.isActive == true) return
        sessionStartedAtMs = System.currentTimeMillis()
        val buffer = SessionAudioBuffer()
        sessionBuffer = buffer

        startForegroundWithType(contentText = CaptureNotifications.phaseText(CapturePhase.Recording(0L, false)))
        state.startRecording()
        CaptureWidgetProvider.refresh(this)
        acquireWake()

        tickerJob = lifecycleScope.launch {
            while (true) {
                delay(1000)
                val elapsed = System.currentTimeMillis() - sessionStartedAtMs
                val nearLimit = elapsed >= SessionAudioBuffer.WARN_DURATION_MS
                state.tickRecording(elapsed, nearLimit)
                updateNotification(CapturePhase.Recording(elapsed, nearLimit))
                if (elapsed >= SessionAudioBuffer.MAX_DURATION_MS) {
                    Log.i(TAG, "Auto-stop at ${elapsed}ms (10-min cap)")
                    beginFinalize()
                    return@launch
                }
            }
        }

        collectorJob = lifecycleScope.launch(Dispatchers.IO) {
            runCatching {
                val model = settings.whisperModel.first()
                val modelFile = modelRepo.modelFile(model)
                check(modelFile.exists()) { "Model file missing: ${modelFile.name}" }
                Log.i(TAG, "Pre-loading whisper model ${model.displayName}")
                transcriber.ensureLoaded(modelFile)

                audioRecorder.stream().collect { frame ->
                    buffer.append(frame)
                    if (!buffer.hasCapacity()) {
                        audioRecorder.requestStop()
                    }
                }
            }.onFailure { err ->
                Log.e(TAG, "Capture pipeline failed", err)
                state.recordError(err.message ?: "pipeline failed")
            }
        }
    }

    /**
     * Stop the recorder, finalize the buffer, run one Whisper pass, then either show preview
     * (if setting enabled) or insert a PENDING [com.hubble.openbrain.data.db.Thought] and exit.
     */
    private fun beginFinalize() {
        if (finalizeJob?.isActive == true) return
        val buffer = sessionBuffer
        val collector = collectorJob
        tickerJob?.cancel(); tickerJob = null
        if (buffer == null) {
            // Nothing to do (e.g., stop received before start completed).
            shutdown()
            return
        }

        state.beginTranscribing()
        updateNotification(CapturePhase.Transcribing)
        CaptureWidgetProvider.refresh(this)
        audioRecorder.requestStop()

        finalizeJob = lifecycleScope.launch {
            withContext(NonCancellable) {
                runCatching { collector?.join() }
                if (buffer.isEmpty()) {
                    Log.i(TAG, "Empty session buffer, nothing to transcribe")
                    state.recordError("Empty recording")
                    cleanupSession()
                    shutdown()
                    return@withContext
                }
                val durationMs = buffer.durationMs()
                val pcm = buffer.finalize()
                sessionBuffer = null

                val transcript = withContext(Dispatchers.IO) {
                    runCatching { transcriber.transcribe(pcm) }
                }.getOrElse { err ->
                    Log.e(TAG, "Transcription failed", err)
                    state.recordError(err.message ?: "transcription failed")
                    runCatching { transcriber.release() }
                    shutdown()
                    return@withContext
                }
                val cleaned = transcript.text.trim()
                if (cleaned.isBlank()) {
                    Log.i(TAG, "Empty transcript")
                    state.recordError("Empty transcript")
                    runCatching { transcriber.release() }
                    shutdown()
                    return@withContext
                }

                val previewEnabled = runCatching { settings.previewBeforeSave.first() }.getOrDefault(false)
                if (previewEnabled) {
                    state.showPreview(cleaned, durationMs)
                    updateNotification(CapturePhase.Preview(cleaned, durationMs))
                    CaptureWidgetProvider.refresh(this@CaptureService)
                    // Stay foreground; service awaits ACTION_CONFIRM_SAVE / ACTION_DISCARD.
                } else {
                    saveAndShutdown(cleaned, durationMs)
                }
            }
        }
    }

    private fun saveAndShutdown(transcript: String, durationMs: Long) {
        state.beginSaving()
        updateNotification(CapturePhase.Saving)
        lifecycleScope.launch {
            withContext(NonCancellable) {
                val savedAt = System.currentTimeMillis()
                runCatching {
                    thoughts.insertPending(
                        text = transcript,
                        createdAt = savedAt,
                        durationMs = durationMs,
                    )
                }.onFailure { err ->
                    Log.e(TAG, "Insert failed", err)
                    state.recordError(err.message ?: "insert failed")
                    runCatching { transcriber.release() }
                    shutdown()
                    return@withContext
                }
                state.recordSaved(transcript, savedAt, durationMs)
                runCatching { transcriber.release() }
                shutdown()
            }
        }
    }

    private fun confirmSave() {
        val phase = state.state.value.phase
        if (phase !is CapturePhase.Preview) {
            Log.w(TAG, "ACTION_CONFIRM_SAVE ignored, phase=$phase")
            return
        }
        saveAndShutdown(phase.transcript, phase.durationMs)
    }

    private fun discardPreview() {
        val phase = state.state.value.phase
        if (phase !is CapturePhase.Preview) {
            Log.w(TAG, "ACTION_DISCARD ignored, phase=$phase")
            return
        }
        Log.i(TAG, "Preview discarded")
        cleanupSession()
        lifecycleScope.launch {
            runCatching { transcriber.release() }
            shutdown()
        }
    }

    private fun cleanupSession() {
        sessionBuffer?.discard()
        sessionBuffer = null
    }

    private fun shutdown() {
        collectorJob = null
        finalizeJob = null
        tickerJob?.cancel(); tickerJob = null
        releaseWake()
        // If the phase is Saved/Error, leave it for the UI to read; otherwise go Idle.
        val phase = state.state.value.phase
        if (phase !is CapturePhase.Saved && phase !is CapturePhase.Error) {
            state.goIdle()
        }
        CaptureWidgetProvider.refresh(this)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        if (finalizeJob == null) {
            collectorJob?.cancel(); collectorJob = null
            tickerJob?.cancel(); tickerJob = null
            cleanupSession()
            releaseWake()
            state.goIdle()
            CaptureWidgetProvider.refresh(this)
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

    private fun updateNotification(phase: CapturePhase) {
        val nm = getSystemService<android.app.NotificationManager>() ?: return
        val notification = CaptureNotifications.build(this, CaptureNotifications.phaseText(phase)).build()
        nm.notify(CaptureNotifications.NOTIFICATION_ID, notification)
    }

    private fun acquireWake() {
        val pm = getSystemService<PowerManager>() ?: return
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "OpenBrain:capture").apply {
            setReferenceCounted(false)
            acquire(15 * 60 * 1000L)
        }
    }

    private fun releaseWake() {
        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = null
    }

    companion object {
        private const val TAG = "CaptureService"
        const val ACTION_STOP = "com.hubble.openbrain.action.STOP_CAPTURE"
        const val ACTION_CONFIRM_SAVE = "com.hubble.openbrain.action.CONFIRM_SAVE"
        const val ACTION_DISCARD = "com.hubble.openbrain.action.DISCARD_PREVIEW"

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

        fun confirmSave(context: Context) {
            context.startService(
                Intent(context, CaptureService::class.java).setAction(ACTION_CONFIRM_SAVE),
            )
        }

        fun discard(context: Context) {
            context.startService(
                Intent(context, CaptureService::class.java).setAction(ACTION_DISCARD),
            )
        }
    }
}
