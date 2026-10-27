# S4 single-shot authoritative runner.
# Runs the FORMAL mixin config (carpet-tis-addition.mixins.json) through the toml,
# no survey copy, no seed exclusion. One defect per run: the server can only report
# the first fatal injection, so fix one, rerun.
# ASCII only: Windows PowerShell 5.1 reads BOM-less files as ANSI.
param(
    [string]$LogName = '_run_s4_iter.log',
    [int]$TimeoutSec = 420
)

$ErrorActionPreference = 'Stop'
$base = 'D:\Minecraft-mod-development\TIS-NeoForge'
$logPath = Join-Path $base $LogName

if (Test-Path $logPath) { Remove-Item $logPath -Force }

$cmdline = '/c .\gradlew.bat --no-daemon --console=plain -PtisFull -Dmixin.debug.verbose=true runServer > ' + $LogName + ' 2>&1'
$p = Start-Process -FilePath 'cmd.exe' -ArgumentList $cmdline -WorkingDirectory $base -PassThru -WindowStyle Hidden
if (-not $p.WaitForExit($TimeoutSec * 1000)) { & taskkill.exe /F /T /PID $p.Id 2>&1 | Out-Null }

$utf8 = New-Object System.Text.UTF8Encoding($false)
if (-not (Test-Path $logPath)) { 'LOG_MISSING'; exit 2 }
$log = [System.IO.File]::ReadAllText($logPath, [System.Text.Encoding]::UTF8)

if ($log -match 'Done \(') {
    'RESULT=DONE'
    ([regex]::Match($log, 'Done \([^)]*\)[^\r\n]*')).Value
    exit 0
}

$name = $null
$m = [regex]::Match($log, 'Mixin apply for mod carpet_tis_addition failed [^\s:]+:([A-Za-z0-9_.$]+)')
if ($m.Success) { $name = $m.Groups[1].Value }

$reason = 'NONE'
$mf = [regex]::Match($log, 'Critical injection failure:[^\r\n]*')
if ($mf.Success) { $reason = ($mf.Value -replace '\s+', ' ') }
else {
    $m2 = [regex]::Match($log, 'InvalidInjectionException:[^\r\n]*')
    if ($m2.Success) { $reason = ($m2.Value -replace '\s+', ' ') }
    else {
        $m3 = [regex]::Match($log, 'Invalid descriptor on[^\r\n]*')
        if ($m3.Success) { $reason = ($m3.Value -replace '\s+', ' ') }
        else {
            $m4 = [regex]::Match($log, 'MixinApplyError[^\r\n]*')
            if ($m4.Success) { $reason = ($m4.Value -replace '\s+', ' ') }
        }
    }
}

if ($name) {
    'RESULT=FATAL'
    "MIXIN=$name"
    "REASON=$reason"
} else {
    'RESULT=OTHER'
    "REASON=$reason"
    '--- tail 25 ---'
    ($log -split "\r?\n" | Where-Object { $_ -ne '' } | Select-Object -Last 25) -join "`n"
}
exit 1
