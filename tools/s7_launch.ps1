# S7 / T4 - minimal launcher: launch the REAL NeoForge client (version-isolated PCL2 instance)
# ASCII only (Windows PowerShell 5.1 ANSI parsing).
param(
  [string]$MC_ROOT   = 'D:\PCl2\.minecraft',
  [string]$VERSION   = '1.21.1-NeoForge_21.1.264',
  [string]$USER_NAME = 'S7Tester',
  [string]$JAVA      = 'C:\Program Files\Java\jdk-21.0.11\bin\java.exe',
  [switch]$MixinVerbose,
  [switch]$DryRun
)
$ErrorActionPreference = 'Stop'
$verDir  = Join-Path $MC_ROOT "versions\$VERSION"
$jsonPath = Join-Path $verDir "$VERSION.json"
$libRoot = Join-Path $MC_ROOT 'libraries'
$assetsRoot = Join-Path $MC_ROOT 'assets'
$natives = 'D:\Minecraft-mod-development\TIS-NeoForge\build\s7\natives'
$j = Get-Content $jsonPath -Raw | ConvertFrom-Json

function Test-Rules($rules) {
  if (-not $rules -or $rules.Count -eq 0) { return $true }
  $allowed = $false
  foreach ($r in $rules) {
    $match = $true
    if ($r.os) {
      if ($r.os.name -and $r.os.name -ne 'windows') { $match = $false }
      if ($r.os.arch -and $r.os.arch -ne 'x86_64') { $match = $false }
    }
    if ($r.features) { $match = $false }   # no optional features enabled
    if ($match) { $allowed = ($r.action -eq 'allow') }
  }
  return $allowed
}

$cp = New-Object System.Collections.Generic.List[string]
$nativesJars = New-Object System.Collections.Generic.List[string]
$missing = New-Object System.Collections.Generic.List[string]
$seen = New-Object 'System.Collections.Generic.HashSet[string]' ([System.StringComparer]::OrdinalIgnoreCase)
foreach ($lib in $j.libraries) {
  if (-not (Test-Rules $lib.rules)) { continue }
  $rel = $lib.downloads.artifact.path
  if (-not $rel) { continue }
  $full = Join-Path $libRoot ($rel -replace '/', '\')
  if (-not (Test-Path $full)) { $missing.Add($full); continue }
  if (-not $seen.Add($full)) { continue }   # securejarhandler dies on duplicate cp keys
  if ($lib.name -match ':natives-') {
    if ($lib.name -match ':natives-windows$') { $nativesJars.Add($full) }   # only win-x64; arm64/x86 would clobber the dlls
    continue
  }
  $cp.Add($full)
}
$verJar = Join-Path $verDir "$VERSION.jar"
if (-not (Test-Path $verJar)) { $missing.Add($verJar) }
$cp.Insert(0, $verJar)
"libraries on classpath : $($cp.Count)"
"natives jars (windows) : $($nativesJars.Count)"
if ($missing.Count -gt 0) { "MISSING FILES:"; $missing | ForEach-Object { "  $_" } } else { "all referenced files present" }

# extract natives
if (Test-Path $natives) { Remove-Item $natives -Recurse -Force }
New-Item -ItemType Directory -Path $natives -Force | Out-Null
Add-Type -AssemblyName System.IO.Compression.FileSystem
foreach ($nj in $nativesJars) {
  $z = [System.IO.Compression.ZipFile]::OpenRead($nj)
  foreach ($e in $z.Entries) {
    if ($e.FullName.EndsWith('.dll') -or $e.FullName.EndsWith('.so') -or $e.FullName.EndsWith('.dylib')) {
      $target = Join-Path $natives ([System.IO.Path]::GetFileName($e.FullName))
      [System.IO.Compression.ZipFileExtensions]::ExtractToFile($e, $target, $true)
    }
  }
  $z.Dispose()
}
"natives extracted      : $((Get-ChildItem $natives -File).Count) files -> $natives"

$classpathSep = ';'
$libDir = $libRoot
function Expand($s) {
  if ($null -eq $s) { return '' }
  return ($s.Replace('${natives_directory}', $natives).
            Replace('${launcher_name}', 's7launcher').
            Replace('${launcher_version}', '1.0').
            Replace('${classpath}', ($cp -join $classpathSep)).
            Replace('${classpath_separator}', $classpathSep).
            Replace('${library_directory}', $libDir).
            Replace('${version_name}', $VERSION).
            Replace('${auth_player_name}', $USER_NAME).
            Replace('${game_directory}', $verDir).
            Replace('${assets_root}', $assetsRoot).
            Replace('${assets_index_name}', $j.assetIndex.id).
            Replace('${auth_uuid}', '00000000000000000000000000000001').
            Replace('${auth_access_token}', '0').
            Replace('${clientid}', 's7client').
            Replace('${auth_xuid}', '0').
            Replace('${user_type}', 'legacy').
            Replace('${version_type}', 'release'))
}
$jvmArgs = New-Object System.Collections.Generic.List[string]
foreach ($a in $j.arguments.jvm) {
  if ($a -is [string]) { $x = Expand $a } else { if ((Test-Rules $a.rules)) { $x = Expand $a.value } else { $x = '' } }
  if ($x -and $x.Trim()) { $jvmArgs.Add($x) }
}
$gameArgs = New-Object System.Collections.Generic.List[string]
foreach ($a in $j.arguments.game) {
  if ($a -is [string]) { $x = Expand $a } else { if ((Test-Rules $a.rules)) { $x = Expand $a.value } else { $x = '' } }
  if ($x -and $x.Trim()) { $gameArgs.Add($x) }
}

# memory + log4j config (NeoForge ships a default; keep vanilla-ish)
$jvmArgs.Insert(0, '-Xmx3G')
$jvmArgs.Insert(1, "-Djava.library.path=$natives")
if ($MixinVerbose) {
  # only verbose + export-free. NOT countInjections: it turns every injector's expected count into a hard
  # `require`, and carpet's own MinecraftServer_pingPlayerSampleLimit injects 0/1 on 1.21.1 (upstream quirk,
  # dev run has countInjections off too) -> it would crash the client for a reason unrelated to this port.
  $jvmArgs.Insert(0, '-Dmixin.debug.verbose=true')
}

$all = New-Object System.Collections.Generic.List[string]
$all.AddRange($jvmArgs); $all.Add($j.mainClass); $all.AddRange($gameArgs)
$argString = ($all | ForEach-Object { if ($_ -match '\s') { '"' + $_ + '"' } else { $_ } }) -join ' '

$batDir = 'D:\Minecraft-mod-development\TIS-NeoForge\build\s7'
New-Item -ItemType Directory -Path $batDir -Force | Out-Null
$bat = Join-Path $batDir 'launch-client.bat'
"@echo off`r`ncd /d `"$verDir`"`r`n`"$JAVA`" $argString > `"$batDir\client-stdout.log`" 2>&1`r`n" | Set-Content -Path $bat -Encoding ASCII
"wrote launch bat: $bat"
"mainClass: $($j.mainClass)"
if ($DryRun) {
  "--- jvm args ---"; $jvmArgs | ForEach-Object { "  $_" }
  "--- game args ---"; $gameArgs | ForEach-Object { "  $_" }
  exit 0
}
$p = Start-Process -FilePath 'cmd.exe' -ArgumentList '/c', "`"$bat`"" -WindowStyle Minimized -PassThru
"started cmd pid=$($p.Id) -> $bat"
Start-Sleep -Seconds 6
$java = Get-CimInstance Win32_Process -Filter "Name='java.exe'" | Where-Object { $_.CommandLine -like '*forgeclient*' }
if ($java) { "java pid(s): $(($java | ForEach-Object { $_.ProcessId }) -join ',')"; ($java | Select-Object -First 1).ProcessId | Set-Content "$batDir\client.pid" } else { "WARN: no java process with forgeclient yet" }
