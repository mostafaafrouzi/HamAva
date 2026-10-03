## فارسی

**1.1.0** - نسخه جدید با بهینه‌سازی کامل دوبله زنده همزمان و کاهش صدای فیلم

**تغییرات و قابلیت‌های جدید:**
- 🚀 **موتور جدید ترجمه زنده گوگل (Gemini 3.5 Live Translate):** مهاجرت به مدل رسمی و اختصاصی `gemini-3.5-live-translate-preview` مخصوص ترجمه همزمان بلادرنگ گفتار به گفتار با تأخیر فوق‌سریع بدون مکث تفکر.
- 🔉 **کاهش هوشمند صدای ویدیوی اصلی (Automatic Audio Ducking):** در زمان پخش صدای دوبله فارسی، صدای فیلم یا کلیپ در حال پخش (در کروم، یوتیوب، تیک‌تاک و ...) با قابلیت `Audio Ducking` ملایم و کم شده و صدای دوبله با بلندی و وضوح کامل پخش می‌شود.
- 🔄 **تطبیق و بازنمونه‌برداری نرخ صدای سیستم (Hardware Audio Resampling):** تشخیص خودکار نرخ نمونه‌برداری صوتی سخت‌افزار (48kHz/44.1kHz استریو) و تبدیل دقیق به مونو 16kHz جهت پردازش توسط جمینای لایو.
- ⚡ **پایداری کامل پروتکل ارتباطی (حذف خطای ۱۰۰۷):** حذف بسته‌های ناقص دستی و استفاده مستقیم از سیستم تشخیص گفتار و سکوت (Server-side VAD) گوگل، جلوگیری از هرگونه قطع اتصال سوکت.
- 🇮🇷 **بومی‌سازی ۱۰۰٪ فارسی و طراحی RTL کامل:** ترجمه روان و فارسی‌سازی تمامی واژگان، منوها، تنظیمات و نشانگرهای سرعت و وضعیت.
- 🎨 **بهبود صف پخش صدا (Async Playback Queue):** حل مشکل پرش یا تاخیر صدا با استفاده از بافر صوتی ناهمگام در `AudioPlayer`.

---

## English

**1.1.0** - Major Live Dubbing & Audio Ducking Optimization

**Key Improvements:**
- 🚀 **Dedicated Live Translation Model:** Switched to Google's official `gemini-3.5-live-translate-preview` model optimized specifically for zero-latency speech-to-speech simultaneous dubbing.
- 🔉 **Intelligent Audio Ducking:** Automatic volume attenuation for underlying media apps (YouTube, Chrome, TikTok, etc.) while playing clear, prioritized Persian dub speech.
- 🔄 **Hardware Audio Resampling:** Native 48kHz/44.1kHz stereo audio capture downsampling to high-fidelity 16kHz mono.
- ⚡ **Rock-Solid WebSocket Protocol:** Fixed 1007 protocol disconnects by integrating server-side Voice Activity Detection (VAD).
- 🇮🇷 **Comprehensive Persian Localization:** Full RTL layout and 100% Persian UI terminology and instructions.
- 🎨 **Asynchronous Audio Player Queue:** Smooth, stutter-free audio streaming without audio glitches.

---

## Downloads

- **APK:** `hamava-v1.1.0.apk` (Direct installation, sideloading, and Cafe Bazaar)
- **AAB:** `hamava-v1.1.0.aab` (Google Play Store bundle)

Package: `com.afrouzi.hamava` | Version: `1.1.0` (Code `2`) | Min SDK: `29` | Target SDK: `35`

Full documentation: [فارسی](README.md) | [English](README.en.md)  
Developed with ❤️ by [Mostafa Afrouzi](https://afrouzi.ir/?utm_source=github&utm_medium=release_notes&utm_campaign=hamava)
