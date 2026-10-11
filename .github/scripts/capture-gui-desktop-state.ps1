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
        public IntPtr Owner;
        public uint ThreadId;
        public int Pid;
        public int Order;
        public bool Visible;
        public bool Enabled;
        public bool EligibleForOverlap;
        public bool HasBounds;
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
    [DllImport("user32.dll")] private static extern bool IsWindowEnabled(IntPtr handle);
    [DllImport("user32.dll")] private static extern IntPtr GetDlgItem(IntPtr dialog, int controlId);
    [DllImport("user32.dll")] private static extern int GetDlgCtrlID(IntPtr control);
    [DllImport("user32.dll")] private static extern bool EnumChildWindows(IntPtr parent, EnumWindowsCallback callback, IntPtr parameter);
    [DllImport("user32.dll")] private static extern bool IsIconic(IntPtr handle);
    [DllImport("user32.dll")] private static extern bool EnumWindows(EnumWindowsCallback callback, IntPtr parameter);
    [DllImport("user32.dll")] private static extern IntPtr GetWindow(IntPtr handle, uint command);

    public static string Capture(int targetPid) {
        var windows = new List<WindowInfo>();
        IntPtr foreground = GetForegroundWindow();
        int order = 0;
        EnumWindows((handle, parameter) => {
            int currentOrder = order++;
            uint pid;
            uint threadId = GetWindowThreadProcessId(handle, out pid);
            bool visible = IsWindowVisible(handle);
            int titleLength = GetWindowTextLength(handle);
            // Keep hidden and untitled windows owned by the GUI test process for
            // modal native-dialog diagnosis. Preserve the existing visible,
            // titled-only inventory for other desktop processes.
            if (pid != targetPid && (!visible || titleLength == 0)) return true;
            Rect bounds;
            bool hasBounds = GetWindowRect(handle, out bounds);
            if (!hasBounds && pid != targetPid) return true;
            string title = titleLength == 0 ? "" : ReadTitle(handle, titleLength)
                .Replace('\r', ' ').Replace('\n', ' ').Replace('|', '/');
            string className = ReadClassName(handle);
            string processName = "unavailable";
            try { processName = Process.GetProcessById((int)pid).ProcessName; } catch { }
            windows.Add(new WindowInfo { Handle = handle, Owner = GetWindow(handle, 4), ThreadId = threadId,
                Pid = (int)pid, Order = currentOrder, Visible = visible, Enabled = IsWindowEnabled(handle),
                EligibleForOverlap = visible && titleLength > 0 && hasBounds, HasBounds = hasBounds,
                ProcessName = processName, ClassName = className, Title = title, Bounds = bounds });
            return true;
        }, IntPtr.Zero);

        var result = new StringBuilder().Append("snapshotUtc=").Append(DateTime.UtcNow.ToString("o")).AppendLine();
        WindowInfo foregroundInfo = windows.Find(window => window.Handle == foreground);
        if (foregroundInfo == null) result.AppendLine("foreground=unavailable");
        else result.Append("foreground pid=").Append(foregroundInfo.Pid).Append(" process=").Append(foregroundInfo.ProcessName)
            .Append(" class=").Append(foregroundInfo.ClassName).Append(" title=").Append(foregroundInfo.Title)
            .Append(" bounds=").Append(Format(foregroundInfo.Bounds)).AppendLine();

        var targetWindows = windows.FindAll(window => window.Pid == targetPid);
        foreach (WindowInfo target in targetWindows)
            result.Append("testWindow pid=").Append(target.Pid).Append(" thread=").Append(target.ThreadId)
                .Append(" hwnd=0x").Append(target.Handle.ToInt64().ToString("X"))
                .Append(" owner=0x").Append(target.Owner.ToInt64().ToString("X"))
                .Append(" visible=").Append(target.Visible).Append(" enabled=").Append(target.Enabled).Append(" class=").Append(target.ClassName)
                .Append(" order=").Append(target.Order).Append(" title=").Append(target.Title)
                .Append(" bounds=").Append(target.HasBounds ? Format(target.Bounds) : "unavailable").AppendLine();

        foreach (WindowInfo dialog in targetWindows) {
            if (!String.Equals(dialog.ClassName, "#32770", StringComparison.Ordinal)) continue;
            uint dialogOwnerPid = 0;
            if (dialog.Owner != IntPtr.Zero) GetWindowThreadProcessId(dialog.Owner, out dialogOwnerPid);
            bool dialogOwnedByTarget = dialog.Owner != IntPtr.Zero && dialogOwnerPid == (uint)targetPid;
            if (dialogOwnedByTarget && dialog.Visible) {
                result.Append("ownedDialog pid=").Append(dialog.Pid).Append(" thread=").Append(dialog.ThreadId)
                    .Append(" hwnd=0x").Append(dialog.Handle.ToInt64().ToString("X"))
                    .Append(" owner=0x").Append(dialog.Owner.ToInt64().ToString("X"))
                    .Append(" ownerPid=").Append(dialogOwnerPid).Append(" class=").Append(dialog.ClassName)
                    .Append(" visible=").Append(dialog.Visible).Append(" enabled=").Append(dialog.Enabled)
                    .Append(" bounds=").Append(dialog.HasBounds ? Format(dialog.Bounds) : "unavailable").AppendLine();
            }

            // Query IDCANCEL directly on each top-level native dialog. Do not
            // infer it from a nested descendant that might reuse control ID 2.
            IntPtr cancel = GetDlgItem(dialog.Handle, 2); // IDCANCEL
            if (cancel != IntPtr.Zero) {
                uint cancelPid;
                uint cancelThreadId = GetWindowThreadProcessId(cancel, out cancelPid);
                int controlId = GetDlgCtrlID(cancel);
                bool visible = IsWindowVisible(cancel);
                bool enabled = IsWindowEnabled(cancel);
                Rect cancelBounds;
                bool hasBounds = GetWindowRect(cancel, out cancelBounds);
                result.Append("cancelControl pid=").Append(cancelPid).Append(" thread=").Append(cancelThreadId)
                    .Append(" hwnd=0x").Append(cancel.ToInt64().ToString("X"))
                    .Append(" dialog=0x").Append(dialog.Handle.ToInt64().ToString("X"))
                    .Append(" dialogClass=").Append(dialog.ClassName)
                    .Append(" dialogOwnedByTarget=").Append(dialogOwnedByTarget)
                    .Append(" dialogVisible=").Append(dialog.Visible).Append(" dialogEnabled=").Append(dialog.Enabled)
                    .Append(" controlId=").Append(controlId).Append(" class=").Append(ReadClassName(cancel))
                    .Append(" visible=").Append(visible).Append(" enabled=").Append(enabled)
                    .Append(" bounds=").Append(hasBounds ? Format(cancelBounds) : "unavailable")
                    .Append(" dialogBounds=").Append(dialog.HasBounds ? Format(dialog.Bounds) : "unavailable").AppendLine();
            }
            EnumChildWindows(dialog.Handle, (child, parameter) => {
                uint childPid;
                uint childThreadId = GetWindowThreadProcessId(child, out childPid);
                Rect childBounds;
                bool hasChildBounds = GetWindowRect(child, out childBounds);
                result.Append("nativeChild pid=").Append(childPid).Append(" thread=").Append(childThreadId)
                    .Append(" hwnd=0x").Append(child.ToInt64().ToString("X"))
                    .Append(" dialog=0x").Append(dialog.Handle.ToInt64().ToString("X"))
                    .Append(" controlId=").Append(GetDlgCtrlID(child))
                    .Append(" class=").Append(ReadClassName(child))
                    .Append(" visible=").Append(IsWindowVisible(child)).Append(" enabled=").Append(IsWindowEnabled(child))
                    .Append(" bounds=").Append(hasChildBounds ? Format(childBounds) : "unavailable").AppendLine();
                return true;
            }, IntPtr.Zero);
        }

        bool foundOverlap = false;
        foreach (WindowInfo other in windows) {
            if (other.Pid == targetPid) continue;
            foreach (WindowInfo target in targetWindows) {
                if (!target.EligibleForOverlap || !Intersects(other.Bounds, target.Bounds)) continue;
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
                || String.Equals(className, "WorkerW", StringComparison.OrdinalIgnoreCase)
                || String.Equals(className, "Shell_TrayWnd", StringComparison.OrdinalIgnoreCase)
                || String.Equals(className, "Shell_SecondaryTrayWnd", StringComparison.OrdinalIgnoreCase));
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
  $logStream = [System.IO.FileStream]::new($LogFile, [System.IO.FileMode]::Append,
    [System.IO.FileAccess]::Write, [System.IO.FileShare]::ReadWrite)
  $logWriter = [System.IO.StreamWriter]::new($logStream, [System.Text.UTF8Encoding]::new($false))
  $logWriter.AutoFlush = $true
  try {
    $logWriter.WriteLine("monitorStarted=" + [DateTime]::UtcNow.ToString('o'))
    $lastOverlap = ''
    while ($true) {
      $overlap = [GuiDesktopWindowProbe]::CaptureForegroundOverlap($TargetPid)
      if ($overlap -ne $lastOverlap -and -not [string]::IsNullOrEmpty($overlap)) {
        $logWriter.WriteLine("GUI_ENVIRONMENT_CONTENDED observed=" + [DateTime]::UtcNow.ToString('o') + " " + $overlap)
      }
      $lastOverlap = $overlap
      Start-Sleep -Milliseconds 50
    }
  } finally {
    $logWriter.Dispose()
  }
} else {
  [GuiDesktopWindowProbe]::Capture($TargetPid)
}
