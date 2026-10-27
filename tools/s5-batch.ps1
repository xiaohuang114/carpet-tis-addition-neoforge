# S5 command-surface acceptance runner.
# Sends every command through tools\s5-rcon.ps1 in the fixed order and records raw responses.
# Output:  build\s5-command-raw.txt      (full raw responses, one block per command)
#          build\s5-command-summary.tsv  (cmd / line count / first response line)
# Keep ASCII-only source (host PowerShell 5.1 parses non-ASCII as ANSI).

param(
    [string]$OutRaw = 'build\s5-command-raw.txt',
    [string]$OutSummary = 'build\s5-command-summary.tsv'
)

$ErrorActionPreference = 'Continue'
$base = (Get-Location).Path
$rcon = Join-Path $base 'tools\s5-rcon.ps1'

# ordered acceptance list (32 entries, exactly as specified)
$commands = @(
    '/carpet',
    '/carpet tisAdditionEnabled',
    '/carpet tisAdditionEnabled false',
    '/carpet tisAdditionEnabled',
    '/carpet tisAdditionEnabled true',
    '/tis',
    '/tis help',
    '/tis info',
    '/tis raid',
    '/tis speedtest',
    '/tick',
    '/tick query',
    '/tick freeze',
    '/tick step 5',
    '/tick rate 40',
    '/tick unfreeze',
    '/player Steve spawn',
    '/player Steve kill',
    '/counter',
    '/counter redstone',
    '/counter dye',
    '/log',
    '/log mobcaps',
    '/log microTiming',
    '/log phantom',
    '/lifetime',
    '/lifetime tracking',
    '/info entity',
    '/info block',
    '/info world',
    '/raid list',
    '/man',
    '/spawn',
    '/stat'
)

$raw = New-Object System.Collections.Generic.List[string]
$summary = New-Object System.Collections.Generic.List[string]
$summary.Add("idx`tcmd`tlines`tfirstLine")
$i = 0
foreach ($c in $commands) {
    $i++
    $resp = ''
    try {
        $resp = (& $rcon -Command $c 2>&1 | Out-String)
    }
    catch {
        $resp = 'RCON-EXCEPTION: ' + $_.Exception.Message
    }
    $resp = $resp -replace "`r`n", "`n"
    $resp = $resp.TrimEnd("`n")
    $lines = @()
    if ($resp.Length -gt 0) { $lines = $resp -split "`n" }
    $raw.Add("=== CMD $i : $c ===")
    $raw.Add($resp)
    $raw.Add("=== END $i ===")
    $first = ''
    if ($lines.Count -gt 0) { $first = $lines[0] }
    $summary.Add(("{0}`t{1}`t{2}`t{3}" -f $i, $c, $lines.Count, $first))
    Write-Output ("[$i] {0}  ->  {1} line(s) | {2}" -f $c, $lines.Count, $first)
    Start-Sleep -Milliseconds 250
}

$rawPath = Join-Path $base $OutRaw
$sumPath = Join-Path $base $OutSummary
[System.IO.File]::WriteAllLines($rawPath, $raw.ToArray(), (New-Object System.Text.UTF8Encoding($false)))
[System.IO.File]::WriteAllLines($sumPath, $summary.ToArray(), (New-Object System.Text.UTF8Encoding($false)))
Write-Output ("WROTE " + $rawPath)
Write-Output ("WROTE " + $sumPath)
Write-Output ("TOTAL COMMANDS = " + $commands.Count)
