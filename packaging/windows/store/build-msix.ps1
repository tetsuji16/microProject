<#
.SYNOPSIS
	Builds the Microsoft Store .msix package for microProject (issue #1256).

.DESCRIPTION
	Combines the jpackage app-image produced by the Gradle task
	packageWindowsAppImage with the Store manifest and logo assets in this
	directory, then packs the result with the Windows SDK makeappx.exe.

	The package is intentionally left unsigned: Microsoft Store re-signs
	every package with the Store certificate during submission, so a
	developer signature is neither needed nor accepted for Store upload.
	(For a local sideloading test outside the Store, the package would
	first have to be signed with a certificate whose subject matches the
	manifest Publisher; see README.md.)

	The script also adds -Dmicroproject.distribution=store to the staged
	launcher configuration. With it the application makes no update
	request, and its "Check for updates" command opens the Store page.

.PARAMETER AppImageDir
	The microProject folder produced by packageWindowsAppImage
	(build\releases\v<version>\app-image\microProject). When omitted, the
	most recently modified app-image under build\releases is used.

.PARAMETER Version
	Release version of the app-image (for example 0.1.0.1324). When
	omitted, it is taken from the app-image folder name.

.PARAMETER PackageVersion
	Explicit MSIX package version with a zero fourth component (for
	example 0.1.1324.0). When omitted, it is derived from -Version:
	Microsoft Store requires revision 0, so a release version
	<major>.<minor>.0.<build> becomes <major>.<minor>.<build>.0.

.PARAMETER OutputDir
	Directory that receives the .msix file. Defaults to
	build\releases\store under the repository root.

.PARAMETER MakeAppxPath
	Explicit path to makeappx.exe. When omitted, the newest x64
	makeappx.exe under the Windows Kits 10 bin folder is used, falling
	back to a makeappx.exe on PATH.

.EXAMPLE
	.\gradlew.bat packageWindowsAppImage "-PreleaseVersion=0.1.0.25" --console=plain
	packaging\windows\store\build-msix.ps1 -Version 0.1.0.25
#>
[CmdletBinding()]
param(
	[string]$AppImageDir,
	[string]$Version,
	[string]$PackageVersion,
	[string]$OutputDir,
	[string]$MakeAppxPath
)

$ErrorActionPreference = 'Stop'

$storeDir = $PSScriptRoot
$repoRoot = (Resolve-Path (Join-Path $storeDir '..\..\..')).Path

function ConvertTo-StorePackageVersion([string]$Value) {
	# Microsoft Store reserves the fourth (revision) component: every
	# submitted package must have revision 0. Release versions put the
	# build number in the fourth component (0.1.0.<build>), so a release
	# version with a zero third component maps to <major>.<minor>.<build>.0.
	$parts = @($Value.Trim().TrimStart('v') -split '\.')
	if ($parts.Count -lt 3 -or $parts.Count -gt 4) {
		throw "Version '$Value' must have three or four numeric components."
	}
	foreach ($part in $parts) {
		if ($part -notmatch '^\d+$' -or [int64]$part -gt 65535) {
			throw "Version '$Value' must have three or four numeric components between 0 and 65535."
		}
	}
	while ($parts.Count -lt 4) { $parts += '0' }
	if ($parts[3] -ne '0') {
		if ($parts[2] -ne '0') {
			throw "Version '$Value' has a non-zero revision and a non-zero third component; pass -PackageVersion with a zero revision."
		}
		$parts = @($parts[0], $parts[1], $parts[3], '0')
	}
	return (($parts | ForEach-Object { [string][int64]$_ }) -join '.')
}

# --- Resolve the app-image -------------------------------------------------
if (-not $AppImageDir) {
	$candidates = @(Get-ChildItem -Path (Join-Path $repoRoot 'build\releases') -Directory -ErrorAction SilentlyContinue |
		ForEach-Object { Join-Path $_.FullName 'app-image\microProject' } |
		Where-Object { Test-Path (Join-Path $_ 'microProject.exe') } |
		Sort-Object { (Get-Item $_).LastWriteTime } -Descending)
	if (-not $candidates) {
		throw 'No app-image found under build\releases. Run .\gradlew.bat packageWindowsAppImage first.'
	}
	$AppImageDir = $candidates[0]
}
$AppImageDir = (Resolve-Path $AppImageDir).Path
if (-not (Test-Path (Join-Path $AppImageDir 'microProject.exe'))) {
	throw "microProject.exe not found in app-image directory: $AppImageDir"
}

# --- Resolve the package version -------------------------------------------
if (-not $Version) {
	$releaseDirName = Split-Path (Split-Path (Split-Path $AppImageDir -Parent) -Parent) -Leaf
	$Version = $releaseDirName
}
if ($PackageVersion) {
	$explicitParts = @($PackageVersion.Trim().TrimStart('v') -split '\.')
	if ($explicitParts.Count -eq 4 -and $explicitParts[3] -ne '0') {
		throw "PackageVersion '$PackageVersion' must have a zero fourth component; Microsoft Store reserves the revision."
	}
	$msixVersion = ConvertTo-StorePackageVersion $PackageVersion
} else {
	$msixVersion = ConvertTo-StorePackageVersion $Version
}
Write-Host "Release version: $Version; MSIX package version: $msixVersion"

# --- Locate the launcher configuration -------------------------------------
$launcherConfig = Join-Path $AppImageDir 'app\microProject.cfg'
if (-not (Test-Path $launcherConfig)) {
	throw "Launcher configuration not found: $launcherConfig"
}

# --- Find makeappx.exe -------------------------------------------------------
if (-not $MakeAppxPath) {
	$kitsBin = Join-Path ${env:ProgramFiles(x86)} 'Windows Kits\10\bin'
	$found = $null
	if (Test-Path $kitsBin) {
		$found = Get-ChildItem -Path $kitsBin -Directory |
			Sort-Object Name -Descending |
			ForEach-Object { Join-Path $_.FullName 'x64\makeappx.exe' } |
			Where-Object { Test-Path $_ } |
			Select-Object -First 1
	}
	if ($found) {
		$MakeAppxPath = $found
	} elseif (Get-Command makeappx.exe -ErrorAction SilentlyContinue) {
		$MakeAppxPath = (Get-Command makeappx.exe).Source
	} else {
		throw 'makeappx.exe not found. Install the Windows 10/11 SDK or pass -MakeAppxPath.'
	}
}

# --- Stage the package layout ------------------------------------------------
$stagingDir = Join-Path $repoRoot 'build\store-msix\staging'
if (Test-Path $stagingDir) { Remove-Item $stagingDir -Recurse -Force }
New-Item -ItemType Directory -Path $stagingDir -Force | Out-Null

Copy-Item -Path (Join-Path $AppImageDir '*') -Destination $stagingDir -Recurse -Force
Copy-Item -Path (Join-Path $storeDir 'assets') -Destination (Join-Path $stagingDir 'assets') -Recurse -Force

$manifestText = Get-Content -Path (Join-Path $storeDir 'AppxManifest.xml') -Raw
$manifestText = $manifestText -replace 'Version="0\.0\.0\.0"', "Version=`"$msixVersion`""
if ($manifestText -notmatch [regex]::Escape("Version=`"$msixVersion`"")) {
	throw 'Failed to stamp the package version into the staged manifest.'
}
Set-Content -Path (Join-Path $stagingDir 'AppxManifest.xml') -Value $manifestText -Encoding UTF8 -NoNewline

# --- Mark the staged launcher as the Store distribution ----------------------
# UpdateChecker reads microproject.distribution: a Store build makes no update
# request and its "Check for updates" command opens the Store product page.
$storeOption = '-Dmicroproject.distribution=store'
$stagedConfig = Join-Path $stagingDir 'app\microProject.cfg'
$configLines = [System.Collections.Generic.List[string]](Get-Content -Path $stagedConfig)
if (-not ($configLines -contains "java-options=$storeOption")) {
	$javaOptionsIndex = $configLines.IndexOf('[JavaOptions]')
	if ($javaOptionsIndex -lt 0) {
		throw "No [JavaOptions] section in the staged launcher configuration: $stagedConfig"
	}
	$configLines.Insert($javaOptionsIndex + 1, "java-options=$storeOption")
	Set-Content -Path $stagedConfig -Value $configLines -Encoding ASCII
}
if (-not (Select-String -Path $stagedConfig -SimpleMatch "java-options=$storeOption" -Quiet)) {
	throw "Failed to mark the staged launcher as the Store distribution ($storeOption)."
}
Write-Host "Staged launcher marked with $storeOption"

# --- Pack (unsigned; the Store re-signs on submission) -----------------------
if (-not $OutputDir) {
	$OutputDir = Join-Path $repoRoot 'build\releases\store'
}
New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null
$msixPath = Join-Path $OutputDir "microProject-$msixVersion.msix"
if (Test-Path $msixPath) { Remove-Item $msixPath -Force }

& $MakeAppxPath pack /d $stagingDir /p $msixPath /o
if ($LASTEXITCODE -ne 0) {
	throw "makeappx.exe failed with exit code $LASTEXITCODE."
}

Write-Host "Created Store package: $msixPath"
Write-Host 'The package is unsigned by design; Microsoft Store re-signs it during submission.'
