# PureHub FOSS distribution submission

This dossier is the source of truth for tracking PureHub from a signed GitHub release to IzzyOnDroid and then to the official F-Droid repository.

## Recommended order

1. Keep the signed F-Droid APK, source tag, checksums, changelog, and screenshots public on GitHub.
2. Request inclusion in the IzzyOnDroid repository at <https://codeberg.org/IzzyOnDroid/repodata/issues/new>.
3. Record the IzzyOnDroid scanner report and resolve every actionable finding.
4. Once the FOSS build is clean and reproducible from a release tag, submit metadata directly to `fdroiddata`, or open an RFP at <https://gitlab.com/fdroid/rfp/-/issues/new>.
5. Track build logs and reviewer feedback until the package is listed.

Command Center stores these stages under **Release & system operations → FOSS distribution**. A submission is not marked as listed until a public package URL exists.

## Current release facts

- App: PureHub
- Application ID: `com.purehub.app`
- Version: `1.0.0-beta.17`
- Version code: `18`
- License: MIT
- Source: <https://github.com/shoyrulove-dev/PureHub>
- Release: <https://github.com/shoyrulove-dev/PureHub/releases/tag/v1.0.0-beta.17>
- F-Droid universal APK: <https://github.com/shoyrulove-dev/PureHub/releases/download/v1.0.0-beta.17/PureHub-1.0.0-beta.17-fdroid.apk>
- F-Droid arm64 APK: <https://github.com/shoyrulove-dev/PureHub/releases/download/v1.0.0-beta.17/PureHub-1.0.0-beta.17-fdroid-arm64-v8a.apk>
- Universal APK SHA-256: `3193981a5a2325d6108c1836928ce97d879d962b89ed1a9e0c18bbeec5eae396`
- Website: <https://hub.blissbiovn.com/en>
- Issue tracker: <https://github.com/shoyrulove-dev/PureHub/issues>

The `fdroid` flavor uses ZXing and Tesseract with bundled English, Vietnamese, and Simplified Chinese models. Its release manifest has no Internet permission, and its runtime graph contains no ML Kit, Google Play Services, Firebase, ads, analytics, or trackers.

## IzzyOnDroid App Inclusion Request form

Use the required **App Inclusion Request** form at:

<https://codeberg.org/IzzyOnDroid/repodata/issues/new/choose>

Do not open a blank issue. IzzyOnDroid closes requests that do not use this form. Fill the form with the source URL, MIT license, categories, summary, description, CLI build instructions, and an accurate AI-assistance disclosure. PureHub has used OpenAI Codex throughout architecture, implementation, UI work, debugging, tests, documentation, and release automation; select `Dominant` if most submitted code/content was AI-generated. Only check the human-review and manual-verification accountability boxes when they are fully true.

Suggested categories: `Office`, `System`, `Time`, `Money`, and `Security`.

Suggested summary:

```text
22 private, offline Android utilities with no ads, analytics, or trackers.
```

Suggested build instructions:

```text
Requirements: Git, JDK 17, and Android SDK API 36.

git clone https://github.com/shoyrulove-dev/PureHub.git
cd PureHub
git checkout v1.0.0-beta.17
./gradlew testFdroidDebugUnitTest lintFdroidDebug assembleFdroidRelease
./gradlew app:dependencies --configuration fdroidReleaseRuntimeClasspath > fdroid-dependencies.txt
! grep -E 'com\.google\.android\.gms|com\.google\.mlkit|com\.google\.firebase' fdroid-dependencies.txt

The APK is generated under app/build/outputs/apk/fdroid/release/.
```

### Previous free-form request

Issue #445 was closed because it did not use the required form. Keep it only as historical context; the replacement issue URL becomes the tracked IzzyOnDroid request.

### Legacy copy-ready request

Suggested title:

```text
Add PureHub (com.purehub.app)
```

Suggested body:

```text
App name: PureHub
Package ID: com.purehub.app
License: MIT
Source code: https://github.com/shoyrulove-dev/PureHub
Latest release: https://github.com/shoyrulove-dev/PureHub/releases/tag/v1.0.0-beta.17
APK: https://github.com/shoyrulove-dev/PureHub/releases/download/v1.0.0-beta.17/PureHub-1.0.0-beta.17-fdroid.apk

PureHub is a free, ad-free, offline-first collection of 22 Android utilities for scanning, OCR/PDF workflows, focus, measurement, finance, audio care, and privacy.

Please use the APK whose filename ends in -fdroid.apk. This flavor uses ZXing and Tesseract, has no INTERNET permission, and has no ML Kit, Google Play Services, Firebase, ads, analytics, or trackers. English, Vietnamese, and Simplified Chinese OCR models are bundled for offline use.

The universal APK SHA-256 is:
3193981a5a2325d6108c1836928ce97d879d962b89ed1a9e0c18bbeec5eae396

FOSS flavor details and verification commands:
https://github.com/shoyrulove-dev/PureHub/blob/main/docs/FDROID_FLAVOR.md

Fastlane metadata and screenshots are stored in the source repository. I am the upstream developer and consent to inclusion.
```

After creating the issue, paste its URL into Command Center and set **IzzyOnDroid request** to `Submitted`.

## F-Droid submission note

Do not submit the current candidate to official F-Droid merely because IzzyOnDroid accepts the signed APK. Official F-Droid builds from source and may require changes that a binary repository does not. Resolve Izzy scanner findings first, verify a clean build in the F-Droid build environment, and tag a source release containing all metadata and build-hardening changes.

A candidate metadata file is maintained at `fdroid/com.purehub.app.yml`. It is a starting point for review, not evidence of acceptance or listing.
