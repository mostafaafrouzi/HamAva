# راهنمای جامع و پرامپت مرجع توسعه و انتشار پروژه‌های اندروید
# Mostafa Afrouzi Android App Development & Store Publishing Framework

این سند دربرگیرنده تمامی قوانین فنی، هویت برند، تفکیک مارکت‌ها، ساختار ریدمی، استانداردهای تایپوگرافی، اسکرین‌شات و امنیت است که در تمام پروژه‌های اندرویدی رعایت می‌شود.

---

## ۱. هویت برند و اطلاعات توسعه‌دهنده (About Screen & Links)

- **نام توسعه‌دهنده:** Mostafa Afrouzi
- **وب‌سایت شخصی همراه با پارامترهای رهگیری UTM:**
  `https://afrouzi.ir/?utm_source={app_name}_app&utm_medium=about_screen&utm_campaign={app_name}`
  *(به جای `{app_name}` نام پکیج یا شناسه برنامه مانند `hamava`، `mycard` و... قرار می‌گیرد)*
- **پروفایل لینکدین:** `https://linkedin.com/in/mostafaafrouzi`
- **مخزن گیت‌هاب:** `https://github.com/mostafaafrouzi/{RepoName}`
- **صفحه توسعه‌دهنده در کافه‌بازار:** `https://cafebazaar.ir/developer/057657612999`
- **صفحه توسعه‌دهنده در مایکت:** `https://myket.ir/developer/dev-102174`

---

## ۲. معماری تفکیک مارکت‌های ایرانی (کافه‌بازار و مایکت)

### سیاست سخت‌گیرانه مارکت‌ها (Cross-Store Conflict):
- کافه‌بازار اگر لینک یا نامی از مایکت ببیند برنامه را رد می‌کند.
- مایکت اگر لینک یا نامی از بازار ببیند برنامه را رد می‌کند.

### راه‌حل استاندارد بدون تغییر پکیج و کلید:
هر دو نسخه باید **یکسان‌ترین حالت ممکن** باشند:
- پکیج نیم یکسان (`applicationId`)
- کلید امضای یکسان
- کدهای نسخه و ورژن یکسان
- تفاوت صرفاً در ارجاع به صفحه سایر برنامه‌ها در استور مربوطه است.

### پیاده‌سازی در `app/build.gradle.kts`:
```kotlin
flavorDimensions += "market"
productFlavors {
    create("bazaar") {
        dimension = "market"
        buildConfigField("String", "MARKET_NAME", "\"bazaar\"")
        buildConfigField("String", "DEVELOPER_MARKET_URL", "\"https://cafebazaar.ir/developer/057657612999\"")
    }
    create("myket") {
        dimension = "market"
        buildConfigField("String", "MARKET_NAME", "\"myket\"")
        buildConfigField("String", "DEVELOPER_MARKET_URL", "\"https://myket.ir/developer/dev-102174\"")
    }
}
```

### استفاده در صفحه «درباره ما» (`AboutScreen.kt`):
```kotlin
val isMyket = BuildConfig.MARKET_NAME == "myket"
AboutLinkItem(
    icon = Icons.Default.Shop,
    title = stringResource(if (isMyket) R.string.myket_page else R.string.cafebazaar_page),
    subtitle = if (isMyket) "myket.ir/developer/dev-102174" else "cafebazaar.ir/developer/057657612999",
    onClick = { openUrl(BuildConfig.DEVELOPER_MARKET_URL) }
)
```

---

## ۳. تایپوگرافی فارسی و فونت ایران‌سنس (Typography Rules)

۱. **محل قرارگیری فایل‌های فونت:** در پوشه `app/src/main/res/font/` (مانند `iran_sans_x_regular.ttf` و `iran_sans_x_bold.ttf`).
۲. **اعمال در Material 3 (`Type.kt`):** تنظیم `IranSansXFamily` و اعمال آن به تمامی سطوح تایپوگرافی (`display`, `headline`, `title`, `body`, `label`).
۳. **قانون قطعی لایسنس و ممیزی مارکت‌ها:**
   - **هرگز و تحت هیچ شرایطی** در صفحه «درباره ما» یا بخش متن‌های عمومی و مجوزهای برنامه نباید متنی مبنی بر استفاده از «فونت ایران‌سنس» یا «IranSans» نوشته شود تا بهانه‌ای برای رد شدن برنامه توسط ممیزان کافه‌بازار و مایکت ایجاد نشود.
۴. **رعایت نیم‌فاصله‌ها:**
   - استفاده دقیق از نیم‌فاصله (ZWNJ) در نام برنامه‌ها (مثال: «هم‌آوا» به جای «هم آوا» یا «هماوا») و رشته‌های متنی فارسی (`strings.xml`).

---

## ۴. استانداردهای آیکون اپ‌استور (Store Icon Specifications)

- **فرمت:** `PNG` با کیفیت بالا
- **نسبت ابعاد:** `1:1` (کاملاً مربعی)
- **ابعاد:** حداقل و دقیقا `512 × 512` پیکسل
- **حجم فایل:** حداکثر ۳ مگابایت (بهینه‌سازی‌شده زیر ۳۰۰ کیلوبایت)
- **لبه‌های تیز (Full Bleed):** گوشه‌ها نباید گرد شوند؛ مارکت‌ها به طور خودکار لبه‌های Squircle را اعمال می‌کنند.
- **محدوده امن (Safe Zone):** طرح اصلی در دایره مرکزی ۳۸۰ پیکسلی قرار گیرد تا با برش‌های استورها ناقص نشود.
- **محل ذخیره فایل‌ها در پروژه:**
  - `tmp/bazaar/icon.png`
  - `tmp/myket/icon.png`
  - `docs/icon_512.png`

---

## ۵. روال ضبط اسکرین‌شات‌های انتشار (Screenshots Workflow)

۱. **نحوه تهیه:** استفاده از شبیه‌ساز (Emulator) یا دستگاه اندرویدی فیزیکی با ADB:
   ```powershell
   adb exec-out screencap -p > output.png
   ```
۲. **قوانین تم و پوشه‌بندی در `tmp/`:**
   - تمامی صفحات اصلی در تم روشن (Light Theme) ضبط شوند + نمونه اسکرین‌شات از صفحه اصلی در تم تاریک (Dark Theme).
   - `tmp/bazaar/`: شامل اسکرین‌شات‌ها + اسکرین‌شات صفحه درباره ما از نسخه بازار.
   - `tmp/myket/`: شامل اسکرین‌شات‌ها + اسکرین‌شات صفحه درباره ما از نسخه مایکت.
   - `docs/screenshots/`: برای درج در ریدمی گیت‌هاب.

---

## ۶. نگارش توضیحات استورها و توجیه دسترسی‌ها

### متن توضیحات استور:
- عنوان فارسی و انگلیسی بهینه‌سازی‌شده برای سئو و جستجو.
- توضیحات کوتاه جذاب (یک یا دو جمله).
- توضیحات کامل با دسته‌بندی ویژگی‌ها، ایموجی‌های منظم و رعایت اصول نگارش فارسی بدون کاراکترهای اضافه یا فواصل نامتعارف.

### فرم توجیه دسترسی‌های حساس (Dangerous Permissions):
برای دسترسی‌هایی نظیر `RECORD_AUDIO` و `SYSTEM_ALERT_WINDOW`:
- متن باید رسمی، فنی و مؤدبانه خطاب به تیم ارزیابی مارکت باشد.
- شفاف‌سازی شود که این دسترسی‌ها صرفاً با دستور و دکمه صریح کاربر در پیش‌زمینه فعال می‌شوند.
- بر **متن‌باز بودن (Open Source)** پروژه تاکید شده و لینک ریپازیتوری رسمی در گیت‌هاب ارائه شود تا صداقت و شفافیت فنی اثبات گردد.

---

## ۷. ساختار مستندات گیت‌هاب (READMEs)

مستندات گیت‌هاب همیشه دو زبانه و طبق ساختار پروژه مرجع `mostafaafrouzi/MyCard` تنظیم می‌شوند:
- `README.md` (فارسی، راست‌چین و کامل)
- `README.en.md` (انگلیسی، فنی و استاندارد بین‌المللی)
- بخش‌ها:
  ۱. ردیف بج‌ها و مدال‌ها (لایسنس، ریلیز، نسخه‌های مینیمم و تارگت اندروید).
  ۲. گالری تصاویر و اسکرین‌شات‌های برنامه.
  ۳. امکانات و ویژگی‌ها با بولت‌های منظم.
  ۴. جدول معماری، پشته فناوری و لایبرری‌ها.
  ۵. راهنمای بیلد محلی و اجرای پروژه.
  ۶. دکمه‌های دریافت از کافه‌بازار، مایکت و ریلیزهای گیت‌هاب.
  ۷. بخش ارتباط با توسعه‌دهنده.

---

## ۸. گردش کار انتشار در GitHub Actions (`release.yml`)

با پوش شدن تگ‌های `v*` (مثلاً `v1.0.0`):
۱. سورس چک‌اوت شده و JDK 17 فعال می‌شود.
۲. کلید امضا از متغیر محرمانه `secrets.KEYSTORE_BASE64` رمزگشایی می‌شود.
۳. دستور بیلد هم‌زمان هر دو طعم:
   ```bash
   ./gradlew assembleBazaarRelease assembleMyketRelease bundleBazaarRelease bundleMyketRelease
   ```
۴. نام‌گذاری و آپلود ۴ فایل خروجی در صفحه GitHub Releases:
   - `{app}-bazaar-v{VERSION}.apk`
   - `{app}-bazaar-v{VERSION}.aab`
   - `{app}-myket-v{VERSION}.apk`
   - `{app}-myket-v{VERSION}.aab`
۵. اعتبارسنجی خودکار امضا با `apksigner` و `jarsigner`.

---

## ۹. امنیت کلیدهای امضا (Keystore)

- فایل‌های کلید: `*.jks`، `keystore.properties`، `keystore-credentials.txt`، `keystore-base64.txt`.
- حتماً در `.gitignore` بوده و هرگز کامیت نمی‌شوند.
- کلید امضا صرفاً از طریق GitHub Secrets برای اکشن‌ها فراهم می‌شود.
- تهیه یک نسخه بکاپ آفلاین محلی از فایل باینری Keystore و رمزهای آن الزامی است.
