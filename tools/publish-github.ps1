param(
    [string]$Token = $env:GITHUB_TOKEN,
    [string]$Owner = $env:GITHUB_OWNER,
    [string]$Repo  = 'carpet-tis-addition-neoforge',
    [string]$Tag   = 'v1.82.0+neoforge.1.21.1',
    [switch]$SkipRelease,
    [switch]$DryRun
)
# Create the GitHub repo (if needed), push the local commit, then publish a release
# with the packaged assets. Requires a PAT in $env:GITHUB_TOKEN (repo scope) and
# the account name in $env:GITHUB_OWNER. ASCII only (PS 5.1 reads .ps1 as ANSI).

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

if ($DryRun) {
    $ErrorActionPreference = 'Continue'
    Write-Host '[dry-run] planned API calls:'
    Write-Host '  GET    https://api.github.com/repos/$Owner/$Repo            (existence check)'
    Write-Host '  POST   https://api.github.com/user/repos                    (create when absent)'
    Write-Host '  POST   https://api.github.com/repos/$Owner/$Repo/releases   (tag v1.82.0+neoforge.1.21.1)'
    Write-Host '  POST   .../releases/{id}/assets?name=...                    (3 assets)'
    Write-Host '[dry-run] local git state:'
    $branch = (& git rev-parse --abbrev-ref HEAD 2>$null)
    $head = (& git log --oneline -1 2>$null)
    $remote = (& git remote get-url origin 2>$null)
    Write-Host ("  branch        : {0}" -f $branch)
    Write-Host ("  head          : {0}" -f $head)
    Write-Host ("  origin        : {0}" -f ($(if ($remote) { $remote } else { '(none yet)' })))
    Write-Host ("  dirty files   : {0}" -f ((& git status --porcelain | Measure-Object).Count))
    Write-Host '[dry-run] assets that would be uploaded:'
    foreach ($a in @((Join-Path $root 'dist\TIS-Addition-NeoForge-1.21.1-single-jar.zip'), (Join-Path $root 'dist\TIS-Addition-NeoForge-1.21.1.zip'), (Join-Path $root '_s9_payload\TIS-single-jarjar.jar'))) {
        $st = 'MISSING'
        if (Test-Path $a) { $st = '{0:N0} bytes' -f (Get-Item $a).Length }
        Write-Host ("  {0,-52} {1}" -f (Split-Path $a -Leaf), $st)
    }
    Write-Host '[dry-run] no request sent; set GITHUB_TOKEN/GITHUB_OWNER and drop -DryRun to publish.' -ForegroundColor Green
    exit 0
}

$secDir = Join-Path $root 'tools\.secrets'
$tokFile = Join-Path $secDir 'github.token'
$ownFile = Join-Path $secDir 'github.owner'
if ([string]::IsNullOrWhiteSpace($Token) -and (Test-Path $tokFile)) {
    $Token = ((Get-Content $tokFile -Raw -Encoding UTF8).Trim() -split "`r?`n")[0].Trim()
    if ($Token) { Write-Host "[publish] token loaded from $tokFile" -ForegroundColor Cyan }
}
if ([string]::IsNullOrWhiteSpace($Owner) -and (Test-Path $ownFile)) {
    $Owner = ((Get-Content $ownFile -Raw -Encoding UTF8).Trim() -split "`r?`n")[0].Trim()
    if ($Owner) { Write-Host "[publish] owner loaded from $ownFile" -ForegroundColor Cyan }
}
if ([string]::IsNullOrWhiteSpace($Token)) {
    Write-Host '[publish] GITHUB_TOKEN is empty.' -ForegroundColor Yellow
    Write-Host '   $env:GITHUB_TOKEN = "ghp_xxxxxxxxxxxxxxxx"; $env:GITHUB_OWNER = "your-login"' -ForegroundColor Yellow
    Write-Host '   tools\publish-github.ps1' -ForegroundColor Yellow
    Write-Host '   or: put the token in tools\.secrets\github.token (+ github.owner), then just run tools\publish-github.ps1' -ForegroundColor Yellow
    exit 2
}
# --- 0. token preflight: fail fast with a readable message instead of a raw 401
$len = $Token.Length
$masked = $Token
if ($len -gt 8) { $masked = $Token.Substring(0, 4) + '...' + $Token.Substring($len - 4) }
Write-Host ("[publish] token: {0} ({1} chars)" -f $masked, $len) -ForegroundColor Cyan

$probeHeaders = @{ Authorization = "token $Token"; 'User-Agent' = 'tis-neoforge-publisher'; Accept = 'application/vnd.github+json' }
$me = $null
try {
    $me = Invoke-RestMethod -Uri 'https://api.github.com/user' -Headers $probeHeaders
} catch {
    $status = 0
    if ($_.Exception.Response) { $status = [int]$_.Exception.Response.StatusCode }
    Write-Host ''
    Write-Host ("[publish] GitHub rejected the token: HTTP {0} {1}" -f $status, $_.Exception.Message) -ForegroundColor Red
    if ($len -lt 20) {
        Write-Host ("[publish] your token is only {0} chars - that looks like the placeholder from the docs, not a real one." -f $len) -ForegroundColor Yellow
    }
    Write-Host ''
    Write-Host '  Classic PAT (40 chars, starts with ghp_):' -ForegroundColor Yellow
    Write-Host '    https://github.com/settings/tokens  ->  Generate new token (classic)  ->  scope: repo' -ForegroundColor Yellow
    Write-Host '  Fine-grained PAT (starts with github_pat_):' -ForegroundColor Yellow
    Write-Host '    https://github.com/settings/personal-access-tokens/new' -ForegroundColor Yellow
    Write-Host '    -> Repository access: All repositories (or pick one)' -ForegroundColor Yellow
    Write-Host '    -> Permissions: Contents = Read and write, Administration = Read and write, Metadata = Read' -ForegroundColor Yellow
    Write-Host ''
    Write-Host '  then either:' -ForegroundColor Yellow
    Write-Host '    $env:GITHUB_TOKEN = "<paste real token>"; $env:GITHUB_OWNER = "<your login>"; tools\publish-github.ps1' -ForegroundColor Yellow
    Write-Host '    or write the token to tools\.secrets\github.token and the login to tools\.secrets\github.owner' -ForegroundColor Yellow
    if ($status -eq 403) {
        Write-Host ''
        Write-Host '  HTTP 403 = the token is valid but missing a scope/permission (needs repo / Contents write).' -ForegroundColor Yellow
    }
    exit 3
}
Write-Host ("[publish] authenticated as {0}" -f $me.login) -ForegroundColor Green
if ([string]::IsNullOrWhiteSpace($Owner)) {
    $me = Invoke-RestMethod -Uri 'https://api.github.com/user' -Headers @{ Authorization = "token $Token"; 'User-Agent' = 'tis-neoforge-publisher' }
    $Owner = $me.login
    Write-Host "[publish] owner resolved from token: $Owner" -ForegroundColor Cyan
}

$headers = @{ Authorization = "token $Token"; 'User-Agent' = 'tis-neoforge-publisher'; Accept = 'application/vnd.github+json' }

# --- 1. repo
$repoExists = $true
try {
    Invoke-RestMethod -Uri "https://api.github.com/repos/$Owner/$Repo" -Headers $headers | Out-Null
} catch {
    $repoExists = $false
}
if (-not $repoExists) {
    $payload = @{
        name        = $Repo
        description = 'Carpet TIS Addition 1.82.0 NeoForge 1.21.1 port (unofficial)'
        private     = $false
        has_issues  = $true
        has_wiki    = $false
    } | ConvertTo-Json
    Invoke-RestMethod -Method POST -Uri 'https://api.github.com/user/repos' -Headers $headers -Body $payload -ContentType 'application/json' | Out-Null
    Write-Host "[publish] created repo $Owner/$Repo" -ForegroundColor Green
} else {
    Write-Host "[publish] repo $Owner/$Repo already exists" -ForegroundColor Cyan
}

# --- 2. push
$remoteUrl = "https://x-access-token:$Token@github.com/$Owner/$Repo.git"
$current = (& git remote get-url origin 2>$null)
if ($LASTEXITCODE -ne 0 -or -not $current) {
    & git remote add origin $remoteUrl
} else {
    & git remote set-url origin $remoteUrl
}
& git branch -M main
& git push -u origin main
if ($LASTEXITCODE -ne 0) { throw 'git push failed' }
Write-Host '[publish] pushed main' -ForegroundColor Green

if ($SkipRelease) { exit 0 }

# --- 3. release + assets
$notes = Join-Path $root 'dist\publish\github-release.md'
$releaseBody = if (Test-Path $notes) { Get-Content $notes -Raw } else { 'See RELEASE-NOTES.md.' }
$rel = @{ tag_name = $Tag; name = "1.82.0+neoforge.1.21.1"; body = $releaseBody; draft = $false; prerelease = $false }

$existingRelease = $null
try { $existingRelease = Invoke-RestMethod -Uri "https://api.github.com/repos/$Owner/$Repo/releases/tags/$Tag" -Headers $headers } catch { }
if (-not $existingRelease) {
    $existingRelease = Invoke-RestMethod -Method POST -Uri "https://api.github.com/repos/$Owner/$Repo/releases" `
        -Headers $headers -Body ($rel | ConvertTo-Json -Depth 6) -ContentType 'application/json'
    Write-Host "[publish] release created: $($existingRelease.html_url)" -ForegroundColor Green
} else {
    Write-Host "[publish] release exists: $($existingRelease.html_url)" -ForegroundColor Cyan
}

$assets = @(
    (Join-Path $root 'dist\TIS-Addition-NeoForge-1.21.1-single-jar.zip'),
    (Join-Path $root 'dist\TIS-Addition-NeoForge-1.21.1.zip'),
    (Join-Path $root '_s9_payload\TIS-single-jarjar.jar')
)
$uploadTemplate = $existingRelease.upload_url -replace '\{\?name,label\}', ''
foreach ($a in $assets) {
    if (-not (Test-Path $a)) { Write-Host "[publish] skip missing asset $a" -ForegroundColor Yellow; continue }
    $name = Split-Path $a -Leaf
    $uri = "$uploadTemplate?name=$([uri]::EscapeDataString($name))"
    try {
        Invoke-RestMethod -Method POST -Uri $uri -Headers $headers -ContentType 'application/octet-stream' -InFile $a | Out-Null
        Write-Host "[publish] uploaded asset $name" -ForegroundColor Green
    } catch {
        Write-Host "[publish] asset upload failed for ${name}: $($_.Exception.Message)" -ForegroundColor Yellow
    }
}

Write-Host "[publish] repo:    https://github.com/$Owner/$Repo" -ForegroundColor Green
Write-Host "[publish] release: $($existingRelease.html_url)" -ForegroundColor Green
