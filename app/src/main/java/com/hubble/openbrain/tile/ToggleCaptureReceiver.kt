package com.hubble.openbrain.tile

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.hubble.openbrain.service.CapturePhase
import com.hubble.openbrain.service.CaptureService
import com.hubble.openbrain.service.CaptureStateHolder
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Broadcast entrypoint for widget taps. Toggles capture based on current in-memory phase.
 * No-op while Transcribing/Saving/Preview so a stray tap doesn't kill an in-flight session.
 */
@AndroidEntryPoint
class ToggleCaptureReceiver : BroadcastReceiver() {

    @Inject lateinit var state: CaptureStateHolder

    override fun onReceive(context: Context, intent: Intent) {
        when (state.state.value.phase) {
            is CapturePhase.Recording -> CaptureService.stop(context)
            CapturePhase.Transcribing, CapturePhase.Saving, is CapturePhase.Preview -> Unit
            else -> CaptureService.start(context)
        }
    }

    companion object {
        const val ACTION = "com.hubble.openbrain.action.TOGGLE_CAPTURE"
    }
}
