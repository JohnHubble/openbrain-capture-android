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
    val transcribing: LifecycleText,
    val preview: LifecycleText,
    val saving: LifecycleText,
    val saved: LifecycleText,
    val nearLimit: LifecycleText,
    val error: LifecycleText,
    val button: ButtonTriplet,
    val savePreview: String,
    val discardPreview: String,
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
    idle = LifecycleText(main = "Ready to capture", sub = "Whisper base · tap to record"),
    listening = LifecycleText(main = "Recording…", sub = "Tap stop when you're done"),
    transcribing = LifecycleText(main = "Transcribing…", sub = "One pass over the full session"),
    preview = LifecycleText(main = "Review your thought", sub = "Save or discard before it's stored"),
    saving = LifecycleText(main = "Saving…", sub = "Queueing for Open Brain"),
    saved = LifecycleText(main = "Saved", sub = "Synced to Open Brain"),
    nearLimit = LifecycleText(main = "Approaching the limit", sub = "Sessions auto-stop at 10 minutes"),
    error = LifecycleText(main = "Something went wrong", sub = "Tap to retry"),
    button = ButtonTriplet(start = "Start", stop = "Stop", processing = "Working…"),
    savePreview = "Save",
    discardPreview = "Discard",
    latestTitle = "Last session",
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
    transcribing = LifecycleText(
        main = "Pressing petals between pages",
        sub = "whisper base · pressing petals",
    ),
    preview = LifecycleText(
        main = "Hold the petal before pressing",
        sub = "save or let the wind carry it",
    ),
    saving = LifecycleText(
        main = "Sealing the page with wax",
        sub = "the petal is bound to the book",
    ),
    saved = LifecycleText(
        main = "The page is closed",
        sub = "kept for a quieter day",
    ),
    nearLimit = LifecycleText(
        main = "The lantern's oil runs low",
        sub = "the garden will rest at ten minutes",
    ),
    error = LifecycleText(
        main = "The wind has stilled",
        sub = "retry when the wind returns",
    ),
    button = ButtonTriplet(start = "Begin", stop = "Rest", processing = "Pressing…"),
    savePreview = "Keep",
    discardPreview = "Release",
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
    transcribing = LifecycleText(
        main = "Filing your report to the archives",
        sub = "TRANSCRIBING FOR THE COLLECTIVE ARCHIVE",
    ),
    preview = LifecycleText(
        main = "Review the dispatch, Comrade",
        sub = "FILE TO THE ARCHIVE OR REDACT",
    ),
    saving = LifecycleText(
        main = "Sealing the dispatch",
        sub = "STAMPED FOR THE COLLECTIVE",
    ),
    saved = LifecycleText(
        main = "Filed with the Ministry",
        sub = "DISPATCH RECEIVED AND ARCHIVED",
    ),
    nearLimit = LifecycleText(
        main = "Tape running short, Comrade",
        sub = "AUTO-CEASE AT TEN MINUTES",
    ),
    error = LifecycleText(
        main = "The stream is broken, Comrade",
        sub = "SIGNAL LOST · REPORT TO HQ",
    ),
    button = ButtonTriplet(start = "Transmit", stop = "Cease", processing = "Filing…"),
    savePreview = "FILE",
    discardPreview = "REDACT",
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
    transcribing = LifecycleText(
        main = "Scribendo · transcribing the spoken gospel",
        sub = "Monachi scribunt · Whisper Base",
    ),
    preview = LifecycleText(
        main = "Read the leaf before binding",
        sub = "Bind to the codex or strike the page",
    ),
    saving = LifecycleText(
        main = "Binding the leaf to the codex",
        sub = "Sigillum apponitur",
    ),
    saved = LifecycleText(
        main = "Sealed in the codex",
        sub = "Verbum servatum est",
    ),
    nearLimit = LifecycleText(
        main = "The candle is short",
        sub = "Codex clauditur post decem minuta",
    ),
    error = LifecycleText(
        main = "The candle has guttered",
        sub = "Candela extincta · the flame is gone",
    ),
    button = ButtonTriplet(start = "Incipit", stop = "Explicit", processing = "Scribendo…"),
    savePreview = "Liga",
    discardPreview = "Dele",
    latestTitle = "from the codex",
    captureTab = NavLabel("Scriptorium"),
    historyTab = NavLabel("Codex"),
    settingsTab = NavLabel("Regula"),
)
