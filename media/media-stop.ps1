$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$pidFile = Join-Path $root ".mediamtx.pid"

if (Test-Path $pidFile) {
    $mediaPid = Get-Content $pidFile -ErrorAction SilentlyContinue
    if ($mediaPid) {
        $process = Get-Process -Id $mediaPid -ErrorAction SilentlyContinue
        if ($process) {
            Stop-Process -Id $mediaPid -Force
            Write-Host "[media] stopped, pid=$mediaPid"
        }
    }
    Remove-Item $pidFile -Force -ErrorAction SilentlyContinue
}
else {
    $processes = Get-Process -Name "mediamtx" -ErrorAction SilentlyContinue
    if ($processes) {
        $processes | Stop-Process -Force
        Write-Host "[media] stopped $($processes.Count) process(es)"
    }
    else {
        Write-Host "[media] not running"
    }
}
