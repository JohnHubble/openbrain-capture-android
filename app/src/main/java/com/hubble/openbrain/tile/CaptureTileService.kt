package com.hubble.openbrain.tile

import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.hubble.openbrain.R
import com.hubble.openbrain.service.CapturePhase
import com.hubble.openbrain.service.CaptureService
import com.hubble.openbrain.service.CaptureState
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
 * Quick Settings tile for toggling capture from anywhere. Tile state mirrors
 * [CaptureStateHolder.state.phase]: ACTIVE while Recording, UNAVAILABLE while
 * Transcribing/Saving/Preview, INACTIVE otherwise.
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
        when (val p = state.state.value.phase) {
            is CapturePhase.Recording -> CaptureService.stop(this)
            CapturePhase.Transcribing, CapturePhase.Saving, is CapturePhase.Preview -> Unit
            else -> {
                @Suppress("UNUSED_VARIABLE") val unused = p
                CaptureService.start(this)
            }
        }
    }

    private fun render(s: CaptureState) {
        val tile = qsTile ?: return
        tile.state = when (s.phase) {
            is CapturePhase.Recording -> Tile.STATE_ACTIVE
            CapturePhase.Transcribing, CapturePhase.Saving, is CapturePhase.Preview -> Tile.STATE_UNAVAILABLE
            else -> Tile.STATE_INACTIVE
        }
        tile.label = when (s.phase) {
            is CapturePhase.Recording -> "Capturing"
            CapturePhase.Transcribing -> "Transcribing…"
            CapturePhase.Saving -> "Saving…"
            is CapturePhase.Preview -> "Awaiting save"
            else -> "Open Brain"
        }
        tile.icon = Icon.createWithResource(this, R.drawable.ic_tile_mic)
        tile.updateTile()
    }
}
