$ErrorActionPreference = "Stop"

$repo = Split-Path -Parent $MyInvocation.MyCommand.Path
$env:JAVA_HOME = "E:\AndroidToolchain\jdk\jdk-17.0.19+10"
$env:ANDROID_HOME = "E:\AndroidToolchain\android-sdk"
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
$env:ANDROID_USER_HOME = "E:\AndroidToolchain\android-home"
$env:GRADLE_USER_HOME = "E:\GradleCache"
$env:Path = "$env:JAVA_HOME\bin;$env:ANDROID_HOME\platform-tools;$env:Path"

Push-Location $repo
try {
    & "C:\Program Files\Git\bin\bash.exe" -lc './gradlew :TMessagesProj:assembleBetaDebug --stacktrace'
    Get-ChildItem "$repo\TMessagesProj\build\outputs\apk\beta\debug\*.apk" |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1 FullName, Length, LastWriteTime
} finally {
    Pop-Location
}
