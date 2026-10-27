# S3 precise fatal loop (require=1 semantics).
# One defect per run: run the survey config with require=1, take the FIRST fatal
# mixin entry from the log, disable exactly that entry, repeat until the server
# reaches "Done (".
# ASCII only on purpose: Windows PowerShell 5.1 reads BOM-less files as ANSI.

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
$inventory = New-Object System.Collections.Generic.List[string]

try {
    for ($iter = 1; $iter -le 20; $iter++) {
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
        $logName = "_run_s3_fatal_$iter.log"
        $cmdline = '/c .\gradlew.bat --no-daemon --console=plain -PtisFull -Dmixin.debug.verbose=true runServer > ' + $logName + ' 2>&1'
        $p = Start-Process -FilePath 'cmd.exe' -ArgumentList $cmdline -WorkingDirectory $base -PassThru -WindowStyle Hidden
        if (-not $p.WaitForExit(300000)) { & taskkill.exe /F /T /PID $p.Id 2>&1 | Out-Null }
        $log = Read-Utf8 (Join-Path $base $logName)
        if ($log -match 'Done \(') {
            $inventory.Add("iter$iter`tDONE`t-")
            break
        }
        $name = $null
        $reason = $null
        if ($log -match 'Mixin apply for mod carpet_tis_addition failed [^\s:]+:([A-Za-z0-9_.$]+)') {
            $name = $matches[1]
            if ($log -match 'Invalid descriptor') { $reason = 'P-b: @Inject handler descriptor mismatch against coffee' }
            elseif ($log -match 'was not located in the target class') { $reason = 'P-a: @Shadow member missing' }
            elseif ($log -match 'Critical injection failure') { $reason = 'P-b: injection point not found' }
            elseif ($log -match 'InvalidInjectionException') { $reason = 'P-b: invalid injection' }
            elseif ($log -match 'InvalidMixinException') { $reason = 'P-a: mixin apply exception' }
            else { $reason = 'P-?: see log' }
        }
        if (-not $name) {
            if ($log -match 'Critical injection failure: .*? in [^\s:]+:([A-Za-z0-9_.$]+)') { $name = $matches[1]; $reason = 'P-b: injection point not found (critical)' }
            elseif ($log -match 'Invalid descriptor on [^\s:]+:([A-Za-z0-9_.$]+)') { $name = $matches[1]; $reason = 'P-b: descriptor mismatch' }
            elseif ($log -match 'InvalidMixinException[^\n]*[^\s:]+:([A-Za-z0-9_.$]+)') { $name = $matches[1]; $reason = 'P-a: mixin exception' }
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
$inventory | Set-Content (Join-Path $base 'build\mixin-fatal-inventory.tsv')
