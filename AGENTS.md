# ProjectLibre agent guide

## Goal and scope

- Maintain this Windows-oriented ProjectLibre desktop fork without regressing existing project files, scheduling behavior, or packaging.
- Prefer the smallest change that fixes the root cause. Trace the relevant input, state transition, model/view conversion, persistence, and reload path before editing.
- Preserve unrelated user changes. Existing uncommitted changes are not a blocker, but inspect `git status` before and after work and never discard changes you did not create.
- Do not claim a fix is complete when only a symptom is hidden. If the root cause or a required verification remains uncertain, state that explicitly.

## Start here

Read only what the task needs:

1. `README.md` for the current build, release, and repository layout.
2. `docs/build-and-run.md` for desktop launch and manual UI verification.
3. `TEST_PLAN.md` for risk-specific regression cases.
4. The target module's `build.gradle.kts`, nearby tests, callers, and persisted formats.

Use `rg` / `rg --files` for discovery. Search by symbol and behavior before introducing a new helper; this codebase contains legacy and compatibility paths that may look duplicated but serve different formats or UI flows.

## Module map

- `modules/micrproject_core`: scheduling engine, data model, shared utilities, and core configuration.
- `modules/micrproject_application`: open/save workflows, document coordination, and file policies.
- `modules/micrproject_ui`: Swing UI, spreadsheet and Gantt views, rendering, menus, and startup.
- `modules/micrproject_exchange`: MPP/POD/XML/XLSX import/export and collaboration metadata integration.
- `modules/micrproject_reports`: report code and templates.
- `modules/micrproject_contrib`: bundled compatibility and third-party code; avoid growing packaged dependencies unnecessarily.
- `packaging`: authoritative icons, licenses, file associations, and Windows release inputs.
- `samples`: manual-verification fixtures. Application runs can modify files here, so inspect changes and do not commit incidental rewrites.

Respect the dependency direction expressed in the Gradle files. Put workflow coordination in `micrproject_application`, reusable domain behavior in `micrproject_core`, format conversion in `micrproject_exchange`, and view-only behavior in `micrproject_ui`.

## Dependency and namespace hygiene (eliminate ProjectLibre coupling)

The fork's canonical namespace is `com.microproject` (and the legacy serialization
alias `com.projectlibre1` kept only inside `SafeObjectInput` for reading old `.pod`
files — see issue #154). Anything that still carries the `projectLibre` / `ProjectLibre`
/ `com.projectlibre` name is unfinished rename work and must not be widened.

- **Module names**: every module under `modules/` must be `micrproject_*`. The
  `modules/projectlibre_*` directories are dead legacy modules (not included in
  `settings.gradle`); do not reference or revive them. Rename or delete before they
  accrue new code.
- **Packages**: source under `com.projectlibre` / `com.projectlibre1` is a leftover
  from the rename. New code always uses `com.microproject`. Do not add new
  `com.projectlibre*` packages or imports from `micrproject_*` modules. The only
  tolerated `com.projectlibre1` reference is the deserialization remap in
  `SafeObjectInput` (backward-compatible `.pod` reads).
- **Brand/identifier names**: method/constant names such as `isProjectLibreFile`,
  `LOCAL_PROJECT_IMPORTER`, `ProjectLibreShell` are acceptable only as established
  internal identifiers for the *file format*; do not introduce new `ProjectLibre`-prefixed
  public types or APIs. Prefer neutral names (`isNativeFile`, `LOCAL_IMPORTER`, …) for
  new code.
- **Third-party / contrib**: keep `micrproject_contrib` for bundled compatibility code
  and avoid growing packaged dependencies. External libraries must not pull the old
  ProjectLibre namespace into `micrproject_*` runtime modules.
- **License headers**: files should carry the MIT header with
  `Copyright (c) 2026 microProject`. Old CPAL/`Copyright (c) 2012-2019 ProjectLibre Inc.`
  headers are being phased out (see issue #43 phase 1); do not copy the old header into
  new or moved files.

## Implementation conventions

- The project requires a full JDK 25 or newer and compiles with Java release 25. Use the checked-in Gradle Wrapper (`.\gradlew.bat` on Windows).
- Follow `.editorconfig`: UTF-8, CRLF, final newline, and tabs by default; YAML uses two spaces and properties files use four spaces.
- Follow nearby Java style and existing package conventions. The rename from
  `com.projectlibre1`/`com.projectlibre` to `com.microproject` is the ongoing goal
  (issue #43); complete partial renames when you touch a file rather than leaving
  mixed namespaces, but do not launch opportunistic broad renames unrelated to the
  task at hand. See "Dependency and namespace hygiene" above.
- Keep UI work Swing-safe. Preserve EDT boundaries, model/view index conversions, selection state, and repaint/revalidation behavior.
- Avoid broad refactors unless they remove duplicated responsibility directly involved in the bug. Do not mix unrelated formatting or cleanup into a functional patch.

## Refactoring when similar implementations appear

This codebase (a large legacy fork) carries multiple overlapping implementations for the
same concern — e.g. several keystroke-binding helpers (`addCtrlAccel`, `addShortcut`,
`applyMicrosoftShortcuts` + `putCtrlAccel`/`putShortcut`), parallel menu/ribbon paths, and
legacy `com.projectlibre*` compatibility code. When you discover **two or more paths that
do the same job**, treat that as a latent bug source (a future change fixes only one path
and silently diverges) and consolidate rather than leave both:

- **Trigger (refactor, do not just patch):** you find duplicated responsibility directly
  involved in the task — duplicate helpers, copy-pasted logic, or parallel code paths that
  a single fix must keep in sync. Symptom-hiding edits (fixing only the path you happened
  to hit) are explicitly disallowed by this guide.
- **Confirm before consolidating:** search every caller (`rg -n "<symbol>" modules/*/src`)
  so you keep the API the rest of the code uses and delete only the dead/duplicated one.
  Some near-duplicates are intentional (different file formats, legacy vs. ribbon UI) —
  preserve those; only merge what is genuinely the same behavior.
- **Consolidate to one canonical implementation:** route all callers through the single
  helper, then delete the dead duplicate. Prefer extracting a small, side-effect-free core
  (e.g. `putCtrlAccel(InputMap, ActionMap, ...)`) that the legacy callers and new callers
  both delegate to.
- **Keep verification proportionate:** if the refactor is within one module, run that
  module's tests; if it touches a shared helper, run the callers' modules too. The
  completion list below still applies — do not claim the merge is safe without running.

### Keyboard-shortcut wiring rule (hardened after issue #47)

Shortcut keys MUST resolve through exactly **one** registration layer: the document
root-pane `InputMap`/`ActionMap` (`WHEN_IN_FOCUSED_WINDOW`), installed by
`GraphicManager.applyMicrosoftShortcuts(...)` via `putCtrlAccel`/`putShortcut`. No other
layer may bind the same key:

- No `KeyListener` on `SpreadSheet` for shortcut keys (was a source of double-firing:
  the listener and the root-pane input map both ran). Removed.
- No `InputMap.put(...)` on the component's `WHEN_FOCUSED` for keys already wired
  globally (`Ctrl+X/C/V/D`, `Delete`, `Insert`, `F2`, `F3`, `Ctrl+F`). Removed the
  `NodeListTransferHandler` and `SpreadSheet` component-level duplicate registrations.
- Every key maps to **one** action constant. Never register the same `KeyStroke` to two
  different constants (a past bug had `Ctrl+Delete` bound twice, last-write-wins, so the
  intended `ClearContents` was silently overwritten by `Delete`).
- Keep shortcut wiring **headless-safe**: `Toolkit.getMenuShortcutKeyMaskEx()` throws
  `HeadlessException`, so resolve the menu-shortcut mask through `menuShortcutMask()`
  (falls back to `InputEvent.CTRL_DOWN_MASK`). This keeps `applyMicrosoftShortcuts`
  unit-testable without a window.

If a new shortcut is needed, add it in `applyMicrosoftShortcuts` only — never re-open a
component-level `InputMap` for it.
- Treat file formats and serialized data as compatibility boundaries. Prefer backward-compatible reads, deterministic writes, clear failure behavior, and tests that cover save/reload or import/export round trips.
- Do not hand-edit generated output under `**/build/`, `build/releases/`, `docs/downloads/`, or `isolated-build/`.

## Efficient verification

Choose the narrowest command that exercises the change, then widen verification in proportion to risk.

```powershell
# One module
.\gradlew.bat :micrproject_core:test --console=plain
.\gradlew.bat :micrproject_application:test --console=plain
.\gradlew.bat :micrproject_exchange:test --console=plain
.\gradlew.bat :micrproject_ui:test --console=plain
.\gradlew.bat :micrproject_reports:test --console=plain

# One test class (replace module and class)
.\gradlew.bat :micrproject_ui:test --tests "com.example.MyTest" --console=plain

# Repository-wide verification
.\gradlew.bat clean build --console=plain
```

- Add or update a focused regression test for a bug fix when practical. Reproduce the failure before the fix when possible.
- `build` runs the module tests locally. CI currently uses `-x test`, so a successful CI-shaped compile is not evidence that tests passed.
- For Swing tests, keep them headless-compatible and perform Swing state changes/assertions on the EDT where required.
- For scheduling, Gantt, progress, or spreadsheet changes, cover zero/empty values, boundaries, intermediate and 100% progress, hierarchy/dependency changes, and save/reload when relevant.
- For import/export or collaboration changes, cover malformed or missing data, round trips, conflicts/concurrency where relevant, and preservation of existing user data.

## Running the desktop app

`build` does not guarantee that an existing runnable layout was refreshed. Before manual UI verification, regenerate `installDist`:

```powershell
.\gradlew.bat clean build installDist --console=plain
.\scripts\run_micrproject_clean.bat
```

For a faster logged incremental launch, use `scripts\run_projectlibre.ps1`; see `docs/build-and-run.md` for its options. The authoritative runnable layout is:

```text
modules\micrproject_ui\build\install\micrproject_ui
```

Never validate against an older `build/install` copy. For UI changes, record the scenario and sample file used, and verify state after redraw plus save/reload when applicable.

## Packaging and release work

- Run packaging only when the change touches distribution, runtime modules, dependencies, icons, licenses, file associations, or release behavior.
- Useful tasks are `stageAppDist`, `verifyPackagedFileImports`, `packageWindowsAppImage`, `packageWindowsMsi`, and `packageWindowsExe`.
- `jpackage` requires a full JDK; MSI/EXE creation also requires WiX. Outputs belong under `build/releases/v<version>/`.
- `docs/index.html` is the GitHub Pages entry point. Publish binaries as GitHub Release assets; `docs/downloads/` is scratch space and must not be committed.

## Completion checklist

- Review `git diff` and `git status`; confirm every changed file is intentional and no generated/sample side effect leaked in.
- Report what changed, the exact verification commands and outcomes, and any manual checks.
- Report skipped or failed verification and the remaining risk. Do not describe tests as passing if they were not run.
