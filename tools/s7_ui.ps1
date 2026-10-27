param(
  [Parameter(Mandatory=$true)][int]$TargetPid,
  [Parameter(Mandatory=$true)][ValidateSet('shot','click','dclick','move','rect')][string]$Action,
  [int]$X = 0, [int]$Y = 0,
  [string]$Name = 'shot'
)
Add-Type -AssemblyName System.Drawing
Add-Type @"
using System;
using System.Runtime.InteropServices;
public class S7UI {
  [DllImport("user32.dll")] public static extern bool GetWindowRect(IntPtr h, out RECT r);
  [DllImport("user32.dll")] public static extern bool GetClientRect(IntPtr h, out RECT r);
  [DllImport("user32.dll")] public static extern bool ClientToScreen(IntPtr h, ref POINT p);
  [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr h);
  [DllImport("user32.dll")] public static extern bool ShowWindow(IntPtr h, int c);
  [DllImport("user32.dll")] public static extern bool SetCursorPos(int x, int y);
  [DllImport("user32.dll")] public static extern void mouse_event(uint f, uint dx, uint dy, uint d, int e);
  public struct RECT { public int Left, Top, Right, Bottom; }
  public struct POINT { public int X, Y; }
}
"@
$root = 'D:\Minecraft-mod-development\TIS-NeoForge\build\shots\s7'
New-Item -ItemType Directory -Path $root -Force | Out-Null
$p = Get-Process -Id $TargetPid -ErrorAction Stop
$h = $p.MainWindowHandle
if ($h -eq [IntPtr]::Zero) { throw "no main window for pid $TargetPid" }
[void][S7UI]::ShowWindow($h, 9)
[void][S7UI]::SetForegroundWindow($h)
Start-Sleep -Milliseconds 350
$wr = New-Object S7UI+RECT; [void][S7UI]::GetWindowRect($h, [ref]$wr)
$cr = New-Object S7UI+RECT; [void][S7UI]::GetClientRect($h, [ref]$cr)
$pt = New-Object S7UI+POINT; $pt.X = 0; $pt.Y = 0
[void][S7UI]::ClientToScreen($h, [ref]$pt)
$cx = $pt.X; $cy = $pt.Y; $cw = $cr.Right - $cr.Left; $ch = $cr.Bottom - $cr.Top
switch ($Action) {
  'rect' { "window=$($wr.Left),$($wr.Top) $($wr.Right-$wr.Left)x$($wr.Bottom-$wr.Top)  client=$cx,$cy ${cw}x${ch}" }
  'move' { [void][S7UI]::SetCursorPos(($cx + $X), ($cy + $Y)); "moved to client $X,$Y (abs $($cx+$X),$($cy+$Y))" }
  'click' {
    [void][S7UI]::SetCursorPos(($cx + $X), ($cy + $Y)); Start-Sleep -Milliseconds 180
    [S7UI]::mouse_event(0x0002, 0, 0, 0, 0); Start-Sleep -Milliseconds 90
    [S7UI]::mouse_event(0x0004, 0, 0, 0, 0)
    "clicked client $X,$Y (abs $($cx+$X),$($cy+$Y))"
  }
  'dclick' {
    [void][S7UI]::SetCursorPos(($cx + $X), ($cy + $Y)); Start-Sleep -Milliseconds 180
    [S7UI]::mouse_event(0x0002, 0, 0, 0, 0); Start-Sleep -Milliseconds 60
    [S7UI]::mouse_event(0x0004, 0, 0, 0, 0); Start-Sleep -Milliseconds 90
    [S7UI]::mouse_event(0x0002, 0, 0, 0, 0); Start-Sleep -Milliseconds 60
    [S7UI]::mouse_event(0x0004, 0, 0, 0, 0)
    "double-clicked client $X,$Y"
  }
  'shot' {
    $bmp = New-Object System.Drawing.Bitmap $cw, $ch
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.CopyFromScreen($cx, $cy, 0, 0, $bmp.Size)
    $out = Join-Path $root "$Name.png"
    $bmp.Save($out, [System.Drawing.Imaging.ImageFormat]::Png)
    $g.Dispose(); $bmp.Dispose()
    "saved $out (${cw}x${ch} client at $cx,$cy)"
  }
}
