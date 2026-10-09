param(
    [string]$SigningDirectory = (Join-Path $env:LOCALAPPDATA 'jk-bms-usb/signing'),
    [switch]$SkipBuild
)

$ErrorActionPreference = 'Stop'
$repositoryRoot = Split-Path $PSScriptRoot -Parent
$projectDirectory = Join-Path $repositoryRoot 'JkBmsApp'
$keystore = Join-Path $SigningDirectory 'tester-release.jks'
$passwordFile = Join-Path $SigningDirectory 'tester-release.password.dpapi'
$keyAlias = 'jk-bms-tester-release'
$versionName = '1.1.0-preview.1'
$outputDirectory = Join-Path $repositoryRoot "dist/$versionName"

function Invoke-Checked {
    param([string]$Program, [string[]]$Arguments)
    & $Program @Arguments
    if ($LASTEXITCODE -ne 0) { throw "$Program failed with exit code $LASTEXITCODE" }
}

if (-not $env:JAVA_HOME -or -not (Test-Path (Join-Path $env:JAVA_HOME 'bin/keytool.exe'))) {
    throw 'Set JAVA_HOME to a complete JDK 21 before running this script.'
}
$sdkDirectory = $env:ANDROID_HOME
if (-not $sdkDirectory) { $sdkDirectory = $env:ANDROID_SDK_ROOT }
if (-not $sdkDirectory) { $sdkDirectory = Join-Path $env:LOCALAPPDATA 'Android/Sdk' }
$apksigner = Join-Path $sdkDirectory 'build-tools/36.0.0/apksigner.bat'
if (-not (Test-Path $apksigner)) { throw "Android SDK build-tools 36.0.0 are required: $apksigner" }

if (-not $SkipBuild) {
    Push-Location $projectDirectory
    try { Invoke-Checked (Join-Path $projectDirectory 'gradlew.bat') @('assembleRelease', '--console=plain') }
    finally { Pop-Location }
}

$unsignedApk = Join-Path $projectDirectory 'app/build/outputs/apk/release/app-release-unsigned.apk'
if (-not (Test-Path $unsignedApk)) { throw "Missing unsigned release APK: $unsignedApk" }
$apkMetadata = Get-Content -LiteralPath (Join-Path (Split-Path $unsignedApk -Parent) 'output-metadata.json') -Raw | ConvertFrom-Json
if ($apkMetadata.elements[0].versionName -ne $versionName -or $apkMetadata.elements[0].versionCode -ne 2) {
    throw 'APK version differs from the packaging script. Rebuild the release and keep both version declarations in sync.'
}
if ((Test-Path $keystore) -ne (Test-Path $passwordFile)) {
    throw 'Signing key/password pair is incomplete. Restore it before building an update; do not replace the key.'
}
New-Item -ItemType Directory -Path $SigningDirectory -Force | Out-Null
New-Item -ItemType Directory -Path $outputDirectory -Force | Out-Null

$previousPassword = $env:JK_BMS_SIGNING_PASSWORD
$securePassword = $null
$passwordPointer = [IntPtr]::Zero
try {
    if (Test-Path $passwordFile) {
        $securePassword = Get-Content -LiteralPath $passwordFile -Raw | ConvertTo-SecureString
    } else {
        $randomBytes = New-Object byte[] 32
        $randomGenerator = [Security.Cryptography.RandomNumberGenerator]::Create()
        try { $randomGenerator.GetBytes($randomBytes) } finally { $randomGenerator.Dispose() }
        $securePassword = ConvertTo-SecureString ([Convert]::ToBase64String($randomBytes)) -AsPlainText -Force
        $securePassword | ConvertFrom-SecureString | Set-Content -LiteralPath $passwordFile -Encoding ASCII
    }
    $passwordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
    $env:JK_BMS_SIGNING_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($passwordPointer)
    if (-not (Test-Path $keystore)) {
        Invoke-Checked (Join-Path $env:JAVA_HOME 'bin/keytool.exe') @(
            '-genkeypair', '-keystore', $keystore, '-storetype', 'JKS', '-alias', $keyAlias,
            '-keyalg', 'RSA', '-keysize', '3072', '-validity', '10000',
            '-dname', 'CN=JK-BMS USB Tester Release',
            '-storepass:env', 'JK_BMS_SIGNING_PASSWORD', '-keypass:env', 'JK_BMS_SIGNING_PASSWORD'
        )
    }
    $apkName = "jk-bms-usb-$versionName.apk"
    $signedApk = Join-Path $outputDirectory $apkName
    Invoke-Checked $apksigner @(
        'sign', '--ks', $keystore, '--ks-key-alias', $keyAlias,
        '--ks-pass', 'env:JK_BMS_SIGNING_PASSWORD', '--key-pass', 'env:JK_BMS_SIGNING_PASSWORD',
        '--out', $signedApk, $unsignedApk
    )
    $verification = & $apksigner verify --verbose --print-certs $signedApk 2>&1
    if ($LASTEXITCODE -ne 0) { throw 'Signed APK verification failed.' }
    $verification | Set-Content -LiteralPath (Join-Path $outputDirectory 'signature-verification.txt') -Encoding UTF8
    $verification | Select-Object -First 12
    $sha256 = (Get-FileHash -LiteralPath $signedApk -Algorithm SHA256).Hash.ToLowerInvariant()
    "$sha256  $apkName" | Set-Content -LiteralPath (Join-Path $outputDirectory 'SHA256SUMS.txt') -Encoding ASCII
    Copy-Item -LiteralPath (Join-Path $repositoryRoot 'docs/tester-release.md') -Destination (Join-Path $outputDirectory 'TESTING.md')
    Copy-Item -LiteralPath (Join-Path $projectDirectory 'app/src/test/resources/fixtures/demo-runtime-capture.json') -Destination $outputDirectory
    $gitProgram = (Get-Command git -ErrorAction Stop).Source
    $commit = & $gitProgram -C $repositoryRoot rev-parse HEAD
    if ($LASTEXITCODE -ne 0) { throw 'Cannot determine source commit.' }
    [ordered]@{
        versionName = $versionName
        versionCode = 2
        commit = $commit.Trim()
        builtAtUtc = [DateTime]::UtcNow.ToString('o')
        apk = $apkName
        sha256 = $sha256
        configurationWritesEnabled = $false
    } | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $outputDirectory 'release.json') -Encoding UTF8
    $archive = Join-Path (Split-Path $outputDirectory -Parent) "jk-bms-usb-$versionName-testing.zip"
    Compress-Archive -Path (Join-Path $outputDirectory '*') -DestinationPath $archive -Force
    Write-Output "APK: $signedApk"
    Write-Output "Testing bundle: $archive"
    Write-Output "Signing key (private, retain for updates): $keystore"
} finally {
    $env:JK_BMS_SIGNING_PASSWORD = $previousPassword
    if ($passwordPointer -ne [IntPtr]::Zero) { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordPointer) }
    if ($securePassword) { $securePassword.Dispose() }
}
