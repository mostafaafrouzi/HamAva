# HamAva (هم‌آوا)

[فارسی](README.md) · **English**

A modern, free, and open-source Android application for **AI-powered real-time live audio dubbing** via Google Gemini Live API (BidiGenerateContent). Stream, translate, and voice-over foreign movies, videos, streams, and spoken conversations with natural human-like AI voices in real time.

<div dir="ltr" align="center">

`Version 1.0.0` · Android 10+ (API 29+) · Package: `com.afrouzi.hamava` · Apache 2.0 License

</div>

---

## App Interface Preview

<div align="center">

| Home Screen (Light) | Target Language Sheet | Interactive Onboarding Tour | Settings & API Keys | Home Screen (Dark) |
| :---: | :---: | :---: | :---: | :---: |
| <img src="docs/screenshots/01_home_screen_light.png" width="180" /> | <img src="docs/screenshots/02_bottom_sheet_languages.png" width="180" /> | <img src="docs/screenshots/03_onboarding_tour.png" width="180" /> | <img src="docs/screenshots/04_settings_screen.png" width="180" /> | <img src="docs/screenshots/06_home_screen_dark.png" width="180" /> |

</div>

---

## What It Does

### 1. Real-Time Internal System Audio Dubbing
Capture internal app audio (YouTube, Instagram, video players, browsers, podcasts) without environmental microphone leakage using Android 10+ `AudioPlaybackCapture`. Raw 16kHz PCM audio is streamed over bidirectional WebSockets directly to Google Gemini Live API and played back in real time. System Audio is configured as the first and default source for friction-free video watching.

### 2. Live Spoken Microphone Dubbing
Switch seamlessly to microphone input for face-to-face meetings, lectures, academic webinars, and voice conversations. Equipped with hardware Acoustic Echo Cancellation (AEC) and silence detection to eliminate audio loopback and minimize bandwidth and API quota consumption.

### 3. Background Video Volume Ducking
Avoid confusing dual-audio playback while watching foreign videos. HamAva provides an adjustable volume slider (0% to 100%) to duck or completely mute the original foreign language audio so that the translated speech is crystal clear.

### 4. Custom Languages, Voice Actors, and Tones
- **70+ Target Languages:** Bidirectional translation across Persian, English, Arabic, Turkish, German, French, Spanish, Russian, Chinese, Japanese, Korean, Hindi, and more.
- **Natural Voice Actors:** Choose from 6 expressive Google AI voices (Aoede, Charon, Fenrir, Kore, Puck, Zephyr).
- **Dubbing Tones:** Tailor the translation register:
  - *Colloquial:* Everyday conversational idioms for movies and social media.
  - *Formal:* Refined terminology for documentaries, news, and official speeches.
  - *Technical:* Preserves specialized engineering, scientific, and computing terminology.

### 5. Floating Overlay & Real-Time Subtitles
A lightweight floating widget that remains accessible on top of any video player. Features in-place **Pause** and **Resume** buttons, complete exit control, and volume sliders without leaving your active application. Synchronized live text subtitles are transcribed on-screen alongside the synthesized audio.

### 6. Fallback API Keys Rotation
Never get interrupted by HTTP 429 quota limits. Configure multiple backup Google Gemini API keys. HamAva automatically tests connections, monitors key health, and seamlessly fails over to backup keys if quota limits are encountered.

### 7. Anti-Sanction Local Proxy (SOCKS5 & HTTP)
Route WebSocket connections through local proxies (such as v2ray or Clash on port 10808) to circumvent geographic restrictions and maintain high reliability.

### 8. Adaptive Light, Dark & System Theme Modes
Built on Material 3, HamAva defaults to your system display preference. A quick toggle button in the top bar allows switching between dark and light themes in one tap.

---

## Dual Market Distribution (Cafe Bazaar & Myket)

In compliance with Iranian app store policies regarding competitor cross-links, HamAva utilizes Gradle Product Flavors to produce two distinct official builds:

| Flavor | Target Marketplace | Developer Link on About Screen | Package ID & Signing Key |
| :--- | :--- | :--- | :--- |
| **Bazaar Build** | Cafe Bazaar | Official Cafe Bazaar developer profile | Identical (`com.afrouzi.hamava`) |
| **Myket Build** | Myket | Official Myket developer store page | Identical (`com.afrouzi.hamava`) |

Both builds share 100% identical codebase, package identity, and cryptographic signatures, ensuring smooth updates across channels.

---

## Privacy & Security

**HamAva has zero intermediate servers and does NOT collect, store, or transmit your data.**

- **Direct Connections:** All audio streams travel directly between your device and Google's official Gemini Live API endpoints over TLS encryption.
- **Hardware Keystore Encryption:** API keys are encrypted with **AES-256 GCM** backed by the Android Keystore.
- **Zero Ads & Zero Trackers:** No telemetry, analytics SDKs, or third-party tracking libraries.
- **100% Open Source:** Complete code transparency auditability on GitHub.

---

## Downloads

- **GitHub Releases:** [Download Latest APK & AAB](https://github.com/mostafaafrouzi/HamAva/releases/latest)
- **Cafe Bazaar:** [HamAva on Cafe Bazaar](https://cafebazaar.ir/developer/057657612999?utm_source=github&utm_medium=readme_en&utm_campaign=hamava)
- **Myket:** [HamAva on Myket](https://myket.ir/developer/dev-102174?utm_source=github&utm_medium=readme_en&utm_campaign=hamava)

---

# For Developers

## Tech Stack

Kotlin · Jetpack Compose · Material 3 · Hilt · Coroutines & Flow · OkHttp (WebSocket) · AudioPlaybackCapture · AudioTrack

<div dir="ltr">

| Parameter | Value |
|---|---|
| Package Name | `com.afrouzi.hamava` |
| minSdk / targetSdk / compileSdk | 29 / 35 / 35 |
| AGP / Kotlin | 8.7.3 / 2.0.21 |
| versionName / versionCode | 1.0.0 / 1 |

</div>

## Architecture Overview

```
app/src/main/java/com/afrouzi/hamava/
├── core/
│   ├── audio/          # System audio capture, microphone, AudioTrack player, and audio pipeline
│   ├── gemini/         # Gemini Live streaming WebSocket client & chunk processing
│   └── utils/          # Audio processing helpers and permission utilities
├── data/
│   ├── model/          # DubSettings, languages, voices, and state models
│   └── prefs/          # EncryptedSharedPreferences SettingsRepository
├── domain/
│   ├── repository/     # Settings repository interfaces
│   └── usecase/        # Start/stop dubbing and API key validation use cases
├── service/            # Foreground service & Android Quick Settings Tiles
└── ui/
    ├── components/     # Compose widgets (waveform, dub button, bottom sheets, tour dialog)
    ├── screens/        # Home, Settings, and About screens
    └── theme/          # Material 3 design system, color palettes, and typography
```

## Local Development & Build

```bash
# Clone the repository
git clone https://github.com/mostafaafrouzi/HamAva.git
cd HamAva

# Build Bazaar debug variant
./gradlew assembleBazaarDebug

# Build Myket debug variant
./gradlew assembleMyketDebug
```

To create signed release binaries locally, configure `keystore.properties` in the project root:

```properties
storeFile=keystore/hamava-release.jks
storePassword=...
keyAlias=...
keyPassword=...
geminiApiKey=...
```

Then generate signed APKs and AABs:

```bash
# Assemble and bundle Bazaar release
./gradlew assembleBazaarRelease bundleBazaarRelease

# Assemble and bundle Myket release
./gradlew assembleMyketRelease bundleMyketRelease
```

## Automated GitHub Actions Release

The `.github/workflows/release.yml` workflow triggers upon pushing any version tag (e.g., `v1.0.0`):
1. Runs automated unit tests.
2. Compiles both `bazaar` and `myket` release variants into 4 distinct signed binaries (2 APKs and 2 AABs).
3. Verifies signatures with `apksigner` and `jarsigner`.
4. Publishes artifacts directly to GitHub Releases with notes sourced from `.github/release-notes.md`.

---

## Author & Maintainer

**Mostafa Afrouzi**

- Website: [afrouzi.ir](https://afrouzi.ir/?utm_source=github&utm_medium=readme_en&utm_campaign=hamava)
- GitHub: [github.com/mostafaafrouzi](https://github.com/mostafaafrouzi)
- LinkedIn: [linkedin.com/in/mostafaafrouzi](https://linkedin.com/in/mostafaafrouzi)
- Cafe Bazaar: [cafebazaar.ir/developer/057657612999](https://cafebazaar.ir/developer/057657612999)
- Myket: [myket.ir/developer/dev-102174](https://myket.ir/developer/dev-102174)

---

## License

Licensed under the [Apache License, Version 2.0](LICENSE).
