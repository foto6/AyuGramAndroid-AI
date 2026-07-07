$ErrorActionPreference = "Stop"

$repo = Split-Path -Parent $MyInvocation.MyCommand.Path
$adb = "E:\AndroidToolchain\android-sdk\platform-tools\adb.exe"
$apk = Get-ChildItem "$repo\TMessagesProj\build\outputs\apk\afat\release\ayuGram-ai-universal-*.apk" |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1

if (-not $apk) {
    throw "Universal APK not found. Run .\build-ai-universal.ps1 first."
}

& $adb devices
& $adb install -r $apk.FullName
