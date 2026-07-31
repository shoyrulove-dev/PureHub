# PureHub System Update Report

Date: 2026-07-31

## Product direction

PureHub is now consistently positioned as a free, no-ads, local-first and open-source utility collection. Core tools are not gated by referral codes, accounts, subscriptions or community badges.

English is the primary/default product language. Vietnamese and Chinese remain available as secondary localized experiences.

Community growth remains connected to:

- Telegram: `https://t.me/aaa_letan_vip_bot`
- GitHub: `https://github.com/shoyrulove-dev/PureHub`

Referral milestones now recognize Community Supporters; they do not unlock product functionality.

## PWA

### Navigation and design

- Replaced the five-category bottom navigation with Home, Tools, Community and Settings.
- Added a calmer light/dark design system with reduced glow, smaller radii and consistent Lucide icons.
- Added system/light/dark theme preferences.
- Added search across the full tool catalog.
- Added favorite and recently used tools.
- Added dedicated Tools, Community and Settings pages.
- Set first-time visits to English while preserving an explicit saved language choice.
- Preserved localized SEO routes for existing mini apps.
- Lazy-loaded the mini-app interaction bundle to reduce the initial application bundle.

### Catalog

The PWA catalog is aligned with Android at 22 tools by adding:

- Smart Flashlight
- Device Cleaner
- Wi-Fi Analyzer
- Wallpaper Studio

Browser limitations are explained instead of being hidden. Hardware torch control, nearby Wi-Fi scanning, device deletion and wallpaper application remain Android-native capabilities.

### Mini-app changes

- Lunar Calendar: replaced moon-cycle approximation with Vietnamese solar-to-lunar conversion for UTC+7.
- Pomodoro: added a central progress ring and clearer session state.
- Zen Breath: added Calm, Box and 4-7-8 patterns, start/pause control and safety copy.
- Decibel Meter: added recent-level visualization, peak information and an explicit estimated-reading warning.
- Smart Flashlight: added a browser-safe screen light and color choices.
- QR Studio: added copy/open smart actions after scanning.
- Doc to PDF: added page reorder and removal controls.
- Device Cleaner: added safe local file review without automatic deletion.
- Wi-Fi Analyzer: added privacy-safe connection information with honest browser capability messaging.
- Speaker Cleaner: added frequency control and safer residual-water guidance.
- Password Vault: added a unique random salt per new entry, stronger PBKDF2 settings, timed preview clearing, deletion and an experimental-vault warning. Existing entries remain decryptable through the legacy salt fallback.
- Wallpaper Studio: added private local image preview and collection selection.
- Decision Wheel: tied the result to the wheel stop angle and switched to cryptographic random selection.
- Expense/Bill formatting: uses VND for Vietnamese locales and USD otherwise instead of hard-coding USD for everyone.
- Community: removed Pro-code storage and replaced it with Telegram/GitHub contribution actions.

## Android

- Replaced the five category-only bottom tabs with Home, Tools, Community and Settings.
- Added labels to bottom navigation.
- Added a friendly Home dashboard with product promises, real lunar information, favorites and quick access.
- Rebuilt Tools as a searchable catalog covering all 22 mini apps.
- Rebuilt Community around Telegram, GitHub, issue reporting and voluntary contribution.
- Updated typography from mixed serif/sans to a consistent modern sans-serif hierarchy.
- Reduced corner radii and adjusted dark surfaces.
- Added honest estimated-reading guidance to Decibel Meter.
- Updated Speaker Cleaner wording and safety instructions.
- Hid password draft input, added timed clipboard clearing and backup guidance.
- Added Android 14 selected-media permission support and optional camera/microphone hardware declarations.
- Fixed CameraX experimental API lint declarations for QR and OCR.

## Command Center and Telegram bot

- Removed Pro-code configuration from the admin form and save flow.
- Kept existing legacy database keys untouched for backward-compatible deployments, but they are no longer used by the bot or UI.
- Updated catalog wording from Community Pro Unlock to PureHub Community.
- Updated Telegram onboarding to state that every tool is free.
- Standardized Telegram commands, profile copy and release announcements in English.
- Configured release announcements for the public channel only; its linked discussion group receives channel posts automatically.
- Changed referral reward messaging to Community Supporter recognition.
- Updated architecture/admin documentation to match the community-first model.

## Verification

- PWA TypeScript production build: passed.
- PWA Oxlint: passed.
- PWA route smoke checks: `/vi`, `/vi/tools`, `/vi/community`, `/vi/settings`, `/vi/lich-am` returned HTTP 200.
- Android Kotlin compilation: passed.
- Android debug unit tests: passed.
- Android lintDebug: passed.
- Android assembleDebug: passed.
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.
- Python command center/API syntax parse: 7 files passed.
- `git diff --check`: passed.

## Known follow-up opportunities

- PWA OCR language packs should be downloadable per language to keep first load small.
- Each PWA mini app can be split into its own route chunk; the current pass already removes the shared mini-app bundle from initial Home loading.
- Password Vault should receive an independent security review before being recommended as the only store for critical credentials.
- Android SDK is installed in duplicate locations (`D:\Dev\Android` and `D:\Dev\Android\Sdk`), which slows Gradle SDK discovery but does not block builds.
- Instrumented camera/sensor tests require a device or emulator and were not run in this pass.
