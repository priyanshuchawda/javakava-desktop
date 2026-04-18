Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$rootDir = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$buildDir = Join-Path $rootDir "build"
$classesDir = Join-Path $buildDir "classes"
$distDir = Join-Path $rootDir "dist"
$manifestFile = Join-Path $buildDir "manifest.mf"
$sourcesFile = Join-Path $buildDir "sources.txt"

Remove-Item -Recurse -Force $classesDir -ErrorAction SilentlyContinue
Remove-Item -Recurse -Force $distDir -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Path $classesDir -Force | Out-Null
New-Item -ItemType Directory -Path $distDir -Force | Out-Null
New-Item -ItemType Directory -Path $buildDir -Force | Out-Null

$javaFiles = Get-ChildItem -Path $rootDir -Recurse -Filter *.java -File | Where-Object {
    $_.FullName -notmatch "\\\.git\\|\\build\\|\\dist\\"
}

if ($javaFiles.Count -eq 0) {
    throw "No Java source files found."
}

$javaFiles.FullName | Set-Content -Path $sourcesFile -Encoding ascii

javac -d $classesDir "@$sourcesFile"

@"
Main-Class: Main
"@ | Set-Content -Path $manifestFile -Encoding ascii

jar --create --file (Join-Path $distDir "javakava.jar") --manifest $manifestFile -C $classesDir .

Write-Output "Built: $(Join-Path $distDir "javakava.jar")"
