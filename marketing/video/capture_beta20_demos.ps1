param(
    [string]$OutputDirectory = "marketing/video/raw",
    [int]$DurationSeconds = 9,
    [string]$PackageName = "com.purehub.app"
)

$ErrorActionPreference = "Stop"
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "../..")).Path
$scrcpy = Join-Path $repoRoot ".tools/scrcpy/scrcpy-win64-v3.3.3/scrcpy.exe"
$outputPath = Join-Path $repoRoot $OutputDirectory

if (-not (Test-Path -LiteralPath $scrcpy)) {
    throw "scrcpy is missing at $scrcpy"
}

New-Item -ItemType Directory -Force -Path $outputPath | Out-Null

$demos = @(
    @{ File = "beta20-authenticator-vault.mp4"; Id = "AUTHENTICATOR_VAULT" },
    @{ File = "beta20-file-studio.mp4"; Id = "FILE_STUDIO" },
    @{ File = "beta20-screen-recorder.mp4"; Id = "SCREEN_RECORDER" },
    @{ File = "beta20-storage-privacy.mp4"; Id = "DEEP_CLEANER" }
)

foreach ($demo in $demos) {
    & adb shell am force-stop $PackageName | Out-Null
    & adb shell am start -W -n "$PackageName/.MainActivity" --es purehub.mini_app_id $demo.Id | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "Could not open $($demo.Id)" }
    Start-Sleep -Milliseconds 1400

    $target = Join-Path $outputPath $demo.File
    $arguments = @(
        "--no-playback", "--no-audio", "--record=$target",
        "--time-limit=$DurationSeconds", "--max-fps=30", "--video-bit-rate=12M"
    )
    $recorder = Start-Process -FilePath $scrcpy -ArgumentList $arguments -WindowStyle Hidden -PassThru
    Start-Sleep -Milliseconds 1600
    & adb shell input swipe 820 1700 820 1050 650 | Out-Null
    Start-Sleep -Milliseconds 1300
    & adb shell input swipe 820 1050 820 1600 600 | Out-Null
    $recorder.WaitForExit()

    if ($recorder.ExitCode -ne 0 -or -not (Test-Path -LiteralPath $target)) {
        throw "Recording failed for $($demo.Id)"
    }
    Write-Host "Recorded $target"
}
