# S9 / T3 - server bench runner.
# Prepares _s8_srv\mods with an explicit jar list, launches the NeoForge 21.1.264 dedicated server,
# streams stdout+stderr into _s9_logs\<env>.stdout.log, waits for either "Done (" or process exit,
# then sends "stop" and archives logs\latest.log as _s9_logs\<env>.latest.log.
# NOTE: keep this file ASCII-only; the host PowerShell parses non-ASCII source as ANSI.
#
# Usage:
#   tools\s9-run-env.ps1 -Env A -Mods "@carpet,@tis" -Expect fail
#   tools\s9-run-env.ps1 -Env B -Mods "@carpet,@tis,@cm,@tiny" -Expect done
param(
    [Parameter(Mandatory = $true)][string]$Env,
    [Parameter(Mandatory = $true)][string]$Mods,
    [Parameter(Mandatory = $true)][ValidateSet('done', 'fail')][string]$Expect,
    [int]$TimeoutSec = 300
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot   # project root (= parent of tools\), relocatable
$srv = Join-Path $root '_s8_srv'
$java = 'C:\Program Files\Java\jdk-21.0.11\bin\java.exe'
$logs = Join-Path $root '_s9_logs'
New-Item -ItemType Directory -Force -Path $logs | Out-Null

# ---- stage payload aliases once ----
$payload = Join-Path $root '_s9_payload'
New-Item -ItemType Directory -Force -Path $payload | Out-Null

# ---- alias table: keeps the command lines in S9-ACCEPT.md short and unambiguous ----
$alias = @{
    '@carpet' = Join-Path $payload 'carpet-1.4.147+v1.0.8-251027.jar'
    '@tis'    = Join-Path $root 'build\libs\carpet-tis-addition-1.82.0+neoforge.1.21.1.jar'
    '@tisold' = Join-Path $payload 'carpet-tis-addition-1.82.0+neoforge.1.21.1.S8-flattened.jar'
    '@cm'     = Join-Path $root 'build\libs\conditional-mixin-neoforge-0.6.4.jar'
    '@cmext'  = Join-Path $payload 'conditional-mixin-neoforge-0.6.4.tweakermore-jij.jar'
    '@tiny'   = Join-Path $root 'build\libs\tiny-mappings-lib.jar'
}
if (-not (Test-Path (Join-Path $payload 'carpet-1.4.147+v1.0.8-251027.jar'))) {
    Copy-Item (Join-Path $root 'libs\carpet.jar') (Join-Path $payload 'carpet-1.4.147+v1.0.8-251027.jar') -Force
}
if (-not (Test-Path (Join-Path $payload 'carpet-tis-addition-1.82.0+neoforge.1.21.1.S8-flattened.jar'))) {
    Copy-Item (Join-Path $root 'dist\mods\carpet-tis-addition-1.82.0+neoforge.1.21.1.jar') `
        (Join-Path $payload 'carpet-tis-addition-1.82.0+neoforge.1.21.1.S8-flattened.jar') -Force
}
if (-not (Test-Path (Join-Path $payload 'conditional-mixin-neoforge-0.6.4.tweakermore-jij.jar'))) {
    Copy-Item (Join-Path $root '_tmp\cm-extract\conditional-mixin-neoforge-0.6.4.jar') `
        (Join-Path $payload 'conditional-mixin-neoforge-0.6.4.tweakermore-jij.jar') -Force
}

# ---- rebuild mods dir ----
$modsDir = Join-Path $srv 'mods'
if (Test-Path $modsDir) { Remove-Item -Recurse -Force $modsDir }
New-Item -ItemType Directory -Force -Path $modsDir | Out-Null
Write-Output "== env $Env : mods =="
foreach ($m in ($Mods -split ',')) {
    $m = $m.Trim(); if (-not $m) { continue }
    $resolved = if ($alias.ContainsKey($m)) { $alias[$m] } else { $m }
    if (-not (Test-Path -LiteralPath $resolved)) { throw "missing mod payload: $resolved (token $m)" }
    $dest = Join-Path $modsDir (Split-Path $resolved -Leaf)
    Copy-Item -LiteralPath $resolved -Destination $dest -Force
    Write-Output ("   {0,-70} {1} bytes" -f (Split-Path $resolved -Leaf), (Get-Item -LiteralPath $dest).Length)
}

# ---- rotate logs so latest.log only carries this run ----
$srvLatest = Join-Path $srv 'logs\latest.log'
if (Test-Path $srvLatest) { Remove-Item -Force $srvLatest }

$stdoutLog = Join-Path $logs ("{0}.stdout.log" -f $Env)
if (Test-Path $stdoutLog) { Remove-Item -Force $stdoutLog }

$argsList = @('-Xmx3G',
    ('@' + (Join-Path $srv 'libraries\net\neoforged\neoforge\21.1.264\win_args.txt')),
    'nogui')
$psi = New-Object System.Diagnostics.ProcessStartInfo
$psi.FileName = $java
$psi.WorkingDirectory = $srv
$psi.Arguments = ($argsList | ForEach-Object { if ($_ -like '@*') { $_ } else { $_ } }) -join ' '
$psi.RedirectStandardInput = $true
$psi.RedirectStandardOutput = $true
$psi.RedirectStandardError = $true
$psi.UseShellExecute = $false
$psi.StandardOutputEncoding = [System.Text.Encoding]::UTF8
$psi.StandardErrorEncoding = [System.Text.Encoding]::UTF8

Write-Output "== env $Env : launching (expect=$Expect, timeout=${TimeoutSec}s) =="
$proc = [System.Diagnostics.Process]::Start($psi)
$writer = New-Object System.IO.StreamWriter($stdoutLog, $false, (New-Object System.Text.UTF8Encoding($false)))
$done = $false
$deadline = (Get-Date).AddSeconds($TimeoutSec)
while (-not $proc.HasExited -and (Get-Date) -lt $deadline) {
    $line = $proc.StandardOutput.ReadLine()
    if ($null -ne $line) {
        $writer.WriteLine($line)
        $writer.Flush()
        if ($line -match 'Done \(' -and -not $done) {
            $done = $true
            Write-Output "   [detected] $line"
            Start-Sleep -Seconds 2
            $proc.StandardInput.WriteLine('stop')
            $proc.StandardInput.Flush()
        }
        if ($line -match 'Missing or unsupported mandatory dependencies|ResolutionException|Duplicate mods|Failure during mod loading|Fatal errors detected|detected errors during startup') {
            Write-Output "   [marker] $line"
        }
    }
    else {
        if ($proc.HasExited) { break }
        Start-Sleep -Milliseconds 50
    }
}
if (-not $proc.HasExited) {
    $proc.Kill($true)
    Write-Output "   [timeout] killed after ${TimeoutSec}s"
}
$proc.WaitForExit(15000) | Out-Null
$errTxt = $proc.StandardError.ReadToEnd()
if ($errTxt) { $writer.WriteLine('--- STDERR ---'); $writer.WriteLine($errTxt) }
$writer.Flush(); $writer.Close()
$exitCode = if ($proc.HasExited) { $proc.ExitCode } else { '?' }

Write-Output "   exit code = $exitCode"
if (Test-Path $srvLatest) {
    $arch = Join-Path $logs ("{0}.latest.log" -f $Env)
    Copy-Item -LiteralPath $srvLatest -Destination $arch -Force
    Write-Output ("   archived  {0} ({1} bytes)" -f $arch, (Get-Item -LiteralPath $arch).Length)
} else {
    Write-Output "   archived  (no logs/latest.log produced)"
}
Write-Output ("   verdict   : {0}" -f $(if ($done) { 'SERVER REACHED Done' } else { 'SERVER DID NOT REACH Done' }))

