# Open Brain Capture (Android)

A voice-capture Android client for [Open Brain (OB1)](https://github.com/NateBJones-Projects/OB1) — your personal persistent memory layer. Press a button (in-app, Quick Settings tile, or home-screen widget), speak a thought, and the app:

1. Records audio in a foreground service so it keeps running with the screen off.
2. Transcribes locally on-device using [whisper.cpp](https://github.com/ggerganov/whisper.cpp) — nothing leaves your phone until you choose to sync.
3. Pushes the transcribed thought to your OB1 backend over JSON-RPC / SSE.

Capture is **session-bounded**: one tap-to-start → tap-to-stop produces a single transcript, with a 10-minute hard cap (a banner warns you at 8 min). Everything user-specific (backend URL, access key, Whisper model size, preview-before-save toggle, theme) is configured in the in-app **Settings** screen — there is nothing to edit in source.

## Screenshots

The capture screen, shown across the four built-in themes (switch from Settings):

| Default | Garden | Codex | Comrade |
|:---:|:---:|:---:|:---:|
| ![Default](screenshots/theme-default.jpg) | ![Garden](screenshots/theme-garden.jpg) | ![Codex](screenshots/theme-codex.jpg) | ![Comrade](screenshots/theme-comrade.jpg) |

## Prerequisites

- **A working OB1 backend** reachable from your phone over HTTPS. See the [OB1 Getting Started guide](https://github.com/NateBJones-Projects/OB1/blob/main/docs/01-getting-started.md).
- **OB1 endpoint URL** and **access key** for that backend.
- **Android 8.0+ device** (`arm64-v8a`).
- **A few hundred MB free** for the on-device Whisper model (size depends on which model you pick: Tiny ≈75 MB, Base ≈140 MB, Small ≈465 MB).

## Install the APK

1. Download the latest `app-release.apk` from the [Releases page](../../releases).
2. On your Android device, open **Settings → Security → Install unknown apps** (wording varies by Android version) and allow the browser or file-manager app you'll open the APK from.
3. Tap the downloaded APK and confirm the install.
4. Launch Open Brain. On first run you'll see a setup screen — pick a Whisper model and let it download.
5. Open **Settings**, paste your OB1 endpoint URL and access key, and tap **Test connection** to confirm the round-trip works.

Updates: future signed releases install over the existing one as long as they're signed with the same key. If you ever see "package conflicts with existing package", uninstall first (this wipes local thought history).

## Expected outcome

When everything is wired up correctly:

- **Setup screen** appears on first launch and downloads the Whisper model you pick (with a SHA-256 verification step). After completion you land on the **Capture** tab.
- **Capture** tab shows a big record button. Tap it → status flips to **Recording** with a live duration counter. Speak. Tap again → **Transcribing** → either **Saved** (default) or **Preview** (if "Preview before save" is enabled in Settings).
- **History** tab shows one row per session — full transcript, never split into 30-second fragments.
- **Settings → Test connection** sends a probe thought to your OB1 backend and shows the response.
- A **Quick Settings tile** and a **home-screen widget** are available that toggle capture without opening the app.

## Permissions

| Permission | Why |
|---|---|
| `RECORD_AUDIO` | Capture your voice — the whole point of the app. |
| `POST_NOTIFICATIONS` | Show the persistent capture-in-progress notification (required for a foreground service). |
| `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_MICROPHONE` | Keep recording when the screen is off or another app is foregrounded. |
| `INTERNET` + `ACCESS_NETWORK_STATE` | Sync transcribed thoughts to your OB1 backend. |
| `WAKE_LOCK` | Prevent the CPU from sleeping mid-capture and dropping audio. |

The app does not request location, contacts, storage, or any ad/analytics permissions.

## Backend

This app is a **client**. You need an OB1 backend reachable over HTTPS that speaks JSON-RPC over SSE. Endpoint and access key are entered in the in-app Settings screen. The access key is stored in `EncryptedSharedPreferences` (Android Keystore-backed AES-GCM) and sent in an `Authorization` header — never in URL query strings. Release builds reject non-HTTPS endpoints.

## Build from source

**Requirements**

- Android Studio (Hedgehog or newer)
- JDK 17
- Android SDK 34
- Android NDK `30.0.14904198`
- minSdk 26 (Android 8.0+)
- Target device or emulator: `arm64-v8a` only

**Debug build** (no keystore needed)

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

**Run unit tests**

```bash
./gradlew test
```

**Release build** (signed APK for distribution)

1. Generate a release keystore once and keep it somewhere safe (1Password, encrypted backup — losing it means future builds can't update existing installs):

   ```bash
   keytool -genkey -v -keystore openbrain-release.keystore \
     -alias openbrain -keyalg RSA -keysize 2048 -validity 10000
   ```

2. Copy `keystore.properties.example` to `keystore.properties` and fill in your passwords. Both files (`*.keystore` and `keystore.properties`) are gitignored.

3. Build:

   ```bash
   ./gradlew assembleRelease
   ls app/build/outputs/apk/release/app-release.apk
   ```

4. Verify the signature is your release key, not the Android debug key:

   ```bash
   keytool -printcert -jarfile app/build/outputs/apk/release/app-release.apk
   ```

## Troubleshooting

**"Package conflicts with existing package" on install** — A previous build was signed with a different key. Uninstall the existing app first (`adb uninstall com.hubble.openbrain` or long-press the app icon). Note this wipes your local thought history.

**Setup screen says "Download failed" or stays at 0 bytes** — Check the device has internet, the Whisper model URL on Hugging Face is reachable (`huggingface.co/ggerganov/whisper.cpp`), and that you have enough free storage. The download is resumable: tap **Retry**.

**Test connection in Settings returns an error** — Verify the endpoint is reachable from the phone's network (try opening it in the phone's browser), the access key is correct, and that release builds are pointed at an `https://` URL (release builds reject `http://`).

**Recording stops immediately or no audio is captured** — Confirm `RECORD_AUDIO` permission was granted. Open **Settings → Apps → Open Brain → Permissions** and toggle Microphone on.

**Transcribed text is wrong language / gibberish** — Whisper auto-detects but is biased toward English in the smaller models. Try a larger model (Settings → Whisper model → Small or Medium) for better multilingual accuracy.

**App stops recording when screen is off** — That shouldn't happen — capture runs as a foreground service. If it does, check that battery optimization isn't restricting the app: **Settings → Battery → Battery usage → Open Brain → Unrestricted**.

## License

MIT — see [LICENSE](LICENSE).

Bundled [whisper.cpp](https://github.com/ggerganov/whisper.cpp) is also MIT-licensed.
