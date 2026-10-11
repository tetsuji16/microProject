# microProject 0.0.24

microProject 0.0.24 is a Windows desktop release for building and maintaining project schedules with a task table and Gantt chart.

## Highlights

- Edit task names, durations, dates, progress, hierarchy, and dependencies in the project views. The release acceptance suite checks table/Gantt coordination, Undo/Redo, and persistence for the core journeys.
- Apply and clear critical chain settings, inspect critical-chain status, and use Undo/Redo. MPO save/reload and existing MPO compatibility are covered by the release test gates.
- Open supported Microsoft Project MPP and legacy POD files, and save native MPO project files. POD serialized data remains compatible with the established format.
- Use the Windows MSI installer or the portable app-image ZIP. Both include the runtime required to launch the desktop application.

The exact four-part package version and source commit are shown in the installer metadata and GitHub release tag. This release does not claim complete Microsoft Project feature parity; consult the issue tracker for remaining compatibility work.

## Verification

This release is published only after the exact candidate passes all module tests, architecture and naming checks, the full Windows GUI locale/scale suite, fixed MPP/POD import checks, Windows package creation, packaged application launch, and SHA-256 generation. The GitHub Release assets contain the MSI, portable ZIP, this note, and `SHA256SUMS`.
