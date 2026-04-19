param(
    [string]$AppVersion = "1.0.0"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$rootDir = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$runtimeDir = Join-Path $rootDir "build\runtime"
$packageInputDir = Join-Path $rootDir "build\package-input"
$distDir = Join-Path $rootDir "dist"
$jarPath = Join-Path $distDir "javakava.jar"
$zipPath = Join-Path $distDir "JavaKava-windows-app-image.zip"

& (Join-Path $PSScriptRoot "build-jar.ps1")

Remove-Item -Recurse -Force (Join-Path $distDir "JavaKava") -ErrorAction SilentlyContinue
Remove-Item -Force $zipPath -ErrorAction SilentlyContinue
Remove-Item -Recurse -Force $packageInputDir -ErrorAction SilentlyContinue
Remove-Item -Recurse -Force $runtimeDir -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Path $packageInputDir -Force | Out-Null

Copy-Item $jarPath (Join-Path $packageInputDir "javakava.jar")

$modules = (jdeps --multi-release 17 --ignore-missing-deps --print-module-deps $jarPath).Trim()
if ([string]::IsNullOrWhiteSpace($modules)) {
    throw "jdeps did not return required modules."
}
$requiredRuntimeModules = @("jdk.crypto.ec")
foreach ($requiredModule in $requiredRuntimeModules) {
    if ($modules -notmatch "(^|,)$([Regex]::Escape($requiredModule))(,|$)") {
        $modules = "$modules,$requiredModule"
    }
}

jlink `
  --add-modules $modules `
  --output $runtimeDir `
  --strip-debug `
  --compress 2 `
  --no-header-files `
  --no-man-pages

jpackage `
  --type app-image `
  --input $packageInputDir `
  --dest $distDir `
  --name "JavaKava" `
  --main-jar "javakava.jar" `
  --main-class "Main" `
  --runtime-image $runtimeDir `
  --app-version $AppVersion

Compress-Archive -Path (Join-Path $distDir "JavaKava") -DestinationPath $zipPath -Force

Write-Output "Built app image: $(Join-Path $distDir "JavaKava")"
Write-Output "Built archive: $zipPath"
