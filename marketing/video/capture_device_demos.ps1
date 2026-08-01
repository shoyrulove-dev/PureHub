param(
    [string]$OutputDirectory = "marketing/video/raw",
    [int]$DurationSeconds = 9,
    [switch]$Resume
)

$ErrorActionPreference = "Stop"
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "../..")).Path
$scrcpy = Join-Path $repoRoot ".tools/scrcpy/scrcpy-win64-v3.3.3/scrcpy.exe"

if (-not (Test-Path -LiteralPath $scrcpy)) {
    throw "scrcpy is missing at $scrcpy"
}

$outputPath = Join-Path $repoRoot $OutputDirectory
New-Item -ItemType Directory -Force -Path $outputPath | Out-Null

function Invoke-Adb {
    param([Parameter(ValueFromRemainingArguments = $true)][string[]]$Arguments)
    & adb @Arguments | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "ADB failed: $($Arguments -join ' ')" }
}

function Get-UiNodes {
    Invoke-Adb shell uiautomator dump /sdcard/purehub-capture-ui.xml
    Invoke-Adb pull /sdcard/purehub-capture-ui.xml (Join-Path $env:TEMP "purehub-capture-ui.xml")
    $document = [xml](Get-Content -Raw -LiteralPath (Join-Path $env:TEMP "purehub-capture-ui.xml"))
    return $document.SelectNodes("//node")
}

function Get-NodeCenter {
    param([string]$Text)
    $node = Get-UiNodes | Where-Object { $_.text -eq $Text } | Select-Object -First 1
    if (-not $node) { return $null }
    $matches = [regex]::Matches([string]$node.bounds, "\d+")
    if ($matches.Count -ne 4) { return $null }
    return @(
        [int](([int]$matches[0].Value + [int]$matches[2].Value) / 2),
        [int](([int]$matches[1].Value + [int]$matches[3].Value) / 2)
    )
}

function Tap-Text {
    param([string]$Text, [switch]$Optional)
    $center = Get-NodeCenter -Text $Text
    if (-not $center) {
        if ($Optional) { return $false }
        throw "Could not find UI text: $Text"
    }
    Invoke-Adb shell input tap $center[0] $center[1]
    return $true
}

function Open-Tool {
    param([string]$Title)
    Invoke-Adb shell am force-stop com.purehub.app
    & adb shell monkey -p com.purehub.app -c android.intent.category.LAUNCHER 1 | Out-Null
    Start-Sleep -Milliseconds 900
    Invoke-Adb shell input tap 400 2200
    Start-Sleep -Milliseconds 650

    for ($attempt = 0; $attempt -lt 12; $attempt++) {
        $center = Get-NodeCenter -Text $Title
        if ($center) {
            Invoke-Adb shell input tap $center[0] $center[1]
            Start-Sleep -Milliseconds 900
            return
        }
        Invoke-Adb shell input swipe 850 1910 850 760 350
        Start-Sleep -Milliseconds 350
    }
    throw "Could not locate miniapp: $Title"
}

$demos = @(
    @{ File = "01-pomodoro.mp4"; Tool = "Zen Pomodoro"; Actions = @("Start", "Cafe") },
    @{ File = "02-zen-breath.mp4"; Tool = "Zen Breath"; Actions = @("Start") },
    @{ File = "03-compass.mp4"; Tool = "Compass"; Actions = @() },
    @{ File = "04-bubble-level.mp4"; Tool = "Bubble Level & Ruler"; Actions = @() },
    @{ File = "05-unit-converter.mp4"; Tool = "Unit Converter"; Actions = @() },
    @{ File = "06-qr-studio.mp4"; Tool = "QR Studio"; Actions = @() },
    @{ File = "07-expense-tracker.mp4"; Tool = "Expense Tracker"; Actions = @() },
    @{ File = "08-password-vault.mp4"; Tool = "Password Vault"; Actions = @() },
    @{ File = "09-bill-splitter.mp4"; Tool = "Bill Splitter"; Actions = @() },
    @{ File = "10-decision-wheel.mp4"; Tool = "Decision Wheel"; Actions = @("Spin") }
)

foreach ($demo in $demos) {
    $target = Join-Path $outputPath $demo.File
    if ($Resume -and (Test-Path -LiteralPath $target)) {
        Write-Host "Keeping existing $target"
        continue
    }
    Write-Host "Opening $($demo.Tool)..."
    Open-Tool -Title $demo.Tool
    if (Test-Path -LiteralPath $target) { Remove-Item -LiteralPath $target }
    $arguments = @(
        "--no-playback", "--no-audio", "--record=$target",
        "--time-limit=$DurationSeconds", "--max-fps=30", "--video-bit-rate=12M"
    )
    $recorder = Start-Process -FilePath $scrcpy -ArgumentList $arguments -WindowStyle Hidden -PassThru
    Start-Sleep -Milliseconds 1100
    foreach ($action in $demo.Actions) {
        [void](Tap-Text -Text $action -Optional)
        Start-Sleep -Seconds 2
    }
    $recorder.WaitForExit()
    if ($recorder.ExitCode -ne 0 -or -not (Test-Path -LiteralPath $target)) {
        throw "Recording failed for $($demo.Tool)"
    }
    Write-Host "Recorded $target"
}

Write-Host "Captured $($demos.Count) PureHub miniapp demos."
