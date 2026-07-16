# ProjectLibre Development Fork

This repository is an unofficial development fork of ProjectLibre. It keeps the original desktop planning tool as its base and extends it with practical improvements for day-to-day scheduling, collaboration, import/export, and Gantt usability.

Current release in this fork:

- `v0.0.16`

Quick links:

- [ProjectLibre GitHub Pages documentation](https://tetsuji16.github.io/ProjectLibre/docs/)

![ProjectLibre Gantt view](docs/images/demo_gannt.png)

## What This Fork Is For

- Keep ProjectLibre usable on a modern JDK and packaging toolchain
- Improve real-world planning workflows instead of only preserving legacy behavior
- Add collaboration-oriented features for teams sharing project files locally
- Make the Gantt and spreadsheet views smoother to use for large schedules

## Baseline And Update Ratio

The comparison baseline in this fork is the ProjectLibre 1.9.8 modernization commit:

- Commit: `0530be227f4a10c5545cce8d3db20ac5a4d76a66`
- Subject: `ProjectLibre 1.9.8 Java and Libraries updates Code using deprecated apis updated Performance fixes Toolbar fixes New builds using jpackage supporting Java 21 and ARM architecture.`

The figures below describe cumulative change volume since that baseline commit.

- Changed tracked file paths since `0530be22`: `2289 / 2386` (`95.9%` of the baseline tracked file count)
- Changed tracked text lines since `0530be22`: `673624`

## What Has Been Added Or Improved Since `0530be22`

- XLSX import/export support and related collaboration refresh behavior
- Local collaboration support for shared project files
- Gantt and Tracking Gantt progress-line improvements
- Gantt zoom restore behavior
- Mouse-wheel and horizontal scrolling refinements
- Startup and initial view loading fixes
- Logging cleanup, resource-leak fixes, and modernized build/CI setup

## Repository Layout

- `modules/projectlibre_core`: scheduling engine, data model, collaboration logic, and configuration
- `modules/projectlibre_application`: application workflows, file policies, and save/open coordination
- `modules/projectlibre_ui`: Swing UI, Gantt rendering, spreadsheet views, menus, and startup flow
- `modules/projectlibre_exchange`: file exchange, import/export, and format integration code
- `modules/projectlibre_reports`: report-related code and templates
- `modules/projectlibre_contrib`: shared third-party dependencies built into the app distribution
- `packaging`: active packaging assets, licenses, and Windows release icons
- `samples`: sample project files for screenshots and manual verification
- `scripts`: launch helpers for local verification

## Requirements

- Windows with a full JDK that includes `jpackage`
- Java 25+ required
- Gradle Wrapper support files are included in this repository
- WiX Toolset on `PATH` for MSI packaging

If `JAVA_HOME` is not set, the Gradle release tasks fall back to `C:\Program Files\Java\jdk-25`.

## Build System Status

- `Gradle` is the supported build and release entrypoint for this repository
- `build.gradle.kts` drives module compilation, installable app layout generation, and Windows `jpackage` packaging
- `packaging` is the source of active packaging assets, icons, and license notices consumed by the Gradle tasks
- Keep `projectlibre_contrib` jars lean when updating dependencies so the packaged app size does not grow unnecessarily
- CI is aligned to the Gradle flow and validates the installable desktop layout on JDK 25

## Build The App

Compile every module and assemble the desktop application layout:

```powershell
.\gradlew.bat build
```

Create the runnable installed app layout:

```powershell
.\gradlew.bat stageAppDist
```

Key Gradle entrypoints:

- `.\gradlew.bat projects`: show the multi-project layout
- `.\gradlew.bat build`: compile the production modules and assemble per-module jars
- `.\gradlew.bat stageAppDist`: create the installed desktop app layout from `:projectlibre_ui:installDist`
- `.\gradlew.bat cleanLegacyPackagingArtifacts`: remove generated legacy packaging scratch output such as `isolated-build`
- `.\gradlew.bat packageWindowsAppImage`: build a Windows app-image with `jpackage`
- `.\gradlew.bat packageWindowsMsi`: build the Windows MSI
- `.\gradlew.bat packageWindowsExe`: build the Windows self-contained EXE
- `.\gradlew.bat publishReleaseToDocs`: publish the split EXE artifacts into local `docs/downloads` scratch space when needed

When you are manually verifying a UI fix, use the installed app layout created by `stageAppDist` / `installDist`, not an older `build/install` copy. See [docs/build-and-run.md](docs/build-and-run.md) for the exact runbook.

On Windows, the safest one-step launcher is `scripts\run_projectlibre_clean.bat`, and `scripts\run_projectlibre.bat` is a double-click entry point to the same flow.

The runnable application layout is generated under:

- `modules\projectlibre_ui\build\install\projectlibre_ui`

The root release work area is generated under:

- `build\releases\v<version>\`

Legacy packaging scratch output, if present, is disposable and should not be committed:

- `isolated-build\`

## Build The Windows Release

Build the Windows release artifacts and stage them locally:

```powershell
.\gradlew.bat packageWindowsMsi
```

The Gradle Windows release flow stages the packaging input under:

- `build\releases\v0.0.16\jpackage-input\`

The local MSI output is generated under:

- `build\releases\v0.0.16\msi\ProjectLibre-0.0.16.msi`

If you need the portable app-image ZIP or split EXE staging flow, use:

```powershell
.\gradlew.bat packageWindowsAppImage
.\gradlew.bat packageWindowsZip
.\gradlew.bat publishReleaseToDocs
```

Files under `docs/downloads/` are treated as scratch space only and should not be committed. Public downloads should be published as GitHub Release assets for `v0.0.16`, and the GitHub Pages site should link to that release page instead of serving binaries from the repository itself.

The GitHub Pages landing page for this release is:

- `docs/index.html`

If WiX was installed per-user rather than system-wide, keep its `bin` directory available on `PATH`. The Gradle MSI task also prepends the common per-user install path automatically:

```text
%LOCALAPPDATA%\Programs\WiX Toolset v7.0\bin
```

## Screenshot Procedure Used In This Repository

The README screenshot is intentionally captured so that only the application UI is visible.

- Launch the app by itself, not the whole desktop workspace
- Open `samples\sampledata.mpp`
- Arrange the main Gantt view at a readable zoom level
- Capture only the app window client area so title-bar paths, taskbar items, IDE windows, notifications, and personal information do not appear

## Quick Verification

- `.\gradlew.bat projects`: the multi-project Gradle layout resolves
- `.\gradlew.bat build`: all production modules compile successfully
- `.\gradlew.bat stageAppDist`: the runnable desktop layout is generated
- `.\gradlew.bat packageWindowsMsi`: the MSI is emitted successfully

## License

This fork builds on ProjectLibre and keeps the original license materials and third-party notices in `packaging/licenses`.
