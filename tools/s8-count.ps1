# S8 / T1 - single-source mixin accounting for any log file (latest.log, srv log, client log).
# Prints exactly three metric lines per input file:
#   1) TIS applied   : "Mixing <mixin> from carpet-tis-addition.mixins.json into <target>"  (own mod ONLY)
#   2) Disabled mixin: "[TISCM] Disabled mixin ..." lines emitted by CarpetTISAdditionMixinPlugin
#   3) ERROR/FATAL/apply-failed: three-class substring count (ERROR, FATAL, Mixin apply failed|apply failed|Critical injection failure)
# NOTE: keep this file ASCII-only; the host PowerShell parses non-ASCII source as ANSI.
# Usage:
#   tools\s8-count.ps1 -Log run\logs\latest.log
#   tools\s8-count.ps1 -Log _run_s5.log,_s6_client4.log -Out _s8_t1_count.log

param(
    [Parameter(Mandatory = $true)][string[]]$Log,
    [string]$Out = ''
)

$ErrorActionPreference = 'Stop'
$lines = New-Object System.Collections.Generic.List[string]

$files = @()
foreach ($item in $Log) {
    if (Test-Path -LiteralPath $item -PathType Container) {
        $files += (Get-ChildItem -LiteralPath $item -Filter '*.log' -File | ForEach-Object { $_.FullName })
    }
    else {
        foreach ($p in ($item -split ',')) {
            $p = $p.Trim()
            if ($p) { $files += (Resolve-Path -LiteralPath $p).Path }
        }
    }
}

foreach ($f in $files) {
    $text = Get-Content -LiteralPath $f -Raw
    if ($null -eq $text) { $text = '' }

    # 1) own-mod applied: only the mixin config of this jar counts. Carpet / neoforge / others excluded by design.
    $applied = ([regex]::Matches($text, '(?m)^.*\bfrom carpet-tis-addition\.mixins\.json into\b.*$')).Count
    # per-config breakdown is printed as a trailing detail line (still one metric line for this row)
    $otherConfigs = [regex]::Matches($text, '\bfrom ([A-Za-z0-9_.\-]+\.mixins(?:\.\w+)?\.json) into\b') |
        ForEach-Object { $_.Groups[1].Value } | Group-Object | Sort-Object Count -Descending

    # 2) conditional-mixin skips
    $disabled = ([regex]::Matches($text, '\[TISCM\] Disabled mixin')).Count

    # 3) three classes
    $errCount = ([regex]::Matches($text, '\bERROR\b')).Count
    $fatalCount = ([regex]::Matches($text, '\bFATAL\b')).Count
    $applyFail = ([regex]::Matches($text, 'Mixin apply failed|apply failed|Critical injection failure')).Count

    $lines.Add(("FILE {0}  ({1} bytes)" -f $f, (Get-Item -LiteralPath $f).Length))
    $lines.Add(("  1) TIS applied (only 'from carpet-tis-addition.mixins.json into') : {0}" -f $applied))
    $lines.Add(("  2) Disabled mixin ('[TISCM] Disabled mixin')                      : {0}" -f $disabled))
    $lines.Add(("  3) ERROR/FATAL/apply-failed                                       : {0} / {1} / {2}" -f $errCount, $fatalCount, $applyFail))
    $breakdown = ($otherConfigs | ForEach-Object { "{0}={1}" -f $_.Name, $_.Count }) -join ' '
    $lines.Add(("     all mixin configs seen in this log                            : {0}" -f $breakdown))
}

$lines | ForEach-Object { Write-Output $_ }
if ($Out) { $lines | Set-Content -LiteralPath $Out -Encoding UTF8 }
