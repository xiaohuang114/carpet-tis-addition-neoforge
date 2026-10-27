# TIS-S3 mixin survey harness (dev only, never shipped; delete together with tis-mixin-survey.json at S3 step 4)
#
# Why iterative: the survey config lowers injectors.defaultRequire to 0, which only downgrades
# "injection point not found" to a warning. Callback-descriptor mismatches, @Overwrite conflicts and
# APPLY-phase failures are still hard Mixin errors (InvalidInjectionException / InvalidMixinException):
# they stay FATAL and abort server startup even with require=0. So "one run lists every failure" does
# not hold on real Mixin 0.8.7.
#
# Loop: run once -> scrape the failed mixin names out of the FATAL block -> append them to
# build\survey-disabled.txt -> regenerate tis-mixin-survey.json as (formal config - disabled) -> repeat
# until the log shows "Done (" or no new failure is found.
# The formal config carpet-tis-addition.mixins.json is never touched by this script.
param(
    [int]$MaxIterations = 40,
    [string]$Log = '_run_s3_survey.log'
)

$ErrorActionPreference = 'Stop'
$repo = Split-Path -Parent $PSScriptRoot
Set-Location $repo

$formal = Get-Content 'src\main\resources\carpet-tis-addition.mixins.json' -Raw | ConvertFrom-Json
$surveyPath = Join-Path $repo 'src\main\resources\tis-mixin-survey.json'
$disabledFile = Join-Path $repo 'build\survey-disabled.txt'
$logDir = Join-Path $repo 'build\survey-logs'
$reasonFile = Join-Path $repo 'build\survey-reasons.tsv'
New-Item -ItemType Directory -Force -Path $logDir | Out-Null
[System.IO.File]::WriteAllText($reasonFile, '', (New-Object System.Text.UTF8Encoding($false)))
if (-not (Test-Path $disabledFile)) { New-Item -ItemType File -Path $disabledFile | Out-Null }
$prefix = $formal.package + '.'
$utf8 = New-Object System.Text.UTF8Encoding($false)

for ($i = 1; $i -le $MaxIterations; $i++)
{
    $disabled = @(Get-Content $disabledFile | ForEach-Object { $_.Trim().TrimStart([char]0xFEFF) } | Where-Object { $_ -ne '' })
    $mixins = @($formal.mixins | Where-Object { $disabled -notcontains $_ })
    $client = @($formal.client | Where-Object { $disabled -notcontains $_ })

    $survey = [ordered]@{
        required           = $true
        package            = $formal.package
        compatibilityLevel = $formal.compatibilityLevel
        plugin             = $formal.plugin
        mixins             = $mixins
        client             = $client
        injectors          = [ordered]@{ defaultRequire = 0; defaultRequireClient = 0 }
    }
    [System.IO.File]::WriteAllText($surveyPath, ($survey | ConvertTo-Json -Depth 8), $utf8)
    Write-Host ("=== iter {0}: {1} mixins + {2} client enabled, {3} disabled ===" -f $i, $mixins.Count, $client.Count, $disabled.Count)

    # A successfully started server never exits by itself, so the runner needs a timeout guard.
    $cmdline = "/c .\gradlew.bat --no-daemon --console=plain -PtisFull -Dmixin.debug.verbose=true -Dmixin.debug.export=true runServer > $Log 2>&1"
    $proc = Start-Process -FilePath 'cmd.exe' -ArgumentList $cmdline -PassThru -WindowStyle Hidden
    if (-not $proc.WaitForExit(300000))
    {
        Write-Host ('    runServer still alive after 300s (startup succeeded), killing process tree pid={0}' -f $proc.Id)
        & taskkill.exe /F /T /PID $proc.Id 2>&1 | Out-Null
        Start-Sleep -Seconds 3
    }
    Copy-Item $Log (Join-Path $logDir ("iter-{0}.log" -f $i)) -Force
    $text = Get-Content $Log -Raw

    $applied = ([regex]::Matches($text, 'Mixing .+? from tis-mixin-survey\.json into')).Count
    Write-Host ("    applied(lines) = {0}" -f $applied)

    if ($text -match 'Done \(') { Write-Host ("    DONE (server reached Done) at iter {0}" -f $i); break }

    # Only the canonical MixinProcessor failure line names a genuinely broken mixin. Scraping
    # "Mixin :" report headers or "tis-mixin-survey.json:<x> from" INFO lines picks up @Accessor
    # rename notices and crash-report mixin tags, which disables healthy mixins (that bug produced
    # 71 false positives in the first pass).
    $names = New-Object System.Collections.Generic.List[string]
    $reasonRows = New-Object System.Collections.Generic.List[string]
    $failPat = 'Mixin apply for mod carpet_tis_addition failed tis-mixin-survey\.json:([\w.$]+) from mod carpet_tis_addition -> ([\w.$]+): (\S+) (.*)$'
    foreach ($ln in ($text -split "`r?`n")) {
        if ($ln -match $failPat) {
            $names.Add($matches[1])
            $reasonRows.Add(("{0}`t{1}`t{2}`t{3}" -f $i, $matches[1], $matches[2], ($matches[3] + ' ' + $matches[4])))
        }
    }
    if ($reasonRows.Count -gt 0) { [System.IO.File]::AppendAllLines($reasonFile, $reasonRows, $utf8) }
    $names = @($names | Sort-Object -Unique)

    if ($names.Count -eq 0)
    {
        Write-Host '    no mixin failure parsed -> stopping (inspect the log by hand)'
        break
    }
    [System.IO.File]::AppendAllText($disabledFile, (($names -join "`r`n") + "`r`n"), $utf8)
    Write-Host ("    disabled +{0}: {1}" -f $names.Count, ($names -join ', '))
}

Write-Host ('=== survey-disabled.txt: {0} entries ===' -f (Get-Content $disabledFile | Where-Object { $_.Trim() -ne '' }).Count)
