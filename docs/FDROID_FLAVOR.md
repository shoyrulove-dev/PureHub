# PureHub F-Droid flavor

PureHub has two Android distribution flavors:

- `standard`: ML Kit OCR for the strongest recognition quality currently available in PureHub.
- `fdroid`: ZXing QR/barcode scanning and Tesseract OCR with bundled `eng`, `vie`, and `chi_sim` models.

The standard flavor is offline-first but retains the Android `INTERNET` permission for explicit connected workflows such as update checks and the beta minigame. The F-Droid flavor is strictly offline-only and removes `INTERNET`; it must also contain no ML Kit, Google Play Services, or Firebase dependency, including transitively.

## Verification

```bash
./gradlew testFdroidDebugUnitTest lintFdroidDebug assembleFdroidRelease
./gradlew app:dependencies --configuration fdroidReleaseRuntimeClasspath > fdroid-dependencies.txt
! grep -E 'com\.google\.android\.gms|com\.google\.mlkit|com\.google\.firebase' fdroid-dependencies.txt
```

The physical-device vision test runs an offline Tesseract recognition and a ZXing QR round trip:

```bash
./gradlew connectedFdroidDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.purehub.app.feature.FdroidVisionInstrumentedTest
```

## Source and licenses

- ZXing: Apache-2.0, <https://github.com/zxing/zxing>
- Tesseract: Apache-2.0, <https://github.com/tesseract-ocr/tesseract>
- Tesseract4Android: Apache-2.0, <https://github.com/adaptech-cz/Tesseract4Android>
- tessdata_fast: Apache-2.0, <https://github.com/tesseract-ocr/tessdata_fast>

The model notice is bundled at `app/src/fdroid/assets/tessdata/NOTICE.txt`. CI blocks a release if a prohibited Google runtime dependency enters the F-Droid graph.
