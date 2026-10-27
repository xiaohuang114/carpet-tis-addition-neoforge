# S3 precise fatal loop, continued (require=1 semantics).
# Seeds the already-known defect names from build\mixin-fatal-inventory.tsv and
# keeps disabling one new fatal entry per run -- the server can only report the
# first fatal injection, so one defect per run is the only precise way.
# ASCII only: Windows PowerShell 5.1 reads BOM-less files as ANSI.

$ErrorActionPreference = 'Stop'
$base = 'D:\Minecraft-mod-development\TIS-NeoForge'
$formalPath = Join-Path $base 'src\main\resources\carpet-tis-addition.mixins.json'
$surveyPath = Join-Path $base 'src\main\resources\tis-mixin-survey.json'
$tomlPath = Join-Path $base 'src\main\resources\META-INF\neoforge.mods.toml'
$utf8 = New-Object System.Text.UTF8Encoding($false)

function Read-Utf8([string]$p) { [System.IO.File]::ReadAllText($p, [System.Text.Encoding]::UTF8) }
function Write-Utf8([string]$p, [string]$t) { [System.IO.File]::WriteAllText($p, $t, $utf8) }

$formal = (Read-Utf8 $formalPath) | ConvertFrom-Json
$tomlOriginal = Read-Utf8 $tomlPath
Write-Utf8 $tomlPath ($tomlOriginal -replace 'config="carpet-tis-addition\.mixins\.json"', 'config="tis-mixin-survey.json"')

$disabled = New-Object System.Collections.Generic.List[string]
$invPath = Join-Path $base 'build\mixin-fatal-inventory.tsv'
if (Test-Path $invPath) {
    foreach ($line in [System.IO.File]::ReadAllLines($invPath)) {
        $parts = $line.Split("`t")
        if ($parts.Count -ge 2 -and $parts[1] -notin @('DONE', 'UNPARSED')) { if (-not $disabled.Contains($parts[1])) { $disabled.Add($parts[1]) } }
    }
}
"seeded=$($disabled.Count)"

$inventory = New-Object System.Collections.Generic.List[string]
foreach ($d in $disabled) { $inventory.Add("seed`t$d`t(known)") }

try {
    for ($iter = 1; $iter -le 80; $iter++) {
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
        $logName = "_run_s3_fatal2_$iter.log"
        $cmdline = '/c .\gradlew.bat --no-daemon --console=plain -PtisFull -Dmixin.debug.verbose=true runServer > ' + $logName + ' 2>&1'
        $p = Start-Process -FilePath 'cmd.exe' -ArgumentList $cmdline -WorkingDirectory $base -PassThru -WindowStyle Hidden
        if (-not $p.WaitForExit(300000)) { & taskkill.exe /F /T /PID $p.Id 2>&1 | Out-Null }
        $log = Read-Utf8 (Join-Path $base $logName)
        if ($log -match 'Done \(') { $inventory.Add("iter$iter`tDONE`t-"); break }

        $name = $null
        $reason = $null
        $m = [regex]::Match($log, 'Critical injection failure:[^\r\n]*?in [^\s:]+:([A-Za-z0-9_.$]+)')
        if ($m.Success) {
            $name = $m.Groups[1].Value
            $reason = ($m.Value -replace '\s+', ' ')
            if ($reason.Length -gt 260) { $reason = $reason.Substring(0, 260) }
        }
        if (-not $name) {
            $m = [regex]::Match($log, 'Invalid descriptor on [^\s:]+:([A-Za-z0-9_.$]+)')
            if ($m.Success) { $name = $m.Groups[1].Value; $reason = 'P-b: Invalid descriptor on handler' }
        }
        if (-not $name) {
            $m = [regex]::Match($log, '@Shadow field [^\r\n]*?was not located in the target class[^\r\n]*')
            if ($m.Success) {
                $reason = ($m.Value -replace '\s+', ' ')
                if ($reason.Length -gt 260) { $reason = $reason.Substring(0, 260) }
                $m2 = [regex]::Match($log, 'in [^\s:]+:([A-Za-z0-9_.$]+)')
                if ($m2.Success) { $name = $m2.Groups[1].Value }
            }
        }
        if (-not $name) { $inventory.Add("iter$iter`tUNPARSED`t$logName"); break }
        $inventory.Add("iter$iter`t$name`t$reason")
        $disabled.Add($name)
    }
}
finally {
    Write-Utf8 $tomlPath ($tomlOriginal -replace 'config="tis-mixin-survey\.json"', 'config="carpet-tis-addition.mixins.json"')
    if (Test-Path $surveyPath) { Remove-Item $surveyPath -Force }
}

$inventory | ForEach-Object { $_ }
"defectCount=" + $disabled.Count
$inventory | Set-Content (Join-Path $base 'build\mixin-fatal-inventory2.tsv')
