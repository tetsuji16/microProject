# Naming policy and compatibility boundaries

This repository intentionally distinguishes the lower-case internal identifier
from the user-visible product name. The policy below is the source of truth for
new code and release work.

| Surface | Canonical spelling | Compatibility rule |
| --- | --- | --- |
| Gradle root identity | `microproject` | Use the corrected lower-case identifier for project paths, caches, and scripts. |
| Gradle modules and JAR artifacts | `microproject_*` | Use the corrected identifier for all eight active modules. The module name is not product branding. |
| Java source packages | `com.microproject` | New code uses this namespace. `com.projectlibre1` is read-only deserialization compatibility in `SafeObjectInput`; do not add other legacy packages. |
| User-visible product and Windows packaging | `microProject` | Use this casing in application metadata, launchers, jpackage output, installer names, and UI text. |
| Native project files | `.mpo` (MPOF) | Preserve reads of files produced by earlier supported microProject releases. New writes may evolve only with an explicit versioned format contract; old application/API compatibility is not required. |
| POD project files | `.pod` | The serialized format is immutable. Preserve its exact wire structure while refactoring the implementation; verify reads/writes against fixed fixtures. |
| Collaboration sidecars | Existing lease/lock path | Preserve the process-lock identity until an explicit migration protocol is implemented. |
| User configuration | `.microproject/microproject.conf` or `microProject/microproject.conf` | Canonical configuration first; legacy directories and `projectlibre.conf` are read-only fallback. |
| Crash recovery | `%LOCALAPPDATA%/microProject/recovery` or `~/.microproject/recovery` | New installation data uses product-owned paths. If unresolved legacy metadata exists, use that store until it has been cleared, then switch on a later launch. |
| Repository and attribution identifiers | `ProjectLibre` | GitHub/repository identity, license/attribution material, and established internal compatibility identifiers may retain the historical name. |

## Enforcement

The root `verifyNamingConventions` Gradle task checks the active eight
`microproject_*` projects, the `microproject` root identity, the Java 25 baseline,
the canonical Windows packaging name/assets, and package declarations in active
source and test trees. `verifyArchitectureBoundaries` separately checks module
dependencies and the legacy namespace compatibility exception. Both checks are
part of the normal `check` lifecycle.

The check intentionally inspects package declarations rather than every text
occurrence. A string in a file-format discriminator, an old recovery directory,
or a deserialization alias is data compatibility, not a Java namespace leak.
Changing those names would make existing files, locks, recovery data, or update
artifacts impossible to find. MPO read compatibility and the immutable POD wire
format are data boundaries; unrelated internal implementation compatibility is
not required. Any permitted MPO format evolution requires a versioned contract
and fixtures proving that previously supported MPO files still load.

The naming gate also compares every configured project directory with the
canonical `modules/<microproject_*>` path and confirms that the directory exists.
This is an on-disk/worktree check, not just a settings-file spelling check, so a
partially renamed module cannot silently become an active project. The gate
keeps the active set exact while still allowing unrelated directories to be
present for migration or audit purposes.

## Change checklist

When adding a module, choose a new `microproject_*` name and update
`settings.gradle.kts`, the architecture allowlist, and this table in the same
change. When changing packaging, retain `microProject` for user-visible output
while leaving module JAR names untouched. When touching Java packages, use
`com.microproject`; compatibility readers may retain their narrowly scoped
legacy alias only where an existing persisted format requires it.

## Legacy inventory and retention policy (issue #529)

The former `micrproject` spelling was a typo. It is retired and must not be
reintroduced in Gradle names, module paths, JAR artifacts, scripts, or source
packages. `verifyNamingConventions` rejects a checkout that still contains an
active or stray `modules/micrproject_*` directory.

`modules/projectlibre_*` is the old module naming convention. Such directories
are not included by `settings.gradle.kts`, are not build inputs, and must not be
revived or used as a dependency. If a checkout still contains one, it is kept
as an unreferenced legacy/audit tree until an explicitly approved removal plan
is available; this issue does not authorize deleting it. The naming gate checks
that a legacy directory can never be the project directory of an active module.

The same distinction applies to old names found in active source text: a
`com.projectlibre*` Java package declaration is forbidden, while narrowly scoped
deserialization aliases, `.pod`/recovery/sidecar identifiers, repository
attribution, and established file-format keys remain compatibility data. These
exceptions are checked by `verifyArchitectureBoundaries` and are not permission
to introduce new legacy APIs or packages.


## Desktop construction boundary (beta.2)

`SessionFactory` constructs the supported `LocalSession` directly. The legacy model-scope boolean resolves to the same desktop backend for both values, matching the previous local-only configuration. Core metadata no longer owns session class names or UI print-service class names. `ExtendedPrintServiceFactory` constructs `MicroProjectPrintService` in the UI module. These runtime factories are not persisted POD descriptors or MPO schema fields. Model/exchange aliases required by existing files remain separate.
