param(
    [string]$Token = $env:MODRINTH_TOKEN,
    [string]$Slug = 'carpet-tis-addition-neoforge',
    [string]$Title = 'Carpet TIS Addition (NeoForge port)',
    [switch]$CreateOnly,
    [switch]$DryRun
)
# Publish the single-jar build to Modrinth (project + version + icon).
# Requires a Modrinth PAT with create/upload scope in $env:MODRINTH_TOKEN.
# ASCII only on purpose: Windows PowerShell 5.1 reads .ps1 as ANSI without a BOM.

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$publish = Join-Path $root 'dist\publish'
if (-not (Test-Path $publish)) { New-Item -ItemType Directory -Path $publish | Out-Null }

$modJar  = Join-Path $root '_s9_payload\TIS-single-jarjar.jar'
$packZip = Join-Path $root 'dist\TIS-Addition-NeoForge-1.21.1-single-jar.zip'
$iconJar = Join-Path $root 'build\icon-D.png'
$iconOut = Join-Path $publish 'icon-512.png'
$bodyMd  = Join-Path $publish 'modrinth-body.md'
$verJson = Join-Path $publish 'version.json'

foreach ($p in @($modJar, $packZip, $bodyMd, $verJson)) {
    if (-not (Test-Path $p)) { throw "missing required file: $p" }
}

if ($DryRun) {
    $ver = Get-Content $verJson -Raw -Encoding UTF8 | ConvertFrom-Json
    $ver | Add-Member -NotePropertyName project_id -NotePropertyValue 'DRYRUN_PROJECT' -Force
    $ver | Add-Member -NotePropertyName file_parts -NotePropertyValue @('file') -Force
    $ver | Add-Member -NotePropertyName primary_file -NotePropertyValue 'file' -Force
    $json = $ver | ConvertTo-Json -Depth 12 -Compress
    $jarLen = (Get-Item $modJar).Length
    Write-Host '[dry-run] project payload fields : slug/title/description/body/categories/client_side/server_side/project_type/license_id'
    Write-Host ("[dry-run] body markdown chars   : {0}" -f (Get-Content $bodyMd -Raw -Encoding UTF8).Length)
    Write-Host ("[dry-run] version data JSON     : {0} chars" -f $json.Length)
    Write-Host ("[dry-run] version json valid    : {0}" -f ($ver.version_number))
    Write-Host ("[dry-run] game_versions/loaders : {0} / {1}" -f ($ver.game_versions -join ','), ($ver.loaders -join ','))
    Write-Host ("[dry-run] upload file           : {0} ({1} bytes)" -f (Split-Path $modJar -Leaf), $jarLen)
    $iconDesc = 'MISSING'
    if (Test-Path $iconJar) { $iconDesc = (Split-Path $iconJar -Leaf) + ' (' + (Get-Item $iconJar).Length + ' bytes)' }
    Write-Host ("[dry-run] icon                  : {0}" -f $iconDesc)
    Write-Host '[dry-run] no request sent; set MODRINTH_TOKEN and drop -DryRun to publish.' -ForegroundColor Green
    exit 0
}
$tokenFile = Join-Path $root 'tools\.secrets\modrinth.token'
if ([string]::IsNullOrWhiteSpace($Token) -and (Test-Path $tokenFile)) {
    $Token = ((Get-Content $tokenFile -Raw -Encoding UTF8).Trim() -split "`r?`n")[0].Trim()
    if ($Token) { Write-Host "[publish] token loaded from $tokenFile" -ForegroundColor Cyan }
}
if ([string]::IsNullOrWhiteSpace($Token)) {
    Write-Host '[publish] MODRINTH_TOKEN is empty.' -ForegroundColor Yellow
    Write-Host '[publish] set it for this shell, then re-run:' -ForegroundColor Yellow
    Write-Host '   $env:MODRINTH_TOKEN = "mrp_xxxxxxxxxxxxxxxx"' -ForegroundColor Yellow
    Write-Host '   tools\publish-modrinth.ps1' -ForegroundColor Yellow
    Write-Host '   or: put the token in tools\.secrets\modrinth.token, then just run tools\publish-modrinth.ps1' -ForegroundColor Yellow
    exit 2
}

# --- 0. token preflight: readable message instead of a raw 401
$len = $Token.Length
$masked = $Token
if ($len -gt 8) { $masked = $Token.Substring(0, 4) + '...' + $Token.Substring($len - 4) }
Write-Host ("[publish] token: {0} ({1} chars)" -f $masked, $len) -ForegroundColor Cyan
try {
    $who = Invoke-RestMethod -Uri 'https://api.modrinth.com/v2/user' -Headers @{ Authorization = $Token; 'User-Agent' = 'tis-neoforge-publisher/1.0' }
    Write-Host ("[publish] authenticated as {0}" -f $who.username) -ForegroundColor Green
} catch {
    $status = 0
    if ($_.Exception.Response) { $status = [int]$_.Exception.Response.StatusCode }
    Write-Host ''
    Write-Host ("[publish] Modrinth rejected the token: HTTP {0} {1}" -f $status, $_.Exception.Message) -ForegroundColor Red
    if ($len -lt 20) { Write-Host ("[publish] your token is only {0} chars - that looks like the placeholder, not a real PAT." -f $len) -ForegroundColor Yellow }
    Write-Host ''
    Write-Host '  Create a PAT: https://modrinth.com/settings/pats  -> Create PAT' -ForegroundColor Yellow
    Write-Host '  Required scopes: Project create, Version create (add Project write if you want to edit later)' -ForegroundColor Yellow
    Write-Host '  The PAT starts with mrp_ . A session token from the website will not work.' -ForegroundColor Yellow
    Write-Host ''
    Write-Host '  then either:' -ForegroundColor Yellow
    Write-Host '    $env:MODRINTH_TOKEN = "<paste mrp_...>"; tools\publish-modrinth.ps1' -ForegroundColor Yellow
    Write-Host '    or write it to tools\.secrets\modrinth.token' -ForegroundColor Yellow
    exit 3
}
$headers = @{ Authorization = $Token; 'User-Agent' = 'carpet-tis-addition-neoforge-publisher/1.0' }

# --- icon (Modrinth wants a square PNG, 512 or 1024)
if (Test-Path $iconJar) { Copy-Item $iconJar $iconOut -Force }

function Invoke-Modrinth([string]$Method, [string]$Path, $BodyObj) {
    $params = @{ Method = $Method; Uri = "https://api.modrinth.com/v2$Path"; Headers = $headers }
    if ($null -ne $BodyObj) {
        $params.ContentType = 'application/json'
        $params.Body = ($BodyObj | ConvertTo-Json -Depth 12 -Compress)
    }
    return Invoke-RestMethod @params
}

# --- 1. project (create once, reuse afterwards)
$projectId = Join-Path $publish 'modrinth-project-id.txt'
$id = $null
if (Test-Path $projectId) { $id = (Get-Content $projectId -Raw).Trim() }
if (-not $id) {
    try {
        $existing = Invoke-Modrinth 'GET' "/project/$Slug" $null
        if ($existing.id) { $id = $existing.id }
    } catch {
        Write-Host "[publish] project '$Slug' not found, creating it." -ForegroundColor Cyan
    }
}

if (-not $id) {
    $body = Get-Content $bodyMd -Raw -Encoding UTF8
    $project = @{
        slug          = $Slug
        title         = $Title
        description   = 'Carpet TIS Addition 1.82.0 的 NeoForge 1.21.1 移植版：Carpet 扩展规则、调试工具与性能剖析器。'
        body          = $body
        categories    = @('utility', 'optimization')
        client_side   = 'required'
        server_side   = 'required'
        project_type  = 'mod'
        license_id    = 'LGPL-3.0'
        source_url    = $env:GITHUB_SOURCE_URL
        issues_url    = $env:GITHUB_ISSUES_URL
    }
    $project = $project | Where-Object { $true }
    $created = Invoke-Modrinth 'POST' '/project' $project
    $id = $created.id
    Set-Content -Path $projectId -Value $id -Encoding ascii
    Write-Host "[publish] created project $id" -ForegroundColor Green
} else {
    Write-Host "[publish] reusing project $id" -ForegroundColor Cyan
}

# --- 2. icon
if (Test-Path $iconOut) {
    try {
        $iconBytes = [IO.File]::ReadAllBytes($iconOut)
        $boundary = [guid]::NewGuid().ToString()
        $ms = New-Object IO.MemoryStream
        $w = New-Object IO.StreamWriter($ms, [Text.Encoding]::ASCII)
        $w.Write("--$boundary`r`n")
        $w.Write("Content-Disposition: form-data; name=""icon""; filename=""icon.png""`r`n")
        $w.Write("Content-Type: image/png`r`n`r`n")
        $w.Flush()
        $ms.Write($iconBytes, 0, $iconBytes.Length)
        $w.Write("`r`n--$boundary--`r`n")
        $w.Flush()
        Invoke-RestMethod -Method POST -Uri "https://api.modrinth.com/v2/project/$id/icon" `
            -Headers $headers -ContentType "multipart/form-data; boundary=$boundary" -Body $ms.ToArray() | Out-Null
        Write-Host '[publish] icon uploaded' -ForegroundColor Green
    } catch {
        Write-Host "[publish] icon upload skipped: $($_.Exception.Message)" -ForegroundColor Yellow
    }
}

if ($CreateOnly) { Write-Host '[publish] -CreateOnly set, stopping before version upload.' -ForegroundColor Cyan; exit 0 }

# --- 3. version (multipart: data JSON + file)
$ver = Get-Content $verJson -Raw -Encoding UTF8 | ConvertFrom-Json
$ver | Add-Member -NotePropertyName project_id -NotePropertyValue $id -Force
$ver | Add-Member -NotePropertyName file_parts -NotePropertyValue @('file') -Force
$ver | Add-Member -NotePropertyName primary_file -NotePropertyValue 'file' -Force
$dataJson = $ver | ConvertTo-Json -Depth 12 -Compress

$boundary = [guid]::NewGuid().ToString()
$ms = New-Object IO.MemoryStream
$w = New-Object IO.StreamWriter($ms, [Text.Encoding]::UTF8)
$w.Write("--$boundary`r`n")
$w.Write("Content-Disposition: form-data; name=""data""`r`n`r`n")
$w.Write($dataJson)
$w.Write("`r`n--$boundary`r`n")
$w.Write("Content-Disposition: form-data; name=""file""; filename=""$(Split-Path $modJar -Leaf)""`r`n")
$w.Write("Content-Type: application/java-archive`r`n`r`n")
$w.Flush()
$jarBytes = [IO.File]::ReadAllBytes($modJar)
$ms.Write($jarBytes, 0, $jarBytes.Length)
$w.Write("`r`n--$boundary--`r`n")
$w.Flush()

$uploaded = Invoke-RestMethod -Method POST -Uri 'https://api.modrinth.com/v2/version' `
    -Headers $headers -ContentType "multipart/form-data; boundary=$boundary" -Body $ms.ToArray()
Write-Host "[publish] version $($uploaded.id) -> https://modrinth.com/mod/$Slug/version/$($uploaded.version_number)" -ForegroundColor Green
Write-Host "[publish] project page: https://modrinth.com/mod/$Slug" -ForegroundColor Green
