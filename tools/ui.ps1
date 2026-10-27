param([string]$Action='shot',[int]$X=0,[int]$Y=0,[string]$Name='shot',[string]$Text='',[int]$TargetPid=9664)
Add-Type -AssemblyName System.Drawing
Add-Type -AssemblyName System.Windows.Forms
Add-Type @"
using System;
using System.Runtime.InteropServices;
public class WU2 {
 [DllImport("user32.dll")] public static extern bool GetWindowRect(IntPtr h, out RECT r);
 [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr h);
 [DllImport("user32.dll")] public static extern bool ShowWindow(IntPtr h, int c);
 [DllImport("user32.dll")] public static extern bool SetCursorPos(int x, int y);
 [DllImport("user32.dll")] public static extern void mouse_event(uint f, uint dx, uint dy, uint d, int e);
 [DllImport("user32.dll")] public static extern IntPtr GetForegroundWindow();
 public struct RECT { public int Left, Top, Right, Bottom; }
}
"@
$root = 'D:\Minecraft-mod-development\TIS-NeoForge'
$p = Get-Process -Id $TargetPid -ErrorAction Stop
$h = $p.MainWindowHandle
[WU2]::ShowWindow($h, 9) | Out-Null
[WU2]::SetForegroundWindow($h) | Out-Null
Start-Sleep -Milliseconds 400
$r = New-Object WU2+RECT
[WU2]::GetWindowRect($h, [ref]$r) | Out-Null
$w = $r.Right - $r.Left; $hh = $r.Bottom - $r.Top
switch ($Action) {
  'click' {
    [WU2]::SetCursorPos(($r.Left + $X), ($r.Top + $Y)) | Out-Null
    Start-Sleep -Milliseconds 150
    [WU2]::mouse_event(0x0002, 0, 0, 0, 0)
    Start-Sleep -Milliseconds 80
    [WU2]::mouse_event(0x0004, 0, 0, 0, 0)
    "clicked $X,$Y (abs $($r.Left+$X),$($r.Top+$Y))"
  }
  'move' { [WU2]::SetCursorPos(($r.Left + $X), ($r.Top + $Y)) | Out-Null; "moved" }
  'shot' {
    $bmp = New-Object System.Drawing.Bitmap $w, $hh
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.CopyFromScreen($r.Left, $r.Top, 0, 0, $bmp.Size)
    $out = "$root\build\shots\$Name.png"
    $bmp.Save($out, [System.Drawing.Imaging.ImageFormat]::Png)
    $g.Dispose(); $bmp.Dispose()
    "saved $out (${w}x${hh} at $($r.Left),$($r.Top))"
  }
  'type' {
    $null = [System.Windows.Forms.SendKeys]::SendWait($Text)
    "typed: $Text"
  }
}
