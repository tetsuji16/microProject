# microProject agent guide

## Instruction hierarchy

This is the authoritative repository instruction set. Read `docs/agent-workflows.md` before issue execution, Microsoft compatibility decisions, GUI/ribbon work, refactoring, verification, file-format or persistence changes, generated-output changes, desktop runs, or packaging/release. `TEST_PLAN.md` records the regression matrix; do not add copy-pasted cases instead of consolidating duplicate behavior. GUI requirements remain in `docs/gui-quality-gate.md` and `docs/gui-recovery-sequence.md`. Before writing, splitting, labelling, or closing an issue, read `docs/issue-authoring.md`; the verification commands an issue must name are listed in `TEST_PLAN.md` section 1.3.

## Goal and scope

- Maintain this ProjectLibre desktop fork without regressing existing project files, scheduling behavior, or packaging. Windows is the reference desktop target; Linux x64 and macOS Apple Silicon packages are also produced, with platform-specific behavior verified separately.
- The compatibility objective is the standalone Microsoft Project desktop application. Project Server, Project Web App (PWA), Project Online, and Microsoft Planner/other cloud-service authentication, synchronization, publishing, and service APIs are outside this product goal; do not add connected-service imports, clients, or dependencies. Local project files, linked/master projects, shared resources, offline collaboration, and local automation/extensions remain distinct desktop coverage. VBA/macros, COM automation/add-ins, and local Office add-ins remain unresolved goal rows unless the user explicitly narrows them later.
- MPO (`.mpo`) is microProject's authoritative native project format and the default save format. Preserve reading files from earlier supported MPO versions. MPP (`.mpp`) is an external Microsoft Project interoperability format, never the native/default format; keep its read and write interoperability gates explicit. POD is a frozen legacy import format; do not alter its wire contract or add new POD write behavior.
- The primary agent owns repository-wide planning, specification interpretation,
  integration, and completion decisions, and remains responsible for reviewing
  and integrating repository-wide changes.
- Judge Microsoft Project Desktop (MSP) compatibility against the applicable
  Microsoft-published specification or support documentation.  An installed
  MSP application is supplementary evidence only and is not required for a
  compatibility decision or issue closure.

For Microsoft Project compatibility decisions, read `docs/agent-workflows.md` before defining behavior or claiming compatibility evidence.

## Module map

- `modules/microproject_core`: scheduling engine, data model, shared utilities, and core configuration.
- `modules/microproject_application`: open/save workflows, document coordination, and file policies.
- `modules/microproject_ui`: Swing UI, spreadsheet and Gantt views, rendering, menus, and startup.
- `modules/microproject_exchange`: MPP/POD/XML/XLSX import/export and collaboration metadata integration.
- `modules/microproject_reports`: report code and templates.
- `modules/microproject_contrib`: bundled compatibility and third-party code; avoid growing packaged dependencies unnecessarily.
- `modules/microproject_ribbon`: shared ribbon model, command IDs/results, icons, and theme abstractions.
- `modules/microproject_bootstrap`: startup and update bootstrap.
- `packaging`: authoritative icons, licenses, file associations, and Windows release inputs.
- `samples`: manual-verification fixtures. Application runs can modify files here, so inspect changes and do not commit incidental rewrites.

Respect the dependency direction expressed in the Gradle files. Put workflow coordination in `microproject_application`, reusable domain behavior in `microproject_core`, format conversion in `microproject_exchange`, and view-only behavior in `microproject_ui`.


## Dependency and namespace hygiene (eliminate ProjectLibre coupling)

The fork's canonical namespace is `com.microproject`. A small set of explicit
legacy aliases remains at the POD deserialization/importer boundary for files
written by older releases (see `SafeObjectInput`, `DefaultFileImporterProvider`,
and issue #154); do not widen those aliases or add old-package production code.
Anything that still carries the `projectLibre` / `ProjectLibre`
/ `com.projectlibre` name is unfinished rename work and must not be widened.

- **Module names**: every production module under `modules/` is `microproject_*`
  and is explicitly included by `settings.gradle.kts`. Do not introduce or
  revive `projectlibre_*` module directories.
- **Packages**: source under `com.projectlibre` / `com.projectlibre1` is a leftover
  from the rename. New code always uses `com.microproject`. Do not add new
  `com.projectlibre*` packages or imports from `microproject_*` modules. Keep
  old class names confined to documented compatibility maps used when reading
  persisted POD options or supported serialized data.
- **Brand/identifier names**: method/constant names such as `isProjectLibreFile`,
  `LOCAL_PROJECT_IMPORTER`, `ProjectLibreShell` are acceptable only as established
  internal identifiers for the *file format*; do not introduce new `ProjectLibre`-prefixed
  public types or APIs. Prefer neutral names (`isNativeFile`, `LOCAL_IMPORTER`, …) for
  new code.
- **Third-party / contrib**: keep `microproject_contrib` for bundled compatibility code
  and avoid growing packaged dependencies. External libraries must not pull the old
  ProjectLibre namespace into `microproject_*` runtime modules.
- **License headers**: files should carry the MIT header with
  `Copyright (c) 2026 microProject`. Old CPAL/`Copyright (c) 2012-2019 ProjectLibre Inc.`
  headers have been removed; do not reintroduce them in new or moved files.


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


## Working approach

- Preserve all existing user changes. Inspect `git status` before and after the task; never discard changes you did not create.
- Trace the relevant callers, state transitions, persistence, and reload paths before editing. Search by symbol and behavior with `rg` / `rg --files`.
- Choose verification proportionate to risk. Report exact commands, results, and any remaining uncertainty; do not claim unrun checks passed.
- Never push directly to `master`. Create a work branch from `master` and open a pull request; the required checks are `build` and `GUI acceptance`. One pull request addresses one issue and says `Closes #N` (or `Refs #N` when part of the issue remains).
- An issue body is the specification for its implementer. Write issues so that someone who has not seen the surrounding discussion can finish them: required sections, checkable completion criteria, and labels are defined in `docs/issue-authoring.md`. If an issue's steps do not match the current code, comment on the issue instead of guessing.
- Do not name the tools or models used for the work in commits, pull requests, issues, or comments.
