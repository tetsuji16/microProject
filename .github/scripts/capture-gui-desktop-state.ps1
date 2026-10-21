param(
  [Parameter(Mandatory = $true)]
  [int]$TargetPid,
  [switch]$Watch,
  [string]$LogFile,
  [string]$DiagnosticLogFile,
  [switch]$PrepareNativeChooser,
  [string]$ProbeTriggerFile,
  [string]$LifecycleLogFile
)

$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)
$probeScriptStartedUtc = [DateTime]::UtcNow

function Write-NativeChooserProbeLine([string]$Line) {
  [Console]::Out.WriteLine($Line)
  [Console]::Out.Flush()
}

if ($PrepareNativeChooser) {
  Write-NativeChooserProbeLine("nativeChooserProbe processStartedUtc=$($probeScriptStartedUtc.ToString('o'))")
}

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
    private sealed class DialogReadiness {
        public bool IsOwnedVisibleEnabled;
        public bool IsReady;
    }
    public sealed class NativeChooserStage {
        public int DialogCount { get; set; }
        public IntPtr Handle { get; set; }
        public IntPtr Owner { get; set; }
        public uint OwnerPid { get; set; }
        public bool Visible { get; set; }
        public bool Enabled { get; set; }
        public bool OwnedByTarget { get; set; }
        public bool Foreground { get; set; }
        public bool Ready { get; set; }
    }
    private delegate bool EnumWindowsCallback(IntPtr handle, IntPtr parameter);
    private delegate void WinEventCallback(IntPtr hook, uint eventType, IntPtr hwnd, int idObject,
        int idChild, uint eventThread, uint eventTime);
    private static IntPtr lifecycleHook;
    private static WinEventCallback lifecycleCallback;
    private static System.IO.StreamWriter lifecycleWriter;
    [StructLayout(LayoutKind.Sequential)] private struct Message {
        public IntPtr Hwnd; public uint MessageId; public UIntPtr WParam; public IntPtr LParam;
        public uint Time; public int PointX; public int PointY;
    }

    [DllImport("user32.dll")] private static extern IntPtr GetForegroundWindow();
    [DllImport("user32.dll")] private static extern int GetSystemMetrics(int index);
    [DllImport("user32.dll")] private static extern bool IsChild(IntPtr parent, IntPtr child);
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
    [DllImport("user32.dll", SetLastError = true)] private static extern IntPtr SetWinEventHook(uint eventMin, uint eventMax,
        IntPtr module, WinEventCallback callback, uint processId, uint threadId, uint flags);
    [DllImport("user32.dll")] private static extern bool UnhookWinEvent(IntPtr hook);
    [DllImport("user32.dll")] private static extern bool PeekMessage(out Message message, IntPtr hwnd, uint min, uint max, uint remove);
    [DllImport("user32.dll")] private static extern bool TranslateMessage(ref Message message);
    [DllImport("user32.dll")] private static extern IntPtr DispatchMessage(ref Message message);

    public static void StartNativeChooserLifecycle(int targetPid, string logFile) {
        const uint create = 0x8000, destroy = 0x8001, show = 0x8002, hide = 0x8003;
        const uint outOfContext = 0x0000;
        var logStream = new System.IO.FileStream(logFile, System.IO.FileMode.Create, System.IO.FileAccess.Write,
            System.IO.FileShare.ReadWrite);
        lifecycleWriter = new System.IO.StreamWriter(logStream, new System.Text.UTF8Encoding(false)) { AutoFlush = true };
        lifecycleCallback = (eventHook, eventType, hwnd, idObject, idChild, eventThread, eventTime) => {
            if (hwnd == IntPtr.Zero || idObject != 0 || idChild != 0) return;
            // SetWinEventHook already filters events to the target process. Keep
            // the callback's event thread ID even for DESTROY, where hwnd may
            // already be invalid by the time window queries run.
            uint pid = (uint)targetPid;
            uint threadId = eventThread;
            string eventName = eventType == create ? "CREATE" : eventType == destroy ? "DESTROY"
                : eventType == show ? "SHOW" : eventType == hide ? "HIDE" : "OTHER";
            IntPtr owner = GetWindow(hwnd, 4);
            Rect bounds;
            bool hasBounds = GetWindowRect(hwnd, out bounds);
            IntPtr foreground = GetForegroundWindow();
            string title = ReadTitle(hwnd, Math.Max(1, GetWindowTextLength(hwnd)))
                .Replace('\r', ' ').Replace('\n', ' ').Replace('|', '/');
            lifecycleWriter.WriteLine("windowEventUtc=" + DateTime.UtcNow.ToString("o") + " event=" + eventName
                + " eventThread=" + eventThread + " eventTimeMs=" + eventTime + " pid=" + pid + " thread=" + threadId
                + " hwnd=0x" + hwnd.ToInt64().ToString("X") + " owner=0x" + owner.ToInt64().ToString("X")
                + " class=" + ReadClassName(hwnd) + " title=" + title
                + " visible=" + IsWindowVisible(hwnd) + " enabled=" + IsWindowEnabled(hwnd)
                + " bounds=" + (hasBounds ? Format(bounds) : "unavailable")
                + " foreground=" + (foreground == hwnd));
        };
        lifecycleHook = SetWinEventHook(create, hide, IntPtr.Zero, lifecycleCallback, (uint)targetPid, 0, outOfContext);
        if (lifecycleHook == IntPtr.Zero) throw new System.ComponentModel.Win32Exception(Marshal.GetLastWin32Error());
        lifecycleWriter.WriteLine("windowEventWatchStartedUtc=" + DateTime.UtcNow.ToString("o") + " targetPid=" + targetPid);
    }

    public static void PumpNativeChooserLifecycle() {
        Message message;
        while (PeekMessage(out message, IntPtr.Zero, 0, 0, 0x0001)) {
            TranslateMessage(ref message);
            DispatchMessage(ref message);
        }
    }

    public static void StopNativeChooserLifecycle() {
        if (lifecycleHook != IntPtr.Zero) {
            UnhookWinEvent(lifecycleHook);
            lifecycleHook = IntPtr.Zero;
            lifecycleWriter.WriteLine("windowEventWatchStoppedUtc=" + DateTime.UtcNow.ToString("o"));
            lifecycleWriter.Dispose();
            lifecycleWriter = null;
            lifecycleCallback = null;
        }
    }

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
        result.Append("display primaryBounds=0,0,").Append(GetSystemMetrics(0)).Append(',').Append(GetSystemMetrics(1)).AppendLine();
        uint foregroundPid;
        uint foregroundThread = GetWindowThreadProcessId(foreground, out foregroundPid);
        string foregroundProcess = "unavailable";
        try { foregroundProcess = Process.GetProcessById((int)foregroundPid).ProcessName; } catch { }
        Rect foregroundBounds;
        bool hasForegroundBounds = GetWindowRect(foreground, out foregroundBounds);
        result.Append("foreground pid=").Append(foregroundPid).Append(" thread=").Append(foregroundThread)
            .Append(" process=").Append(foregroundProcess)
            .Append(" hwnd=0x").Append(foreground.ToInt64().ToString("X"))
            .Append(" class=").Append(ReadClassName(foreground)).Append(" title=").Append(ReadTitle(foreground, Math.Max(1, GetWindowTextLength(foreground))))
            .Append(" bounds=").Append(hasForegroundBounds ? Format(foregroundBounds) : "unavailable").AppendLine();

        int ownedVisibleDialogs = 0;
        int readyDialogs = 0;
        foreach (WindowInfo dialog in windows) {
            DialogReadiness readiness = InspectDialog(dialog.Handle, targetPid, foreground);
            if (readiness.IsOwnedVisibleEnabled) ownedVisibleDialogs++;
            if (readiness.IsReady) readyDialogs++;
        }
        result.Append("nativeChooserReady=").Append(ownedVisibleDialogs == 1 && readyDialogs == 1).AppendLine();

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

    public static bool IsNativeChooserReady(int targetPid) {
        IntPtr foreground = GetForegroundWindow();
        int ownedVisibleDialogs = 0;
        int readyDialogs = 0;
        EnumWindows((handle, parameter) => {
            DialogReadiness readiness = InspectDialog(handle, targetPid, foreground);
            if (readiness.IsOwnedVisibleEnabled) ownedVisibleDialogs++;
            if (readiness.IsReady) readyDialogs++;
            return true;
        }, IntPtr.Zero);
        return ownedVisibleDialogs == 1 && readyDialogs == 1;
    }

    public static NativeChooserStage CaptureNativeChooserStage(int targetPid) {
        IntPtr foreground = GetForegroundWindow();
        var candidates = new List<NativeChooserStage>();
        EnumWindows((handle, parameter) => {
            uint pid;
            GetWindowThreadProcessId(handle, out pid);
            if (pid != (uint)targetPid || !String.Equals(ReadClassName(handle), "#32770", StringComparison.Ordinal))
                return true;

            IntPtr owner = GetWindow(handle, 4);
            uint ownerPid = 0;
            if (owner != IntPtr.Zero) GetWindowThreadProcessId(owner, out ownerPid);
            candidates.Add(new NativeChooserStage {
                Handle = handle,
                Owner = owner,
                OwnerPid = ownerPid,
                Visible = IsWindowVisible(handle),
                Enabled = IsWindowEnabled(handle),
                OwnedByTarget = owner != IntPtr.Zero && ownerPid == (uint)targetPid,
                Foreground = foreground == handle || IsChild(handle, foreground),
                Ready = InspectDialog(handle, targetPid, foreground).IsReady
            });
            return true;
        }, IntPtr.Zero);

        NativeChooserStage selected = null;
        foreach (NativeChooserStage candidate in candidates) {
            if (selected == null || (candidate.OwnedByTarget && !selected.OwnedByTarget)) selected = candidate;
        }
        if (selected == null) return new NativeChooserStage();
        selected.DialogCount = candidates.Count;
        return selected;
    }

    private static DialogReadiness InspectDialog(IntPtr handle, int targetPid, IntPtr foreground) {
        var result = new DialogReadiness();
        uint pid;
        GetWindowThreadProcessId(handle, out pid);
        if (pid != (uint)targetPid || !IsWindowVisible(handle) || !IsWindowEnabled(handle)
                || !String.Equals(ReadClassName(handle), "#32770", StringComparison.Ordinal)) return result;
        IntPtr owner = GetWindow(handle, 4);
        uint ownerPid = 0;
        if (owner != IntPtr.Zero) GetWindowThreadProcessId(owner, out ownerPid);
        if (owner == IntPtr.Zero || ownerPid != (uint)targetPid) return result;
        result.IsOwnedVisibleEnabled = true;
        if (!(foreground == handle || IsChild(handle, foreground))) return result;
        IntPtr cancel = GetDlgItem(handle, 2);
        if (cancel == IntPtr.Zero || !IsWindowVisible(cancel) || !IsWindowEnabled(cancel)
                || GetDlgCtrlID(cancel) != 2 || !String.Equals(ReadClassName(cancel), "Button", StringComparison.Ordinal)) return result;
        Rect dialogBounds, cancelBounds;
        if (!GetWindowRect(handle, out dialogBounds) || !GetWindowRect(cancel, out cancelBounds)) return result;
        result.IsReady = cancelBounds.Left >= dialogBounds.Left && cancelBounds.Top >= dialogBounds.Top
            && cancelBounds.Right <= dialogBounds.Right && cancelBounds.Bottom <= dialogBounds.Bottom;
        return result;
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
  if ([string]::IsNullOrWhiteSpace($DiagnosticLogFile)) {
    $DiagnosticLogFile = [System.IO.Path]::ChangeExtension($LogFile, '.diagnostic.log')
  }
  $logStream = [System.IO.FileStream]::new($LogFile, [System.IO.FileMode]::Append,
    [System.IO.FileAccess]::Write, [System.IO.FileShare]::ReadWrite)
  $logWriter = [System.IO.StreamWriter]::new($logStream, [System.Text.UTF8Encoding]::new($false))
  $logWriter.AutoFlush = $true
  $diagnosticWriter = $null
  $diagnosticStream = $null
  try {
    $diagnosticStream = [System.IO.FileStream]::new($DiagnosticLogFile, [System.IO.FileMode]::Append,
      [System.IO.FileAccess]::Write, [System.IO.FileShare]::ReadWrite)
    $diagnosticWriter = [System.IO.StreamWriter]::new($diagnosticStream, [System.Text.UTF8Encoding]::new($false))
    $diagnosticWriter.AutoFlush = $true
    $diagnosticWriter.WriteLine("monitorStarted=" + [DateTime]::UtcNow.ToString('o') + " monitorPid=$PID targetPid=$TargetPid")
    $lastOverlap = ''
    $firstTargetCheck = $true
    $firstOverlapProbe = $true
    while ($true) {
      $targetCheckStarted = [System.Diagnostics.Stopwatch]::StartNew()
      if ($firstTargetCheck) {
        $diagnosticWriter.WriteLine("monitorFirstTargetCheckEntered=" + [DateTime]::UtcNow.ToString('o'))
      }
      $targetProcess = $null
      $targetRunning = $true
      try {
        $targetProcess = [System.Diagnostics.Process]::GetProcessById($TargetPid)
        $targetProcess.Refresh()
        $targetRunning = -not $targetProcess.HasExited
      } catch [System.ArgumentException] {
        $targetRunning = $false
      } catch [System.InvalidOperationException] {
        $targetRunning = $false
      } finally {
        if ($null -ne $targetProcess) { $targetProcess.Dispose() }
      }
      $targetCheckStarted.Stop()
      if ($firstTargetCheck) {
        $diagnosticWriter.WriteLine("monitorFirstTargetCheckExited=" + [DateTime]::UtcNow.ToString('o') + " elapsedMs=$([int]$targetCheckStarted.ElapsedMilliseconds) running=$targetRunning")
        $firstTargetCheck = $false
      }
      if (-not $targetRunning) {
        $diagnosticWriter.WriteLine("monitorStopped=" + [DateTime]::UtcNow.ToString('o') + " reason=target-exited")
        break
      }
      $probeStarted = [System.Diagnostics.Stopwatch]::StartNew()
      if ($firstOverlapProbe) {
        $diagnosticWriter.WriteLine("monitorFirstOverlapProbeEntered=" + [DateTime]::UtcNow.ToString('o'))
      }
      $overlap = [GuiDesktopWindowProbe]::CaptureForegroundOverlap($TargetPid)
      $probeStarted.Stop()
      if ($firstOverlapProbe) {
        $diagnosticWriter.WriteLine("monitorFirstOverlapProbeExited=" + [DateTime]::UtcNow.ToString('o') + " elapsedMs=$([int]$probeStarted.ElapsedMilliseconds)")
        $firstOverlapProbe = $false
      }
      if ($probeStarted.ElapsedMilliseconds -ge 1000) {
        $diagnosticWriter.WriteLine("monitorSlowOverlapProbe=" + [DateTime]::UtcNow.ToString('o') + " elapsedMs=$([int]$probeStarted.ElapsedMilliseconds)")
      }
      if ($overlap -ne $lastOverlap -and -not [string]::IsNullOrEmpty($overlap)) {
        $logWriter.WriteLine("GUI_ENVIRONMENT_CONTENDED observed=" + [DateTime]::UtcNow.ToString('o') + " " + $overlap)
      }
      $lastOverlap = $overlap
      Start-Sleep -Milliseconds 50
    }
  } finally {
    try {
      if ($null -ne $diagnosticWriter) { $diagnosticWriter.Dispose() }
      elseif ($null -ne $diagnosticStream) { $diagnosticStream.Dispose() }
    } finally {
      $logWriter.Dispose()
    }
  }
} else {
  if ($PrepareNativeChooser) {
    if ([string]::IsNullOrWhiteSpace($ProbeTriggerFile) -or [string]::IsNullOrWhiteSpace($LifecycleLogFile)) {
      throw 'ProbeTriggerFile and LifecycleLogFile are required with PrepareNativeChooser.'
    }
    [GuiDesktopWindowProbe]::StartNativeChooserLifecycle($TargetPid, $LifecycleLogFile)
    Write-NativeChooserProbeLine("nativeChooserProbe preparedUtc=$([DateTime]::UtcNow.ToString('o'))")
    while (-not [System.IO.File]::Exists($ProbeTriggerFile)) {
      [GuiDesktopWindowProbe]::PumpNativeChooserLifecycle()
      Start-Sleep -Milliseconds 10
    }
    $triggerText = [System.IO.File]::ReadAllText($ProbeTriggerFile)
    if ($triggerText -notmatch '(?m)^deadlineUtcTicks=(\d+)$') { throw 'Probe trigger lacks deadlineUtcTicks.' }
    $DeadlineUtcTicks = [long]$Matches[1]
    if ($DeadlineUtcTicks -le 0) { throw 'Probe trigger deadlineUtcTicks must be positive.' }
    $callId = if ($triggerText -match '(?m)^callId=([^\r\n]+)$') { $Matches[1] } else { 'unknown' }
    Write-NativeChooserProbeLine("nativeChooserProbe waitStartedUtc=$([DateTime]::UtcNow.ToString('o')) callId=$callId deadlineUtc=$([DateTime]::new($DeadlineUtcTicks, [DateTimeKind]::Utc).ToString('o'))")
    $firstDialogUtc = $null
    $firstOwnedUtc = $null
    $firstVisibleUtc = $null
    $firstForegroundUtc = $null
    $firstReadyUtc = $null
    do {
      [GuiDesktopWindowProbe]::PumpNativeChooserLifecycle()
      $stage = [GuiDesktopWindowProbe]::CaptureNativeChooserStage($TargetPid)
      $sampleUtc = [DateTime]::UtcNow
      $handle = if ($stage.Handle -eq [IntPtr]::Zero) { 'none' } else { '0x' + $stage.Handle.ToInt64().ToString('X') }
      $owner = if ($stage.Owner -eq [IntPtr]::Zero) { 'none' } else { '0x' + $stage.Owner.ToInt64().ToString('X') }
      $stageFacts = "hwnd=$handle owner=$owner ownerPid=$($stage.OwnerPid) targetPid=$TargetPid visible=$($stage.Visible) enabled=$($stage.Enabled) ownedByTarget=$($stage.OwnedByTarget) foreground=$($stage.Foreground) ready=$($stage.Ready) dialogCount=$($stage.DialogCount)"
      if ($stage.Handle -ne [IntPtr]::Zero -and $null -eq $firstDialogUtc) {
        $firstDialogUtc = $sampleUtc
        Write-NativeChooserProbeLine("nativeChooserFirstHwndObservedUtc=$($sampleUtc.ToString('o')) $stageFacts")
      }
      if ($stage.OwnedByTarget -and $null -eq $firstOwnedUtc) {
        $firstOwnedUtc = $sampleUtc
        Write-NativeChooserProbeLine("nativeChooserFirstOwnedObservedUtc=$($sampleUtc.ToString('o')) $stageFacts")
      }
      if ($stage.Visible -and $null -eq $firstVisibleUtc) {
        $firstVisibleUtc = $sampleUtc
        Write-NativeChooserProbeLine("nativeChooserFirstVisibleObservedUtc=$($sampleUtc.ToString('o')) $stageFacts")
      }
      if ($stage.Foreground -and $null -eq $firstForegroundUtc) {
        $firstForegroundUtc = $sampleUtc
        Write-NativeChooserProbeLine("nativeChooserFirstForegroundObservedUtc=$($sampleUtc.ToString('o')) $stageFacts")
      }
      if ($stage.Ready -and $sampleUtc.Ticks -lt $DeadlineUtcTicks -and $null -eq $firstReadyUtc) {
        $firstReadyUtc = $sampleUtc
        Write-NativeChooserProbeLine("nativeChooserFirstReadyObservedUtc=$($sampleUtc.ToString('o')) $stageFacts")
      }
      $ready = $false
      if ($sampleUtc.Ticks -lt $DeadlineUtcTicks) {
        $ready = [GuiDesktopWindowProbe]::IsNativeChooserReady($TargetPid)
        if ([DateTime]::UtcNow.Ticks -ge $DeadlineUtcTicks) { $ready = $false; break }
        if ($ready) { break }
      } else {
        break
      }
      Start-Sleep -Milliseconds 25
    } while ([DateTime]::UtcNow.Ticks -lt $DeadlineUtcTicks)
    $completionReason = if ($ready) { 'ready' } else { 'deadline' }
    [GuiDesktopWindowProbe]::PumpNativeChooserLifecycle()
    Write-NativeChooserProbeLine("nativeChooserProbe completedUtc=$([DateTime]::UtcNow.ToString('o')) reason=$completionReason ready=$ready callId=$callId")
    [GuiDesktopWindowProbe]::StopNativeChooserLifecycle()
    [GuiDesktopWindowProbe]::Capture($TargetPid)
    exit 0
  }
  [GuiDesktopWindowProbe]::Capture($TargetPid)
}
