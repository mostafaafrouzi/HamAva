## فارسی

**1.0.0** - انتشار نخست همآوا (HamAva)

**تغییرات جدید:**
- 🎙️ **دوبله زنده دوطرفه:** برقراری ارتباط بلادرنگ صوتی با استفاده از پروتکل `BidiGenerateContent` در Google Gemini Live API.
- 🔊 **دو حالت ورودی صدا:** پشتیبانی از میکروفون برای مکالمات و پشتیبانی از ضبط داخلی صدای سیستم (Android 10+) بدون تداخل نویز محیط.
- 🌍 **پشتیبانی گسترده از زبان‌ها:** ترجمه و گویندگی همزمان برای بیش از ۷۰ زبان زنده، با بهینه‌سازی ویژه برای مکالمات و ترجمه به زبان فارسی.
- 🗣️ **۶ صدای طبیعی هوش مصنوعی:** امکان انتخاب از بین صداهای Aoede, Charon, Fenrir, Kore, Puck, Zephyr.
- 🎨 **رابط کاربری پیشرفته:** پیاده‌سازی با Jetpack Compose و Material Design 3، پشتیبانی روان از حالت تاریک، پشتیبانی کامل RTL و فونت ایران‌سنس ایکس (IranSansX).
- 🔐 **امنیت و حریم خصوصی:** رمزنگاری کلیدهای API با الگوریتم AES-256 در سخت‌افزار امنیتی دستگاه (Android Keystore). بدون هیچ‌گونه لاگ یا ارسال اطلاعات به سرورهای واسط.
- 🎛️ **کاشی‌های تنظیمات سریع (Quick Settings Tiles):** فعال‌سازی و توقف سریع دوبله مستقیماً از نوار نوتیفیکیشن‌ها.
- ⚡ **سرویس پس‌زمینه پایدار:** پردازش بدون وقفه صدا در هنگام خاموش بودن نمایشگر یا کار با سایر نرم‌افزارها.

**حریم خصوصی:** بدون اینترنت به سرور ما. هیچ داده‌ای به هیچ سرور میانی ارسال نمی‌شود و تمام ارتباطات به‌صورت WSS رمزنگاری‌شده مستقیماً به سرورهای هوش مصنوعی گوگل متصل می‌گردند.

---

## English

**1.0.0** - Initial Release of HamAva

**New Features:**
- 🎙️ **Real-Time Live Dubbing:** Bidirectional low-latency audio streaming via Google Gemini Live API (`BidiGenerateContent`).
- 🔊 **Dual Audio Sources:** Full support for both microphone input and internal device playback audio capture (Android 10+).
- 🌍 **70+ Global Languages:** Real-time translation and voice synthesis supporting over 70 languages with native Persian (Farsi) localization.
- 🗣️ **6 Natural AI Voices:** Choose among high-fidelity voices: Aoede, Charon, Fenrir, Kore, Puck, and Zephyr.
- 🎨 **Modern Compose UI:** Material Design 3 design system, sleek dark mode, full RTL/LTR responsiveness, and IranSansX typography.
- 🔐 **Keystore AES-256 Security:** Cryptographic protection for Gemini API keys backed by Android Keystore.
- 🎛️ **Quick Settings Tiles:** Convenient system tiles to start/stop microphone and internal audio dubbing.
- ⚡ **Background Dubbing Service:** Continuous audio pipeline execution with Android Foreground Service.

**Privacy:** No intermediate servers or analytics. Your API key and raw audio stream directly to Google's official endpoints.

---

## Downloads

- **APK:** `hamava-v1.0.0.apk` (Direct installation, sideloading, and Cafe Bazaar)
- **AAB:** `hamava-v1.0.0.aab` (Google Play Store bundle)

Package: `com.afrouzi.hamava` | Version: `1.0.0` (Code `1`) | Min SDK: `29` | Target SDK: `35`

Full documentation: [فارسی](README.md) | [English](README.en.md)  
Developed with ❤️ by [Mostafa Afrouzi](https://afrouzi.ir/?utm_source=github&utm_medium=release_notes&utm_campaign=hamava)
