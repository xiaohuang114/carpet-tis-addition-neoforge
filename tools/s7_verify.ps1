# S7 / T3 - release jar health check (ASCII only: Windows PowerShell 5.1 reads non-BOM files as ANSI)
param(
    [string]$Jar = 'D:\Minecraft-mod-development\TIS-NeoForge\build\libs\carpet-tis-addition-1.82.0+mc1.21.1-neoforge.jar'
)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::OpenRead($Jar)
$names = @($zip.Entries | ForEach-Object { $_.FullName })
"JAR    : $Jar"
"SIZE   : $((Get-Item $Jar).Length) bytes"
"ENTRIES: $($names.Count)  (dirs: $(($names | Where-Object { $_.EndsWith('/') }).Count))"
$script:fail = 0
function Check([string]$label, [scriptblock]$test, [string]$detail) {
    $ok = & $test
    if ($ok) { "[PASS] $label  -- $detail" } else { "[FAIL] $label  -- $detail"; $script:fail++ }
}
function EntryText($entry) {
    $sr = New-Object System.IO.StreamReader($entry.Open())
    $t = $sr.ReadToEnd(); $sr.Close(); return $t
}

# ---------- 1. neoforge.mods.toml ----------
$tomlEntries = @($zip.Entries | Where-Object { $_.FullName -eq 'META-INF/neoforge.mods.toml' })
$toml = ''
if ($tomlEntries.Count -ge 1) { $toml = EntryText $tomlEntries[0] }
$modsBlocks = ([regex]::Matches($toml, '(?m)^\[\[mods\]\]')).Count
$mixinsBlock = ([regex]::Matches($toml, '(?m)^\[\[mixins\]\]')).Count
$modIds = @([regex]::Matches($toml, '(?m)^modId="([^"]+)"') | ForEach-Object { $_.Groups[1].Value })
"--- META-INF/neoforge.mods.toml ---"
Check '1a toml present & unique' { $tomlEntries.Count -eq 1 } "count=$($tomlEntries.Count)"
Check '1b [[mods]] == 1' { $modsBlocks -eq 1 } "count=$modsBlocks"
Check '1c [[mixins]] == 1' { $mixinsBlock -eq 1 } "count=$mixinsBlock"
Check '1d primary modId' { $modIds -contains 'carpet_tis_addition' } "modId lines=[$($modIds -join ', ')]"
"----- toml verbatim -----"
$toml
"----- toml end -----"

# ---------- 2. mixins.json ----------
$mxEntries = @($zip.Entries | Where-Object { $_.FullName -eq 'carpet-tis-addition.mixins.json' })
$mx = $null
Check '2a mixins.json present' { $mxEntries.Count -eq 1 } "count=$($mxEntries.Count)"
if ($mxEntries.Count -ge 1) { $mx = (EntryText $mxEntries[0]) | ConvertFrom-Json }
Check '2b mixins.Count == 587' { $mx.mixins.Count -eq 587 } "mixins=$($mx.mixins.Count)"
Check '2c client.Count == 12' { $mx.client.Count -eq 12 } "client=$($mx.client.Count)"
Check '2d required == true' { $mx.required -eq $true } "required=$($mx.required) compatibilityLevel=$($mx.compatibilityLevel) injectors.defaultRequire=$($mx.injectors.defaultRequire)"
Check '2e mixin plugin class in jar' { @($names | Where-Object { $_ -eq 'carpettisaddition/utils/mixin/CarpetTISAdditionMixinPlugin.class' }).Count -eq 1 } "plugin=$($mx.plugin)"

# ---------- 3. lang ----------
$enEntries = @($zip.Entries | Where-Object { $_.FullName -eq 'assets/carpettisaddition/lang/en_us.json' })
$zhEntries = @($zip.Entries | Where-Object { $_.FullName -eq 'assets/carpettisaddition/lang/zh_cn.json' })
Check '3a en_us.json present' { $enEntries.Count -eq 1 } "count=$($enEntries.Count)"
Check '3b zh_cn.json present' { $zhEntries.Count -eq 1 } "count=$($zhEntries.Count)"
$en = (EntryText $enEntries[0]) | ConvertFrom-Json
$zh = (EntryText $zhEntries[0]) | ConvertFrom-Json
$enKeys = @($en.PSObject.Properties.Name)
$zhKeys = @($zh.PSObject.Properties.Name)
Check '3c en_us key count == 1142' { $enKeys.Count -eq 1142 } "count=$($enKeys.Count)"
Check '3d zh_cn key count == 1142' { $zhKeys.Count -eq 1142 } "count=$($zhKeys.Count)"
$badTail = @(@($enKeys + $zhKeys) | Where-Object { $_.EndsWith('..') })
Check '3e no key ends with ..' { $badTail.Count -eq 0 } "bad=$($badTail.Count) [$($badTail -join ', ')]"
"----- 7 fixed keys (en_us) -----"
foreach ($k in @('carpettisaddition.command.info.world.weather.forecast.rain_in',
                 'carpettisaddition.command.speedtest.help',
                 'carpettisaddition.command.raid.status',
                 'carpettisaddition.tracker.lifetime.spawn_reason.trans_dimension',
                 'carpettisaddition.tracker.lifetime.removal_reason.death',
                 'carpettisaddition.tracker.lifetime.removal_reason.trans_dimension',
                 'carpettisaddition.util.teleport_hint')) {
    "[INFO] $k = $($en.$k)"
}

# ---------- 4. conditional-mixin ----------
$cmClasses = @($names | Where-Object { $_ -like 'me/fallenbreath/conditionalmixin/*.class' })
$plugin = @($names | Where-Object { $_ -eq 'me/fallenbreath/conditionalmixin/api/mixin/RestrictiveMixinConfigPlugin.class' })
Check '4a conditional-mixin classes' { $cmClasses.Count -ge 15 } "me/fallenbreath/conditionalmixin/**.class=$($cmClasses.Count)"
Check '4b RestrictiveMixinConfigPlugin' { $plugin.Count -eq 1 } "count=$($plugin.Count)"

# ---------- 5. FabricLoader uniqueness ----------
$fl = @($names | Where-Object { $_ -eq 'net/fabricmc/loader/api/FabricLoader.class' })
Check '5a FabricLoader.class present' { $fl.Count -ge 1 } "occurrences=$($fl.Count)"
Check '5b FabricLoader.class unique' { $fl.Count -eq 1 } "occurrences=$($fl.Count)"
$shimAll = @($names | Where-Object { $_ -like 'net/fabricmc/loader/api/*.class' })
"[INFO] shim classes in jar: $($shimAll.Count)"
$shimAll | ForEach-Object { "[INFO]   $_" }

# ---------- 6. tiny-mappings ----------
$tmReader = @($names | Where-Object { $_ -like 'net/fabricmc/mapping/reader/v2/*.class' })
$tmTree = @($names | Where-Object { $_ -like 'net/fabricmc/mapping/tree/*.class' })
$tmMap = @($names | Where-Object { $_ -like 'net/fabricmc/mappings/*' })
$tvf = @($names | Where-Object { $_ -eq 'net/fabricmc/mapping/reader/v2/TinyV2Factory.class' })
Check '6a tiny reader/v2 classes' { $tmReader.Count -eq 16 } "net/fabricmc/mapping/reader/v2/*.class=$($tmReader.Count)  (T1 inventory: 16 .class + 2 dir entries)"
Check '6b tiny tree classes' { $tmTree.Count -ge 20 } "net/fabricmc/mapping/tree/*.class=$($tmTree.Count)"
Check '6c tiny mappings classes' { $tmMap.Count -ge 30 } "net/fabricmc/mappings/**=$($tmMap.Count)"
Check '6d TinyV2Factory present' { $tvf.Count -eq 1 } "count=$($tvf.Count)"

# ---------- 7. dirty stuff ----------
$arch = @($names | Where-Object { $_ -like 'architectury_inject_*' })
$prev = @($names | Where-Object { $_ -eq 'previous-compilation-data.bin' })
$sig = @($names | Where-Object { $_.EndsWith('.SF') -or $_.EndsWith('.DSA') -or $_.EndsWith('.RSA') })
Check '7a no architectury_inject_*' { $arch.Count -eq 0 } "count=$($arch.Count)"
Check '7b no previous-compilation-data.bin' { $prev.Count -eq 0 } "count=$($prev.Count)"
Check '7c no SF/RSA/DSA' { $sig.Count -eq 0 } "count=$($sig.Count)"

# ---------- 8. sources / build script ----------
$cs = @($names | Where-Object { $_.StartsWith('compat-src') })
$bg = @($names | Where-Object { $_ -eq 'build.gradle' -or $_.EndsWith('/build.gradle') })
$java = @($names | Where-Object { $_.EndsWith('.java') })
Check '8a no compat-src content' { $cs.Count -eq 0 } "count=$($cs.Count)"
Check '8b no build.gradle' { $bg.Count -eq 0 } "count=$($bg.Count)"
Check '8c no .java sources' { $java.Count -eq 0 } "count=$($java.Count)"

"----- top level entry distribution -----"
$names | ForEach-Object { ($_ -split '/')[0] } | Group-Object | Sort-Object Name | ForEach-Object { "[INFO] $($_.Name)  x$($_.Count)" }
"TOTAL FAIL COUNT: $script:fail"
$zip.Dispose()
