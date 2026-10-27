param(
    [ValidateSet('github', 'modrinth')][string]$Service,
    [string]$Token,
    [string]$Owner,
    [switch]$Quiet
)
# Save a publish token into tools\.secrets\ (gitignored) so the publisher scripts
# pick it up automatically. ASCII only (PS 5.1 reads .ps1 as ANSI without a BOM).
#
#   tools\save-token.ps1 -Service github   -Token ghp_xxx -Owner your-login
#   tools\save-token.ps1 -Service modrinth -Token mrp_xxx
#
# Omit -Token to get a masked prompt instead of putting the secret in shell history.

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$secDir = Join-Path $root 'tools\.secrets'
if (-not (Test-Path $secDir)) { New-Item -ItemType Directory -Path $secDir -Force | Out-Null }

function Read-Secret([string]$Prompt) {
    $sec = Read-Host -Prompt $Prompt -AsSecureString
    $bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($sec)
    try { return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr) }
    finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr) }
}

if ([string]::IsNullOrWhiteSpace($Service)) {
    $Service = (Read-Host -Prompt 'service (github / modrinth)').Trim().ToLower()
    if ($Service -notin @('github', 'modrinth')) { throw "unknown service: $Service" }
}

if ([string]::IsNullOrWhiteSpace($Token)) { $Token = Read-Secret "paste the $Service token (input is hidden)" }
$Token = $Token.Trim()

$expectPrefix = if ($Service -eq 'github') { 'ghp_' } else { 'mrp_' }
if ($Token.Length -lt 20) {
    Write-Host ("[save-token] token is only {0} chars; {1} tokens are 20+ chars. Not writing it." -f $Token.Length, $Service) -ForegroundColor Red
    exit 3
}
if (-not $Token.StartsWith($expectPrefix) -and $Token -notlike 'github_pat_*') {
    Write-Host ("[save-token] warning: token does not start with {0} (fine-grained GitHub tokens start with github_pat_)." -f $expectPrefix) -ForegroundColor Yellow
}

$tokenFile = Join-Path $secDir "$Service.token"
Set-Content -Path $tokenFile -Value $Token -Encoding ascii -NoNewline
Write-Host ("[save-token] wrote {0} ({1} chars)" -f $tokenFile, $Token.Length) -ForegroundColor Green

if ($Service -eq 'github') {
    if ([string]::IsNullOrWhiteSpace($Owner)) { $Owner = (Read-Host -Prompt 'your GitHub login (no spaces)').Trim() }
    if ($Owner -match '\s') {
        Write-Host ("[save-token] '{0}' contains a space; GitHub logins cannot. Not writing the owner." -f $Owner) -ForegroundColor Red
        exit 3
    }
    Set-Content -Path (Join-Path $secDir 'github.owner') -Value $Owner -Encoding ascii -NoNewline
    Write-Host ("[save-token] wrote github.owner = {0}" -f $Owner) -ForegroundColor Green
}

# .gitignore guard
$gitignore = Join-Path $root '.gitignore'
if ((Test-Path $gitignore) -and -not ((Get-Content $gitignore -Raw -Encoding UTF8) -match '\.secrets/')) {
    Add-Content -Path $gitignore -Value "`r`ntools/.secrets/`r`n" -Encoding utf8
    Write-Host '[save-token] added tools/.secrets/ to .gitignore' -ForegroundColor Cyan
}

if (-not $Quiet) {
    Write-Host ''
    Write-Host 'next:' -ForegroundColor Cyan
    if ($Service -eq 'github') { Write-Host '  tools\publish-github.ps1' } else { Write-Host '  tools\publish-modrinth.ps1' }
}
