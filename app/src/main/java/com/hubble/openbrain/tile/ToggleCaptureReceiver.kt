package com.hubble.openbrain.tile

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.hubble.openbrain.service.CaptureService
import com.hubble.openbrain.service.CaptureStateHolder
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Broadcast entrypoint for widget taps. Toggles capture based on current in-memory state.
 * Kept intentionally small so the widget intent fires cleanly and returns control.
 */
@AndroidEntryPoint
class ToggleCaptureReceiver : BroadcastReceiver() {

    @Inject lateinit var state: CaptureStateHolder

    override fun onReceive(context: Context, intent: Intent) {
        val s = state.state.value
        if (s.isCapturing || s.isProcessing) CaptureService.stop(context)
        else CaptureService.start(context)
    }

    companion object {
        const val ACTION = "com.hubble.openbrain.action.TOGGLE_CAPTURE"
    }
}
