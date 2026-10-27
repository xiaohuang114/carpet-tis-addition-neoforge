# S9b - build the "single jar" distribution zip (JarJar variant).
#
# Layout (2 mod jars only, no optional\):
#   mods\carpet-1.4.147+v1.0.8-251027.jar
#   mods\carpet-tis-addition-1.82.0+neoforge.1.21.1.jar   <- JarJar variant (nested conditional-mixin + tiny-mappings)
#   README.txt
#
# NOTE: keep this file ASCII-only; the host PowerShell parses non-ASCII source as ANSI.
param(
    [string]$ZipName = 'TIS-Addition-NeoForge-1.21.1-single-jar.zip'
)

$ErrorActionPreference = 'Stop'
$root  = Split-Path -Parent $PSScriptRoot
$dist  = Join-Path $root 'dist'
$stage = Join-Path $root '_tmp\dist_single'
$jarjar = Join-Path $root '_s9_payload\TIS-single-jarjar.jar'
$carpet = Join-Path $dist 'mods\carpet-1.4.147+v1.0.8-251027.jar'
$readmeSrc = Join-Path $root 'tools\texts\README-single.txt'
$zip = Join-Path $dist $ZipName

foreach ($f in @($jarjar, $carpet, $readmeSrc)) {
    if (-not (Test-Path -LiteralPath $f)) { throw "missing input: $f" }
}

if (Test-Path -LiteralPath $stage) { Remove-Item -LiteralPath $stage -Recurse -Force }
New-Item -ItemType Directory -Force -Path (Join-Path $stage 'mods') | Out-Null
Copy-Item -LiteralPath $carpet -Destination (Join-Path $stage 'mods\carpet-1.4.147+v1.0.8-251027.jar') -Force
Copy-Item -LiteralPath $jarjar -Destination (Join-Path $stage 'mods\carpet-tis-addition-1.82.0+neoforge.1.21.1.jar') -Force
Copy-Item -LiteralPath $readmeSrc -Destination (Join-Path $stage 'README.txt') -Force

if (Test-Path -LiteralPath $zip) { Remove-Item -LiteralPath $zip -Force }

Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zs = [System.IO.Compression.ZipFile]::Open($zip, [System.IO.Compression.ZipArchiveMode]::Create)
try {
    foreach ($rel in @('README.txt', 'mods/carpet-1.4.147+v1.0.8-251027.jar', 'mods/carpet-tis-addition-1.82.0+neoforge.1.21.1.jar')) {
        $src = Join-Path $stage ($rel.Replace('/', [System.IO.Path]::DirectorySeparatorChar))
        $e = $zs.CreateEntry($rel, [System.IO.Compression.CompressionLevel]::Optimal)
        $es = $e.Open(); $fs = [System.IO.File]::OpenRead($src)
        try { $fs.CopyTo($es) } finally { $fs.Dispose(); $es.Dispose() }
        Write-Output ("[TIS-S9b] + {0}  ({1} bytes)" -f $rel, $e.Length)
    }
}
finally { $zs.Dispose() }

$i = Get-Item -LiteralPath $zip
Write-Output ("[TIS-S9b] zip     {0}  ({1} bytes)" -f $i.FullName, $i.Length)
Write-Output ("[TIS-S9b] sha256  {0}" -f (Get-FileHash -Algorithm SHA256 $zip).Hash)
