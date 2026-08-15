# PureHub 1.0.0-beta.19 release report

Date: 2026-08-15

## Product outcome

- Bubble Level is now a complete flagship workflow on web and Android: Surface, Edge X and Edge Y modes, adjustable tolerance, zero calibration, steady-reading confirmation, haptic feedback, accuracy guidance, and a calibrated ruler on Android.
- QR Studio, OCR Studio and Bubble Level remain the three acquisition workflows linked directly from the repository and public product surfaces.
- The public download path remains open to everyone. A verified-tester gate is intentionally deferred until the community target is reached.

## Honest analytics

- APK download clicks, accepted PWA installs, PWA standalone opens and completed mini-app results are separate events.
- GitHub Release APK download totals are displayed separately in Command Center.
- Download counts are not presented as confirmed Android installs. The offline Android app does not transmit installation telemetry.
- The August outcome score now rewards completed useful results instead of the volume of discovered social leads.

## Community operations

- Social discovery remains capped at 30 qualified leads per day across four scan windows.
- Discovery rejects unrelated Android chatter, social-network support requests, dating apps and other broad false positives.
- Existing failed automatic posts are retried at most three times.
- DEV campaign posts no longer reuse one canonical URL, preventing duplicate-canonical failures.
- Replies remain human-approved; PureHub's own posts and replies are excluded from opportunity discovery.

## Distribution and media

- Standard and F-Droid Android flavors use version name `1.0.0-beta.19` and version code `20`.
- The signed release is produced by the Android Release GitHub workflow from tag `v1.0.0-beta.19`.
- Three new vertical workflow demos are scheduled after the existing 22-app YouTube queue:
  - Bubble Level: 2026-09-23 19:30 ICT — <https://youtu.be/sv6htCEadio>
  - QR safety: 2026-09-25 19:30 ICT — <https://youtu.be/N_mS4jkTD-M>
  - OCR to searchable PDF: 2026-09-27 19:30 ICT — <https://youtu.be/ohQ_qcoupCU>

## Verification

- Command Center: 50 tests passed, plus 4 subtests.
- PWA lint and production build passed.
- Android standard and F-Droid unit tests passed.
- Android standard and F-Droid lint passed.
- Android Kotlin compilation and debug installation passed.
- Physical sensor interaction is verified on the connected Android device before the signed APK replaces the prior beta.
