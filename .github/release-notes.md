## فارسی

**1.1.1** - حل مشکل زبان خروجی دوبله و تضمین ترجمه و گویندگی به زبان فارسی

**تغییرات و رفع باگ:**
- 🎯 **حل ریشه‌ای مشکل زبان دوبله (پیکربندی TranslationConfig):** با اعمال مستقیم شیء `translationConfig` حاوی کد زبان مقصد (`targetLanguageCode: "fa"`) درون `generationConfig` در پروتکل زنده جمینای (Multimodal Live API)، مشکل دوبله شدن ناخواسته به زبان انگلیسی به طور کامل حل شد و صداها دقیقاً به زبان انتخابی کاربر (فارسی) ترجمه و گویندگی می‌شوند.
- 🌐 **سازگاری با کلیه مدل‌های زنده گوگل:** تنظیمات زبان مقصد بر روی مدل‌های `gemini-3.5-live-translate-preview`، `gemini-3.8-live` و `gemini-3.1-flash-live-preview` با موفقیت تست و تأیید شد.

---

## English

**1.1.1** - Fix Target Language Configuration & Ensure Persian Output

**Changes & Bug Fixes:**
- 🎯 **Native Target Language Config (translationConfig):** Added `translationConfig.targetLanguageCode` directly inside `generationConfig` in the Multimodal Live WebSocket setup frame. This resolves the issue where the live translation model defaulted to English despite prompt instructions, ensuring speech is translated and spoken in the user's selected language (Persian / "fa").
- 🌐 **Cross-Model Live API Compatibility:** Validated and confirmed compatibility across `gemini-3.5-live-translate-preview`, `gemini-3.8-live`, and `gemini-3.1-flash-live-preview`.

---

## Downloads

- **APK:** `hamava-v1.1.1.apk` (Direct installation, sideloading, and Cafe Bazaar)
- **AAB:** `hamava-v1.1.1.aab` (Google Play Store bundle)

Package: `com.afrouzi.hamava` | Version: `1.1.1` (Code `3`) | Min SDK: `29` | Target SDK: `35`

Full documentation: [فارسی](README.md) | [English](README.en.md)  
Developed with ❤️ by [Mostafa Afrouzi](https://afrouzi.ir/?utm_source=github&utm_medium=release_notes&utm_campaign=hamava)
