param(
  [Parameter(Mandatory = $true)]
  [string]$LogFile,
  [int]$MaximumMinutes = 115
)

$ErrorActionPreference = 'Stop'

if (-not ('MicroProject.HostedDialogWatcher' -as [type])) {
  Add-Type -TypeDefinition @'
using System;
using System.Collections.Generic;
using System.Runtime.InteropServices;
using System.Text;

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
  }
}
'@
}

$deadline = [DateTimeOffset]::Now.AddMinutes($MaximumMinutes)
$logDirectory = Split-Path -Parent $LogFile
New-Item -ItemType Directory -Force -Path $logDirectory | Out-Null
"$(Get-Date -Format o) watcher started; strict System Properties or Performance Options title and diagnostic match only" |
  Add-Content -LiteralPath $LogFile -Encoding utf8

while ([DateTimeOffset]::Now -lt $deadline) {
  try {
    $dismissedDialog = [MicroProject.HostedDialogWatcher]::DismissKnownHostedConfigurationDialog()
    if ($null -ne $dismissedDialog) {
      "$(Get-Date -Format o) dismissed exact hosted configuration dialog; $dismissedDialog" |
        Add-Content -LiteralPath $LogFile -Encoding utf8
    }
  } catch {
    "$(Get-Date -Format o) watcher error: $_" | Add-Content -LiteralPath $LogFile -Encoding utf8
  }
  Start-Sleep -Milliseconds 250
}

"$(Get-Date -Format o) watcher reached its bounded lifetime" |
  Add-Content -LiteralPath $LogFile -Encoding utf8
