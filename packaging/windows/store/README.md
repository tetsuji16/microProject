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
`windows-latest` and uploads the `.msix` as a workflow artifact.

Each Store submission needs a package version higher than the previous
submission; the four-component version is the Gradle release version.

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
- The in-app `UpdateChecker` (startup check and the About dialog) is
  **not** gated by that property. It is controlled by the user
  preference `checkForUpdates` (default on) and by the
  development-build marker in `version.properties`, which is off for
  release builds. A Store build can therefore still offer the GitHub
  Releases MSI from inside the app. This is a known gap: before the
  first Store submission, decide whether to gate `UpdateChecker` on
  the same property (a distribution-channel check in code) — no code
  change has been made for this yet.

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
