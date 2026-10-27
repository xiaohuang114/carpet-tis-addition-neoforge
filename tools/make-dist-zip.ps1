# TIS-S9: build dist zip with explicit entry names (ASCII only on purpose:
# Windows PowerShell 5.1 mis-decodes UTF-8 .ps1 files that have no BOM).
param(
    [string]$DistDir = (Join-Path (Split-Path -Parent $PSScriptRoot) 'dist'),
    [string]$ZipPath = (Join-Path (Split-Path -Parent $PSScriptRoot) 'dist\TIS-Addition-NeoForge-1.21.1.zip')
)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem

$rel = @(
    'README.txt',
    'THIRD-PARTY-NOTICES.txt',
    'mods/carpet-1.4.147+v1.0.8-251027.jar',
    'mods/carpet-tis-addition-1.82.0+neoforge.1.21.1.jar',
    'mods/tiny-mappings-parser-0.3.0+build.17.jar',
    'optional/conditional-mixin-neoforge-0.6.4.jar'
)

function ToLocal([string]$relPath) {
    return (Join-Path $DistDir ($relPath.Replace('/', [System.IO.Path]::DirectorySeparatorChar)))
}

$zipFull = [System.IO.Path]::GetFullPath($ZipPath)
if (Test-Path $zipFull) { Remove-Item $zipFull -Force }

$expectedAbs = @{}
foreach ($r in $rel) { $expectedAbs[[System.IO.Path]::GetFullPath((ToLocal $r))] = $true }
# Top-level *.zip files are build outputs (this zip and the single-jar variant), not payload.
$unexpected = @(Get-ChildItem $DistDir -Recurse -File |
    Where-Object { -not $expectedAbs.ContainsKey($_.FullName) -and $_.Extension -ne '.zip' })
if ($unexpected.Count -gt 0) {
    Write-Host '[TIS-S9] dist contains unregistered files, refusing to pack:'
    $unexpected | ForEach-Object { Write-Host ('   ' + $_.FullName) }
    exit 2
}
foreach ($r in $rel) {
    $p = ToLocal $r
    if (-not (Test-Path $p)) { Write-Host "[TIS-S9] missing file: $p"; exit 3 }
}

$zip = [System.IO.Compression.ZipFile]::Open($zipFull, [System.IO.Compression.ZipArchiveMode]::Create)
try {
    foreach ($r in $rel) {
        $p = ToLocal $r
        $entry = $zip.CreateEntry($r, [System.IO.Compression.CompressionLevel]::Optimal)
        $es = $entry.Open()
        $fs = [System.IO.File]::OpenRead($p)
        try { $fs.CopyTo($es) } finally { $fs.Dispose(); $es.Dispose() }
        Write-Host ('[TIS-S9] + {0}  ({1} bytes)' -f $r, (Get-Item $p).Length)
    }
} finally { $zip.Dispose() }

$fi = Get-Item $zipFull
Write-Host ('[TIS-S9] dist zip: {0}  ({1} bytes)' -f $fi.FullName, $fi.Length)
Write-Host ('[TIS-S9] sha256  : {0}' -f (Get-FileHash $zipFull -Algorithm SHA256).Hash)
exit 0
