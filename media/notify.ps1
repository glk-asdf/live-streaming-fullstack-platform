param(
    [ValidateSet("online", "offline")]
    [string]$Event
)

$ErrorActionPreference = "SilentlyContinue"

$path = $env:MTX_PATH
if (-not $path) {
    exit 0
}

$token = "dev-media-callback-token"
$uri = "http://127.0.0.1:8080/api/v1/media/$Event" +
    "?token=$([uri]::EscapeDataString($token))" +
    "&path=$([uri]::EscapeDataString($path))"

try {
    Invoke-RestMethod -Method Post -Uri $uri -TimeoutSec 3 | Out-Null
}
catch {
    $line = "$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss') $Event $path failed: $($_.Exception.Message)"
    Add-Content -Path "notify.log" -Value $line -Encoding utf8
}

exit 0
