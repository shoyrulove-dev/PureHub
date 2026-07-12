# PureHub Release Polish

## Current State

- 5 bottom tabs are wired to live feature screens.
- `assembleDebug`, `testDebugUnitTest`, and `assembleRelease` should be the minimum pre-release checks.
- The app remains fully offline-first and intentionally omits `INTERNET`.

## Final Release Checklist

1. Verify `AndroidManifest.xml` still omits `android.permission.INTERNET`.
2. Run:
   - `.\gradlew.bat :app:assembleDebug`
   - `.\gradlew.bat :app:testDebugUnitTest`
   - `.\gradlew.bat :app:assembleDebugAndroidTest`
   - `.\gradlew.bat :app:assembleRelease`
3. Smoke test on-device:
   - Tab 1: lunar calendar, habit add/check, pomodoro sound, breath animation
   - Tab 2: compass, bubble level, mic meter, torch, converter
   - Tab 3: QR scan/generate, doc export, OCR, color grabber
   - Tab 4: cleaner scan, speaker tone, Wi-Fi card, vault save, wallpaper rotation
   - Tab 5: bill split, expense save/delete, decision wheel, community unlock
4. Replace placeholder Telegram bot username before shipping.
5. Set production `versionCode` and `versionName`.
6. Add signing config outside repo before generating store-ready AAB.

## Known Non-Blocking Warnings

- `EncryptedSharedPreferences` and `MasterKey` show deprecation warnings, but remain functional for local encrypted storage in the current implementation.
- `WifiManager.connectionInfo` and `calculateSignalLevel` are deprecated on newer APIs, but are still acceptable for the current lightweight analyzer.
- Some native libraries from CameraX/ML Kit cannot be stripped in debug or release; this is expected.
