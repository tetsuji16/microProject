param(
  [Parameter(Mandatory = $true)]
  [string]$LogFile,
  [int]$MaximumMinutes = 115
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Windows.Forms

if (-not ('MicroProject.HostedDialogWatcher' -as [type])) {
  Add-Type -TypeDefinition @'
using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Runtime.InteropServices;
using System.Text;
using System.IO;
using System.Threading;

namespace MicroProject {
  public static class HostedDialogWatcher {
    public delegate bool EnumWindowsCallback(IntPtr window, IntPtr parameter);

    [DllImport("user32.dll")] public static extern bool EnumWindows(EnumWindowsCallback callback, IntPtr parameter);
    [DllImport("user32.dll")] public static extern bool EnumChildWindows(IntPtr parent, EnumWindowsCallback callback, IntPtr parameter);
    [DllImport("user32.dll")] public static extern bool IsWindowVisible(IntPtr window);
    [DllImport("user32.dll", CharSet = CharSet.Unicode)] public static extern int GetWindowText(IntPtr window, StringBuilder text, int capacity);
    [DllImport("user32.dll", CharSet = CharSet.Unicode)] public static extern int GetClassName(IntPtr window, StringBuilder text, int capacity);
    [DllImport("user32.dll", CharSet = CharSet.Auto)] public static extern IntPtr SendMessage(IntPtr window, uint message, IntPtr wParam, IntPtr lParam);

    private static string ReadText(IntPtr window) {
      var text = new StringBuilder(2048);
      GetWindowText(window, text, text.Capacity);
      return text.ToString();
    }

    private static string ReadClass(IntPtr window) {
      var name = new StringBuilder(128);
      GetClassName(window, name, name.Capacity);
      return name.ToString();
    }

    public static string DismissKnownHostedConfigurationDialog() {
      var warningPrefix = "Windows created a temporary paging file on your computer";
      var warningSuffix = "paging file configuration";
      var performanceHeading = "Processor scheduling";
      var virtualMemoryHeading = "Virtual memory";
      string dismissed = null;

      EnumWindows((window, parameter) => {
        if (!IsWindowVisible(window)) return true;
        var title = ReadText(window);
        if (!String.Equals(title, "System Properties", StringComparison.Ordinal)
            && !String.Equals(title, "Performance Options", StringComparison.Ordinal)) return true;
        var parts = new List<string>();
        var candidateButton = IntPtr.Zero;
        EnumChildWindows(window, (child, childParameter) => {
          var text = ReadText(child);
          if (!String.IsNullOrWhiteSpace(text)) parts.Add(text);
          if (String.Equals(ReadClass(child), "Button", StringComparison.Ordinal) && String.Equals(text, "OK", StringComparison.OrdinalIgnoreCase)) candidateButton = child;
          return true;
        }, IntPtr.Zero);
        var content = String.Join(" ", parts);
        if (String.Equals(title, "System Properties", StringComparison.Ordinal)
            && candidateButton != IntPtr.Zero && content.Contains(warningPrefix) && content.Contains(warningSuffix)) {
          SendMessage(candidateButton, 0x00F5, IntPtr.Zero, IntPtr.Zero); // BM_CLICK
          dismissed = "title=System Properties; text=" + content;
          return false;
        }
        if (String.Equals(title, "Performance Options", StringComparison.Ordinal)
            && content.Contains(performanceHeading) && content.Contains(virtualMemoryHeading)) {
          SendMessage(window, 0x0010, IntPtr.Zero, IntPtr.Zero); // WM_CLOSE, equivalent to the title-bar X (discard changes)
          dismissed = "title=Performance Options; text=" + content;
          return false;
        }
        return true;
      }, IntPtr.Zero);

      return dismissed;
    }

    public static string DismissHostedQuickSettingsPanel() {
      string dismissed = null;
      EnumWindows((window, parameter) => {
        if (!IsWindowVisible(window)
            || !String.Equals(ReadClass(window), "ControlCenterWindow", StringComparison.Ordinal)
            || !String.Equals(ReadText(window), "Quick Settings", StringComparison.OrdinalIgnoreCase)) return true;
        SendMessage(window, 0x0010, IntPtr.Zero, IntPtr.Zero); // WM_CLOSE the exact hosted shell panel
        dismissed = "class=ControlCenterWindow; title=Quick settings";
        return false;
      }, IntPtr.Zero);
      return dismissed;
    }
  }
}

// Windows calls a WH_KEYBOARD_LL hook only while the thread that installed it pumps messages.
// If the callback does not return within LowLevelHooksTimeout, the key passes through and, on
// Windows 7 and later, the hook is silently removed. The dialog loop below calls GetWindowText
// and a synchronous SendMessage on other processes' windows, which can block longer than that
// timeout, so the hook owns a dedicated thread that does nothing but pump messages (#1278).
public static class HostedWindowsHotkeyGuard {
  private const int WhKeyboardLl = 13;
  private const int WmKeyDown = 0x0100;
  private const int WmKeyUp = 0x0101;
  private const int WmSysKeyDown = 0x0104;
  private const int WmSysKeyUp = 0x0105;
  private const uint WmQuit = 0x0012;
  private const int VkA = 0x41;
  private const int VkLWin = 0x5B;
  private const int VkRWin = 0x5C;

  [StructLayout(LayoutKind.Sequential)]
  private struct NativeMessage {
    public IntPtr Window;
    public uint Message;
    public IntPtr WParam;
    public IntPtr LParam;
    public uint Time;
    public int PointX;
    public int PointY;
  }

  private delegate IntPtr LowLevelKeyboardProc(int code, IntPtr message, IntPtr data);
  private static readonly LowLevelKeyboardProc CallbackRoot = HandleKeyboard;
  private static readonly object LogLock = new object();
  private static readonly ConcurrentQueue<string> PendingLog = new ConcurrentQueue<string>();
  private static readonly AutoResetEvent PendingLogSignal = new AutoResetEvent(false);
  private static IntPtr hook;
  private static string logFile;
  private static volatile bool windowsKeyDown;
  private static volatile bool stopping;
  private static Thread hookThread;
  private static Thread logThread;
  private static uint hookThreadId;

  [DllImport("user32.dll", SetLastError = true)]
  private static extern IntPtr SetWindowsHookEx(int idHook, LowLevelKeyboardProc callback, IntPtr module, uint threadId);
  [DllImport("user32.dll", SetLastError = true)]
  private static extern bool UnhookWindowsHookEx(IntPtr hookHandle);
  [DllImport("user32.dll")]
  private static extern IntPtr CallNextHookEx(IntPtr hookHandle, int code, IntPtr message, IntPtr data);
  [DllImport("user32.dll")]
  private static extern int GetMessage(out NativeMessage message, IntPtr window, uint filterMin, uint filterMax);
  [DllImport("user32.dll")]
  private static extern bool TranslateMessage(ref NativeMessage message);
  [DllImport("user32.dll")]
  private static extern IntPtr DispatchMessage(ref NativeMessage message);
  [DllImport("user32.dll", SetLastError = true)]
  private static extern bool PostThreadMessage(uint threadId, uint message, IntPtr wParam, IntPtr lParam);
  [DllImport("kernel32.dll")]
  private static extern uint GetCurrentThreadId();
  [DllImport("kernel32.dll", CharSet = CharSet.Auto, SetLastError = true)]
  private static extern IntPtr GetModuleHandle(string moduleName);

  public static void Configure(string path) {
    logFile = path;
  }

  public static void Start(string path) {
    Configure(path);
    logThread = new Thread(DrainLog);
    logThread.IsBackground = true;
    logThread.Name = "hosted-hotkey-guard-log";
    logThread.Start();

    var ready = new ManualResetEventSlim(false);
    Exception failure = null;
    hookThread = new Thread(() => {
      hookThreadId = GetCurrentThreadId();
      hook = SetWindowsHookEx(WhKeyboardLl, CallbackRoot, GetModuleHandle(null), 0);
      if (hook == IntPtr.Zero) {
        failure = new InvalidOperationException("Could not install the hosted Win+A keyboard guard; Win32 error " + Marshal.GetLastWin32Error());
        ready.Set();
        return;
      }
      ready.Set();
      NativeMessage message;
      while (GetMessage(out message, IntPtr.Zero, 0, 0) > 0) {
        TranslateMessage(ref message);
        DispatchMessage(ref message);
      }
      UnhookWindowsHookEx(hook);
      hook = IntPtr.Zero;
    });
    hookThread.IsBackground = true;
    hookThread.Name = "hosted-hotkey-guard-hook";
    hookThread.Priority = ThreadPriority.Highest;
    hookThread.Start();
    if (!ready.Wait(10000)) throw new InvalidOperationException("The hosted Win+A keyboard guard thread did not start within 10 seconds.");
    if (failure != null) throw failure;
    Log("hosted Win+A keyboard guard installed on a dedicated message-loop thread");
  }

  public static void Stop() {
    if (hookThread != null) {
      PostThreadMessage(hookThreadId, WmQuit, IntPtr.Zero, IntPtr.Zero);
      if (!hookThread.Join(5000)) throw new InvalidOperationException("The hosted Win+A keyboard guard thread did not stop within 5 seconds.");
      hookThread = null;
    }
    stopping = true;
    PendingLogSignal.Set();
    if (logThread != null) {
      logThread.Join(5000);
      logThread = null;
    }
    FlushLog();
  }

  public static void Log(string message) {
    if (logFile == null) return;
    lock (LogLock) {
      try { File.AppendAllText(logFile, DateTimeOffset.Now.ToString("o") + " " + message + Environment.NewLine); } catch { }
    }
  }

  private static IntPtr HandleKeyboard(int code, IntPtr message, IntPtr data) {
    if (code >= 0) {
      int msg = message.ToInt32();
      int key = Marshal.ReadInt32(data);
      bool down = msg == WmKeyDown || msg == WmSysKeyDown;
      bool up = msg == WmKeyUp || msg == WmSysKeyUp;
      if ((key == VkLWin || key == VkRWin) && (down || up)) {
        windowsKeyDown = down;
        if (down) Enqueue("blocked Windows key to keep hosted shell panels off the GUI desktop");
        return (IntPtr)1;
      }
      if (key == VkA && windowsKeyDown && (down || up)) {
        if (down) Enqueue("blocked Win+A to keep Windows Quick Settings off the hosted GUI desktop");
        return (IntPtr)1;
      }
    }
    return CallNextHookEx(hook, code, message, data);
  }

  // The hook callback never touches the file system; the log thread writes the queued lines.
  private static void Enqueue(string message) {
    PendingLog.Enqueue(DateTimeOffset.Now.ToString("o") + " " + message);
    PendingLogSignal.Set();
  }

  private static void DrainLog() {
    while (!stopping) {
      PendingLogSignal.WaitOne(250);
      FlushLog();
    }
  }

  private static void FlushLog() {
    string line;
    while (PendingLog.TryDequeue(out line)) {
      lock (LogLock) {
        try { File.AppendAllText(logFile, line + Environment.NewLine); } catch { }
      }
    }
  }
}
'@
}

$deadline = [DateTimeOffset]::Now.AddMinutes($MaximumMinutes)
$logDirectory = Split-Path -Parent $LogFile
New-Item -ItemType Directory -Force -Path $logDirectory | Out-Null
[HostedWindowsHotkeyGuard]::Configure($LogFile)
[HostedWindowsHotkeyGuard]::Log('watcher started; strict hosted-dialog matching and temporary Win+A hotkey guard enabled')

# The guard pumps its own messages on a dedicated thread, so this loop may block on other
# processes' dialogs without delaying the keyboard hook. All log lines go through one lock.
[HostedWindowsHotkeyGuard]::Start($LogFile)

while ([DateTimeOffset]::Now -lt $deadline) {
  try {
    $dismissedShellPanel = [MicroProject.HostedDialogWatcher]::DismissHostedQuickSettingsPanel()
    if ($null -ne $dismissedShellPanel) {
      [HostedWindowsHotkeyGuard]::Log("dismissed exact hosted shell panel; $dismissedShellPanel")
    }
    $dismissedDialog = [MicroProject.HostedDialogWatcher]::DismissKnownHostedConfigurationDialog()
    if ($null -ne $dismissedDialog) {
      [HostedWindowsHotkeyGuard]::Log("dismissed exact hosted configuration dialog; $dismissedDialog")
    }
  } catch {
    [HostedWindowsHotkeyGuard]::Log("watcher error: $_")
  }
  Start-Sleep -Milliseconds 50
}

[HostedWindowsHotkeyGuard]::Stop()
[HostedWindowsHotkeyGuard]::Log('watcher reached its bounded lifetime')
