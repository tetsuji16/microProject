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

- `modules/projectlibre_core`: scheduling engine, data model, shared utilities, and core configuration.
- `modules/projectlibre_application`: open/save workflows, document coordination, and file policies.
- `modules/projectlibre_ui`: Swing UI, spreadsheet and Gantt views, rendering, menus, and startup.
- `modules/projectlibre_exchange`: MPP/POD/XML/XLSX import/export and collaboration metadata integration.
- `modules/projectlibre_reports`: report code and templates.
- `modules/projectlibre_contrib`: bundled compatibility and third-party code; avoid growing packaged dependencies unnecessarily.
- `packaging`: authoritative icons, licenses, file associations, and Windows release inputs.
- `samples`: manual-verification fixtures. Application runs can modify files here, so inspect changes and do not commit incidental rewrites.

Respect the dependency direction expressed in the Gradle files. Put workflow coordination in `projectlibre_application`, reusable domain behavior in `projectlibre_core`, format conversion in `projectlibre_exchange`, and view-only behavior in `projectlibre_ui`.

## Implementation conventions

- The project requires a full JDK 25 or newer and compiles with Java release 25. Use the checked-in Gradle Wrapper (`.\gradlew.bat` on Windows).
- Follow `.editorconfig`: UTF-8, CRLF, final newline, and tabs by default; YAML uses two spaces and properties files use four spaces.
- Follow nearby Java style and existing package conventions; both `com.projectlibre1` and compatibility namespaces exist, so do not rename packages opportunistically.
- Keep UI work Swing-safe. Preserve EDT boundaries, model/view index conversions, selection state, and repaint/revalidation behavior.
- Avoid broad refactors unless they remove duplicated responsibility directly involved in the bug. Do not mix unrelated formatting or cleanup into a functional patch.
- Treat file formats and serialized data as compatibility boundaries. Prefer backward-compatible reads, deterministic writes, clear failure behavior, and tests that cover save/reload or import/export round trips.
- Do not hand-edit generated output under `**/build/`, `build/releases/`, `docs/downloads/`, or `isolated-build/`.

## Efficient verification

Choose the narrowest command that exercises the change, then widen verification in proportion to risk.

```powershell
# One module
.\gradlew.bat :projectlibre_core:test --console=plain
.\gradlew.bat :projectlibre_application:test --console=plain
.\gradlew.bat :projectlibre_exchange:test --console=plain
.\gradlew.bat :projectlibre_ui:test --console=plain
.\gradlew.bat :projectlibre_reports:test --console=plain

# One test class (replace module and class)
.\gradlew.bat :projectlibre_ui:test --tests "com.example.MyTest" --console=plain

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
.\scripts\run_projectlibre_clean.bat
```

For a faster logged incremental launch, use `scripts\run_projectlibre.ps1`; see `docs/build-and-run.md` for its options. The authoritative runnable layout is:

```text
modules\projectlibre_ui\build\install\projectlibre_ui
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
