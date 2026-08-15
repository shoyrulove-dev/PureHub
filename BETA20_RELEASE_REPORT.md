# PureHub 1.0.0-beta.20 release report

Date: 2026-08-15  
Catalog: 25 mini-app experiences  
Platforms: PWA and native Android

## Five-phase update

### 1. Secure identity

- Added Authenticator Vault on web and Android.
- Generates standard time-based one-time passwords locally.
- Web secrets are encrypted with AES-GCM and a PBKDF2-derived key.
- Android storage uses the platform keystore and encrypted preferences, with device-credential unlock and screenshot blocking.

### 2. Storage and photo privacy

- Added exact duplicate detection using SHA-256 rather than filename guesses.
- Added Photo Privacy export, which creates a fresh JPEG without copying EXIF or GPS metadata.
- Kept review-first behavior: PureHub does not silently delete user files.

### 3. Private file workflows

- Added File Studio on web and Android.
- Supports local ZIP creation, ZIP extraction on web, SHA-256 verification, downloads, and system sharing.
- File content is not uploaded to a PureHub server.

### 4. Creation and visual intelligence

- Added Screen Recorder with the operating system's explicit capture-consent flow.
- Android saves a local MP4 through a foreground service; the web version records a local WebM when browser support is available.
- Retained OCR Studio quick actions for detected URLs, email addresses, and phone numbers.

### 5. Global product polish and distribution

- Expanded catalog, localized labels, manifest, structured SEO, social copy, and public descriptions from 22 to 25 tools.
- Added a compact TikTok desktop-upload handoff in Command Center with caption copy and TikTok Studio access.
- Captured four real-device demos and prepared a YouTube Shorts queue.
- Kept VPN, auto-clicker, and universal TV remote ideas research-gated because they require permissions, infrastructure, or hardware coverage that conflict with PureHub's current privacy and reliability standard.

## Verification

- PWA lint: passed.
- PWA production build and prerender: passed.
- Backend tests: 52 passed, including 4 subtests.
- Android standard debug unit tests and APK assembly: passed.
- Android physical-device install: passed on Redmi Note 8 Pro (`begonia`), Android package version code 21.
- Native smoke tests: Authenticator Vault, File Studio, Screen Recorder, Storage Insight, and Photo Privacy opened without a fatal runtime exception.

## Video queue

Four English-first, 13.4-second vertical demos are prepared for Authenticator Vault, File Studio, Screen Recorder, and Photo Privacy. They are scheduled after the existing YouTube queue to avoid duplicate publishing slots.

## Known boundaries

- Screen recording always requires Android/browser consent; PureHub cannot and should not bypass it.
- A browser cannot preselect a local file inside TikTok's website. Command Center opens TikTok Studio and copies the approved caption; the operator selects the prepared video and confirms the final post.
- Authenticator exports and account recovery remain intentionally conservative until a dedicated encrypted backup UX is reviewed.
