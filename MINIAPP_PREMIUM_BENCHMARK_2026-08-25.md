# PureHub miniapp premium benchmark — 2026-08-25

This review compares PureHub's 26 miniapps with representative paid or freemium leaders currently active on Google Play and the App Store. The goal is not feature-count parity at any cost: PureHub stays free, ad-free, account-free, offline-first, and F-Droid compatible.

## Executive result

- **Market-leading PureHub position:** QR Studio, OCR/Document workflow, Photo Privacy, Bill Splitter, Deep Cleaner safety model, and the combined local backup story.
- **Premium parity after this round:** Compass target bearing, Bubble Level held measurements, Sound Meter session history/CSV, Authenticator grouping/search/encrypted backup, Money budget/category/CSV, and Password Vault local health/generator.
- **Deliberate gaps:** cloud sync, breach lookup, LAN/port scanning, live price comparison, generative document summaries, social habit rooms, and streaming. These require a server, account, tracking-sensitive permission, or ongoing paid API and do not fit the core promise.
- **Best future candidates:** screen-recording quality controls, batch Photo Privacy, recurring Money entries, document edge auto-detection, and scheduled habit reminders.

## 26-miniapp matrix

| PureHub miniapp | Representative premium benchmark | PureHub position now | Best remaining upgrade | Priority |
|---|---|---|---|---|
| Lunar Calendar | Premium lunar/calendar utilities | Offline conversion and month browsing cover the core | Saved dates and calendar export | Medium |
| Zen Habit | Streaks / HabitStreak | Weekly targets, one-tap history, insights, archive and JSON export are competitive | Per-habit reminders and longer heatmap | High |
| Zen Pomodoro | Forest | Accurate service timer, presets, soundscapes and weekly totals; no addictive coins or ads | Focus tags, daily target and session timeline | Medium |
| Zen Breath | Calm-style breathing tools | Clean offline breathing without subscription | Custom inhale/hold/exhale patterns and haptics | Medium |
| Compass | Smart Compass Pro | Now adds smooth heading, accuracy state, target bearing and shortest-turn guidance | Optional true north with local WMM and waypoint distance | Medium |
| Bubble Level & Ruler | Smart Level / Bubble Level Pro | Surface/edge modes, calibration, tolerance, haptic settle, ruler calibration, held/shareable readings | Camera plumb overlay | Medium |
| Decibel Meter | Decibel X | Calibration, rolling windows, peak, saved min/avg/max sessions and one-second CSV; no audio retention | Frequency spectrum and A/C weighting | Medium, specialist |
| Smart Flashlight | Premium flashlight tools | Core torch use is already commodity-grade and private | SOS/custom patterns only if safety UX is clear | Low |
| Unit Converter | Premium unit converters | Fast offline conversion covers daily use | Favorites and calculation history | Low |
| QR Studio | Gamma Play QR Scanner Pro | PureHub is stronger on safety review, verified URLs, batch/history, private sessions, generation and contextual actions | Product lookup only with explicit user-opened web search | Low |
| Doc to PDF | Adobe Scan | Multipage capture, local PDF, reorder and handoff are strong | Automatic edge detection/deskew and redaction | High |
| OCR Studio | Adobe Scan | Local OCR, crop/filter, modes, library, quick actions and TXT/PDF export avoid Adobe cloud dependency | Searchable PDF text layer and confidence highlighting | High |
| Color Grabber | Premium color pickers | Camera/image color extraction and useful color values cover the core | Palette harmony and accessibility contrast audit | Medium |
| Photo Privacy | EXIF metadata editors | PureHub's inspect-before/after and save-copy flow is safer and clearer | Batch sanitize and selectable metadata policy | High |
| Deep Cleaner | Files by Google | Exact SHA-256 duplicates, large-file evidence and Android-confirmed deletion provide a trustworthy alternative | Old/downloaded media recommendations and scan exclusions | Medium |
| Speaker Cleaner | Premium speaker cleaners | Local tone patterns cover the real capability without fake cleaning claims | Guided before/after speaker test | Low |
| Wi-Fi Analyzer | Fing / NetSpot | Channel pressure, security labels and 32-AP scan are competitive for radio analysis | LAN device/port scan conflicts with offline-only INTERNET removal; do not add by default | Deliberately excluded |
| Password Vault | 1Password | Encrypted local vault, screenshot block, timed clipboard, health and strong generator are solid | Android Autofill/passkeys and biometric prompt require a dedicated security architecture | Future standalone review |
| Authenticator Vault | Aegis | Device lock, encrypted TOTP, groups, search and inclusion in AES backup now reach core parity | QR camera import, icons and advanced HOTP/SHA variants | High |
| File Studio | Files by Google / archive tools | Local ZIP/file workflows cover the promised scope | Archive preview, selective extraction and integrity report | Medium |
| Wallpaper Changer | Premium wallpaper schedulers | Local wallpaper workflows avoid tracking feeds | Time/sunrise schedule and crop preview | Medium |
| Bill Splitter | Splitwise Pro | Receipt OCR, item-level assignments, tax/tip, reusable presets and share summary are unusually strong without accounts | Unequal percentages/shares and named people | High |
| Expense Tracker | Money Manager | Monthly budget, category totals, receipt OCR, search and CSV are competitive for a private ledger | Income, recurring entries and local accounts | High |
| Decision Wheel | Premium randomizers | Offline decision flow already fulfills the job | Weighted choices and saved wheel templates | Medium |
| PureHub Community | Product communities | Relevant discovery without paywall | Better opt-in feedback routing and public roadmap links | Medium |
| Screen Recorder | XRecorder | Safe Android consent and local MP4 are correct but materially behind specialist recorders | Resolution/FPS/bitrate/audio controls, pause/resume and trim | Highest remaining gap |

## Changes implemented after comparison

### Authenticator Vault

- Backward-compatible encrypted account repository.
- Quick/Pro UI, search, and local groups.
- TOTP accounts are now included in the AES-256 whole-app backup and restore flow.
- Malformed-data fail-closed and migration tests.

### Sensor Lab

- Compass target bearing with normalized shortest-turn guidance.
- Bubble Level held measurement and explicit share action.
- Sound Meter stores up to ten local session summaries with one-second sample CSV; microphone audio is never stored.
- Privacy receipts explain sensor and recording boundaries.

## Sources checked

- Adobe Scan: https://play.google.com/store/apps/details?id=com.adobe.scan.android
- 1Password: https://play.google.com/store/apps/details?id=com.onepassword.android
- Aegis Authenticator: https://play.google.com/store/apps/details?id=com.beemdevelopment.aegis
- Files by Google: https://play.google.com/store/apps/details?id=com.google.android.apps.nbu.files
- Fing: https://play.google.com/store/apps/details?id=com.overlook.android.fing
- Money Manager: https://play.google.com/store/apps/details?id=com.realbyteapps.moneymanagerfree
- Splitwise: https://play.google.com/store/apps/details?id=com.Splitwise.SplitwiseMobile
- Forest: https://play.google.com/store/apps/details?id=cc.forestapp
- Decibel X: https://play.google.com/store/apps/details?id=com.skypaw.decibel
- Smart Level: https://play.google.com/store/apps/details?id=kr.sira.level
- Smart Compass Pro: https://play.google.com/store/apps/details?id=kr.aboy.compass
- QR & Barcode Scanner Pro: https://play.google.com/store/apps/details?id=com.gamma.scan2
- XRecorder: https://play.google.com/store/apps/details?id=videoeditor.videorecorder.screenrecorder

Store listings and feature sets were checked on 2026-08-25. Ratings, pricing, and features may change.
