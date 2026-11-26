# Microsoft Store (MSIX) packaging

This directory contains everything needed to package microProject as an
MSIX for Microsoft Store distribution (issue #1256: the unsigned
executable is blocked by Windows 11 Smart App Control, and Store
packages are signed by the Store, which removes that block).

## Contents

- `AppxManifest.xml` — Store package manifest. Its Identity values are
  the ones reserved for this application in Partner Center and must not
  change:
  - Store ID: `9N89VPNDHP93`
  - Identity Name: `tetsuji.microProject`
  - Publisher: `CN=43D05264-95D7-4B0C-89CD-FD36B3613B22`
  - PublisherDisplayName: `tetsuji`
  - Package Family Name: `tetsuji.microProject_ay08s11nqabca`
  - The `Version` attribute is a placeholder; `build-msix.ps1` stamps
    the real version into the staged copy before packing.
  - The file type associations (`.mpo`, `.mpp`, `.pod`, `.xml`) mirror
    `packaging/windows/file-associations/*.properties`.
- `assets/` — logo images generated from
  `packaging/windows/icons/microproject.ico` (the 256x256 PNG entry,
  resized with Lanczos resampling; the wide logo centers that image on
  a transparent 310x150 canvas). To regenerate, extract the largest
  PNG entry from the .ico and resize it to each file name's pixel size.
- `build-msix.ps1` — stages the app-image together with the manifest
  and assets and packs the `.msix` with `makeappx.exe`. The package is
  left unsigned on purpose: the Store re-signs submissions.

## Prerequisites

- Windows 10 or 11 with a full JDK 25 (for `jpackage` via Gradle).
- Windows 10/11 SDK (provides `makeappx.exe`), or pass `-MakeAppxPath`.
- The Gradle `packageWindowsAppImage` output for the version being
  submitted.

## Build

```powershell
.\gradlew.bat packageWindowsAppImage "-PreleaseVersion=0.1.0.25" --console=plain
packaging\windows\store\build-msix.ps1 -Version 0.1.0.25
```

The `.msix` is written to `build\releases\store\`. Alternatively, run
the manually dispatched GitHub Actions workflow
`.github/workflows/store-msix.yml`, which performs the same steps on
`windows-latest` for the release version given as its input and
uploads the `.msix` as a workflow artifact.

Microsoft Store reserves the fourth (revision) component of the package
version: every submitted package must have revision 0. Release versions
put the build number in the fourth component (`0.1.0.<build>`), so
`build-msix.ps1` packs release `0.1.0.1324` as package version
`0.1.1324.0`. The app-image itself keeps the release version. Pass
`-PackageVersion` to choose a package version explicitly. Each Store
submission needs a package version higher than the previous one.

## Update policy for the Store build

Store packages must be serviced by the Store, not by the application
itself. Two update mechanisms exist in the codebase:

- The bootstrap launcher (`MicroProjectUpdater`, update4j) is gated by
  the system property `microproject.updates.enabled`.
  `packageWindowsAppImage` already writes
  `-Dmicroproject.updates.enabled=false` into `microProject.cfg`, so
  an app-image built by that task never self-updates. `build-msix.ps1`
  verifies the option is present in the staged launcher configuration
  and fails the build if it is missing.
- The in-app `UpdateChecker` only notifies; it never downloads or
  installs a release. `build-msix.ps1` adds
  `-Dmicroproject.distribution=store` to the staged
  `microProject.cfg`. With that property the application makes no
  update request at startup, hides the update preference, and the
  Backstage "Check for updates" command opens the Store product page
  (`ms-windows-store://pdp/?ProductId=9N89VPNDHP93`).

## Submitting to Partner Center

1. Sign in to Partner Center and open **Apps and games** → the reserved
   **microProject** app (Store ID `9N89VPNDHP93`).
2. Start a new submission and upload the `.msix` under **Packages**.
   Partner Center validates the package identity against the reserved
   values above and re-signs the package.
3. Complete the listing (description, screenshots), age ratings, and
   pricing/availability sections, then submit for certification.

## Local install testing

An unsigned MSIX cannot be installed directly. For a local test,
either submit a test flight through Partner Center, or sign the
package with a self-signed certificate whose subject is exactly the
manifest Publisher (`CN=43D05264-95D7-4B0C-89CD-FD36B3613B22`), trust
that certificate on the test machine, and install with
`Add-AppxPackage`. Note that the app writes its settings and logs
under `%LOCALAPPDATA%\microProject`; under MSIX those writes are
redirected into the package container, which is expected.
