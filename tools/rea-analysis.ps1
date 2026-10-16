param(
    [Parameter(Mandatory=$true)][string]$Apk,
    [Parameter(Mandatory=$true)][string]$JadxJar,
    [Parameter(Mandatory=$true)][string]$Jdk,
    [Parameter(Mandatory=$true)][string]$Report
)
$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path -Parent $PSScriptRoot
$taskJarPath = (Resolve-Path -LiteralPath $JadxJar).Path
$taskApkPath = (Resolve-Path -LiteralPath $Apk).Path
$taskDigest = (Get-FileHash -Algorithm SHA256 -LiteralPath $taskJarPath).Hash.ToLowerInvariant()
if ($taskDigest -ne '6e5eacf500b64292bfb73c49797c1958f6ee44646e43e868039ae7feb573ff75') {
    throw 'The JADX engine does not match the pinned REA release. Do not analyze with this JAR.'
}
$taskLauncher = Join-Path $taskRoot 'tools-local\rea\node_modules\rea-agents\scripts\rea.mjs'
if (-not (Test-Path -LiteralPath $taskLauncher)) {
    throw 'Install locally first: npm.cmd install --prefix tools-local/rea --ignore-scripts --no-audit --no-fund rea-agents@6.5.0'
}
$taskReportPath = [System.IO.Path]::GetFullPath($Report)
$taskPrivateRoot = [System.IO.Path]::GetFullPath((Join-Path $taskRoot 'private-analysis')) + [System.IO.Path]::DirectorySeparatorChar
if (-not $taskReportPath.StartsWith($taskPrivateRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
    throw 'Save raw proprietary analysis inside ignored private-analysis/.'
}
New-Item -ItemType Directory -Force -Path (Split-Path -Parent $taskReportPath) | Out-Null
$taskPreviousJava = $env:JAVA_HOME
$taskPreviousJar = $env:REA_JADX_MCP_JAR
try {
    $env:JAVA_HOME = (Resolve-Path -LiteralPath $Jdk).Path
    $env:REA_JADX_MCP_JAR = $taskJarPath
    & node $taskLauncher inspect-android-package $taskApkPath --format json | Set-Content -LiteralPath $taskReportPath -Encoding utf8
    if ($LASTEXITCODE -ne 0) { throw "REA analysis failed. Read the private report: $taskReportPath" }
} finally {
    $env:JAVA_HOME = $taskPreviousJava
    $env:REA_JADX_MCP_JAR = $taskPreviousJar
}
