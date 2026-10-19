$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$exe = Join-Path $root "bin\mediamtx.exe"
$config = Join-Path $root "mediamtx.yml"
$pidFile = Join-Path $root ".mediamtx.pid"
$logFile = Join-Path $root "mediamtx.log"
$errFile = Join-Path $root "mediamtx.err.log"
$version = "v1.21.2"

if (-not (Test-Path $exe)) {
    Write-Host "[media] mediamtx.exe not found, downloading $version ..."
    New-Item -ItemType Directory -Force -Path (Join-Path $root "bin") | Out-Null
    $zip = Join-Path $env:TEMP "mediamtx_$version.zip"
    if (-not (Test-Path $zip)) {
        Invoke-WebRequest -Uri "https://github.com/bluenviron/mediamtx/releases/download/$version/mediamtx_${version}_windows_amd64.zip" -OutFile $zip -TimeoutSec 300
    }
    Expand-Archive -Path $zip -DestinationPath (Join-Path $root "bin") -Force
}

if (Test-Path $pidFile) {
    $oldPid = Get-Content $pidFile -ErrorAction SilentlyContinue
    if ($oldPid -and (Get-Process -Id $oldPid -ErrorAction SilentlyContinue)) {
        Write-Host "[media] already running, pid=$oldPid"
        exit 0
    }
    Remove-Item $pidFile -Force -ErrorAction SilentlyContinue
}

$busy = @()
$busy += foreach ($port in 1935, 8888, 8889, 9997) {
    Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
}
# 8189 是 WebRTC 的 UDP 端口，Get-NetTCPConnection 查不到
$busy += Get-NetUDPEndpoint -LocalPort 8189 -ErrorAction SilentlyContinue
if ($busy.Count -gt 0) {
    $ports = ($busy | Select-Object -ExpandProperty LocalPort -Unique) -join ", "
    Write-Warning "[media] port(s) already in use: $ports"
    exit 1
}

Write-Host "[media] starting mediamtx $version ..."
$process = Start-Process -FilePath $exe -ArgumentList $config -WorkingDirectory $root -PassThru `
    -RedirectStandardOutput $logFile -RedirectStandardError $errFile -WindowStyle Hidden
Set-Content -Path $pidFile -Value $process.Id -Encoding ascii

Start-Sleep -Seconds 2
try {
    $null = Invoke-RestMethod -Uri "http://127.0.0.1:9997/v3/config/global/get" -TimeoutSec 5
    Write-Host "[media] ready. pid=$($process.Id)"
    Write-Host "[media] RTMP  rtmp://localhost:1935/live/{roomId}?key={streamKey}"
    Write-Host "[media] HLS   http://localhost:8888/live/{roomId}/index.m3u8"
    Write-Host "[media] WHEP  http://localhost:8889/live/{roomId}/whep"
    exit 0
}
catch {
    Stop-Process -Id $process.Id -Force -ErrorAction SilentlyContinue
    Remove-Item $pidFile -Force -ErrorAction SilentlyContinue
    Write-Error "[media] startup check failed, process stopped. See $logFile and $errFile"
    exit 1
}
