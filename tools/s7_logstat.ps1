# S7 - log statistics (ASCII only). Usage: s7_logstat.ps1 -Path <log> [-Label dev]
param([string]$Path, [string]$Label = 'log')
if (-not (Test-Path $Path)) { "$Label : FILE MISSING $Path"; exit 1 }
$t = Get-Content $Path
"===== $Label ====="
"file      : $Path"
"lines     : $($t.Count)"
"bytes     : $((Get-Item $Path).Length)"
function C([string]$pattern) { return @($t | Select-String -Pattern $pattern).Count }
"ERROR                        : $((C '\] ERROR\]') + (C '\[ERROR\]' ))"
"FATAL                        : $((C '\] FATAL\]') + (C '\[FATAL\]'))"
"WARN                         : $((C '\] WARN\]') + (C '\[WARN\]'))"
"Mixin apply failed           : $(C 'Mixin apply failed')"
"Critical injection failure   : $(C 'Critical injection failure')"
"NoClassDefFoundError         : $(C 'NoClassDefFoundError')"
"NoSuchMethodError            : $(C 'NoSuchMethodError')"
"NoSuchFieldError             : $(C 'NoSuchFieldError')"
"ClassNotFoundException       : $(C 'ClassNotFoundException')"
"applied  (Mixin: ... applied) : $(C 'Mixing|has been applied|Mixin apply')"
"[TISCM] Disabled mixin       : $(C '\[TISCM\] Disabled mixin')"
"[TISCM] lines total          : $(C '\[TISCM\]')"
"mixin debug 'Mixing '        : $(C 'Mixing ')"
"mixin verbose 'Successfully' : $(C 'Successfully')"
"Exception in thread          : $(C 'Exception in thread')"
"caused by                    : $(C 'Caused by')"
"--- ERROR/FATAL raw lines (max 20) ---"
$t | Select-String -Pattern '\] (ERROR|FATAL)\]' | Select-Object -First 20 | ForEach-Object { "  L$($_.LineNumber): $($_.Line)" }
"--- mixin summary lines (max 12) ---"
$t | Select-String -Pattern 'Mixing|applied|Mixins' | Select-Object -First 12 | ForEach-Object { "  L$($_.LineNumber): $($_.Line.Substring(0,[Math]::Min(200,$_.Line.Length)))" }
