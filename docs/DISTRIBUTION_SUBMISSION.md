# PureHub distribution submission

This dossier tracks the signed GitHub release, direct submission to the official F-Droid repository, and the future Google Play readiness gate.

## Current release facts

- App: PureHub
- Application ID: `com.purehub.app`
- Version: `1.0.0-beta.44`
- Version code: `45`
- License: MIT
- Source: <https://github.com/shoyrulove-dev/PureHub>
- Release: <https://github.com/shoyrulove-dev/PureHub/releases/tag/v1.0.0-beta.44>
- F-Droid universal APK: <https://github.com/shoyrulove-dev/PureHub/releases/download/v1.0.0-beta.44/PureHub-1.0.0-beta.44-fdroid.apk>
- Checksums: `SHA256SUMS.txt` on the same release
- Website: <https://hub.blissbiovn.com/en>
- Issue tracker: <https://github.com/shoyrulove-dev/PureHub/issues>

The `fdroid` flavor uses ZXing and Tesseract with bundled English, Vietnamese, and Simplified Chinese models. Its release manifest has no Internet permission, and its runtime graph contains no ML Kit, Google Play Services, Firebase, ads, analytics, or trackers.

## Official F-Droid: where to submit

There is no Play-Console-style registration form. The official F-Droid process is code review in GitLab:

**Submission status:** [`fdroiddata!45848`](https://gitlab.com/fdroid/fdroiddata/-/merge_requests/45848) is open for official F-Droid review. GitHub issue [#2](https://github.com/shoyrulove-dev/PureHub/issues/2) remains open until the app is merged and published.

1. Read the [F-Droid Inclusion Policy](https://f-droid.org/docs/Inclusion_Policy/).
2. Sign in to GitLab and fork [`fdroid/fdroiddata`](https://gitlab.com/fdroid/fdroiddata).
3. Create a branch such as `com.purehub.app` from `master`.
4. Copy this repository's candidate file [`fdroid/com.purehub.app.yml`](../fdroid/com.purehub.app.yml) to `metadata/com.purehub.app.yml` in the fork.
5. Run the official metadata checks and isolated build described in the [F-Droid Quick Start Guide](https://f-droid.org/docs/Submitting_to_F-Droid_Quick_Start_Guide/).
6. Commit with a clear new-app message, push the branch, and open a [merge request to `fdroiddata`](https://gitlab.com/fdroid/fdroiddata/-/merge_requests).
7. Keep the merge request updated until F-Droid's scanner, CI build, and reviewer checks pass.

If preparing a working metadata merge request is not yet possible, the secondary route is a Request For Packaging issue in the [F-Droid RFP queue](https://gitlab.com/fdroid/rfp/issues). GitLab hides issue creation controls until the user signs in, which is why no public form may appear.

Suggested merge-request title:

```text
New App: PureHub
```

Relevant current F-Droid categories include `Calculator`, `Finance Manager`, `OCR`, `Security`, and `System`; the submitted primary category is `System`.

Suggested summary:

```text
26 private, offline Android utilities with no ads, analytics, or trackers.
```

## Local verification

```text
Requirements: Git, JDK 17, and Android SDK API 36.

git clone https://github.com/shoyrulove-dev/PureHub.git
cd PureHub
git checkout v1.0.0-beta.44
./gradlew testFdroidDebugUnitTest lintFdroidDebug assembleFdroidRelease
./gradlew app:dependencies --configuration fdroidReleaseRuntimeClasspath > fdroid-dependencies.txt
! grep -E 'com\.google\.android\.gms|com\.google\.mlkit|com\.google\.firebase' fdroid-dependencies.txt
```

The release workflow repeats these checks, builds the F-Droid APKs, rejects the Android Internet permission, publishes SHA256 checksums, and creates build provenance.

## Google Play readiness gate

Google Play is not planned for immediate submission. The earliest review date is **October 16, 2026**, two months after the beta21 release work. Submission proceeds only when all of these are true:

- tester coverage is large enough for a meaningful staged rollout;
- crash, ANR, accessibility, privacy, and core workflow goals are complete;
- store listing, data-safety declarations, screenshots, support contact, and release notes are final;
- a production-signed build has passed the release checklist and physical-device verification.

Missing the target means the review is postponed; the date is not an automatic launch commitment.
