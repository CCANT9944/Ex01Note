[CmdletBinding()]
param(
    [string]$Serial
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Fail([string]$Message) {
    Write-Error $Message
    exit 1
}

function Test-AdbDeviceReady([string]$AdbPath, [string]$Serial) {
    $stateOutput = & $AdbPath -s $Serial get-state 2>&1 | Out-String
    if ($LASTEXITCODE -ne 0 -or ($stateOutput.Trim() -ne 'device')) {
        return $false
    }

    $probeOutput = & $AdbPath -s $Serial shell echo ok 2>&1 | Out-String
    return ($LASTEXITCODE -eq 0 -and ($probeOutput.Trim() -eq 'ok'))
}

function Resolve-JavaHome {
    if ($env:JAVA_HOME) {
        $javaExe = Join-Path $env:JAVA_HOME 'bin\java.exe'
        if (Test-Path $javaExe) {
            return $env:JAVA_HOME
        }
    }

    $candidates = @(
        (Join-Path $env:USERPROFILE '.gradle\jdks'),
        'C:\Program Files\Android\Android Studio\jbr',
        'C:\Program Files\JetBrains\Android Studio\jbr',
        'C:\Program Files\JetBrains\PyCharm 2025.3.2.1\jbr'
    )

    foreach ($candidateRoot in $candidates) {
        if (-not (Test-Path $candidateRoot)) { continue }

        if (Test-Path (Join-Path $candidateRoot 'bin\java.exe')) {
            return $candidateRoot
        }

        $nested = Get-ChildItem -Path $candidateRoot -Directory -ErrorAction SilentlyContinue |
            Where-Object { Test-Path (Join-Path $_.FullName 'bin\java.exe') } |
            Select-Object -First 1
        if ($nested) {
            return $nested.FullName
        }
    }

    Fail 'Could not locate a valid Java installation. Please install JDK 21 or fix JAVA_HOME.'
}

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$localPropertiesPath = Join-Path $projectRoot 'local.properties'
$gradleWrapperPath = Join-Path $projectRoot 'gradlew.bat'
$appBuildFilePath = Join-Path $projectRoot 'app\build.gradle.kts'
$manifestPath = Join-Path $projectRoot 'app\src\main\AndroidManifest.xml'
$apkPath = Join-Path $projectRoot 'app\build\outputs\apk\debug\app-debug.apk'

if (-not (Test-Path $localPropertiesPath)) {
    Fail "Could not find local.properties at '$localPropertiesPath'."
}

if (-not (Test-Path $gradleWrapperPath)) {
    Fail "Could not find gradlew.bat at '$gradleWrapperPath'."
}

if (-not (Test-Path $appBuildFilePath)) {
    Fail "Could not find app build file at '$appBuildFilePath'."
}

if (-not (Test-Path $manifestPath)) {
    Fail "Could not find AndroidManifest.xml at '$manifestPath'."
}

$localProperties = Get-Content -Path $localPropertiesPath
$sdkLine = $localProperties | Where-Object { $_ -match '^sdk\.dir=' } | Select-Object -First 1
if (-not $sdkLine) {
    Fail 'sdk.dir was not found in local.properties.'
}

$sdkDir = ($sdkLine -replace '^sdk\.dir=', '').Trim()
$sdkDir = $sdkDir.Replace('\:', ':').Replace('\\', '\')
$adbPath = Join-Path $sdkDir 'platform-tools\adb.exe'
if (-not (Test-Path $adbPath)) {
    Fail "adb.exe was not found at '$adbPath'. Install Android SDK Platform-Tools in Android Studio."
}

$appBuildText = Get-Content -Path $appBuildFilePath -Raw
$appIdMatch = [regex]::Match($appBuildText, 'applicationId\s*=\s*"([^"]+)"')
if (-not $appIdMatch.Success) {
    Fail 'Could not determine applicationId from app/build.gradle.kts.'
}
$appId = $appIdMatch.Groups[1].Value

$javaHome = Resolve-JavaHome
$env:JAVA_HOME = $javaHome
$env:PATH = (Join-Path $javaHome 'bin') + ';' + $env:PATH

$manifestText = Get-Content -Path $manifestPath -Raw
$activityMatch = [regex]::Match(
    $manifestText,
    '<activity\b[^>]*android:name="([^"]+)"[\s\S]*?<action\s+android:name="android.intent.action.MAIN"\s*/>[\s\S]*?<category\s+android:name="android.intent.category.LAUNCHER"\s*/>',
    [System.Text.RegularExpressions.RegexOptions]::IgnoreCase
)
if (-not $activityMatch.Success) {
    Fail 'Could not determine launcher activity from AndroidManifest.xml.'
}
$launcherActivity = $activityMatch.Groups[1].Value
$component = if ($launcherActivity.StartsWith('.')) { "$appId/$launcherActivity" } elseif ($launcherActivity.Contains('/')) { $launcherActivity } else { "$appId/$launcherActivity" }

Write-Host "Using SDK: $sdkDir"
Write-Host "Using ADB: $adbPath"
Write-Host "Using JAVA_HOME: $javaHome"
Write-Host "App: $appId"
Write-Host "Launcher: $component"

$null = & $adbPath start-server 2>$null

$adbOutput = & $adbPath devices -l
if ($LASTEXITCODE -ne 0) {
    Fail 'Failed to query devices with adb.'
}

$deviceLines = @($adbOutput | Select-Object -Skip 1 | Where-Object { $_.Trim() })
if (-not $deviceLines.Count) {
    Fail 'No Android devices detected. Connect a device or start an emulator.'
}

$knownStates = @('device', 'unauthorized', 'offline', 'bootloader', 'recovery', 'sideload', 'rescue', 'host')

$parsedDevices = foreach ($line in $deviceLines) {
    $parts = @($line -split '\s+' | Where-Object { $_ })
    if ($parts.Length -lt 2) { continue }

    $state = $null
    $stateIndex = -1
    foreach ($part in $parts | Select-Object -Skip 1) {
        $stateIndex++
        if ($knownStates -contains $part) {
            $state = $part
            $stateIndex++
            break
        }
    }

    if (-not $state) {
        # Fallback: adb devices -l normally places the state early in the line,
        # but wireless debugging may inject mDNS/TLS tokens before it.
        $state = 'unknown'
        $serial = $parts[0]
    } else {
        $serialTokenCount = $stateIndex
        $serial = ($parts | Select-Object -First $serialTokenCount) -join ' '
    }

    [pscustomobject]@{
        Serial = $serial
        State = $state
        Raw = $line
        Preferred = (($serial -notmatch '(_adb-tls|mdns|_tcp)') -and ($line -notmatch '(_adb-tls|mdns|_tcp)'))
    }
}

$healthyDevices = @($parsedDevices | Where-Object { $_.State -eq 'device' })
if (-not $healthyDevices.Count) {
    $states = ($parsedDevices | ForEach-Object { "{0} [{1}]" -f $_.Serial, $_.State }) -join ', '
    $hints = @()
    if ($parsedDevices | Where-Object { $_.State -eq 'unauthorized' }) {
        $hints += 'Unlock the phone and accept the USB debugging / wireless debugging authorization prompt.'
        $hints += 'If no prompt appears: on the phone go to Developer options > Revoke USB debugging authorizations, then reconnect and accept again.'
    }
    if ($parsedDevices | Where-Object { $_.State -eq 'offline' }) {
        $hints += 'Restart adb with: adb kill-server ; adb start-server, then reconnect the device.'
    }
    if ($parsedDevices | Where-Object { $_.Raw -match '(_adb-tls|mdns|_tcp)' }) {
        $hints += 'A wireless debugging endpoint was detected. If needed, disable/re-pair Wireless debugging or pass -Serial with a specific healthy device serial.'
    }

    $hintSuffix = if ($hints.Count) { "`n`nSuggested fixes:`n- " + ($hints -join "`n- ") } else { '' }
    Fail "No authorized online device found. Current adb states: $states$hintSuffix"
}

if ($Serial) {
    $selectedDevice = $healthyDevices | Where-Object { $_.Serial -eq $Serial } | Select-Object -First 1
    if (-not $selectedDevice) {
        $available = ($healthyDevices | ForEach-Object { $_.Serial }) -join ', '
        Fail "Requested serial '$Serial' is not available. Available healthy devices: $available"
    }
} else {
    $preferredDevices = @($healthyDevices | Where-Object { $_.Preferred })
    if ($preferredDevices.Count -eq 1) {
        $selectedDevice = $preferredDevices[0]
    } elseif ($preferredDevices.Count -gt 1) {
        $available = ($preferredDevices | ForEach-Object { $_.Serial }) -join ', '
        Fail "Multiple devices are connected. Re-run with -Serial. Available preferred devices: $available"
    } elseif ($healthyDevices.Count -eq 1) {
        $selectedDevice = $healthyDevices[0]
    } else {
        $available = ($healthyDevices | ForEach-Object { $_.Serial }) -join ', '
        Fail "Multiple devices are connected. Re-run with -Serial. Available devices: $available"
    }
}

Write-Host "Selected device: $($selectedDevice.Serial)"

Push-Location $projectRoot
try {
    Write-Host 'Checking selected device readiness...'
    $deviceReady = $false
    for ($attempt = 1; $attempt -le 10; $attempt++) {
        if (Test-AdbDeviceReady -AdbPath $adbPath -Serial $selectedDevice.Serial) {
            $deviceReady = $true
            break
        }

        Write-Host "Device not ready yet (attempt $attempt/10). Retrying..."
        Start-Sleep -Seconds 1
    }

    if (-not $deviceReady) {
        $latestDevices = (& $adbPath devices -l 2>&1 | Out-String).Trim()
        Fail "The selected device did not become command-ready.`n`nSelected: $($selectedDevice.Serial)`n`nLatest adb devices -l output:`n$latestDevices"
    }

    Write-Host 'Installing debug build...'
    & $gradleWrapperPath ':app:assembleDebug' '--no-daemon'
    if ($LASTEXITCODE -ne 0) {
        Fail 'Gradle assembleDebug failed.'
    }

    if (-not (Test-Path $apkPath)) {
        Fail "Expected debug APK was not found at '$apkPath'."
    }

    Write-Host 'Installing APK on selected device...'
    & $adbPath -s $selectedDevice.Serial install -r $apkPath
    if ($LASTEXITCODE -ne 0) {
        Fail 'ADB install failed.'
    }

    Write-Host 'Stopping previous app process...'
    & $adbPath -s $selectedDevice.Serial shell am force-stop $appId

    Write-Host 'Launching app...'
    & $adbPath -s $selectedDevice.Serial shell am start -W -n $component
    if ($LASTEXITCODE -ne 0) {
        Fail 'Failed to launch the app on the device.'
    }

    Start-Sleep -Seconds 2
    # Try to obtain the app PID. adb may sometimes close the connection and print 'error: closed'.
    # Retry several times with a short delay and surface diagnostics if it keeps failing.
    $appPid = ''
    $attempt = 0
    $maxAttempts = 5
    while ($attempt -lt $maxAttempts -and -not $appPid) {
        $attempt++
        Write-Host "Attempt $attempt to get PID of $appId..."
        $pidOutput = & $adbPath -s $selectedDevice.Serial shell pidof $appId 2>&1 | Out-String
        # Trim and sanitize
        $pidOutputTrimmed = $pidOutput.Trim()
        if ($LASTEXITCODE -eq 0 -and $pidOutputTrimmed -and -not ($pidOutputTrimmed -match '^error:')) {
            # pidof may return multiple PIDs; take first
            $appPid = ($pidOutputTrimmed -split '\s+')[0]
            break
        }

        Write-Warning "Could not obtain PID (adb output):`n$pidOutputTrimmed"
        if ($attempt -lt $maxAttempts) { Start-Sleep -Milliseconds 500 }
    }

    if (-not $appPid) {
        Write-Host "Warning: failed to determine app PID after $maxAttempts attempts. Proceeding without PID." -ForegroundColor Yellow
    }
} finally {
    Pop-Location
}

Write-Host ''
Write-Host 'Deploy complete.'
Write-Host "Device : $($selectedDevice.Serial)"
Write-Host "App    : $appId"
Write-Host "Launch : $component"
if ($appPid) {
    Write-Host "PID    : $appPid"
}

