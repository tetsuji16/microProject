# Build And Run Runbook

This project has two different build outputs that matter during verification:

- `build` compiles and tests the code.
- `installDist` refreshes the runnable desktop app layout under `modules/micrproject_ui/build/install/micrproject_ui`.

When you need to execute the app for manual verification, always run the installed layout produced by `installDist`, not an older copy of the generated files.

## Safe Verification Flow

Use this sequence when you want to build and immediately test the desktop app:

```powershell
.\gradlew.bat clean build installDist --console=plain
```

If you also want to remove generated legacy packaging scratch output before rebuilding, use:

```powershell
.\gradlew.bat clean cleanLegacyPackagingArtifacts installDist --console=plain
```

Then launch the installed app from:

```powershell
modules\micrproject_ui\build\install\micrproject_ui\bin\micrproject_ui.bat
```

For the quickest repeatable Windows workflow, use the repository launcher instead:

```powershell
.\scripts\run_micrproject_clean.bat
```

Or double-click:

```text
scripts\run_projectlibre.bat
```

That launcher:

1. Stops only existing ProjectLibre Java processes instead of killing every Java process on the machine.
2. Runs `clean build installDist --console=plain`.
3. Starts the installed app layout with a fixed classpath order so `DefaultFormBuilder` resolves to the bundled compatibility shim.

## Why This Matters

`build` alone can leave an already-generated `build/install/...` layout behind from an earlier run. If you execute that stale layout, you may be testing an old `micrproject_ui.jar` even though the source tree has already been rebuilt.

To avoid that trap:

1. Run `clean build installDist` before manual UI verification.
2. If you only changed code and want a quicker refresh, at minimum run `installDist` again before launching the app.
3. Treat `modules/micrproject_ui/build/install/micrproject_ui` as disposable generated output, not a source-controlled artifact.

## Incremental Launcher

Use `scripts\run_projectlibre.ps1` when you want a logged incremental run instead of a clean rebuild:

```powershell
.\scripts\run_projectlibre.ps1
.\scripts\run_projectlibre.ps1 -SkipBuild
.\scripts\run_projectlibre.ps1 -Clean
.\scripts\run_projectlibre.ps1 -UiDebug
```

It writes per-run logs under:

- `build\logs\projectlibre\yyyyMMdd-HHmmss\`
- `build\logs\projectlibre\latest\`
- `launcher.log`
- `app.stdout.log`
- `app.stderr.log`

`-UiDebug` also enables a focused UI interaction trace at `ui-debug.log`. It records
selection changes, the Information ribbon command, action enablement, and task-dialog
creation/visibility. Use it to determine whether a reported click failed to dispatch,
lost its selection, was rejected by routing, or reached the dialog.

GUI受入は同じRobotケースをlocale／DPI軸でも実行できる。標準は日本語・100%で、
U-21の視覚検査では次のように別プロセスで実行する。

```powershell
.\gradlew.bat :micrproject_ui:guiTest '-PguiTestLocale=ja' '-PguiTestUiScale=1' --max-workers=1 --no-daemon --console=plain
.\gradlew.bat :micrproject_ui:guiTest '-PguiTestLocale=ja' '-PguiTestUiScale=1.25' --max-workers=1 --no-daemon --console=plain
.\gradlew.bat :micrproject_ui:guiTest '-PguiTestLocale=ja' '-PguiTestUiScale=1.5' --max-workers=1 --no-daemon --console=plain
.\gradlew.bat :micrproject_ui:guiTest '-PguiTestLocale=en' '-PguiTestUiScale=1.25' --max-workers=1 --no-daemon --console=plain
```

各実行で画面キャプチャとコンポーネント境界を確認し、倍率指定だけで成功扱いにしない。

Use `run_projectlibre.ps1` when you want to reuse an existing `installDist` output or capture logs. Use `run_micrproject_clean.bat` when you want the safest one-step clean rebuild and launch.

## Quick Checks

- Confirm the installed app layout was refreshed recently.
- Confirm the title bar and UI reflect the latest source change.
- If a fix appears missing, verify that the executable came from `modules/micrproject_ui/build/install/micrproject_ui`, not an older local copy.

## Notes For Codex

When you are asked to "build and run" or to validate a UI fix, use the installed app layout created by `installDist`. If the user reports that a fix "did not take," check whether the run was done from stale `build/install` output before assuming the source change failed.
