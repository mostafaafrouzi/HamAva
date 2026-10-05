## فارسی

**1.2.0** - اضافه شدن حباب کنترل شناور، انتخاب لحن دوبله، زیرنویس زنده، چرخش خودکار کلیدهای API، پروکسی ضدتحریم و حذف اکوی سخت‌افزاری

**قابلیت‌های جدید و بهبودهای چشمگیر:**
- 🪟 **حباب کنترل شناور روی همه برنامه‌ها (Floating Overlay Bubble):**
  - امکان کنترل کامل دوبله هنگام تماشای ویدیو در یوتیوب، تیک‌تاک، اینستاگرام، تلگرام یا هر اپلیکیشن دیگر بدون نیاز به سوئیچ میان برنامه‌ها.
  - جابه‌جایی آزادانه و لمسی با انیمیشن روان جذب به لبه‌های صفحه (Snap-to-Edge).
  - پنل کنترل شیشه‌ای مدرن (Glassmorphism) مجهز به نمایش تأخیر لحظه‌ای (Latency Badge)، متن زیرنویس زنده، اسلایدر تنظیم صدای دوبله و دکمه‌های دسترسی سریع.
- 🎭 **انتخاب لحن ترجمه و دوبله (Dubbing Tone):**
  - **محاوره‌ای و عامیانه (Colloquial):** لحن بسیار روان، صمیمی و طبیعی مخصوص تماشای فیلم، تیک‌تاک و یوتیوب.
  - **رسمی و آکادمیک (Formal):** ادبیات دقیق و رسمی مناسب سخنرانی‌ها، اخبار و دوره‌های آموزشی دانشگاهی.
  - **فنی و تخصصی (Technical):** حفظ کلمات کلیدی و تخصصی مهندسی و تکنولوژی همراه با ترجمه استاندارد.
- 💬 **زیرنویس زنده همگام (Real-time Live Subtitles):**
  - استخراج خودکار و بلادرنگ متن ترجمه همگام با صوت هوش مصنوعی و نمایش در کادر اختصاصی صفحه اصلی و پنل شناور.
- 🔄 **چرخش خودکار کلیدهای پشتیبان API (Multi-Key Rotation):**
  - پشتیبانی از چندین کلید API پشتیبان در بخش تنظیمات؛ در صورت مواجهه با محدودیت سهمیه رایگان گوگل (429 Too Many Requests)، سشن بلافاصله و بدون وقفه به کلید بعدی سوئیچ می‌کند.
- 🛡️ **پروکسی ضدتحریم اختصاصی (SOCKS5 & HTTP Proxy):**
  - امکان هدایت مستقیم ترافیک وب‌سوکت جمینای از طریق پروکسی محلی (v2ray، Clash و ...) برای پایداری ۱۰۰ درصدی اتصال بدون نیاز به VPN سرتاسری گوشی.
- 🎙️ **حذف اکو و نویز سخت‌افزاری (Hardware AEC & Noise Suppression):**
  - فعال‌سازی سخت‌افزاری AcousticEchoCanceler و NoiseSuppressor روی پردازش صوت برای جلوگیری از بازخورد صدای بلندگو به میکروفون.
- ⚡ **استخر بافر بازیافتی و کاهش مصرف رم (Zero-Allocation BufferPool):**
  - بهینه‌سازی جریان صوتی PCM با تخصیص صفر شیء جدید در حافظه (Buffer Pool)، ارتقای سرعت پاسخ‌دهی و پایداری بلندمدت بدون افت فریم.

---

## English

**1.2.0** - Floating Overlay Bubble, Dubbing Tones, Live Subtitles, Multi-API Key Failover, Anti-Sanction Proxy & Hardware AEC

**Major Features & Improvements:**
- 🪟 **Floating Control Bubble Overlay:**
  - Control dubbing seamlessly while watching videos on YouTube, TikTok, Instagram, or any other app.
  - Smooth drag-and-drop with spring snap-to-edge animation.
  - Modern glassmorphic panel with live latency counter, real-time subtitle stream, dub volume slider, and one-tap controls.
- 🎭 **Dubbing Tone Modes:**
  - **Colloquial:** Natural, expressive, and conversational everyday spoken Persian for social media & movies.
  - **Formal / Academic:** Precise and refined language tailored for courses, news, and speeches.
  - **Technical:** Preserves technical terminology and engineering keywords with exact context.
- 💬 **Synchronized Real-Time Subtitles:**
  - Real-time text extraction displayed synchronously with audio in both the home screen and floating panel.
- 🔄 **Multi-API Key Rotation & Instant Failover:**
  - Store backup API keys; automatically recovers and switches keys upon HTTP 429 quota exhaustion without interrupting the user.
- 🛡️ **Anti-Sanction Local Proxy (SOCKS5 / HTTP):**
  - Native OkHttpClient proxy support for bypassing regional restrictions via local clients (e.g., v2ray, Clash).
- 🎙️ **Hardware AEC & Noise Suppression:**
  - Integrated Android hardware `AcousticEchoCanceler` and `NoiseSuppressor` on the audio capture session.
- ⚡ **Zero-Allocation Audio Buffer Pool:**
  - High-performance reusable audio buffer pool dramatically reducing GC overhead and maintaining sub-60ms processing latency.

---

## Downloads

- **APK:** `hamava-v1.2.0.apk` (Direct installation, sideloading, and Cafe Bazaar)
- **AAB:** `hamava-v1.2.0.aab` (Google Play Store bundle)

Package: `com.afrouzi.hamava` | Version: `1.2.0` (Code `4`) | Min SDK: `29` | Target SDK: `35`

Full documentation: [فارسی](README.md) | [English](README.en.md)  
Developed with ❤️ by [Mostafa Afrouzi](https://afrouzi.ir/?utm_source=github&utm_medium=release_notes&utm_campaign=hamava)
