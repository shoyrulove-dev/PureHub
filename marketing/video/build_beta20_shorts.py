from __future__ import annotations

from pathlib import Path

import build_beta19_shorts as renderer


ROOT = Path(__file__).resolve().parent
renderer.BUILD_DIR = ROOT / "build" / "beta20-shorts"
renderer.OUTPUT_DIR = ROOT / "output" / "beta20-shorts"
renderer.SHORTS = (
    (
        "beta20-authenticator-vault.mp4",
        "Keep 2FA codes offline",
        "Encrypted vault + device lock + local TOTP",
        "Offline Authenticator With Device Lock | PureHub #Shorts",
        "Authenticator Vault generates time-based 2FA codes locally and protects the encrypted vault with your device lock.",
    ),
    (
        "beta20-file-studio.mp4",
        "Archive files without uploading them",
        "ZIP + SHA-256 + local sharing",
        "Private ZIP and File Hashing on Android | PureHub #Shorts",
        "File Studio creates ZIP archives, verifies SHA-256 hashes, and shares files without sending them to a PureHub server.",
    ),
    (
        "beta20-screen-recorder.mp4",
        "Record your screen with clear consent",
        "Android capture prompt + local MP4 + no account",
        "Local Screen Recording With No Account | PureHub #Shorts",
        "Screen Recorder uses Android's system capture consent, records a local MP4, and never requires a PureHub account.",
    ),
    (
        "beta20-storage-privacy.mp4",
        "Clean photo metadata before sharing",
        "Fresh local copy + no EXIF or GPS",
        "Remove Photo Metadata Before Sharing | PureHub #Shorts",
        "Photo Privacy creates a fresh local JPEG without copied EXIF or GPS metadata, while Storage Insight finds exact duplicates by hash.",
    ),
)


if __name__ == "__main__":
    renderer.main()
