param(
    [switch]$SetupAndroidSDK
)

Write-Output "=== Triads Sound - APK Builder ==="
Write-Output ""

# Check Java
try {
    $javaVer = (java -version 2>&1 | Out-String)
    if ($javaVer -match '"(\d+)\.') {
        $major = $Matches[1]
        if ($major -ge 17) {
            Write-Output "[OK] Java $major.x found"
        } else {
            Write-Output "[FAIL] Need Java 17+, found Java $major.x"
            Write-Output "       Download: https://adoptium.net/"
            exit 1
        }
    } else {
        Write-Output "[FAIL] Java not found"
        exit 1
    }
} catch {
    Write-Output "[FAIL] Java not found"
    Write-Output "       Download: https://adoptium.net/"
    exit 1
}

# Check ANDROID_HOME
if (Test-Path $env:ANDROID_HOME) {
    $sdk = $env:ANDROID_HOME
    Write-Output "[OK] ANDROID_HOME = $sdk"
} elseif (Test-Path "$env:LOCALAPPDATA\Android\Sdk") {
    $sdk = "$env:LOCALAPPDATA\Android\Sdk"
    $env:ANDROID_HOME = $sdk
    [Environment]::SetEnvironmentVariable("ANDROID_HOME", $sdk, "Process")
    Write-Output "[OK] Found Android SDK at $sdk"
} else {
    if ($SetupAndroidSDK) {
        Write-Output "[...] Downloading Android SDK command line tools..."
        $toolsUrl = "https://dl.google.com/android/repository/commandlinetools-win-10406996_latest.zip"
        $toolsZip = "$env:TEMP\cmdline-tools.zip"
        Invoke-WebRequest -Uri $toolsUrl -OutFile $toolsZip
        Expand-Archive -Path $toolsZip -DestinationPath "$env:LOCALAPPDATA\Android\Sdk"
        $sdk = "$env:LOCALAPPDATA\Android\Sdk"
        New-Item -ItemType Directory -Path "$sdk\cmdline-tools\latest" -Force
        Move-Item -Path "$sdk\cmdline-tools\*" -Destination "$sdk\cmdline-tools\latest\" -ErrorAction SilentlyContinue
        $env:ANDROID_HOME = $sdk
        Write-Output "[OK] Android SDK installed at $sdk"
        Write-Output "[...] Accepting licenses..."
        & "$sdk\cmdline-tools\latest\bin\sdkmanager.bat" --licenses 2>&1 | Out-Null
        Write-Output "[...] Installing platform Android 34..."
        & "$sdk\cmdline-tools\latest\bin\sdkmanager.bat" "platforms;android-34" "build-tools;34.0.0" 2>&1 | Out-Null
    } else {
        Write-Output "[FAIL] Android SDK not found"
        Write-Output "       Install Android Studio: https://developer.android.com/studio"
        Write-Output "       Or run: .\build-apk.ps1 -SetupAndroidSDK"
        exit 1
    }
}

# Check local.properties
if (-not (Test-Path "local.properties")) {
    Set-Content -Path "local.properties" -Value "sdk.dir=$([regex]::Escape($sdk))"
    Write-Output "[OK] Created local.properties"
}

# Build APK
Write-Output ""
Write-Output "=== Building APK ==="
if (Test-Path "gradlew.bat") {
    & .\gradlew.bat assembleDebug
} else {
    & gradle assembleDebug
}

if ($LASTEXITCODE -eq 0) {
    $apk = "app\build\outputs\apk\debug\app-debug.apk"
    if (Test-Path $apk) {
        Write-Output ""
        Write-Output "=== SUCCESS ==="
        Write-Output "APK: $((Get-Item $apk).FullName)"
        Write-Output "Size: $((Get-Item $apk).Length / 1KB) KB"
    }
} else {
    Write-Output "[FAIL] Build failed"
    exit 1
}
