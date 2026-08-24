# PureHub Flagship Premium report — 2026-08-25

## Release identity

- Android: `1.0.0-beta.31` (`versionCode 32`)
- PWA package: `1.0.0-beta.31`
- Android application ID: `com.purehub.app`

## Premium upgrades completed

### Wi-Fi Analyzer

- Quick and Pro modes keep the default experience approachable.
- Nearby scan depth increased to 32 access points.
- Adds 2.4/5/6 GHz channel mapping, observed channel pressure, strongest competing signal, security labels, and a cautious lowest-pressure recommendation.
- Adds an explicit privacy receipt: SSIDs, BSSIDs, signal history, and scan results remain on-device.

### Money Studio

- Adds this-month and all-time totals.
- Adds an optional local monthly budget with progress.
- Adds category totals, search, dated entries, receipt OCR handoff, and portable CSV export.
- The ledger remains local; no bank connection or advertising/analytics SDK receives entries.

### Password Vault

- Adds a local 0–100 health audit for weak and reused passwords.
- Adds a cryptographically secure password generator that guarantees upper/lower/digit/symbol coverage.
- Adds search and deliberate reveal/hide controls.
- Retains Android-backed encrypted storage, screenshot blocking, sensitive clipboard marking, and timed clipboard clearing.

### Deep Cleaner

- Adds Quick and Pro modes.
- Pro exposes large-file and exact byte-identical duplicate evidence; Quick keeps a simple scan-first path.
- Adds an explicit review/delete privacy receipt. Android remains the final deletion authority.

### Product-wide foundation

- Keeps the eight goal-based workspaces and related-tool navigation added in Flagship 4.0.
- Keeps QR/OCR advanced flows rather than duplicating controls: live/gallery scan, safety review, history/batch, document crop/filter, multipage OCR, local library, and TXT/PDF export.
- Version metadata is synchronized across Android, PWA, lockfile, and README.

## Verification completed

- Standard Android unit tests: passed.
- F-Droid Android unit tests: passed.
- Standard debug lint: passed.
- F-Droid debug lint: passed.
- Standard release R8 build: passed.
- F-Droid release R8 build: passed.
- PWA lint, TypeScript build, Vite production build, service worker generation, and 10 prerendered SEO route checks: passed.

New unit coverage includes Wi-Fi channel/security insights, expense totals/CSV escaping, and password generation/health scoring.

## Device installation status

The connected `M1906G7G` currently has only the official `com.purehub.app` build `1.0.0-beta.29` (`versionCode 30`). Its release certificate SHA-256 is:

`323050a93655afe2d9ee572467a4dc3fbb1a862c8300019ead67554931592a5a`

Local beta.31 release APKs are intentionally unsigned because the release keystore is stored only in GitHub Actions secrets. They cannot safely update the official installation. Installing a debug flavor would create a second icon; uninstalling beta.29 would risk local data. Neither action was taken.

The safe completion path is: commit/push, create the `v1.0.0-beta.31` release through the signed Android Release workflow, verify its certificate matches the installed app, then run `adb install -r` with the signed arm64 APK.
