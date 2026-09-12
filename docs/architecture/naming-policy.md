# Naming policy and compatibility boundaries

This repository intentionally contains two related spellings. They identify
different compatibility surfaces and must not be normalized by a global rename.
The policy below is the source of truth for new code and release work.

| Surface | Canonical spelling | Compatibility rule |
| --- | --- | --- |
| Gradle root identity | `micrproject` | Keep the existing build identity for project paths, caches, and scripts. |
| Gradle modules and JAR artifacts | `micrproject_*` | Keep all eight active module names stable. The module name is not product branding. |
| Java source packages | `com.microproject` | New code uses this namespace. `com.projectlibre1` is read-only deserialization compatibility in `SafeObjectInput`; do not add other legacy packages. |
| User-visible product and Windows packaging | `microProject` | Use this casing in application metadata, launchers, jpackage output, installer names, and UI text. |
| Native project files | `.mpo` (MPOF) | This is the fork's custom format and must not be renamed as part of branding cleanup. |
| Legacy project files | `.pod` | Preserve reads and writes required by existing ProjectLibre/OpenProj-compatible files. |
| Collaboration/recovery sidecars | `.projectlibre-sync.*`, legacy recovery roots | Preserve these paths and suffixes so existing locks, recovery files, and concurrent sessions remain discoverable. |
| Repository and attribution identifiers | `ProjectLibre` | GitHub/repository identity, license/attribution material, and established internal compatibility identifiers may retain the historical name. |

## Enforcement

The root `verifyNamingConventions` Gradle task checks the active eight
`micrproject_*` projects, the `micrproject` root identity, the Java 25 baseline,
the canonical Windows packaging name/assets, and package declarations in active
source and test trees. `verifyArchitectureBoundaries` separately checks module
dependencies and the legacy namespace compatibility exception. Both checks are
part of the normal `check` lifecycle.

The check intentionally inspects package declarations rather than every text
occurrence. A string in a file-format discriminator, an old recovery directory,
or a deserialization alias is data compatibility, not a Java namespace leak.
Changing those names would make existing files, locks, recovery data, or update
artifacts impossible to find. Any proposed change to one of those boundaries
requires a versioned migration and a compatibility test instead of a rename.

The naming gate also compares every configured project directory with the
canonical `modules/<micrproject_*>` path and confirms that the directory exists.
This is an on-disk/worktree check, not just a settings-file spelling check, so a
partially renamed module cannot silently become an active project. The gate
keeps the active set exact while still allowing unrelated directories to be
present for migration or audit purposes.

## Change checklist

When adding a module, choose a new `micrproject_*` name and update
`settings.gradle.kts`, the architecture allowlist, and this table in the same
change. When changing packaging, retain `microProject` for user-visible output
while leaving module JAR names untouched. When touching Java packages, use
`com.microproject`; compatibility readers may retain their narrowly scoped
legacy alias only where an existing persisted format requires it.

## Legacy inventory and retention policy (issue #529)

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
