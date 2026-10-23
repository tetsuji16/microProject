# microProject 0.0.24 stable release

microProject 0.0.24 is a stable Windows desktop release for the implemented project scheduling workflows described below. “Stable” describes the tested release scope; it does not claim complete compatibility with Microsoft Project desktop. Complete compatibility remains an active product goal.

## Highlights

- Edit task names, durations, dates, progress, hierarchy, and dependencies in the project views. The release acceptance suite checks table/Gantt coordination, Undo/Redo, and persistence for the core journeys.
- Apply and clear critical chain settings, inspect critical-chain status, and use Undo/Redo. MPO save/reload is covered by release tests; earlier-version coverage remains tracked in the compatibility roadmap.
- Import supported Microsoft Project MPP files and frozen legacy POD files, and save native MPO project files. MPO backward-read tests cover supported earlier versions. MPP write interoperability remains a separate compatibility gate; POD's serialized contract stays frozen.
- Use responsive ribbon commands with synchronized popup state, edit Gantt dependencies by double-clicking their link lines, and reach calendar controls on constrained displays.
- Use the Windows MSI installer or the portable app-image ZIP. Both include the runtime required to launch the desktop application.

VBA and macros, the Microsoft Project object model, COM automation and add-ins, VSTO, and local Office add-ins remain unresolved roadmap coverage and are not claimed by this release. Other Microsoft Project compatibility gaps remain tracked in the roadmap; this stable release does not mark those goals complete. The exact four-part package version and source commit are shown in the installer metadata and GitHub release tag.

## Verification

This release is published only after the exact candidate passes all module tests, architecture and naming checks, the full Windows GUI locale/scale suite, fixed MPP/POD import checks, Windows package creation, packaged application launch, and SHA-256 generation. The GitHub Release assets contain the MSI, portable ZIP, this note, and `SHA256SUMS`.
