# همآوا (HamAva) — دوبله زنده صدا در اندروید با هوش مصنوعی

<div align="center">

![HamAva Logo](app/src/main/res/mipmap-xxxhdpi/ic_launcher.png)

[![Release](https://img.shields.io/github/v/release/mostafaafrouzi/HamAva?style=for-the-badge&color=6750A4)](https://github.com/mostafaafrouzi/HamAva/releases/latest)
[![License](https://img.shields.io/badge/License-Apache%202.0-00D4AA?style=for-the-badge)](LICENSE)
[![Android](https://img.shields.io/badge/Android-10%2B%20(API%2029%2B)-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)

[English Documentation](README.en.md) | [تغییرات نسخه‌ها](.github/release-notes.md) | [دانلود مستقیم](https://github.com/mostafaafrouzi/HamAva/releases/latest)

</div>

---

## 📖 درباره برنامه

**همآوا (HamAva)** یک اپلیکیشن اندروید رایگان، متن‌باز و بسیار سریع است که با استفاده از پروتکل بلادرنگ **Google Gemini Live API (BidiGenerateContent)** صدای میکروفون یا صدای در حال پخش سیستم (ویدیوها، پادکست‌ها، لایوها، بازی‌ها) را به‌صورت همزمان دریافت کرده، پردازش نموده و با صدای طبیعی به زبان مقصد (از جمله فارسی روان) دوبله و پخش می‌کند.

این برنامه با الهام از ایده نرم‌افزارهای تجاری مانند Livdub اما با هدف حذف هزینه‌های اشتراک، ارائه سورس‌کد کامل، پشتیبانی بی‌نقص از زبان فارسی، امنیت حداکثری و معماری مدرن بر پایه استانداردهای رسمی گوگل طراحی و پیاده‌سازی شده است.

---

## ✨ ویژگی‌های کلیدی

- 🎙️ **دو منبع ورودی صدا:**
  - **میکروفون (Microphone):** دوبله زنده مکالمات، سخنرانی‌ها و کلاس‌های درسی.
  - **صدای سیستم (System Audio):** ضبط مستقیم صدای برنامه‌ها، ویدیوها و فیلم‌ها بدون نویز محیطی از طریق `AudioPlaybackCapture` (اندروید 10 به بالا).
- 🪟 **حباب کنترل شناور (Floating Bubble Overlay):** مدیریت، تنظیم صدا و توقف دوبله هنگام تماشای ویدیو در یوتیوب، تیک‌تاک، اینستاگرام و هر برنامه دیگر بدون خروج از برنامه، با انیمیشن روان جذب به لبه (Snap-to-Edge) و پنل شیشه‌ای مدرن.
- 🎭 **شخصی‌سازی لحن دوبله (Dubbing Tone):** انتخاب ادبیات و لحن گفتار در ۳ حالت کاربردی: محاوره‌ای و عامیانه (فیلم و شبکه‌های اجتماعی)، رسمی و آکادمیک (دوره‌ها و اخبار)، فنی و تخصصی (حفظ اصطلاحات مهندسی).
- 💬 **زیرنویس زنده همگام (Live Subtitles):** استخراج و نمایش لحظه‌ای متن ترجمه‌شده به موازات پخش صدای دوبله در صفحه اصلی و پنل شناور.
- 🔄 **چرخش کلیدهای پشتیبان API (Failover):** تعریف چندین کلید اختصاصی و سوئیچ خودکار بدون توقف در صورت مواجهه با خطای سهمیه (429 Too Many Requests).
- 🛡️ **پروکسی ضدتحریم درون‌برنامه‌ای (SOCKS5 & HTTP Proxy):** اتصال مستقیم وب‌سوکت جمینای به پروکسی‌های محلی (مانند v2ray یا Clash) جهت پایداری ۱۰۰ درصدی در ایران.
- 🎙️ **حذف اکوی سخت‌افزاری (Hardware AEC) و استخر بافر:** جلوگیری از فیدبک و ضبط مجدد صدای دوبله توسط میکروفون و استخر بافر بازیافتی با صفر تخصیص حافظه اضافه برای حفظ تأخیر زیر ۶۰ میلی‌ثانیه.
- ⚡ **تأخیر فوق‌العاده پایین (Ultra-low Latency):** انتقال استریم PCM 16-bit در چانک‌های 160 میلی‌ثانیه‌ای بر بستر اتصال پایدار و دائمی WebSocket.
- 🌍 **پشتیبانی از بیش از ۷۰ زبان:** ترجمه و دوبله به زبان‌های فارسی، انگلیسی، عربی، ترکی، آلمانی، فرانسوی، روسی، چینی، ژاپنی و ده‌ها زبان دیگر.
- 🗣️ **انتخاب صداهای طبیعی هوش مصنوعی:** انتخاب از میان ۶ صدای پیش‌ساخته باکیفیت گوگل شامل Aoede, Charon, Fenrir, Kore, Puck, Zephyr.
- 🎨 **رابط کاربری چشم‌نواز و مدرن:**
  - طراحی‌شده بر مبنای آخرین اصول **Material Design 3**.
  - تم پیش‌فرض تاریک (Dark Theme) با پالت رنگی حرفه‌ای.
  - پشتیبانی کامل و اصولی از چیدمان راست‌به‌چپ (RTL).
  - استفاده از فونت اصیل و زیبای **ایران‌سنس ایکس (IranSansX)**.
  - انیمیشن‌های بصری جذاب و نمایشگر زنده امواج صوتی (Audio Waveform).
- 🔒 **امنیت و حریم خصوصی مطلق:**
  - ذخیره‌سازی کلید API اختصاصی شما با الگوریتم **AES-256 GCM** در Android Keystore.
  - برقراری ارتباط مستقیم با سرورهای گوگل (بدون سرور میانی، بدون رهگیری و تبلیغات).
- 🎛️ **کاشی‌های تنظیمات سریع (Quick Settings Tiles):** شروع و توقف سریع دوبله از طریق نوار اعلان‌های اندروید برای میکروفون و صدای سیستم.
- ⚙️ **سرویس پایدار پس‌زمینه (Foreground Service):** حفظ پایداری فرآیند دوبله حتی در زمان خروج از برنامه و قفل بودن صفحه با WakeLock بهینه.

---

## 📥 دانلود و نصب

| منبع دانلود | لینک | وضعیت |
| :--- | :--- | :--- |
| **گیت‌هاب (GitHub Releases)** | [دانلود فایل APK / AAB](https://github.com/mostafaafrouzi/HamAva/releases/latest) | ✅ نسخه ۱.۲.۰ آماده |
| **کافه‌بازار (Cafe Bazaar)** | [صفحه همآوا در بازار](https://cafebazaar.ir/developer/057657612999?utm_source=github&utm_medium=readme_fa&utm_campaign=hamava) | ⏳ در حال بررسی |
| **گوگل‌پلی (Google Play)** | صفحه دانلود پلی‌استور | ⏳ به‌زودی |

---

## 🛠️ پیش‌نیازها

1. گوشی یا تبلت دارای **اندروید 10 (API 29)** یا بالاتر.
2. کلید اختصاصی **Google Gemini API Key** (رایگان از [Google AI Studio](https://aistudio.google.com/apikey)).
3. اتصال اینترنت پایدار و بدون تحریم (به دلیل محدودیت‌های منطقه‌ای گوگل ممکن است نیاز به ابزارهای عبور از تحریم باشد).

---

## 🏗️ معماری نرم‌افزار

پروژه همآوا بر اساس اصول **Clean Architecture** و الگوی **MVVM** پیاده‌سازی شده است:

```mermaid
graph TD
    UI[Compose UI: HomeScreen / SettingsScreen] --> VM[ViewModels]
    VM --> UC[UseCases: StartDubbing, StopDubbing, ValidateApiKey]
    UC --> REPO[SettingsRepository]
    UC --> SVC[DubForegroundService]
    SVC --> PIPE[AudioPipeline]
    PIPE --> CAP[AudioCapture / SystemAudioCapture (16kHz PCM)]
    PIPE --> WS[GeminiLiveSession (OkHttp WSS)]
    WS --> PLY[AudioPlayer (AudioTrack 24kHz PCM)]
```

### ساختار پوشه‌بندی:
- `com.afrouzi.hamava.core`: لایه ضبط، پردازش و پخش صدا (`AudioCapture`, `SystemAudioCapture`, `AudioPlayer`, `AudioPipeline`) و اتصال وب‌سوکت جمینای (`GeminiLiveSession`).
- `com.afrouzi.hamava.data`: مدل‌های داده و مخازن تنظیمات رمزنگاری‌شده (`SettingsRepository`).
- `com.afrouzi.hamava.domain`: موارد استفاده (Use Cases) و قراردادها.
- `com.afrouzi.hamava.service`: سرویس پس‌زمینه و کاشی‌های تنظیمات سریع (`DubForegroundService`, `MicTileService`, `SystemAudioTileService`).
- `com.afrouzi.hamava.ui`: کامپوننت‌های Compose، صفحه‌ها، تم، رنگ‌ها، و تایپوگرافی ایران‌سنس ایکس.

---

## 🚀 راهنمای بیلد و توسعه (Build & Run)

```bash
# کلون کردن ریپازیتوری
git clone https://github.com/mostafaafrouzi/HamAva.git
cd HamAva

# اجرای تست‌های واحد
./gradlew testDebugUnitTest

# بیلد نسخه دیباگ و نصب روی شبیه‌ساز/گوشی
./gradlew installDebug

# بیلد نسخه نهایی ریلیز (Signed Release)
./gradlew assembleRelease bundleRelease
```

---

## 👨‍💻 نویسنده و توسعه‌دهنده

**مصطفی افروزی (Mostafa Afrouzi)**

- 🌐 وبسایت رسمی: [afrouzi.ir](https://afrouzi.ir/?utm_source=github&utm_medium=readme_fa&utm_campaign=hamava)
- 🐙 گیت‌هاب: [github.com/mostafaafrouzi](https://github.com/mostafaafrouzi)
- 💼 لینکدین: [linkedin.com/in/mostafaafrouzi](https://linkedin.com/in/mostafaafrouzi)
- 🛍️ پنل کافه‌بازار: [Mostafa Afrouzi on Cafe Bazaar](https://cafebazaar.ir/developer/057657612999?utm_source=github&utm_medium=readme_fa&utm_campaign=hamava)

---

## 📄 مجوز انتشار (License)

این پروژه تحت مجوز بین‌المللی **Apache License 2.0** منتشر شده است. استفاده، مطالعه، ایجاد انشعاب و توسعه آن مطابق شروط این مجوز آزاد است. متن کامل مجوز در فایل [LICENSE](LICENSE) قرار دارد.
