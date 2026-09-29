param(
  [int]$Width = 1920,
  [int]$Height = 1080
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Windows.Forms

if ($Width -lt 1920 -or $Height -lt 1080) {
  throw "GUI acceptance requires at least 1920x1080 physical pixels; requested ${Width}x${Height}."
}

$displayApi = @'
using System;
using System.Runtime.InteropServices;

public static class GuiDisplaySettings {
  public const int TestMode = 0x00000002;
  public const int WidthField = 0x00080000;
  public const int HeightField = 0x00100000;

  [StructLayout(LayoutKind.Sequential, CharSet = CharSet.Unicode)]
  public struct DevMode {
    [MarshalAs(UnmanagedType.ByValTStr, SizeConst = 32)] public string DeviceName;
    public short SpecVersion;
    public short DriverVersion;
    public short Size;
    public short DriverExtra;
    public int Fields;
    public int PositionX;
    public int PositionY;
    public int DisplayOrientation;
    public int DisplayFixedOutput;
    public short Color;
    public short Duplex;
    public short YResolution;
    public short TTOption;
    public short Collate;
    [MarshalAs(UnmanagedType.ByValTStr, SizeConst = 32)] public string FormName;
    public short LogPixels;
    public int BitsPerPixel;
    public int PelsWidth;
    public int PelsHeight;
    public int DisplayFlags;
    public int DisplayFrequency;
    public int ICMMethod;
    public int ICMIntent;
    public int MediaType;
    public int DitherType;
    public int Reserved1;
    public int Reserved2;
    public int PanningWidth;
    public int PanningHeight;
  }

  [DllImport("user32.dll", CharSet = CharSet.Unicode)]
  public static extern int ChangeDisplaySettings(ref DevMode settings, int flags);
}
'@
Add-Type -TypeDefinition $displayApi

$current = [System.Windows.Forms.Screen]::PrimaryScreen.Bounds
Write-Host "Initial GUI runner display: $($current.Width)x$($current.Height) pixels"
if ($current.Width -lt $Width -or $current.Height -lt $Height) {
  # ChangeDisplaySettings only needs a DEVMODE size and the fields being
  # changed. Avoid EnumDisplaySettings because hosted runner sessions may not
  # expose a current mode even though their desktop is usable by Robot.
  $mode = [GuiDisplaySettings+DevMode]::new()
  $mode.Size = [Runtime.InteropServices.Marshal]::SizeOf($mode)
  $mode.Fields = [GuiDisplaySettings]::WidthField -bor [GuiDisplaySettings]::HeightField
  $mode.PelsWidth = $Width
  $mode.PelsHeight = $Height
  $testResult = [GuiDisplaySettings]::ChangeDisplaySettings([ref]$mode, [GuiDisplaySettings]::TestMode)
  if ($testResult -ne 0) {
    throw "Windows rejected the ${Width}x${Height} GUI test resolution (ChangeDisplaySettings test=$testResult)."
  }
  $applyResult = [GuiDisplaySettings]::ChangeDisplaySettings([ref]$mode, 0)
  if ($applyResult -ne 0) {
    throw "Could not apply the ${Width}x${Height} GUI test resolution (ChangeDisplaySettings=$applyResult)."
  }
  Start-Sleep -Seconds 2
}

$actual = [System.Windows.Forms.Screen]::PrimaryScreen.Bounds
Write-Host "Verified GUI runner display: $($actual.Width)x$($actual.Height) pixels"
if ($actual.Width -lt $Width -or $actual.Height -lt $Height) {
  throw "Windows reported only $($actual.Width)x$($actual.Height) pixels after requesting ${Width}x${Height}."
}
