Add-Type @"
    using System;
    using System.Runtime.InteropServices;
    public class WA {
        [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr h);
        [DllImport("user32.dll")] public static extern bool ShowWindow(IntPtr h, int n);
        [DllImport("user32.dll")] public static extern bool GetWindowRect(IntPtr h, out RECT r);
        public struct RECT { public int L,T,R,B; }
    }
"@
$proc = Get-Process java | Where-Object { $_.MainWindowTitle -like "*- C:*" } | Select-Object -First 1
if (-not $proc) { Write-Output "NO_APP"; exit 1 }
$h = $proc.MainWindowHandle
[WA]::ShowWindow($h, 3) | Out-Null
[WA]::SetForegroundWindow($h) | Out-Null
Start-Sleep -Seconds 1
$r = New-Object WA+RECT
[WA]::GetWindowRect($h, [ref]$r) | Out-Null
Write-Output "RECT L=$($r.L) T=$($r.T) R=$($r.R) B=$($r.B) W=$($r.R-$r.L) H=$($r.B-$r.T)"
$ts = Get-Date -Format "yyyyMMdd-HHmmss"
$out = "C:/Users/tetsu/vscode/ProjectLibre/build/gui-shot-$ts.png"
Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName System.Drawing
$s = [System.Windows.Forms.Screen]::PrimaryScreen.Bounds
$b = New-Object System.Drawing.Bitmap($s.Width, $s.Height)
$g = [System.Drawing.Graphics]::FromImage($b)
$g.CopyFromScreen($s.Location, [System.Drawing.Point]::Empty, $s.Size)
$b.Save($out, [System.Drawing.Imaging.ImageFormat]::Png)
$g.Dispose(); $b.Dispose()
Write-Output "SAVED:$out"
