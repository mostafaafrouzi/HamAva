## فارسی

**1.3.0** - قابلیت توقف موقت و ادامه (Pause/Resume) بدون نیاز به مجوز مجدد، کنترل صدای ویدیوی اصلی و پس‌زمینه، حالت فوق سریع (Ultra-Low Latency) و بازطراحی ظاهر برنامه به سبک مدرن iOS

**قابلیت‌های جدید و بهبودهای کلیدی:**
- ⏸️ **حالت توقف موقت (Pause) و ادامه (Resume) درون دکمه شناور و نوتیفیکیشن:**
  - اکنون در دکمه شناور روی برنامه‌ها (یوتیوب، تیک‌تاک، فیلم‌ها و...) می‌توانید هر زمان خواستید دوبله را متوقف (Pause) و بلافاصله ادامه دهید، بدون اینکه سشن بسته شود یا نیاز باشد به برنامه برگردید و دوباره مجوز تصویربرداری کل صفحه (Screen Capture) صادر کنید.
  - تفکیک کامل میان توقف موقت (Pause) و خروج نهایی (Exit).
- 🔊 **اصلاح اساسی و معکوس‌سازی تنظیم صدا: کنترل صدای ویدیوی اصلی (پس‌زمینه):**
  - صدای دوبله فارسی همواره با حداکثر توان و هماهنگ با کلیدهای ولوم گوشی پخش می‌شود.
  - اسلایدر تنظیم صدا (از ۰٪ تا ۱۰۰٪) برای کنترل بلندی صدای ویدیوی پس‌زمینه (زبان اصلی) طراحی شده است؛ کاربر می‌تواند صدای انگلیسی/اصلی را کم، ملایم یا کاملاً قطع کند تا دوبله فارسی با نهایت شفافیت شنیده شود.
- ⚡ **حالت فوق سریع و کاهش حداکثری تأخیر (Ultra-Low Latency Mode):**
  - استفاده از چانک‌های ۱۰۰ میلی‌ثانیه‌ای (۳۲۰۰ بایت در ۱۶ کیلوهرتز).
  - بهینه‌سازی بافر سخت‌افزاری AudioTrack برای حداقل بافرینگ خروجی.
  - بهینه‌سازی پرامپت استریم Gemini Live برای ترجمه آنی عبارات بدون انتظار برای جملات طولانی.
- 🎨 **بازطراحی UI/UX جذاب به سبک مدرن iOS:**
  - هدر جزیره پویا (Dynamic Island Capsule) در بالای صفحه با انیمیشن زنده وضعیت و نشانگر تأخیر.
  - کنترل سگمنتد سبک اپل (iOS Segmented Control) برای انتخاب منبع صدا (میکروفون / صدای سیستم).
  - کارت‌های گلس‌مورفیسم سبک iOS Inset Grouped با کادرهای ظریف و گوشه‌های گرد ۲۲dp.
  - دکمه‌های کنترل دوگانه (توقف موقت + پایان دوبله) در صفحه اصلی و حباب شناور.

---

## English

**1.3.0** - In-Place Pause & Resume without Screen Capture Re-prompts, Background Video Volume Control, Ultra-Low Latency Mode & iOS-Inspired UI/UX Redesign

**Key Features & Enhancements:**
- ⏸️ **In-Place Pause & Resume in Floating Overlay & Notifications:**
  - Pause and resume live dubbing seamlessly over third-party apps (YouTube, TikTok, Netflix, etc.) without terminating the MediaProjection session. No more returning to the app or re-prompting for screen capture permissions!
  - Clear separation between temporary Pause and permanent Exit.
- 🔊 **Inverted Volume Control: Background / Original Video Audio Level:**
  - Persian dubbing audio plays at full native media volume.
  - The volume slider (0% to 100%) now directly modulates the original background foreign video audio (via intelligent AudioFocus ducking and muting), giving the user full control over background sound levels.
- ⚡ **Ultra-Low Latency Streaming Mode:**
  - 100ms PCM chunk transmission (3200-byte buffers @ 16kHz).
  - Minimized AudioTrack hardware buffer for ultra-responsive playback.
  - Incremental streaming prompt engineering for instantaneous clause-by-clause translation.
- 🎨 **Sleek iOS-Inspired UI/UX Redesign:**
  - Dynamic Island status capsule with real-time breathing activity indicators and latency counters.
  - iOS Segmented Control for switching between Microphone and System Audio modes.
  - Inset grouped cards with frosted glassmorphic borders and rounded squircles.
  - Dual playback controls (Pause/Resume + End Session) in both the main screen and floating overlay.

---

## Downloads

- **APK:** `hamava-v1.3.0.apk` (Direct installation, sideloading, and Cafe Bazaar)
- **AAB:** `hamava-v1.3.0.aab` (Google Play Store bundle)

Package: `com.afrouzi.hamava` | Version: `1.3.0` (Code `5`) | Min SDK: `29` | Target SDK: `35`

Full documentation: [فارسی](README.md) | [English](README.en.md)  
