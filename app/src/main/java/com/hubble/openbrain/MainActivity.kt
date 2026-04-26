package com.hubble.openbrain

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.hubble.openbrain.data.prefs.SettingsStore
import com.hubble.openbrain.transcribe.ModelState
import com.hubble.openbrain.transcribe.WhisperModelRepository
import com.hubble.openbrain.transcribe.WhisperTranscriber
import com.hubble.openbrain.ui.nav.OpenBrainNavHost
import com.hubble.openbrain.ui.setup.ModelSetupScreen
import com.hubble.openbrain.ui.theme.OpenBrainTheme
import com.hubble.openbrain.ui.theme.ThemeId
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var settingsStore: SettingsStore
    @Inject lateinit var modelRepository: WhisperModelRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        runCatching { Log.i("OpenBrain", "whisper systemInfo: ${WhisperTranscriber.systemInfo()}") }
            .onFailure { Log.e("OpenBrain", "whisper JNI load failed", it) }
        setContent {
            val themeId by settingsStore.themeId.collectAsState(initial = ThemeId.MaterialDefault)
            val modelState by modelRepository.state.collectAsState()
            OpenBrainTheme(themeId = themeId) {
                if (modelState is ModelState.Ready) {
                    OpenBrainNavHost()
                } else {
                    ModelSetupScreen()
                }
            }
        }
    }
}
