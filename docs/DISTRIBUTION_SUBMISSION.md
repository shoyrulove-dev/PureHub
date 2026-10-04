# PureHub distribution submission

This dossier tracks signed GitHub releases and Google Play readiness. F-Droid publishing is discontinued.

## Current release facts

- App: PureHub
- Application ID: `com.purehub.app`
- Version: `1.0.0-beta.54`
- Version code: `54`
- License: MIT
- Source: <https://github.com/shoyrulove-dev/PureHub>
- Release candidate: `v1.0.0-beta.54`
- Distribution assets: lightweight signed ARM64 APK, Play-ready AAB, checksums, and provenance
- F-Droid artifacts: discontinued and intentionally excluded from current releases
- Checksums: `SHA256SUMS.txt` on the same release
- Website: <https://hub.blissbiovn.com/en>
- Issue tracker: <https://github.com/shoyrulove-dev/PureHub/issues>

The legacy `fdroid` flavor remains in source only to reproduce older releases. It is not part of current CI, release assets, store metadata, or future distribution plans.

## Retired F-Droid submission

The historical [`fdroiddata!45848`](https://gitlab.com/fdroid/fdroiddata/-/merge_requests/45848) submission was never merged and no official PureHub package page was published. GitHub issue [#2](https://github.com/shoyrulove-dev/PureHub/issues/2) is closed. Do not update or resubmit the F-Droid metadata; the owner should close the remaining GitLab merge request when signed in.

The active release workflow verifies the standard Android build, publishes one lightweight ARM64 APK plus the Play-ready AAB, writes SHA256 checksums, and creates build provenance. It does not build or publish F-Droid artifacts.

## Google Play readiness gate

PureHub is preparing a closed Google Play test. Upload to the closed track proceeds only when all of these are true:

- tester coverage is large enough for a meaningful staged rollout;
- crash, ANR, accessibility, privacy, and core workflow goals are complete;
- store listing, data-safety declarations, screenshots, support contact, and release notes are final;
- a production-signed build has passed the release checklist and physical-device verification.

Missing the target means the review is postponed; the date is not an automatic launch commitment.
