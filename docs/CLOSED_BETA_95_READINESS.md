# Closed beta 9.5 readiness

This score is an evidence target, not a marketing claim. Automated checks must be green and the manual evidence below must be attached to the release candidate before promotion.

## Automated evidence

- Android unit tests and lint pass.
- Physical-device instrumentation covers navigation, four-language flagship discovery, database migration, security contracts, OCR memory, and document processing.
- Android ABI debug APKs stay at or below 50 MiB; the universal debug APK stays at or below 80 MiB.
- PWA locale validation rejects missing keys and accidental English fallback values in Vietnamese, Simplified Chinese, and Spanish.
- PWA production chunks stay at or below 550 KiB and CSS stays at or below 160 KiB.
- PWA install manifest, maskable icon, old-cache cleanup, protected-route fallbacks, service worker, and prerendered routes pass.
- Only the Android launcher activity may be exported by PureHub; services, widget receivers, and providers remain private.

## Manual evidence still required

| Area | Evidence required before 9.5 |
| --- | --- |
| Signed upgrade | Install beta.55 or beta.56 with sample favorites, QR/OCR history, Wi-Fi history, lunar reminders, habits and expenses. Update with the candidate without clearing data and compare every record. |
| Accessibility | Record TalkBack traversal on Home, Settings and five flagships. Verify 200% font, 360dp width, landscape, 48dp targets and light/dark contrast. |
| Permissions | Deny camera, microphone, nearby Wi-Fi/location, notifications and screen capture; verify explanation, recovery action, Back and Home remain usable. |
| Reliability | Run a 60-minute foreground/background soak on a low-memory phone and review logcat for crashes, ANRs and repeated exceptions. |
| Performance | Record cold/warm start, peak memory and final signed APK/AAB size on the release candidate. Investigate regressions from the previous candidate. |
| PWA devices | Verify install/update/offline reload on Android Chrome and iOS Safari, including a real old-cache-to-new-cache update. |
| Play Console | Review the uploaded AAB permission list, Data safety, App content, content rating, target audience, ads and foreground-service declarations. Clear the pre-launch report. |
| Beta health | Review Android Vitals and tester feedback on days 1, 3, 7 and 14. No reproducible crash, ANR, data loss or blocked primary flow may remain open. |

## Promotion rule

Do not call the release 9.5-ready merely because CI is green. All manual evidence must be completed against the exact signed AAB submitted to the closed track.
