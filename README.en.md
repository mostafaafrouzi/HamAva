# HamAva — Live Audio Dubbing for Android with Google Gemini Live API

<div align="center">

![HamAva Logo](app/src/main/res/mipmap-xxxhdpi/ic_launcher.png)

[![Release](https://img.shields.io/github/v/release/mostafaafrouzi/HamAva?style=for-the-badge&color=6750A4)](https://github.com/mostafaafrouzi/HamAva/releases/latest)
[![License](https://img.shields.io/badge/License-Apache%202.0-00D4AA?style=for-the-badge)](LICENSE)
[![Android](https://img.shields.io/badge/Android-10%2B%20(API%2029%2B)-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)

[مستندات فارسی](README.md) | [Release Notes](.github/release-notes.md) | [Direct Download](https://github.com/mostafaafrouzi/HamAva/releases/latest)

</div>

---

## 📖 About The Project

**HamAva (همآوا)** is an ultra-fast, free, and open-source native Android application that performs real-time audio dubbing and voice-over using the bidirectional streaming protocol of **Google Gemini Live API (BidiGenerateContent)**.

Whether capturing live microphone speech or internal device playback (videos, podcasts, games, live streams), HamAva streams raw PCM audio over low-latency WebSockets, translates it into the desired target language preserving tone and natural pacing, and synthesizes expressive speech back to your speakers in real time.

Built to overcome the limitations of proprietary tools like Livdub, HamAva requires **no subscriptions**, features **zero telemetry**, provides **100% open source transparency**, and delivers full native support for Persian (Farsi) and 70+ global languages.

---

## ✨ Features

- 🎙️ **Dual Audio Input Modes:**
  - **Microphone Mode:** Live dubbing for conversations, lectures, meetings, and classes.
  - **Internal System Audio Mode:** High-fidelity internal audio capture via Android 10+ `AudioPlaybackCapture` without ambient room noise.
- 🪟 **iOS-Inspired Floating Overlay:** Floating bubble widget with snap-to-edge animation, dedicated in-place **Pause** and **Resume** buttons (no need to re-prompt for screen capture permissions), full exit control, and background video volume control over apps like YouTube and TikTok.
- 🔊 **Original Video / Background Volume Control:** Intelligent audio ducking and volume slider (0% to 100%) allows lowering, muting, or adjusting the original foreign language audio so that the Persian dubbing is heard crystal clear.
- ⚡ **Ultra-Low Latency Mode:** 100ms chunk processing, 3200-byte zero-allocation buffer pool, minimal AudioTrack buffer, and incremental streaming prompt optimization for the lowest possible real-time dubbing delay.
- 🎭 **Tailored Dubbing Tones:** Choose between **Colloquial** (conversational everyday speech for movies & social media), **Formal** (refined academic speech for lectures and news), and **Technical** (preserves specialized engineering terminology).
- 💬 **Real-Time Synchronized Subtitles:** Simultaneous live text subtitle stream rendered alongside the synthesized audio in the main UI and overlay.
- 🔄 **Multi-API Key Rotation & Failover:** Store backup API keys; automatically recovers and switches keys upon HTTP 429 quota exhaustion without interrupting playback.
- 🛡️ **Anti-Sanction Local Proxy (SOCKS5 / HTTP):** Direct WebSocket routing via local proxies (e.g., v2ray, Clash) for reliable connections under regional network restrictions.
- 🎙️ **Hardware Acoustic Echo Cancellation (AEC):** Hardware AEC and Noise Suppressor integration prevents speaker feedback loop into the microphone; zero-allocation BufferPool keeps latency sub-60ms.
- 🌍 **Over 70 Supported Languages:** Full bidirectional dubbing between Persian (fa-IR), English (en-US), Arabic, Turkish, German, French, Russian, Chinese, Japanese, and more.
- 🗣️ **6 Natural Google AI Voices:** Select between Aoede, Charon, Fenrir, Kore, Puck, and Zephyr.
- 🎨 **Sleek iOS-Inspired UI/UX Design:**
  - **Dynamic Island Status Capsule** at the top with live pulsing activity dots and latency badges.
  - **iOS Segmented Control** for switching between Microphone and System Audio.
  - Inset grouped cards with frosted glassmorphism borders.
  - Native RTL and LTR support with **IranSansX** typography.
  - Real-time animated audio waveforms and dual-action playback controls.
- 🔒 **End-to-End Privacy & Keystore Security:**
  - API keys stored securely using **AES-256 GCM** encryption via Android Keystore.
  - Zero intermediate servers. All traffic is directly exchanged with Google Generative Language APIs.
- 🎛️ **Quick Settings Tiles:** Quick toggle tiles in Android notification shade for both microphone and system dubbing.
- ⚙️ **Persistent Background Service:** Foreground Service with notification action controls (Pause, Resume, Exit) for seamless background operation.

---

## 📥 Download

| Platform | Link | Status |
| :--- | :--- | :--- |
| **GitHub Releases** | [Download APK / AAB](https://github.com/mostafaafrouzi/HamAva/releases/latest) | ✅ v1.3.0 Available |
| **Cafe Bazaar** | [HamAva on Cafe Bazaar](https://cafebazaar.ir/developer/057657612999?utm_source=github&utm_medium=readme_en&utm_campaign=hamava) | ⏳ Reviewing |
| **Google Play** | Google Play Store link | ⏳ Coming Soon |

---

## 🛠️ Prerequisites

- Android device running **Android 10 (API 29)** or newer.
- A free Google Gemini API key obtained from [Google AI Studio](https://aistudio.google.com/apikey).

---

## 🏗️ Technical Architecture

HamAva strictly follows **Clean Architecture** and **MVVM (Model-View-ViewModel)** design principles:

```
app/src/main/java/com/afrouzi/hamava/
├── core/
│   ├── audio/          # AudioCapture, SystemAudioCapture, AudioPlayer, AudioPipeline
│   ├── gemini/         # GeminiLiveSession, WebSocket protocols, AudioChunkProcessor
│   └── utils/          # AudioUtils, PermissionUtils
├── data/
│   ├── model/          # DubSettings, DubLanguage, DubState
│   └── prefs/          # EncryptedSharedPreferences SettingsRepository
├── domain/
│   ├── repository/     # SettingsRepositoryInterface
│   └── usecase/        # StartDubbingUseCase, StopDubbingUseCase, ValidateApiKeyUseCase
├── service/            # DubForegroundService, Quick Settings Tiles
└── ui/
    ├── components/     # AudioWaveform, DubButton, LanguageSelector, VoiceSelector
    ├── screens/        # HomeScreen, SettingsScreen, AboutScreen
    └── theme/          # Material3 Theme, Color, Type (IranSansX)
```

---

## 🚀 Building from Source

```bash
# Clone the repository
git clone https://github.com/mostafaafrouzi/HamAva.git
cd HamAva

# Execute unit tests
./gradlew testDebugUnitTest

# Assemble and install debug APK
./gradlew installDebug

# Build signed release APK and AAB bundle
./gradlew assembleRelease bundleRelease
```

---

## 👨‍💻 Author & Maintainer

**Mostafa Afrouzi**

- 🌐 Official Website: [afrouzi.ir/en](https://afrouzi.ir/en/?utm_source=github&utm_medium=readme_en&utm_campaign=hamava)
- 🐙 GitHub: [github.com/mostafaafrouzi](https://github.com/mostafaafrouzi)
- 💼 LinkedIn: [linkedin.com/in/mostafaafrouzi](https://linkedin.com/in/mostafaafrouzi)
- 🛍️ Cafe Bazaar: [Mostafa Afrouzi Developer Profile](https://cafebazaar.ir/developer/057657612999?utm_source=github&utm_medium=readme_en&utm_campaign=hamava)

---

## 📄 License

HamAva is licensed under the **Apache License, Version 2.0**. See the [LICENSE](LICENSE) file for terms and conditions.
