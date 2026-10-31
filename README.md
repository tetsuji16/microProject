# microProject

**microProject** is a desktop project planning application for Windows, Linux,
and macOS. It brings the task outline, schedule, dependencies, resources,
progress, and Gantt chart together in one workspace, with an Office-style
ribbon and keyboard-driven editing.

**microProject** は、タスク階層・日程・依存関係・リソース・進捗をガント
チャートと同じ画面で扱うデスクトップ計画アプリです（[日本語の概要](#日本語)）。

[Download the latest release](https://github.com/tetsuji16/microProject/releases/latest) ·
[Documentation and user guide](https://o-server.main.jp/micro-project/) ·
[Compatibility roadmap](docs/msp-compatibility-roadmap.md)

![microProject commercial construction project plan in the Gantt view](docs/images/demo_gannt.png)

*A commercial construction plan: the WBS task outline on the left, linked tasks,
milestones, and parallel work streams on the shared Gantt timeline on the right.*

## Features

- **Task table and Gantt in one view** — edit a WBS outline in the spreadsheet
  and see durations, links, milestones, and progress on the timeline.
- **Scheduling** — dependencies, calendars, constraints, baselines, and
  resource assignments with automatic rescheduling.
- **Critical Chain (CCPM)** — preview and apply resource-aware leveling, view
  the critical-chain network, and track project and feeding buffer consumption.
- **Office-style desktop UI** — a ribbon modelled on current Microsoft 365
  applications, Microsoft Project–style shortcut keys, Undo/Redo, printing, and
  PDF output.
- **Open file formats** — the native `.mpo` format, with import of Microsoft
  Project `.mpp` and MPX files and exchange through Project XML and XLSX.
- **Self-contained packages** — the Java runtime is bundled; no separate Java
  installation is needed.

## Download

| Platform | Package | Link |
|---|---|---|
| Windows | MSI installer (recommended) | [microProject-latest.msi](https://github.com/tetsuji16/microProject/releases/latest/download/microProject-latest.msi) |
| Windows | Portable ZIP — unzip and run `microProject.exe` | [microProject-latest-app-image.zip](https://github.com/tetsuji16/microProject/releases/latest/download/microProject-latest-app-image.zip) |
| Linux x64 | App-image ZIP (published with major updates) | [Releases](https://github.com/tetsuji16/microProject/releases) |
| macOS Apple Silicon | App-image ZIP (published with major updates) | [Releases](https://github.com/tetsuji16/microProject/releases) |

Windows is the reference platform. Linux and macOS packages are built from the
same source, and platform-specific behavior is verified separately. The
[documentation site](https://o-server.main.jp/micro-project/) always points at
the newest stable package for each platform.

## File formats

| Format | Role |
|---|---|
| `.mpo` (MPO) | Native format and default save format. Newer releases keep reading files from earlier supported releases. |
| `.mpp` | Microsoft Project files — import only. Writing MPP is out of scope; save as `.mpo` instead. |
| Project XML, XLSX | External interchange formats — open and save. |
| MPX | Older Microsoft Project exchange format — import only. |
| `.pod` | ProjectLibre's standard format — opens and saves, but CCPM settings are not stored; use `.mpo` to keep them. |

## Microsoft Project compatibility

microProject's goal is compatibility with the standalone **Microsoft Project
desktop** application: scheduling semantics, views, commands, keyboard and
mouse interaction, and project files. **That goal is not yet achieved**; the
milestones below are tracked publicly.

| Gate | Scope | Status |
|---|---|---|
| M0 | Target and complete coverage inventory | Open — [#792](https://github.com/tetsuji16/microProject/issues/792) |
| M1 | Current Office-style ribbon visuals | Open — [#793](https://github.com/tetsuji16/microProject/issues/793) |
| M2 | Offline desktop workflow parity | Open — [#794](https://github.com/tetsuji16/microProject/issues/794) |
| M3 | MPP reading and other file interoperability (MPP writing out of scope) | Open — [#796](https://github.com/tetsuji16/microProject/issues/796) |
| M4 | Microsoft Project-specific automation and extensions | Excluded by product scope — [#797](https://github.com/tetsuji16/microProject/issues/797) |
| M5 | Release compatibility acceptance | Open — [#795](https://github.com/tetsuji16/microProject/issues/795) |

Out of scope: Project Server, Project Web App, Project Online, Microsoft
Planner and other cloud sign-in or synchronization; VBA/macros, COM, VSTO, and
Microsoft Office add-ins. Local files, linked/master projects, shared
resources, offline collaboration, and microProject's own automation and
extensions remain in scope. See the [compatibility goal](docs/msp-compatibility-goal.md),
the [roadmap](docs/msp-compatibility-roadmap.md), and the
[ribbon command inventory](docs/MSP_STANDARD_COMMAND_INVENTORY.md).

## Build from source

Requirements:

- Windows, Linux, or macOS with a full JDK **25 or newer** that includes
  `jpackage`. Every module compiles with `--release 25`. If `JAVA_HOME` is not
  set, the Windows release tasks fall back to `C:\Program Files\Java\jdk-25`.
- The checked-in Gradle Wrapper (no separate Gradle install).
- WiX Toolset on `PATH` for the Windows MSI. A per-user WiX install under
  `%LOCALAPPDATA%\Programs\WiX Toolset v7.0\bin` is picked up automatically.

```powershell
.\gradlew.bat build          # compile and test all modules
.\gradlew.bat stageAppDist   # create the runnable desktop layout
.\scripts\run_microproject_clean.bat   # clean build and launch on Windows
```

The runnable layout is `modules\microproject_ui\build\install\microproject_ui`.
For manual UI checks always use a freshly generated layout, never an older
`build/install` copy; see [docs/build-and-run.md](docs/build-and-run.md).

Packaging tasks (outputs go to `build\releases\v<version>\`):

| Task | Output |
|---|---|
| `packageWindowsMsi` | Windows MSI with English and Japanese installer UI |
| `packageWindowsExe` | Windows self-contained EXE installer |
| `packageWindowsAppImage` / `packageWindowsZip` | Portable app-image and ZIP |

Platform packages must be built on their target OS. Public binaries are
GitHub Release assets; `docs/downloads/` is local scratch space and is never
committed.

## Repository layout

The Gradle build has eight production modules, listed authoritatively in
[`settings.gradle.kts`](settings.gradle.kts). Module and JAR identifiers use the
lower-case `microproject` spelling; see the
[naming policy](docs/architecture/naming-policy.md).

| Path | Contents |
|---|---|
| `modules/microproject_core` | Scheduling engine, data model, collaboration logic, configuration |
| `modules/microproject_application` | Open/save workflows, file policies, document coordination |
| `modules/microproject_ui` | Swing UI, spreadsheet and Gantt views, menus, startup |
| `modules/microproject_exchange` | MPO, MPP, XML, XLSX, and other import/export |
| `modules/microproject_reports` | Reports and templates |
| `modules/microproject_ribbon` | Reusable Swing ribbon API and extension SPI |
| `modules/microproject_bootstrap` | Update/bootstrap entry point |
| `modules/microproject_contrib` | Bundled third-party compatibility code |
| `packaging` | Icons, licenses, file associations, installer resources |
| `samples` | Sample projects for screenshots and manual checks |
| `website` | Source of the [documentation site](https://o-server.main.jp/micro-project/) |

The reports and exchange modules must not depend on the desktop UI or the
application workflow; `.\gradlew.bat verifyIndependentBoundaries` enforces this.
CI builds and tests on Linux, Windows, and macOS with Temurin JDK 25
([build.yml](.github/workflows/build.yml)), and
Windows GUI acceptance runs in [gui-acceptance.yml](.github/workflows/gui-acceptance.yml).

## Contributing and releases

- Contributor and agent rules: [AGENTS.md](AGENTS.md)
- Regression matrix: [TEST_PLAN.md](TEST_PLAN.md) and the
  [GUI quality gate](docs/gui-quality-gate.md)
- Stable release procedure: [docs/releasing.md](docs/releasing.md);
  prerelease route: [docs/trial-release-plan.md](docs/trial-release-plan.md)
- Documentation publishing: [docs/site-publishing.md](docs/site-publishing.md)

The release workflow packages the Windows MSI and portable ZIP for every stable
release, and Linux x64 and macOS Apple Silicon ZIPs for major updates. A stable
release requires the full GUI gate. Beta releases are prereleases and never
replace the stable `latest` assets or the update feed.

## Development status

microProject is being built through clean-room implementations based on
documented Microsoft Project behavior and independent fixtures. That work is
still in progress, so parts of the source tree still mix legacy, compatibility,
and newly implemented code, and internal boundaries may change.

## License

License texts and third-party notices are kept in
[`packaging/licenses`](packaging/licenses). New and moved source files carry
the MIT header `Copyright (c) 2026 microProject`.

## 日本語

microProject は、タスク階層、日程、依存関係、リソース、進捗を Gantt チャートと
同じ画面で管理するデスクトップ計画アプリです。Microsoft 365 風のリボン、
Microsoft Project 形式のショートカット、CCPM（クリティカルチェーン）による
バッファ管理、印刷・PDF 出力を備え、Java ランタイム同梱のパッケージで配布します。

- **ダウンロード**: [最新リリース](https://github.com/tetsuji16/microProject/releases/latest)
  （Windows MSI / Portable ZIP。Linux x64・macOS Apple Silicon は大きな更新時に提供）
- **使い方**: [オンラインヘルプ・ダウンロード案内](https://o-server.main.jp/micro-project/)
- **ファイル形式**: `.mpo`（MPO）が正式なネイティブ形式かつ標準の保存形式です。
  Microsoft Project の `.mpp` と MPX は読込のみ対応（MPP 書出しは対象外）。
  Project XML と XLSX は読込・保存の両方に対応します。`.pod` は ProjectLibre の標準形式で、開いて保存できますが CCPM 設定は保存されません。
- **互換性**: スタンドアロンの Microsoft Project デスクトップとの完全互換を目標に
  していますが、まだ達成していません。Project Server、PWA、Project Online、
  Microsoft Planner のクラウド連携と、VBA / マクロ、COM、VSTO、Office アドインは
  対象外です。
- **開発状況**: 公開仕様と独自のテストデータに基づくクリーンルーム実装を
  進めています。
