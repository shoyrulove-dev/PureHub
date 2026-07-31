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

## Android release and community distribution

### Signed Android delivery

- Added environment-driven Android `versionCode` and `versionName`.
- Added local release-signing support through an ignored `signing.properties` file or protected environment variables.
- Added GitHub Actions CI for every relevant push and pull request.
- Added separate PWA/Command Center CI for TypeScript lint/build, Python compilation, and admin template parsing.
- Added a protected Android Release workflow for tags and manual runs.
- The release workflow restores the signing keystore from GitHub Secrets, runs tests and release lint, creates signed APK/AAB assets, generates `SHA256SUMS.txt`, adds build provenance attestation, and creates the GitHub release.
- Added `ANDROID_RELEASE_CHECKLIST.md` and `signing.properties.example`.
- Keystores, signing properties, and release secrets are excluded from Git.

### Release Hub

- Mongo schema upgraded to version 7 with `releases` and `release_publications` collections and unique indexes.
- Added release records for version, summary, changelog, GitHub/APK/AAB links, checksum, prerelease status, and publication time.
- Added DeepSeek and Groq-compatible content generation. DeepSeek is configured as the active provider; no API key is stored in the repository.
- Added English-first content bundles for Telegram, DEV Community, Bluesky, Mastodon, Reddit, Hacker News, Product Hunt, LinkedIn, and Facebook.
- Added a secret-protected GitHub release hook. A successful signed GitHub release automatically creates or updates its Release Hub record and generates the review drafts.
- Telegram also receives optional Vietnamese and Chinese drafts, but only the reviewed English channel post is eligible for API publishing.
- Added an approval queue with editable content and explicit `draft`, `approved`, and `ready_manual` states.
- Regenerating content does not overwrite an already-published post.
- API publishers are implemented for Telegram, public or draft DEV Community articles, Bluesky, and Mastodon.
- Reddit, Hacker News, Product Hunt, LinkedIn, and Facebook remain review-ready manual drafts to respect platform rules and avoid spam.
- Missing credentials move an approved publication to `waiting_credentials` instead of losing it.
- Added AI-assisted community reply drafts in the admin panel.
- Added `/ask` to the Telegram bot. Optional private-chat auto reply can be enabled through `community_reply_mode`; group auto reply is intentionally disabled to prevent noisy or unsafe bot behavior.
- Added a secret-protected Telegram webhook endpoint for reliable bot replies on Vercel serverless while retaining the existing polling worker for local/long-running deployments.

### Community Support Center

- Upgraded Mongo to schema 8 with deduplicated `support_messages` and per-platform `support_sync_state` collections.
- Added a responsive Support Inbox to the primary admin dashboard with open, draft, approved, replied, manual, and failed states.
- Telegram group/private messages are captured through the existing webhook. Bot commands handled elsewhere are excluded from the inbox.
- DEV comments, Bluesky replies/mentions/quotes, and Mastodon mentions synchronize every five minutes while the admin dashboard is open, can be refreshed manually, and receive a daily unattended Vercel Hobby cron fallback.
- Added AI triage for question, bug, feature request, privacy, installation, praise, spam, and other categories, including language and priority detection.
- AI replies are drafts only. An editor must approve before Telegram, Bluesky, or Mastodon can publish a reply.
- DEV comments receive an editable AI draft and a source link for manual reply because the official Forem API does not expose comment creation.
- Added duplicate protection, reply threading metadata, idempotent Mastodon publishing, failure states, audit events, and direct links to source messages/replies.
- Added a live admin notification badge that refreshes support counts every minute while the dashboard is open.
- Added a protected Vercel cron endpoint using `CRON_SECRET`; the production secret is stored as a sensitive Vercel environment variable.

### Public release experience

- Added localized `/download` and `/changelog` pages to the PWA.
- The download page only displays records explicitly published through Release Hub.
- Added signed APK, AAB/GitHub, SHA-256, prerelease, and safe “build pending” states.
- Added public JSON at `/public-api/releases`.
- Added escaped RSS XML at `/public-api/releases.xml`.
- Added Android entry points from the desktop navigation and Community page.

### Additional verification

- Mongo migration: schema 7 applied successfully.
- DeepSeek live completion: passed.
- FastAPI public release JSON: HTTP 200.
- FastAPI release RSS: HTTP 200.
- Admin auth redirect and Jinja template parse: passed.
- PWA lint and production build after Download/Changelog additions: passed.
- Android debug unit tests, `lintDebug`, and `assembleDebug`: passed.
- Android `lintRelease`, minified APK release build, and AAB build: passed (0 lint errors).
- Debug APK generated at `app/build/outputs/apk/debug/app-debug.apk` (not for public distribution).

## Owner action checklist before the first public APK

These items require account ownership or a long-lived secret and therefore are not generated or committed automatically:

1. Create the permanent PureHub Android signing keystore and keep at least two encrypted backups.
2. Create the GitHub environment `android-release`, preferably with required reviewer approval.
3. Add `PUREHUB_KEYSTORE_BASE64`, `PUREHUB_KEYSTORE_PASSWORD`, `PUREHUB_KEY_ALIAS`, and `PUREHUB_KEY_PASSWORD` as environment secrets.
4. Generate one random release-hook secret. Add it to Vercel as `RELEASE_WEBHOOK_SECRET` and to the GitHub `android-release` environment as `PUREHUB_RELEASE_HOOK_SECRET`.
5. Generate a Telegram webhook secret, add it to Vercel as `TELEGRAM_WEBHOOK_SECRET`, then call Telegram `setWebhook` with `https://hub.blissbiovn.com/public-api/telegram-webhook` and the same `secret_token`.
6. Run the Android Release workflow for `1.0.0-beta.1`, install its signed APK on a physical phone, and confirm install/update behavior.
7. Replace the current Groq key if Groq is desired; the discovered key returns HTTP 403. DeepSeek already works and is sufficient.
8. Optional: create DEV Community, Bluesky, and/or Mastodon accounts, then add their API credentials in Command Center. Telegram is already the default channel.
9. For Reddit, Hacker News, Product Hunt, LinkedIn, and Facebook, review each generated draft and post manually according to each community's rules.
10. After the APK is confirmed, review the automatically generated Release Hub drafts, approve only the desired English posts, and publish.

## Known follow-up opportunities

- PWA OCR language packs should be downloadable per language to keep first load small.
- Each PWA mini app can be split into its own route chunk; the current pass already removes the shared mini-app bundle from initial Home loading.
- Password Vault should receive an independent security review before being recommended as the only store for critical credentials.
- Android SDK is installed in duplicate locations (`D:\Dev\Android` and `D:\Dev\Android\Sdk`), which slows Gradle SDK discovery but does not block builds.
- Instrumented camera/sensor tests require a device or emulator and were not run in this pass.
- The 22 mini-app feature upgrades are intentionally deferred until Support Center operations are stable.
