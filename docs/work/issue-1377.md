# Issue #1377 — UpdateChecker: read the release check and installer with HttpClient

**State:** Implementation complete, local verification green, CI pending
**Contract frozen at:** 2026-11-26
**Baseline HEAD:** `5cfbe1053` (origin/master)
**Worktree:** `C:/Users/tetsu/Documents/codex/mp-1377`
**Branch:** `issue/1377-updatechecker-httpclient`

## Acceptance criteria (from issue)

| ID | Required result/evidence | Status | Evidence |
|---|---|---|---|
| AC1 | `rg -n "HttpURLConnection\|openConnection\(" modules/microproject_ui/src/main` returns 0 matches | done | `git grep -E` over that tree: 0 files |
| AC2 | Existing `UpdateCheckerTest` tests pass with unchanged expectations | done | `UpdateCheckerTest` 26 tests, 0 failures |
| AC3 | Response over 1 MiB throws `IOException` (test) | done | `sharedTransportRejectsOversizedResponses` |
| AC4 | Untrusted redirect throws `IOException`, no `.part`/`.msi` left (test) | done | `redirectToUntrustedHostIsNotFollowed` (asserts an empty staging directory) |
| AC5 | Six redirects throw `IOException` (test) | done | `tooManyRedirectsFailWithoutLeavingArtifacts` |
| AC6 | A failed download leaves no `.part` and no `.msi` (test) | done | `failedDownloadRemovesBothPartialAndPublishedArtifacts` |
| AC7 | 50 ms deadline on a slow release server throws `IOException` | done | `sharedTransportHonorsReadTimeout` |
| AC8 | `:microproject_ui:test` passes | done | 1,986 tests, 4 skipped, 0 failures |
| AC9 | `test_source_inventory.py --check` passes | done | exit 0 |
| AC10 | `git diff --check` is empty | done | exit 0 |
| AC11 | PR `build` and `GUI acceptance` pass | pending | CI on the pushed head |

## Implementation

- `modules/microproject_ui/src/main/java/com/microproject/ui/dialog/UpdateChecker.java`
  - one shared `HttpClient` (5 s connect timeout, `Redirect.NEVER`), a `send(...)` helper that
    applies a deadline, cancels the future on timeout and maps the failure to
    `HttpTimeoutException` / `IOException` / `InterruptedIOException`
  - `fetchLatestReleaseJson(URI, int)` uses `BodyHandlers.limiting(ofByteArray(), 1 MiB)`
  - `downloadInstaller(...)` follows redirects hop by hop with the per-hop `trustedUrl` check,
    writes only a 200 body to the `.part` file and returns `HttpResponse<Path>`
  - `openReleaseConnection`, `readReleaseJson`, `openInstallerConnection` and `copyDownload` removed
- `modules/microproject_ui/src/test/java/com/microproject/ui/dialog/UpdateCheckerTest.java`
  - `endpoint(...)` returns `URI`; expectations unchanged
  - added the six-redirect case and the empty-staging assertion for an untrusted redirect

## Behavior difference to report in the PR

The installer download no longer has a 30 s idle read timeout. Each hop has a 10-minute
total deadline (`INSTALLER_DOWNLOAD_DEADLINE`). Redirect trust checks and the SHA-256
verification are unchanged.

## Verification commands run

```text
.\gradlew.bat :microproject_ui:test --tests "com.microproject.ui.dialog.UpdateCheckerTest" --console=plain
.\gradlew.bat :microproject_ui:test --console=plain
python scripts/test_source_inventory.py --check --output build/audit/test-source-inventory.json
git diff --check
```

All four exited 0.

## Next step

Push the branch, open the PR with every completion criterion, wait for `build` and
`GUI acceptance`, then merge.
