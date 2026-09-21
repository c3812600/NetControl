# 鏈湴鎵撶鍚?Release APK锛堝皬绫冲晢搴楃瓑锛?# 鐢ㄦ硶锛氬湪 NetControl 鐩綍鎵ц  .\build-release.ps1
$ErrorActionPreference = "Stop"
$root = $PSScriptRoot
if (-not $root) { $root = (Get-Location).Path }
Set-Location $root

$propsPath = Join-Path $root "keystore.properties"
if (-not (Test-Path $propsPath)) {
    Write-Error "鏈壘鍒?keystore.properties锛堣涓?netcontrol-release.jks 涓€璧蜂繚绠★級"
    exit 1
}

$map = @{}
Get-Content $propsPath | ForEach-Object {
    $line = $_.Trim()
    if (-not $line -or $line.StartsWith("#")) { return }
    $i = $line.IndexOf("=")
    if ($i -lt 1) { return }
    $k = $line.Substring(0, $i).Trim()
    $v = $line.Substring($i + 1).Trim()
    $map[$k] = $v
}

$storeFile = $map["storeFile"]
if (-not $storeFile) { $storeFile = "app/release/netcontrol-release.jks" }
$storeFileAbs = if ([IO.Path]::IsPathRooted($storeFile)) { $storeFile } else { Join-Path $root $storeFile }

if (-not (Test-Path $storeFileAbs)) {
    Write-Error "鏈壘鍒?keystore 鏂囦欢: $storeFileAbs"
    exit 1
}

$env:JAVA_HOME = if (Test-Path "C:\Program Files\Java\jdk-17") { "C:\Program Files\Java\jdk-17" } else { $env:JAVA_HOME }
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
$env:RELEASE_STORE_FILE = $storeFileAbs
$env:RELEASE_STORE_PASSWORD = $map["storePassword"]
$env:RELEASE_KEY_ALIAS = if ($map["keyAlias"]) { $map["keyAlias"] } else { "netcontrol" }
$env:RELEASE_KEY_PASSWORD = if ($map["keyPassword"]) { $map["keyPassword"] } else { $map["storePassword"] }

Write-Host "JAVA_HOME=$env:JAVA_HOME"
Write-Host "RELEASE_STORE_FILE=$env:RELEASE_STORE_FILE"
Write-Host "keyAlias=$env:RELEASE_KEY_ALIAS"
Write-Host "寮€濮?assembleRelease ..."

& "$root\gradlew.bat" assembleRelease --no-daemon --console=plain
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$apk = Join-Path $root "app\build\outputs\apk\release\app-release.apk"
if (Test-Path $apk) {
    Write-Host ""
    Write-Host "绛惧悕 APK: $apk"
    & "$env:JAVA_HOME\bin\jarsigner.exe" -verify -verbose -certs $apk | Select-Object -Last 20
} else {
    Write-Error "鏈壘鍒颁骇鐗?$apk"
    exit 1
}
