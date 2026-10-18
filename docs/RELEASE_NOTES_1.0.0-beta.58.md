# PureHub 1.0.0-beta.58

PureHub beta.58 hardens the refreshed Android and PWA experience for Google Play closed testing.

## Accessibility and layout

- Improved Home recommendations at large Android font scales so labels remain readable.
- Kept bottom navigation compact at large font scales while preserving localized TalkBack descriptions.
- Localized Android launcher shortcuts in English, Vietnamese, Simplified Chinese, and Spanish.

## Release quality gates

- Added Android instrumentation coverage for localized primary navigation in all four languages.
- Added APK size budgets for ABI-specific and universal debug builds.
- Added PWA checks for untranslated English copy and JavaScript/CSS bundle growth.
- Added a closed-beta readiness checklist covering signed upgrades, permissions, accessibility, cache updates, Play Console review, and tester health.

## Verification

- Android unit tests and lint passed.
- Fourteen physical-device Android instrumentation tests passed on an ARM64 phone.
- PWA localization, lint, production build, prerender, service-worker, install-manifest, and bundle-budget checks passed.
