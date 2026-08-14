# Phase 1 module review

This review records the technical boundary work completed before Phase 2. It
does not make a legal conclusion from package names or current license headers.

| Module | Review result | Evidence | Remaining work |
|---|---|---|---|
| `projectlibre_core` | REVIEW | provenance ledger; compatibility identifiers retained | hunk-level OpenProj 1.4 comparison for scheduling/persistence |
| `projectlibre_exchange` | boundary verified | `verifyIndependentBoundaries`; exchange tests and round-trip fixtures | classify individual ProjectLibre format extensions |
| `projectlibre_reports` | boundary verified | `verifyIndependentBoundaries`; reports tests | compare JRXML and adapters with OpenProj template history |
| `projectlibre_ui` | selected unused/service deltas removed | compile/test; runtime launch; UI link audit | review remaining mixed UI hunks and translations |
| `projectlibre_contrib` | third-party candidates retained | third-party notices and dependency metadata | reconcile bundled sources with upstream licenses |
| `projectlibre_application` | fork workflow layer retained | no OpenProj module counterpart; application tests | document authorship/evidence for each workflow class |
| packaging/docs/samples | branding slice completed | microProject assets, URLs, distribution names; clean installDist launch | review historical assets and license pages with legal owner |

The OpenProj 1.4 source archive is now recorded at
`docs/legal/openproj-1.4-src.tar.gz` with SHA-256
`20071b090d841388860049ce49724e2773b8cec250d76e74264c71adf2a79ac6`.

The unresolved rows remain `REVIEW`; they are not treated as ProjectLibre
differences and are not changed in Phase 2 without hunk-level evidence.
