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

    public static bool DismissKnownPagingFileWarning() {
      var warningPrefix = "Windows created a temporary paging file on your computer";
      var warningSuffix = "paging file configuration";
      IntPtr matchedDialog = IntPtr.Zero;
      IntPtr okButton = IntPtr.Zero;
      string matchedText = null;

      EnumWindows((window, parameter) => {
        if (!IsWindowVisible(window) || !String.Equals(ReadText(window), "System Properties", StringComparison.Ordinal)) return true;
        var parts = new List<string>();
        var candidateButton = IntPtr.Zero;
        EnumChildWindows(window, (child, childParameter) => {
          var text = ReadText(child);
          if (!String.IsNullOrWhiteSpace(text)) parts.Add(text);
          if (String.Equals(ReadClass(child), "Button", StringComparison.Ordinal) && String.Equals(text, "OK", StringComparison.OrdinalIgnoreCase)) candidateButton = child;
          return true;
        }, IntPtr.Zero);
        var content = String.Join(" ", parts);
        if (candidateButton != IntPtr.Zero && content.Contains(warningPrefix) && content.Contains(warningSuffix)) {
          matchedDialog = window;
          okButton = candidateButton;
          matchedText = content;
          return false;
        }
        return true;
      }, IntPtr.Zero);

      if (matchedDialog == IntPtr.Zero) return false;
      SendMessage(okButton, 0x00F5, IntPtr.Zero, IntPtr.Zero); // BM_CLICK
      Console.WriteLine("{0:o} dismissed exact hosted paging-file warning; title=System Properties; text={1}", DateTimeOffset.Now, matchedText);
      return true;
    }
  }
}
'@
}

$deadline = [DateTimeOffset]::Now.AddMinutes($MaximumMinutes)
$logDirectory = Split-Path -Parent $LogFile
New-Item -ItemType Directory -Force -Path $logDirectory | Out-Null
"$(Get-Date -Format o) watcher started; strict System Properties title and paging-file diagnostic match only" |
  Add-Content -LiteralPath $LogFile -Encoding utf8

while ([DateTimeOffset]::Now -lt $deadline) {
  try {
    [void][MicroProject.HostedDialogWatcher]::DismissKnownPagingFileWarning()
  } catch {
    "$(Get-Date -Format o) watcher error: $_" | Add-Content -LiteralPath $LogFile -Encoding utf8
  }
  Start-Sleep -Milliseconds 250
}

"$(Get-Date -Format o) watcher reached its bounded lifetime" |
  Add-Content -LiteralPath $LogFile -Encoding utf8
