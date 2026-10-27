param(
  [Parameter(Mandatory=$true)][int]$TargetPid,
  [Parameter(Mandatory=$true)][string[]]$Seq,
  [int]$DelayMs = 120
)
Add-Type -AssemblyName System.Windows.Forms
Add-Type @"
using System;
using System.Runtime.InteropServices;
public class WU3 {
 [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr h);
 [DllImport("user32.dll")] public static extern bool ShowWindow(IntPtr h, int c);
 [DllImport("user32.dll")] public static extern IntPtr GetForegroundWindow();
 [DllImport("user32.dll")] public static extern uint GetWindowThreadProcessId(IntPtr h, out uint pid);
 [DllImport("user32.dll")] public static extern bool AttachThreadInput(uint a, uint b, bool f);
 [DllImport("kernel32.dll")] public static extern uint GetCurrentThreadId();
}
"@
$p = Get-Process -Id $TargetPid
$h = $p.MainWindowHandle
[WU3]::ShowWindow($h, 9) | Out-Null
$fgTid = [WU3]::GetWindowThreadProcessId([WU3]::GetForegroundWindow(), [ref]([uint32]0))
[WU3]::AttachThreadInput([WU3]::GetCurrentThreadId(), $fgTid, $true) | Out-Null
[WU3]::SetForegroundWindow($h) | Out-Null
[WU3]::AttachThreadInput([WU3]::GetCurrentThreadId(), $fgTid, $false) | Out-Null
Start-Sleep -Milliseconds 700
foreach ($k in $Seq) {
  [System.Windows.Forms.SendKeys]::SendWait($k)
  Start-Sleep -Milliseconds $DelayMs
}
"seq sent: $($Seq -join '|')"
