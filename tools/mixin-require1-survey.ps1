# S3 survey harness (require=1 mode).
# Rebuilds tis-mixin-survey.json from the formal config minus the entries that keep
# failing, runs runServer, parses the next failing entry, repeats.
# ASCII only on purpose: Windows PowerShell 5.1 reads BOM-less files as ANSI.

$ErrorActionPreference = 'Stop'
$base = 'D:\Minecraft-mod-development\TIS-NeoForge'
$formalPath = Join-Path $base 'src\main\resources\carpet-tis-addition.mixins.json'
$surveyPath = Join-Path $base 'src\main\resources\tis-mixin-survey.json'
$tomlPath = Join-Path $base 'src\main\resources\META-INF\neoforge.mods.toml'
$utf8 = New-Object System.Text.UTF8Encoding($false)

function Read-Utf8([string]$p) { [System.IO.File]::ReadAllText($p, [System.Text.Encoding]::UTF8) }
function Write-Utf8([string]$p, [string]$t) { [System.IO.File]::WriteAllText($p, $t, $utf8) }

$formalText = Read-Utf8 $formalPath
$formal = $formalText | ConvertFrom-Json
$tomlOriginal = Read-Utf8 $tomlPath
Write-Utf8 $tomlPath ($tomlOriginal -replace 'config="carpet-tis-addition\.mixins\.json"', 'config="tis-mixin-survey.json"')

$disabled = New-Object System.Collections.Generic.List[string]
$seedPath = Join-Path $base 'build\survey-req1-disabled.txt'
if (Test-Path $seedPath) { Get-Content $seedPath | Where-Object { $_.Trim() } | ForEach-Object { $disabled.Add($_.Trim()) } }
"seeded=" + $disabled.Count
$report = New-Object System.Collections.Generic.List[string]

try {
    for ($iter = 1; $iter -le 40; $iter++) {
        $mixins = @($formal.mixins | Where-Object { -not $disabled.Contains($_) })
        $survey = [ordered]@{
            required           = $true
            package            = $formal.package
            compatibilityLevel = $formal.compatibilityLevel
            plugin             = $formal.plugin
            mixins             = $mixins
            client             = @($formal.client)
            injectors          = [ordered]@{ defaultRequire = 1; defaultRequireClient = 1 }
        }
        Write-Utf8 $surveyPath ($survey | ConvertTo-Json -Depth 8)
        $logName = "_run_s3_req1_$iter.log"
        $logPath = Join-Path $base $logName
        $cmdline = '/c .\gradlew.bat --no-daemon --console=plain -PtisFull -Dmixin.debug.verbose=true runServer > ' + $logName + ' 2>&1'
        $p = Start-Process -FilePath 'cmd.exe' -ArgumentList $cmdline -WorkingDirectory $base -PassThru -WindowStyle Hidden
        if (-not $p.WaitForExit(300000)) { & taskkill.exe /F /T /PID $p.Id 2>&1 | Out-Null }
        $log = Read-Utf8 $logPath
        $done = $log -match 'Done \('
        $failed = @([regex]::Matches($log, '(?:tis-mixin-survey\.json|carpet-tis-addition\.mixins\.json):([A-Za-z0-9_.$]+)') | ForEach-Object { $_.Groups[1].Value } | Sort-Object -Unique)
        $report.Add("iter$iter done=$done failed=" + ($failed -join ','))
        if ($done -or $failed.Count -eq 0) { break }
        foreach ($f in $failed) { if (-not $disabled.Contains($f)) { $disabled.Add($f) } }
    }
}
finally {
    Write-Utf8 $tomlPath ($tomlOriginal -replace 'config="tis-mixin-survey\.json"', 'config="carpet-tis-addition.mixins.json"')
    if (Test-Path $surveyPath) { Remove-Item $surveyPath -Force }
}

$report | ForEach-Object { $_ }
"disabledCount=" + $disabled.Count
$disabled | Set-Content (Join-Path $base 'build\survey-req1-disabled.txt')
$disabled | ForEach-Object { $_ }
