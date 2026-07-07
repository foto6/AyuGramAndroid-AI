$ErrorActionPreference = "Stop"

$repo = Split-Path -Parent $MyInvocation.MyCommand.Path
$adb = "E:\AndroidToolchain\android-sdk\platform-tools\adb.exe"
$apk = Get-ChildItem "$repo\TMessagesProj\build\outputs\apk\beta\debug\*.apk" |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1

if (-not $apk) {
    throw "APK not found. Run .\build-ai-debug.ps1 first."
}

& $adb devices
& $adb install -r $apk.FullName
