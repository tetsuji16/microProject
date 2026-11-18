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
using System.Collections.Generic;
using System.Runtime.InteropServices;
using System.Text;
using System.IO;

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
            || !String.Equals(ReadText(window), "Quick settings", StringComparison.Ordinal)) return true;
        SendMessage(window, 0x0010, IntPtr.Zero, IntPtr.Zero); // WM_CLOSE the exact hosted shell panel
        dismissed = "class=ControlCenterWindow; title=Quick settings";
        return false;
      }, IntPtr.Zero);
      return dismissed;
    }
  }
}

public static class HostedWindowsHotkeyGuard {
  private const int WhKeyboardLl = 13;
  private const int WmKeyDown = 0x0100;
  private const int WmKeyUp = 0x0101;
  private const int WmSysKeyDown = 0x0104;
  private const int WmSysKeyUp = 0x0105;
  private const int VkA = 0x41;
  private const int VkLWin = 0x5B;
  private const int VkRWin = 0x5C;

  private delegate IntPtr LowLevelKeyboardProc(int code, IntPtr message, IntPtr data);
  private static readonly LowLevelKeyboardProc CallbackRoot = HandleKeyboard;
  private static IntPtr hook;
  private static string logFile;
  private static bool windowsKeyDown;

  [DllImport("user32.dll", SetLastError = true)]
  private static extern IntPtr SetWindowsHookEx(int idHook, LowLevelKeyboardProc callback, IntPtr module, uint threadId);
  [DllImport("user32.dll", SetLastError = true)]
  private static extern bool UnhookWindowsHookEx(IntPtr hookHandle);
  [DllImport("user32.dll")]
  private static extern IntPtr CallNextHookEx(IntPtr hookHandle, int code, IntPtr message, IntPtr data);
  [DllImport("kernel32.dll", CharSet = CharSet.Auto, SetLastError = true)]
  private static extern IntPtr GetModuleHandle(string moduleName);

  public static void Start(string path) {
    logFile = path;
    hook = SetWindowsHookEx(WhKeyboardLl, CallbackRoot, GetModuleHandle(null), 0);
    if (hook == IntPtr.Zero) throw new InvalidOperationException("Could not install the hosted Win+A keyboard guard; Win32 error " + Marshal.GetLastWin32Error());
    WriteLog("hosted Win+A keyboard guard installed");
  }

  public static void Stop() {
    if (hook != IntPtr.Zero) {
      if (!UnhookWindowsHookEx(hook)) throw new InvalidOperationException("Could not remove the hosted Win+A keyboard guard; Win32 error " + Marshal.GetLastWin32Error());
      hook = IntPtr.Zero;
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
        if (down) WriteLog("blocked Windows key to keep hosted shell panels off the GUI desktop");
        return (IntPtr)1;
      }
      if (key == VkA && windowsKeyDown && (down || up)) {
        if (down) WriteLog("blocked Win+A to keep Windows Quick Settings off the hosted GUI desktop");
        return (IntPtr)1;
      }
    }
    return CallNextHookEx(hook, code, message, data);
  }

  private static void WriteLog(string message) {
    try { File.AppendAllText(logFile, DateTimeOffset.Now.ToString("o") + " " + message + Environment.NewLine); } catch { }
  }
}
'@
}

$deadline = [DateTimeOffset]::Now.AddMinutes($MaximumMinutes)
$logDirectory = Split-Path -Parent $LogFile
New-Item -ItemType Directory -Force -Path $logDirectory | Out-Null
"$(Get-Date -Format o) watcher started; strict hosted-dialog matching and temporary Win+A hotkey guard enabled" |
  Add-Content -LiteralPath $LogFile -Encoding utf8

[HostedWindowsHotkeyGuard]::Start($LogFile)
$messagePump = [Windows.Forms.Timer]::new()
$messagePump.Interval = 100
$messagePump.Start()

while ([DateTimeOffset]::Now -lt $deadline) {
  [Windows.Forms.Application]::DoEvents()
  try {
    $dismissedShellPanel = [MicroProject.HostedDialogWatcher]::DismissHostedQuickSettingsPanel()
    if ($null -ne $dismissedShellPanel) {
      "$(Get-Date -Format o) dismissed exact hosted shell panel; $dismissedShellPanel" |
        Add-Content -LiteralPath $LogFile -Encoding utf8
    }
    $dismissedDialog = [MicroProject.HostedDialogWatcher]::DismissKnownHostedConfigurationDialog()
    if ($null -ne $dismissedDialog) {
      "$(Get-Date -Format o) dismissed exact hosted configuration dialog; $dismissedDialog" |
        Add-Content -LiteralPath $LogFile -Encoding utf8
    }
  } catch {
    "$(Get-Date -Format o) watcher error: $_" | Add-Content -LiteralPath $LogFile -Encoding utf8
  }
  Start-Sleep -Milliseconds 50
}

$messagePump.Stop()
$messagePump.Dispose()
[HostedWindowsHotkeyGuard]::Stop()

"$(Get-Date -Format o) watcher reached its bounded lifetime" |
  Add-Content -LiteralPath $LogFile -Encoding utf8
