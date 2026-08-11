# PureHub System Update Report

## Beta.18 cross-platform parity release - 2026-08-11

- Added passphrase-protected AES-GCM backup and restore to Zen Habit and Expense Tracker on the PWA. PBKDF2 uses a random salt and 310,000 SHA-256 iterations; imports validate the tool type and record shape before merging.
- Added local receipt OCR to the PWA Expense Tracker and Bill Splitter. A receipt image is processed on-device with Tesseract, likely merchant/total/tax/item fields are prefilled, and the user must review them before saving or sharing.
- Kept CSV export as a portable non-sensitive option while making encrypted `.purehub` backup the primary complete-history workflow.
- Synchronized PWA and Android release versions to `1.0.0-beta.18`; Android uses monotonic `versionCode 19`, with Standard and F-Droid release metadata aligned.
- Verification: PWA production build, TypeScript, lint, and five prerender checks pass; Android Standard/F-Droid unit tests, release lint, and debug assembly pass; all 49 Command Center tests pass.
- Physical Android verification: the SHA-256-verified, signed beta.18 arm64 APK was installed over beta.17 on a Xiaomi M1906G7G. The device reports `versionName 1.0.0-beta.18` and `versionCode 19`; launch produced no AndroidRuntime crash, app data was preserved by the in-place upgrade, and only `com.purehub.app` remains installed.

## Beta.18 release and community publication verification - 2026-08-11

- Production PWA: `https://hub.blissbiovn.com`
- GitHub implementation: `https://github.com/shoyrulove-dev/PureHub/commit/5828f3e86f690baf9fdd4fff1e8a085c32a82ae3`
- Signed Android release: `https://github.com/shoyrulove-dev/PureHub/releases/tag/v1.0.0-beta.18`
- Telegram update: `https://t.me/purehubaaa/33`
- DEV article: `https://dev.to/purehub/purehub-beta18-encrypted-backups-and-local-receipt-ocr-jo9`
- Bluesky update: `https://bsky.app/profile/purehub.bsky.social/post/3msszga6ceu25`
- Mastodon update: `https://mastodon.social/@purehub/117077784237195550`
- GitHub Actions published signed Standard and fully FOSS APK splits, a universal APK, an Android App Bundle, SHA-256 checksums, and build provenance. All four configured community channels report `published` with no API error.

## Production and community publication verification - 2026-08-11

- Production deployment: `https://hub.blissbiovn.com`
- GitHub implementation: `https://github.com/shoyrulove-dev/PureHub/commit/c0f8c35`
- Telegram update: `https://t.me/purehubaaa/32`
- DEV article: `https://dev.to/purehub/from-qr-scan-to-searchable-pdf-a-practical-purehub-workflow-update-c3k`
- Bluesky update: `https://bsky.app/profile/purehub.bsky.social/post/3mssv27ndgc2g`
- Mastodon update: `https://mastodon.social/@purehub/117077478516506084`
- All four public post URLs and the production QR Studio route returned HTTP 200 after publication.
- Channel copy is differentiated: Telegram is a compact release summary, DEV is a technical workflow article with AI-assistance disclosure, Bluesky leads with QR link safety, and Mastodon emphasizes the open-source local-first workflow.

## Workflow, analytics, SEO, and growth quality pass — 2026-08-11

- Upgraded QR Studio on web and Android with multi-image batch scanning, safer destination inspection, clearer risk reasons, and a more useful local library. The web creator now warns about low-contrast codes and exports library data as CSV.
- Connected OCR Studio directly to Doc to PDF. OCR now exposes page confidence, transfers processed pages and recognized text locally, and produces image-first PDFs with a searchable text layer.
- Made web Pomodoro sessions resumable across reloads, added task context, optional completion notifications, honest completion analytics, and shareable weekly result cards.
- Persisted Compass and Bubble Level calibration, added magnetic-jump guidance, and added Sound Meter calibration plus local CSV export. Android Standard and F-Droid flavors retain their existing foreground timer, receipt OCR, encrypted export/import, and searchable PDF implementations.
- Replaced inflated analytics semantics: mini-app opens are deduplicated daily, completed results are counted separately, first installed opens only come from standalone PWA use, and PureHub's own support replies no longer inflate audience engagement.
- Added reusable 1200×630 privacy-first share cards that use native file sharing when supported and save/copy a fallback on desktop.
- Preserved generated route-level SEO pages by removing the Vercel catch-all that previously replaced every route with generic homepage metadata. The production build now validates QR, OCR, PDF, Vietnamese, and Chinese prerender outputs.
- Reworked the 30-day automation calendar around concrete search problems and measurable results. Social posts now deep-link to the matching mini-app; DEV fallback articles include an AI-assistance disclosure; future Shorts lead with a problem/result rather than a generic product intro.
- Verification: PWA production build and lint pass; five prerender routes pass metadata validation; Android Standard and F-Droid Kotlin compilation pass; all 49 Command Center tests pass.

Date: 2026-08-11

## Zen Suite flagship and shared polish — beta.10

- Rebuilt Zen Pomodoro as a complete flagship experience on web and Android: shared Zen Suite identity, clearer selected presets, custom web focus lengths, resilient timing, local soundscapes, private weekly totals, and a seven-day focus rhythm chart.
- Rebuilt Zen Breath as a complete flagship experience on web and Android: 1/3/5-minute goals, Calm/Box/4-7-8 rhythms, goal progress, optional haptic and phase cues, reduced-motion control, private completion totals, and a friendly completion state.
- Added a reusable flagship hero pattern with consistent privacy, offline, and no-ads trust signals, compact spacing, 44px+ controls, and 150–200ms interaction feedback.
- Improved the Finance Suite with shared USD/EUR/GBP/VND/JPY display selection and a private monthly budget with remaining-balance feedback.
- Improved Speaker Cleaner with an explicit completion state and a private recent-run history, and added fullscreen instrument mode across the web Sensor Suite.
- Verified clean PWA and Android builds, Android unit tests, source lint, and both updated Zen screens on a USB-connected Android device.

Date: 2026-08-10

## Utility flagship suites — beta.9

- Promoted Speaker Cleaner into an Audio Care flagship with safe presets, frequency control, 15/30/60-second cycles, progress feedback, and local-only playback.
- Joined Doc to PDF and OCR Studio into a Document Suite. Web users can import up to 20 pages, reorder, rotate, frame, control image quality, and export locally; Android keeps its camera, crop, page, PDF, share, and OCR workflows.
- Joined Expense Tracker and Bill Splitter into a private Finance Suite. The web ledger adds monthly/all-time totals, category trends, local persistence and CSV export; Android retains item assignment and reusable split presets.
- Joined Compass, Bubble Level, and Sound Meter into a Sensor Suite with a shared flagship identity, smoother visual readings, calibration guidance, local microphone processing, and explicit estimate warnings.
- Promoted all seven utility routes to flagship priority in the catalog and Command Center, closed the shipped money-tools roadmap request, and expanded the 14-day product monitor.
- Updated English SEO titles/descriptions around high-intent no-ads, private, offline, speaker-water, document-scanner, group-expense, and sensor queries.
- Built and device-tested the Android debug APK on a connected Xiaomi device. PWA production build, Android compilation, and 43 Command Center tests pass.
- Created four 13.4-second vertical Shorts from real Android screens and scheduled YouTube publication for 22, 24, 26, and 28 August at 19:30 (UTC+7).

## Privacy-first growth and Early Testers upgrade

- Added an anonymous 30-day journey funnel for daily visits, tool opens, downloads, first browser opens, Early Tester joins, and device reports.
- Attribution accepts only a fixed safe list of community sources and campaigns. It stores no IP, account, user ID, device ID, free-form referrer, or personal identifier.
- Added funnel counts and conversion rates to the Admin Growth Overview, including a 20-report Early Tester goal.
- Promoted Zen Pomodoro, Zen Breath, and QR Studio as the first three flagship tools across the public dashboard, database priorities, and automated community topics.
- Added a public Early Testers form. Anonymous device reports enter Support Inbox as high-priority review items; the form explicitly asks users not to include personal information.
- Connected PWA install acceptance and signed APK clicks to the same aggregate funnel, while respecting Do Not Track and the existing anonymous-metrics preference.
- TikTok Sandbox Direct Post was validated with a private post. The Production app configuration and two end-to-end review videos have been submitted and are currently in review.
- TikTok review recordings remain local and are excluded from Git because they are temporary platform-review artifacts.
- Growth Autopilot now treats an active scheduled YouTube queue as complete coverage. It no longer creates duplicate script-only upload tasks while future Shorts are already scheduled, and Admin prioritizes the next automatic publish time over optional upload drafts.

## August community command center

- Added a compact August mission dashboard with live progress for audience, qualified views, interactions, useful feedback, and YouTube Shorts.
- Added a weighted outcome score, month/campaign timeline, four-week roadmap, daily automation strip, and data-driven next actions.
- Reordered the admin overview around goal, support, and approvals. Platform details, Growth Autopilot schedules, Reddit, credentials, resolved support, and system operations are collapsed until needed.
- Growth content now uses recent platform signals to improve topic emphasis without exposing private or low metrics.
- Expanded safe question discovery to eight focused searches and up to nine review-only opportunities per day. Replies still require human approval.
- Reduced overview-only database work; warm dashboard context loads are under roughly 1.1 seconds in local production-data checks.
- Added privacy-preserving daily miniapp counters for opens, helpful taps, shares, and feedback; no account, cookie, device ID, raw interaction, or stored IP is used.
- Added a compact Product Signals row to Admin with helpful rate, shares, roadmap votes, and the leading community request.
- Added contextual Helpful, Feedback, and Share actions to all 22 web miniapps. Product feedback enters Support Inbox and can be marked reviewed in one click.
- Added a public roadmap vote to Community and an opt-out anonymous counters switch to Settings, with automatic Do Not Track support.

## Product direction

PureHub is now consistently positioned as a free, no-ads, local-first and open-source utility collection. Core tools are not gated by referral codes, accounts, subscriptions or community badges.

The repository is explicitly licensed under the MIT License. Copyright is held by PureHub contributors.

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

### Admin information architecture refresh

- Reorganized the primary dashboard around work that needs attention instead of exposing every control at once.
- Kept Support Inbox visible and moved channels, release/system operations, resolved support, and inactive queues into collapsed disclosure groups.
- Limited the main approval queue to actionable English API channels; secondary-language and manual-community drafts remain available in Advanced Controls.
- Added a shared inline SVG icon system for navigation, metrics, actions, and disclosure groups without adding another client dependency.
- Rebuilt Advanced Controls as a toolbox: connections, content jobs, insights, catalog/access, and Release Hub stay hidden until explicitly selected.
- Simplified the Advanced header and removed duplicated dashboard metrics from that power-user screen.
- Verified the new layout in desktop and mobile Chromium viewports.

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

- Password Vault has completed an internal hardening review but still requires an independent third-party security review before being recommended as the only store for critical credentials.
- Android SDK is installed in duplicate locations (`D:\Dev\Android` and `D:\Dev\Android\Sdk`), which slows Gradle SDK discovery but does not block builds.
- Expanded instrumentation now covers all 22 mini-app routes and security/permission contracts. The complete 4-test suite passes on the physical RMX3941 device.
- Deeper market-parity feature work can continue per mini app after community feedback; the shared mobile UI/accessibility pass is complete.

## Light admin and community reach update

- Converted the login, Operations Dashboard, and Advanced Controls to a modern light visual system.
- Reduced form height, spacing, corner radius, and unused whitespace; credential fields now use responsive two- and three-column settings grids.
- Kept advanced tool groups closed by default so infrequent controls do not compete with daily operations.
- Removed raw configuration text areas from the interface. Admins now download a private backup file and restore it with a file picker and plain-language merge/replace choices.
- Added schema migration 9 and a `community_metrics` collection for platform engagement snapshots.
- Added live Community Reach cards for Telegram members, DEV views/reactions/comments, Bluesky followers/likes/replies/reposts, and Mastodon followers/likes/boosts/replies.
- Engagement refresh runs with the existing support sync and preserves the last successful snapshot when a platform API is temporarily unavailable.
- Live verification passed for all four connected platforms.

## Community expansion foundation

- Enabled GitHub Discussions on the public repository and created the first pinned-style welcome announcement.
- Added structured GitHub Discussion forms for Q&A and community ideas.
- Linked the public Community page directly to Discussions and its Ideas category.
- Added a dedicated Reddit Review panel to the Operations Dashboard. AI produces Reddit-specific copy while the destination is manually curated; posting remains manual and requires a current rules check.
- Generated and stored the first Reddit review draft for `v1.0.0-beta.2` without publishing it externally.
- Added a reproducible vertical-video pipeline and generated the first 22.5-second English teaser at 1080×1920 using current production screens.
- The video is encoded as H.264 `yuv420p` with AAC silent audio for broad YouTube Shorts and TikTok compatibility; platform-native music can be added during upload.

## Admin contrast and icon refinement

- Increased light-theme contrast across Login, Operations Dashboard, and Advanced Controls with darker secondary text, stronger borders, heavier labels, and readable status colors.
- Replaced repeated engagement labels with accessible icon-and-number tiles for members, views, likes, comments, posts, replies, and reposts.
- Replaced platform initials and legacy Unicode control symbols with a shared SVG icon language and retained tooltips/screen-reader labels.
- Compacted metric refresh, support sync, Reddit generation/copy/open, toolbox, backup, restore, logout, and visibility controls into icon-led actions.

## Current completion summary

Completed:

- Signed Android release pipeline, protected signing secrets, GitHub prereleases, checksum generation, and physical-device beta installation.
- Production PWA, localized routes, Download/Changelog pages, all 22 tools in the catalog, and English-first language behavior.
- Telegram channel publishing and bot support, plus DEV, Bluesky, and Mastodon publishing/monitoring credentials.
- AI-assisted release content, approval workflow, Support Inbox, reply drafts, notifications, and community reach metrics.
- Light, compact admin dashboard and UI-based Advanced Controls.
- GitHub Discussions, Reddit manual review workflow, and the first vertical YouTube/TikTok teaser.
- MIT License at the repository root and package metadata.

Remaining owner/external actions:

1. Monitor the scheduled YouTube queue and keep TikTok test posts private until the Production review is approved.
2. Recover and establish the Reddit account before posting once to `r/droidappshowcase`; do not repost to `r/androidapps`. Ask `r/FOSSdroid` moderators before a later MIT-licensed development/release post.
3. Commission an independent Password Vault review before presenting it as suitable as the only store for critical credentials; the internal review and hardening work are complete.
4. Replace the first PWA-based teaser with polished Android screen recordings after community review of this UI pass.
5. Defer Play Console production preparation, store listing assets, policy declarations, and staged rollout until the APK beta is stable.
6. Track the remaining React Router RSC-only advisory until an upstream patched release exists. PureHub is a client-side SPA and does not enable React Server Components or server actions; downgrading would reintroduce broader historical router vulnerabilities.

## Five-proposal engineering pass

### Dependency security

- Applied compatible `postcss` and `fast-uri` fixes and pinned patched `brace-expansion` through npm overrides.
- Moved `vite-plugin-pwa` to build-only `devDependencies`.
- Reduced npm audit findings from 12 high entries to two entries representing one React Router RSC/server-action advisory that is not reachable in PureHub's client-only architecture.
- Rejected npm's forced downgrade to React Router 7.11 because that version carries multiple older redirect, XSS, SSR, and route-expansion advisories.

### All-22 mini-app experience

- Added one modern, high-contrast mini-app surface system with compact fields, strong focus states, tactile buttons, safety/no-ads cues, and one-handed mobile spacing across all 22 tools.
- Added per-tool privacy/no-ads/capability chips and clearer Android catalog affordances.
- Added consistent Android content margins so card-based tools no longer touch screen edges.
- Added an instrumentation traversal that searches, opens, renders, and returns from every catalog item.

### Hardware and Android security coverage

- Added packaged-APK contracts for offline-only permission behavior, optional camera/microphone hardware, sensitive-data backup prohibition, camera, microphone, Wi-Fi, and wallpaper permissions.
- Built both the debug APK and Android instrumentation APK successfully.
- Added a separate `.debug` application ID so future instrumentation runs cannot overwrite or remove the installed release app.
- Ran the complete suite on the physical RMX3941 device: all 4 tests passed, including all-22 navigation and packaged security contracts.
- Reinstalled the signed `1.0.0-beta.2` release after testing and verified its published SHA-256 checksum, package version, and launcher startup.

### Password Vault hardening

- Added root `SECURITY.md` and `docs/PASSWORD_VAULT_SECURITY_REVIEW.md` with scope, threat model, controls, limitations, and responsible reporting guidance.
- PWA: raised new-entry PBKDF2-HMAC-SHA-256 work factor to 600,000, versioned KDF metadata, retained legacy decryption, enforced field/entry bounds, added five-minute auto-lock, 30-second reveal timeout, validated encrypted backup import/export, and explicit plaintext-label disclosure.
- Android: disabled cloud backup, blocked screenshots while Vault is visible, marked clipboard content sensitive, cancelled stale clipboard-clear jobs, committed encrypted writes synchronously, bounded records, and made malformed JSON fail closed.
- Added Android Vault malformed-data and codec round-trip tests.

### OCR and code delivery

- OCR now offers English, Vietnamese, and Simplified Chinese and downloads only the selected Tesseract language pack on first use.
- OCR and Password Vault now compile into independent lazy route chunks instead of living in the shared mini-app chunk.
- The shared mini-app JavaScript chunk decreased from roughly 48.6 KB to 43.4 KB before gzip, while OCR and Vault load only on their own routes.

## Growth Autopilot

- Added an idempotent 30-day community campaign covering the full PureHub tool and engineering story.
- Added safe platform cadence: daily Bluesky/Mastodon, three Telegram channel posts per week, and one educational DEV article per week.
- Added AI-generated, platform-specific copy with verified-fact constraints and deterministic fallback content.
- Added automatic publishing, per-channel failure retention/retry, aggregate engagement refresh, per-post metrics, and audit logging.
- Kept public replies in the existing human-approval workflow and Reddit in manual-review mode.
- Added YouTube content packages three times per week with a title, link-free description, and short shot list.
- Added Google OAuth connection and browser-to-YouTube resumable uploads. PureHub never accepts the owner's Google password, and videos default to Unlisted.
- Added compact Growth Autopilot controls, history, status, metrics, YouTube queue, and upload actions to Admin.
- Added a protected daily Vercel cron at 19:00 Asia/Bangkok and retained the independent daily support monitor.
