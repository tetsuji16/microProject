# Build And Run Runbook

This project has two different build outputs that matter during verification:

- `build` compiles and tests the code.
- `installDist` refreshes the runnable desktop app layout under `projectlibre_ui/build/install/projectlibre_ui`.

When you need to execute the app for manual verification, always run the installed layout produced by `installDist`, not an older copy of the generated files.

## Safe Verification Flow

Use this sequence when you want to build and immediately test the desktop app:

```powershell
.\gradlew.bat clean build installDist --console=plain
```

Then launch the installed app from:

```powershell
projectlibre_ui\build\install\projectlibre_ui\bin\projectlibre_ui.bat
```

For the quickest repeatable Windows workflow, use the repository launcher instead:

```powershell
.\run_projectlibre.ps1
```

Or double-click:

```text
run_projectlibre.bat
```

That launcher:

1. Refreshes `:projectlibre_ui:installDist` unless `-SkipBuild` is passed.
2. Stops only existing ProjectLibre Java processes instead of killing every Java process on the machine.
3. Starts the installed app layout and writes per-run logs under `build\logs\projectlibre\`.

## Why This Matters

`build` alone can leave an already-generated `build/install/...` layout behind from an earlier run. If you execute that stale layout, you may be testing an old `projectlibre_ui.jar` even though the source tree has already been rebuilt.

To avoid that trap:

1. Run `clean build installDist` before manual UI verification.
2. If you only changed code and want a quicker refresh, at minimum run `installDist` again before launching the app.
3. Treat `projectlibre_ui/build/install/projectlibre_ui` as disposable generated output, not a source-controlled artifact.

## Launcher Log Locations

- Timestamped launcher sessions: `build\logs\projectlibre\yyyyMMdd-HHmmss\`
- Latest copied session: `build\logs\projectlibre\latest\`
- Launcher status log: `launcher.log`
- App stdout log: `app.stdout.log`
- App stderr log: `app.stderr.log`

Useful launcher options:

```powershell
.\run_projectlibre.ps1 -SkipBuild
.\run_projectlibre.ps1 -Clean
```

## Quick Checks

- Confirm the installed app layout was refreshed recently.
- Confirm the title bar and UI reflect the latest source change.
- If a fix appears missing, verify that the executable came from `projectlibre_ui/build/install/projectlibre_ui`, not an older local copy.

## Notes For Codex

When you are asked to "build and run" or to validate a UI fix, use the installed app layout created by `installDist`. If the user reports that a fix "did not take," check whether the run was done from stale `build/install` output before assuming the source change failed.
