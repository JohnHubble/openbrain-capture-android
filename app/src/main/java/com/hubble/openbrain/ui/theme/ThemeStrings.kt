package com.hubble.openbrain.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Per-theme capture-lifecycle copy. Each theme overrides every lifecycle surface with its own
 * vocabulary. Material Default keeps neutral English.
 *
 * Strings were picked from the design-review picker at
 * apps/openbrain-capture-prototype/theme-copy-picker.html and are frozen here.
 * Layout targets: sakura-v2.html, v7-comrade.html, v8-codex.html.
 */
data class LifecycleText(val main: String, val sub: String)

data class ButtonTriplet(val start: String, val stop: String, val processing: String)

/** Bottom-nav label for one tab. [ornament] is a small theme-specific flourish
 *  (kanji for Sakura, empty for most themes). */
data class NavLabel(val label: String, val ornament: String = "")

data class ThemeStrings(
    val appTitle: String,
    val pageSubtitle: String,
    val idle: LifecycleText,
    val listening: LifecycleText,
    val processing: LifecycleText,
    val error: LifecycleText,
    val button: ButtonTriplet,
    val latestTitle: String,
    val captureTab: NavLabel,
    val historyTab: NavLabel,
    val settingsTab: NavLabel,
)

val LocalOpenBrainStrings = staticCompositionLocalOf<ThemeStrings> {
    error("ThemeStrings not provided")
}

val LocalOpenBrainThemeId = staticCompositionLocalOf<ThemeId> {
    error("ThemeId not provided")
}

fun resolveStrings(themeId: ThemeId): ThemeStrings = when (themeId) {
    ThemeId.MaterialDefault -> materialDefaultStrings()
    ThemeId.SakuraMinimal -> sakuraStrings()
    ThemeId.ComradeNotes -> comradeStrings()
    ThemeId.IlluminatedCodex -> codexStrings()
}

private fun materialDefaultStrings() = ThemeStrings(
    appTitle = "OB Notes",
    pageSubtitle = "Tap to start listening",
    idle = LifecycleText(main = "Ready to capture", sub = "VAD: off · Whisper base"),
    listening = LifecycleText(main = "Listening…", sub = "VAD: off · Whisper base"),
    processing = LifecycleText(main = "Processing…", sub = "Finishing the last window with Whisper"),
    error = LifecycleText(main = "Something went wrong", sub = "Tap to retry"),
    button = ButtonTriplet(start = "Start", stop = "Stop", processing = "Processing…"),
    latestTitle = "Latest thought",
    captureTab = NavLabel("Capture"),
    historyTab = NavLabel("History"),
    settingsTab = NavLabel("Settings"),
)

private fun sakuraStrings() = ThemeStrings(
    appTitle = "garden notes 庭",
    pageSubtitle = "for thoughts carried on the wind",
    idle = LifecycleText(
        main = "The moon is full, the page is blank",
        sub = "whisper base · listening in stillness",
    ),
    listening = LifecycleText(
        main = "The bamboo bends and catches",
        sub = "each breath held, shaped, remembered",
    ),
    processing = LifecycleText(
        main = "Pressing petals between pages",
        sub = "whisper base · pressing petals",
    ),
    error = LifecycleText(
        main = "The wind has stilled",
        sub = "retry when the wind returns",
    ),
    button = ButtonTriplet(start = "Begin", stop = "Rest", processing = "Reminiscing…"),
    latestTitle = "from the garden",
    captureTab = NavLabel(label = "listen", ornament = "聴"),
    historyTab = NavLabel(label = "memory", ornament = "記"),
    settingsTab = NavLabel(label = "form", ornament = "形"),
)

private fun comradeStrings() = ThemeStrings(
    appTitle = "COMRADE NOTES",
    pageSubtitle = "the People's recording apparatus",
    idle = LifecycleText(
        main = "Awaiting your dispatch, Comrade",
        sub = "AWAITING TRANSMISSION FROM COMRADE",
    ),
    listening = LifecycleText(
        main = "The Ministry is listening",
        sub = "THE VOICE OF THE PEOPLE IS RECORDED",
    ),
    processing = LifecycleText(
        main = "Filing your report to the archives",
        sub = "TRANSCRIBING FOR THE COLLECTIVE ARCHIVE",
    ),
    error = LifecycleText(
        main = "The stream is broken, Comrade",
        sub = "SIGNAL LOST · REPORT TO HQ",
    ),
    button = ButtonTriplet(start = "Transmit", stop = "Cease", processing = "Filing…"),
    latestTitle = "Last dispatch",
    captureTab = NavLabel("DISPATCH"),
    historyTab = NavLabel("ARCHIVE"),
    settingsTab = NavLabel("APPARATUS"),
)

private fun codexStrings() = ThemeStrings(
    appTitle = "Codex Vocis",
    pageSubtitle = "a scriptorium for fleeting thoughts",
    idle = LifecycleText(
        main = "Ink is drawn. The quill waits.",
        sub = "Scriptorium ieiunum · Whisper Base",
    ),
    listening = LifecycleText(
        main = "The monks are listening, faithfully",
        sub = "Verba tua super membranam",
    ),
    processing = LifecycleText(
        main = "Scribendo · transcribing the spoken gospel",
        sub = "Monachi scribunt · Whisper Base",
    ),
    error = LifecycleText(
        main = "The candle has guttered",
        sub = "Candela extincta · the flame is gone",
    ),
    button = ButtonTriplet(start = "Incipit", stop = "Explicit", processing = "Scribendo…"),
    latestTitle = "from the codex",
    captureTab = NavLabel("Scriptorium"),
    historyTab = NavLabel("Codex"),
    settingsTab = NavLabel("Regula"),
)
