# Desktop backend and Undo notification boundary

2026-10-10 follow-up to beta.2; this source change does not replace the published beta.2 assets.

## Supported desktop routes

The shipped backend is `LocalSession`. Startup initializes a local default user without server authentication. Menu/ribbon Open and welcome Open use the existing local file chooser. Insert Project uses the existing local subproject file chooser even when an imported project retains nonlocal provenance; `Project.isLocal()` is not a selector for a backend installed in this application.

The unused login and server project-list dialogs, ID-based startup path, server configuration download/version check, and unused server URL constructor state are removed. `--serverUrl`, `--projectId`, and `--credentials` are rejected before option logging, including flags without values. Plain filenames and `--fileNames` continue to select local files. Server startup is an unsupported contract, not a silently ignored option.

| Command | Preconditions and route | Result / regression coverage |
| --- | --- | --- |
| Startup file arguments | Supported local filenames | Existing sequential local open and recovery behavior retained; option parsing covered |
| Open | Global menu/ribbon or welcome | One local chooser route, independent of legacy standalone flag |
| Insert Project | Active project and valid local subproject | Existing validation, insertion, Undo and persistence pipeline retained |

## Core edit notifications

Core services and node models now publish edits through `EditSupport` and `EditListener`, not Swing `UndoableEditSupport` and `UndoableEditEvent`. Listeners receive the edit directly. Registration-order notification ensures the history controller records an edit before the document refreshes Undo/Redo button state. Listener snapshots allow registration changes during a callback without changing the current dispatch. Nested updates publish a single completed compound edit; undo runs child edits in reverse order and redo in forward order.

This is a notification boundary migration, not a Swing-free core. `UndoableEdit`, `CompoundEdit`, and `UndoManager` remain behind it. The exact core presentation reference inventory falls from 213 to 203 entries; all 14 direct Undo event/support references are removed. Migrated edit-type references are explicitly tracked by the architecture gate.

## Persistence and remaining work

No POD serialized field layout, identifiers, version markers or wire details change. Project's undo controller remains transient and node-model classes are not serialized. Existing POD fixed fixtures, MPO backward reads and supported external formats remain the compatibility gates.

Retain server-named DTOs/providers and the nonlocal POD resource adapter until their actual format/model contracts are separated. Do not delete `Project.isLocal()`, collaboration lock identity, field metadata or serialization aliases based on names. Remaining priorities are the edit/history type boundary, tree-model events and Swing tree nodes, and Project's presentation settings. Each needs a separate caller and fixed-fixture audit.

## Validation

JDK 25: `bash ./gradlew clean build installDist verifyArchitectureBoundaries verifyPackagedFileImports --console=plain` succeeded (1m54s). All eight modules: 2,226 tests, zero failures/errors, one Windows-only skip. Architecture/naming detector and its violating fixtures passed. Packaged runtime imported fixed MPP and POD samples with 145 tasks each. `python -m unittest discover -s scripts/tests -v`: eight tests passed. `git diff --check` passed. GUI acceptance sources are compiled separately; physical Windows GUI verification is recorded with the PR CI result.

New tests cover nested compound undo/redo, listener ordering, retired startup rejection and local file argument preservation. The Open action regression verifies it reaches the local chooser even with the legacy standalone flag false; existing subproject, scheduling and Undo tests retain their contracts.

The repository references a local `microproject-gui-implementation` skill which is absent from the checkout and available skill catalog. This change uses the checked-in `docs/gui-quality-gate.md`; it preserves supported physical file routes and adds no layout or interaction design.
