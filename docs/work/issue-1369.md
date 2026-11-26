# Issue #1369 — Remove the Flamingo dependency

**State:** Implementation complete, local verification green, CI pending
**Contract frozen at:** 2026-11-26
**Baseline HEAD:** `5cfbe1053` (origin/master)
**Worktree:** `C:/Users/tetsu/Documents/codex/mp-1369`
**Branch:** `issue/1369-remove-flamingo`

## Acceptance criteria (from issue)

| ID | Required result/evidence | Status | Evidence |
|---|---|---|---|
| AC1 | `rg -n "pushingpixels\.flamingo\|flamengo" modules gradle build.gradle.kts` is empty | done | `git grep -E` over the same paths: exit 1, no match |
| AC2 | `rg -n "AbstractCommandButton\|JCommandToggleButton\|RichTooltip" modules` is empty | done | `git grep -E` over `modules`: exit 1, no match |
| AC3 | `installDist` lib has no `flamengo-5.0.jar` or `trident-1.3.jar` | done | the lib directory lists only `radiance-trident-4.5.0.jar` |
| AC4 | `verifyDependencyAllowlist` passes | done | BUILD SUCCESSFUL |
| AC5 | `:microproject_ui:test` passes | done | 1,985 tests, 4 skipped, 0 failures |
| AC6 | PR `build` and `GUI acceptance` pass | pending | CI on the pushed head |
| AC7 | The removed `verification-metadata.xml` components are listed in the PR | done | see the list below |

## Implementation

- `MenuManager`: dropped the three Flamingo imports, the `AbstractCommandButton` and
  `JCommandToggleButton` branches in `setActionVisible` / `setActionSelected` / `setText`,
  and the unused `richTooltip(String)` helper. `AbstractButton` stays the only branch.
- `AssignmentDialogGuiAcceptanceTest#buttonByText`: dropped the Flamingo branch.
- `modules/microproject_ui/build.gradle.kts` and `modules/microproject_contrib/build.gradle.kts`:
  removed `implementation(libs.flamingo)`.
- `gradle/libs.versions.toml`: removed the `flamingo` version and library entry.
- Root `build.gradle.kts`: dropped `org.pushingpixels` from the allowlist (kept
  `org.pushing-pixels` for Radiance) and replaced the three-line Flamingo comment with
  `// Keep the obsolete com.jgoodies:forms artifact out; jgoodies-forms 1.9.0 is the only provider.`
- `docs/agent-workflows.md` and ADR-0002: Flamingo is recorded as no longer a dependency.
- `packaging/licenses/` is untouched; the Radiance section is handled by #1370.

## Removed `verification-metadata.xml` components

Selected by diffing `dependencies` for all eight modules before and after the change; every
entry below disappeared from every module configuration.

```text
batik:batik-awt-util:1.6
batik:batik-bridge:1.6
batik:batik-ext:1.6
batik:batik-extension:1.6
batik:batik-gvt:1.6
batik:batik-script:1.6
batik:batik-swing:1.6
batik:batik-transcoder:1.6
batik:batik-util:1.6
ch.ethz.ganymed:ganymed-ssh2:build210
com.google.android:android:2.3.3
com.svnkit:svnkit:1.1.0
org.apache.httpcomponents:httpclient:4.0.1
org.apache.httpcomponents:httpcore:4.0.1
org.eclipse.swt.gtk.linux:x86:3.3.0-v3346
org.eclipse.swt.gtk.linux:x86_64:3.3.0-v3346
org.json:json:20080701
org.khronos:opengl-api:gl1.1-android-2.1_r1
org.pushingpixels:flamengo:5.0
org.pushingpixels:trident:1.3
xerces:xmlParserAPIs:2.6.2
xpp3:xpp3:1.1.4c
```

`commons-codec:commons-codec:1.3` also disappeared but was not pinned, so nothing was removed
for it. `jgoodies-binding` and `jgoodies-common` did not disappear, so they stay.

## Verification commands run

```text
.\gradlew.bat :microproject_ui:compileJava :microproject_ui:compileGuiTestJava verifyDependencyAllowlist --console=plain
.\gradlew.bat build installDist --console=plain
git diff --check
```

All exited 0. The before/after dependency dumps came from
`./gradlew.bat :<module>:dependencies` for all eight modules.

## Next step

Push the branch, open the PR with every completion criterion and the component list, wait for
`build` and `GUI acceptance`, then merge.
