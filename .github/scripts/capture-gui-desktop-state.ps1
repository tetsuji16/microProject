param(
  [Parameter(Mandatory = $true)]
  [int]$TargetPid,
  [switch]$Watch,
  [string]$LogFile
)

$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)

Add-Type -TypeDefinition @'
using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Runtime.InteropServices;
using System.Text;

public static class GuiDesktopWindowProbe {
    [StructLayout(LayoutKind.Sequential)]
    private struct Rect { public int Left, Top, Right, Bottom; }
    private sealed class WindowInfo {
        public IntPtr Handle;
        public int Pid;
        public int Order;
        public string ProcessName;
        public string ClassName;
        public string Title;
        public Rect Bounds;
    }
    private delegate bool EnumWindowsCallback(IntPtr handle, IntPtr parameter);

    [DllImport("user32.dll")] private static extern IntPtr GetForegroundWindow();
    [DllImport("user32.dll")] private static extern uint GetWindowThreadProcessId(IntPtr handle, out uint processId);
    [DllImport("user32.dll", CharSet = CharSet.Unicode)] private static extern int GetWindowText(IntPtr handle, StringBuilder text, int capacity);
    [DllImport("user32.dll", CharSet = CharSet.Unicode)] private static extern int GetWindowTextLength(IntPtr handle);
    [DllImport("user32.dll", CharSet = CharSet.Unicode)] private static extern int GetClassName(IntPtr handle, StringBuilder className, int capacity);
    [DllImport("user32.dll")] private static extern bool GetWindowRect(IntPtr handle, out Rect bounds);
    [DllImport("user32.dll")] private static extern bool IsWindowVisible(IntPtr handle);
    [DllImport("user32.dll")] private static extern bool IsIconic(IntPtr handle);
    [DllImport("user32.dll")] private static extern bool EnumWindows(EnumWindowsCallback callback, IntPtr parameter);

    public static string Capture(int targetPid) {
        var windows = new List<WindowInfo>();
        IntPtr foreground = GetForegroundWindow();
        int order = 0;
        EnumWindows((handle, parameter) => {
            int currentOrder = order++;
            if (!IsWindowVisible(handle)) return true;
            int titleLength = GetWindowTextLength(handle);
            if (titleLength == 0) return true;
            uint pid;
            GetWindowThreadProcessId(handle, out pid);
            Rect bounds;
            if (!GetWindowRect(handle, out bounds)) return true;
            string title = ReadTitle(handle, titleLength).Replace('\r', ' ').Replace('\n', ' ').Replace('|', '/');
            string className = ReadClassName(handle);
            string processName = "unavailable";
            try { processName = Process.GetProcessById((int)pid).ProcessName; } catch { }
            windows.Add(new WindowInfo { Handle = handle, Pid = (int)pid, Order = currentOrder,
                ProcessName = processName, ClassName = className, Title = title, Bounds = bounds });
            return true;
        }, IntPtr.Zero);

        var result = new StringBuilder();
        WindowInfo foregroundInfo = windows.Find(window => window.Handle == foreground);
        if (foregroundInfo == null) result.AppendLine("foreground=unavailable");
        else result.Append("foreground pid=").Append(foregroundInfo.Pid).Append(" process=").Append(foregroundInfo.ProcessName)
            .Append(" class=").Append(foregroundInfo.ClassName).Append(" title=").Append(foregroundInfo.Title)
            .Append(" bounds=").Append(Format(foregroundInfo.Bounds)).AppendLine();

        var targetWindows = windows.FindAll(window => window.Pid == targetPid);
        foreach (WindowInfo target in targetWindows)
            result.Append("testWindow pid=").Append(target.Pid).Append(" order=").Append(target.Order)
                .Append(" title=").Append(target.Title).Append(" bounds=").Append(Format(target.Bounds)).AppendLine();

        bool foundOverlap = false;
        foreach (WindowInfo other in windows) {
            if (other.Pid == targetPid) continue;
            foreach (WindowInfo target in targetWindows) {
                if (!Intersects(other.Bounds, target.Bounds)) continue;
                foundOverlap = true;
                bool obscures = other.Order < target.Order;
                result.Append("overlapCandidate obscuresTestWindow=").Append(obscures)
                    .Append(" process=").Append(other.ProcessName).Append(" pid=").Append(other.Pid)
                    .Append(" title=").Append(other.Title).Append(" bounds=").Append(Format(other.Bounds))
                    .Append(" testWindow=").Append(target.Title).Append(" testBounds=").Append(Format(target.Bounds)).AppendLine();
                break;
            }
        }
        if (!foundOverlap) result.AppendLine("overlapCandidate=none");
        return result.ToString();
    }

    public static string CaptureForegroundOverlap(int targetPid) {
        IntPtr foreground = GetForegroundWindow();
        uint foregroundPid;
        GetWindowThreadProcessId(foreground, out foregroundPid);
        if (foregroundPid == targetPid || IsIconic(foreground)) return "";
        Rect foregroundBounds;
        if (!GetWindowRect(foreground, out foregroundBounds)) return "";
        int foregroundLength = GetWindowTextLength(foreground);
        string foregroundTitle = ReadTitle(foreground, Math.Max(1, foregroundLength))
            .Replace('\r', ' ').Replace('\n', ' ').Replace('|', '/');
        string foregroundClass = ReadClassName(foreground);
        string foregroundProcess = "unavailable";
        try { foregroundProcess = Process.GetProcessById((int)foregroundPid).ProcessName; } catch { }
        if (IsDesktopShellWindow(foregroundProcess, foregroundClass)) return "";

        var overlaps = new List<string>();
        EnumWindows((handle, parameter) => {
            if (!IsWindowVisible(handle) || IsIconic(handle)) return true;
            uint pid;
            GetWindowThreadProcessId(handle, out pid);
            if (pid != targetPid) return true;
            Rect targetBounds;
            if (!GetWindowRect(handle, out targetBounds) || !Intersects(foregroundBounds, targetBounds)) return true;
            string targetTitle = ReadTitle(handle, Math.Max(1, GetWindowTextLength(handle)))
                .Replace('\r', ' ').Replace('\n', ' ').Replace('|', '/');
            overlaps.Add("testWindow=" + targetTitle + " testBounds=" + Format(targetBounds));
            return true;
        }, IntPtr.Zero);
        if (overlaps.Count == 0) return "";
        return "foreground pid=" + foregroundPid + " process=" + foregroundProcess + " class=" + foregroundClass + " title=" + foregroundTitle
            + " bounds=" + Format(foregroundBounds) + " overlaps=" + String.Join(";", overlaps);
    }

    private static string ReadClassName(IntPtr handle) {
        var className = new StringBuilder(256);
        GetClassName(handle, className, className.Capacity);
        return className.ToString();
    }
    private static bool IsDesktopShellWindow(string processName, string className) {
        return String.Equals(processName, "explorer", StringComparison.OrdinalIgnoreCase)
            && (String.Equals(className, "Progman", StringComparison.OrdinalIgnoreCase)
                || String.Equals(className, "WorkerW", StringComparison.OrdinalIgnoreCase));
    }

    private static string ReadTitle(IntPtr handle, int length) {
        var text = new StringBuilder(length + 1);
        GetWindowText(handle, text, text.Capacity);
        return text.ToString();
    }
    private static bool Intersects(Rect first, Rect second) {
        return first.Left < second.Right && first.Right > second.Left && first.Top < second.Bottom && first.Bottom > second.Top;
    }
    private static string Format(Rect bounds) {
        return bounds.Left + "," + bounds.Top + "," + (bounds.Right - bounds.Left) + "," + (bounds.Bottom - bounds.Top);
    }
}
'@

if ($Watch) {
  if ([string]::IsNullOrWhiteSpace($LogFile)) { throw 'LogFile is required with Watch.' }
  Add-Content -LiteralPath $LogFile -Encoding utf8 -Value ("monitorStarted=" + [DateTime]::UtcNow.ToString('o'))
  $lastOverlap = ''
  while ($true) {
    $overlap = [GuiDesktopWindowProbe]::CaptureForegroundOverlap($TargetPid)
    if ($overlap -ne $lastOverlap -and -not [string]::IsNullOrEmpty($overlap)) {
      Add-Content -LiteralPath $LogFile -Encoding utf8 -Value ("GUI_ENVIRONMENT_CONTENDED observed=" + [DateTime]::UtcNow.ToString('o') + " " + $overlap)
    }
    $lastOverlap = $overlap
    Start-Sleep -Milliseconds 50
  }
} else {
  [GuiDesktopWindowProbe]::Capture($TargetPid)
}
