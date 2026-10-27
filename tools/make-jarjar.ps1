# S9b - build a SINGLE-file "every JarJar" variant of the release jar.
#
# Goal: answer "can we ship one jar again?" without going back to flattening classes.
# NeoForge's JarJar loads nested jars as SEPARATE modules, so the packages are never
# exported twice -> no java.lang.module.ResolutionException, even when another mod
# (e.g. TweakerMore) also provides conditional_mixin.
#
# Output: _s9_payload\TIS-single-jarjar.jar  =  release jar + META-INF/jarjar/{metadata.json, <libs>.jar}
#
# NOTE: keep this file ASCII-only; the host PowerShell parses non-ASCII source as ANSI.
param(
    [string]$OutName = 'TIS-single-jarjar.jar'
)

$ErrorActionPreference = 'Stop'
$root    = Split-Path -Parent $PSScriptRoot
$base    = Join-Path $root 'build\libs\carpet-tis-addition-1.82.0+neoforge.1.21.1.jar'
$cm      = Join-Path $root 'build\libs\conditional-mixin-neoforge-0.6.4.jar'
$tiny    = Join-Path $root 'dist\mods\tiny-mappings-parser-0.3.0+build.17.jar'
$payload = Join-Path $root '_s9_payload'
$out     = Join-Path $payload $OutName

foreach ($f in @($base, $cm, $tiny)) {
    if (-not (Test-Path -LiteralPath $f)) { throw "missing input: $f" }
}
New-Item -ItemType Directory -Force -Path $payload | Out-Null
if (Test-Path -LiteralPath $out) { Remove-Item -LiteralPath $out -Force }
Copy-Item -LiteralPath $base -Destination $out -Force
Write-Output ("[TIS-S9b] base  {0}  ({1} bytes)" -f (Split-Path $base -Leaf), (Get-Item -LiteralPath $base).Length)

$metadata = @'
{
  "jars": [
    {
      "identifier": {
        "group": "com.github.Fallen-Breath.conditional-mixin",
        "artifact": "conditional-mixin-neoforge"
      },
      "version": {
        "range": "[0.6.4,)",
        "artifactVersion": "0.6.4"
      },
      "path": "META-INF/jarjar/conditional-mixin-neoforge-0.6.4.jar",
      "isObfuscated": false
    },
    {
      "identifier": {
        "group": "net.fabricmc",
        "artifact": "tiny-mappings-parser"
      },
      "version": {
        "range": "[0.3.0,)",
        "artifactVersion": "0.3.0"
      },
      "path": "META-INF/jarjar/tiny-mappings-parser-0.3.0+build.17.jar",
      "isObfuscated": false
    }
  ]
}
'@

Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::Open($out, [System.IO.Compression.ZipArchiveMode]::Update)
try {
    $entry = $zip.CreateEntry('META-INF/jarjar/metadata.json', [System.IO.Compression.CompressionLevel]::Optimal)
    $sw = New-Object System.IO.StreamWriter($entry.Open(), (New-Object System.Text.UTF8Encoding($false)))
    $sw.Write($metadata); $sw.Flush(); $sw.Dispose()
    Write-Output ("[TIS-S9b] + META-INF/jarjar/metadata.json  ({0} bytes)" -f $entry.Length)

    foreach ($lib in @($cm, $tiny)) {
        $name = 'META-INF/jarjar/' + (Split-Path $lib -Leaf)
        $e = $zip.CreateEntry($name, [System.IO.Compression.CompressionLevel]::Optimal)
        $es = $e.Open()
        $fs = [System.IO.File]::OpenRead($lib)
        try { $fs.CopyTo($es) } finally { $fs.Dispose(); $es.Dispose() }
        Write-Output ("[TIS-S9b] + {0}  ({1} bytes)" -f $name, $e.Length)
    }
}
finally { $zip.Dispose() }

$i = Get-Item -LiteralPath $out
Write-Output ("[TIS-S9b] out     {0}  ({1} bytes)" -f $i.FullName, $i.Length)
Write-Output ("[TIS-S9b] sha256  {0}" -f (Get-FileHash -Algorithm SHA256 $out).Hash)
