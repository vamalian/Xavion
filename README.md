# XAVION AI — Android Tablet Personal Assistant

XAVION AI is a futuristic, JARVIS-inspired Android tablet personal AI companion built with **Kotlin**, **Jetpack Compose (Material 3)**, **Room**, **DataStore**, **Android Keystore AES-256-GCM**, **CameraX**, and the **Google Gemini Live Bidirectional Streaming API**.

---

## Key Capabilities

1. **Real-Time Gemini Live Voice (`GeminiLiveClient` + `XavionAudioEngine`)**
   - Full-duplex WebSocket streaming (`BidiGenerateContent`) over OkHttp.
   - 16kHz PCM16 microphone input with hardware `AcousticEchoCanceler` and `NoiseSuppressor` (when available).
   - 24kHz PCM16 native generated audio output via `AudioTrack`.
   - Energy-based Voice Activity Detection (VAD), barge-in interruption, automatic reconnection with exponential backoff, and live input/output transcript streaming.
   - Configurable Gemini Live & REST models, male/neutral voices (`Charon`, `Orus`, `Fenrir`, `Puck`, `Aoede`, `Kore`), English & Urdu bilingual support, verbosity, personality presets, and custom system instructions.

2. **Three Switchable Holographic Visual Modes (`XavionAvatarView`)**
   - **Male Virtual Avatar**: Futuristic male holographic portrait with animated cyan telemetry rings, scanlines, iris luminance, and audio-synchronized vocal modulation.
   - **Holographic Energy Core**: Multi-ring counter-rotating arc-reactor geometry pulsing with live audio RMS.
   - **Minimal Voice Assistant**: Clean acoustic orb and caption interface.

3. **Real Android Device Integration & Function Calling**
   - Registered Gemini tool declarations (`DeviceCommandRegistry`) validated and executed on-device (`DeviceCommandExecutor`):
     - Launch installed applications (`PackageManager`)
     - Open Android Settings (Wi-Fi, Bluetooth, Display, Sound, Battery, Apps, General)
     - Adjust Media / Ring / Alarm / Notification volume (`AudioManager`)
     - Adjust screen brightness (`Settings.System.canWrite` gated)
     - Toggle rear camera flashlight/torch (`CameraManager`)
     - Set Alarms & Countdown Timers (`AlarmClock`)
     - Create & persist Notes, Tasks, Reminders, and Memory entries (Room SQLite)
     - Read permitted notifications (`NotificationListenerService`)
     - Share text (`Intent.ACTION_SEND`) and open verified `https://` websites (`Intent.ACTION_VIEW`)
     - Read & compose Calendar events (`CalendarContract`)
   - Sensitive device actions prompt for explicit user confirmation before executing.

4. **Vision Mode (`CameraX` + Photo Picker)**
   - Live CameraX preview with Capture & Ask, Object Identification, OCR & Document Reading, zero-permission Android Photo Picker, authorized screenshot analysis, and optional Live Vision frame streaming during active Gemini Live sessions.

5. **Security, Keystore Vault & Background Controls**
   - Manually entered Gemini API keys are encrypted with **Android Keystore (`AES/GCM/NoPadding`)** and stored in DataStore—never hardcoded or logged.
   - Optional Owner PIN (salted SHA-256) and hardware-checked `BiometricPrompt` lock gate.
   - Optional `FLAG_SECURE` screenshot protection.
   - Explicit Foreground Service (`XavionAssistantService`), Floating Assistant Bubble (`SYSTEM_ALERT_WINDOW`), and honest foreground-only `"Hey XAVION"` wake-phrase listener.

---

## Setup & API Key Configuration

1. Open **Xavion AI** on your Android tablet (Android 10 / API 29+ recommended; `minSdk = 24`).
2. Navigate to **API Key** (`ApiConfigScreen`) in the left Navigation Rail.
3. Paste your Google AI Studio **Gemini API Key** and tap **Save Key**.
4. Tap **Test Link** to verify connectivity, latency, and available Gemini models.
5. Grant **Microphone** and **Camera** permissions in **Permissions** as needed.

> **Production Security Note:** Entering an API key on-device is intended for personal testing and prototyping. For production distribution, issue short-lived ephemeral tokens from a protected backend server rather than embedding permanent API keys in an APK.

---

## Building the APK

### Debug APK (Local Android Studio or CLI)
```bash
./gradlew :app:assembleDebug
```
Output APK: `app/build/outputs/apk/debug/app-debug.apk`

### Unit Tests
```bash
./gradlew :app:testDebugUnitTest
```

### Release Build Configuration
Configure signing environment variables before building a release bundle or APK:
```bash
export KEYSTORE_PATH=/path/to/your-release-key.jks
export STORE_PASSWORD=your_store_password
export KEY_PASSWORD=your_key_password
./gradlew :app:assembleRelease
```
Output APK: `app/build/outputs/apk/release/app-release.apk`

### GitHub Actions CI (Automatic Debug APK)
A ready-to-run workflow is included at `.github/workflows/build-apk.yml`. It triggers automatically on pushes to `main` or `master`, pull requests, and manual `workflow_dispatch` runs, builds `./gradlew clean assembleDebug --stacktrace --no-daemon`, and uploads **`XAVION-AI-DEBUG-APK`** (30-day retention).

#### Download & Install on an Android Tablet
1. Open your GitHub repository → **Actions** tab → select the latest **Build Android Debug APK** workflow run.
2. Under **Artifacts**, click **XAVION-AI-DEBUG-APK** to download the ZIP archive, then extract `app-debug.apk`.
3. Transfer `app-debug.apk` to your Android tablet (or download it directly in the tablet's browser from GitHub).
4. On the tablet, open the file from the **Files** app and allow **Install unknown apps** for your file manager/browser if prompted.
5. Tap **Install** and launch **Xavion AI**.
