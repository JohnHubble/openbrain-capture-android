package com.hubble.openbrain.tile

import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.hubble.openbrain.R
import com.hubble.openbrain.service.CaptureService
import com.hubble.openbrain.service.CaptureStateHolder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Quick Settings tile for toggling capture from anywhere. Expanded-notification
 * tile state mirrors [CaptureStateHolder]: ACTIVE while listening, INACTIVE otherwise.
 */
@AndroidEntryPoint
class CaptureTileService : TileService() {

    @Inject lateinit var state: CaptureStateHolder

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var observeJob: Job? = null

    override fun onStartListening() {
        super.onStartListening()
        render(state.state.value)
        observeJob = scope.launch {
            state.state.onEach(::render).collect { }
        }
    }

    override fun onStopListening() {
        observeJob?.cancel(); observeJob = null
        super.onStopListening()
    }

    override fun onClick() {
        val s = state.state.value
        if (s.isCapturing || s.isProcessing) CaptureService.stop(this)
        else CaptureService.start(this)
    }

    private fun render(s: com.hubble.openbrain.service.CaptureState) {
        val tile = qsTile ?: return
        tile.state = when {
            s.isCapturing -> Tile.STATE_ACTIVE
            s.isProcessing -> Tile.STATE_UNAVAILABLE
            else -> Tile.STATE_INACTIVE
        }
        tile.label = when {
            s.isCapturing -> "Capturing"
            s.isProcessing -> "Finishing…"
            else -> "Open Brain"
        }
        tile.icon = Icon.createWithResource(this, R.drawable.ic_tile_mic)
        tile.updateTile()
    }
}
