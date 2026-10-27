param(
  [Parameter(Mandatory=$true)][int]$TargetPid,
  [Parameter(Mandatory=$true)][string]$Text,
  [switch]$OpenChat,
  [switch]$PressEnter,
  [int]$CharDelayMs = 40
)
Add-Type @"
using System;
using System.Runtime.InteropServices;
public class WU4 {
 [DllImport("user32.dll")] public static extern bool PostMessage(IntPtr h, uint msg, IntPtr w, IntPtr l);
}
"@
$p = Get-Process -Id $TargetPid
$h = $p.MainWindowHandle
$WM_KEYDOWN = 0x100; $WM_KEYUP = 0x101; $WM_CHAR = 0x102
function Key([int]$vk, [int]$sc) {
  $lp = [IntPtr](($sc -shl 16) -bor 1)
  [WU4]::PostMessage($h, $WM_KEYDOWN, [IntPtr]$vk, $lp) | Out-Null
  Start-Sleep -Milliseconds 60
  $up = [IntPtr](($sc -shl 16) -bor 1 -bor (1 -shl 30) -bor (1 -shl 31))
  [WU4]::PostMessage($h, $WM_KEYUP, [IntPtr]$vk, $up) | Out-Null
  Start-Sleep -Milliseconds 120
}
if ($OpenChat) { Key 0x54 0x14; Start-Sleep -Milliseconds 500 }
foreach ($ch in $Text.ToCharArray()) {
  [WU4]::PostMessage($h, $WM_CHAR, [IntPtr][int]$ch, [IntPtr]1) | Out-Null
  Start-Sleep -Milliseconds $CharDelayMs
}
if ($PressEnter) { Key 0x0D 0x1C; Start-Sleep -Milliseconds 400 }
"posted: openChat=$OpenChat text='$Text' enter=$PressEnter"
