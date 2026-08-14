# Provenance and license audit

The Phase 0/1 audit separates OpenProj origin, ProjectLibre additions, fork-original work, and third-party code. It is deliberately conservative: an unknown or mixed file is recorded as `REVIEW` until a human compares the OpenProj source, ProjectLibre history, and current implementation at hunk level.

The planned independent product name is **microProject**. This name applies to the independent product brand only; provenance labels such as OpenProj and ProjectLibre remain in the audit records because they identify source history and licensing obligations.

Run from the repository root:

```powershell
python scripts/license_audit.py
```

Outputs:

- `license-provenance.csv`: one row per tracked file under `modules/**` and `packaging/**`.
- `license-provenance-summary.md`: disposition counts and required follow-up.

The script uses the repository's initial OpenProj-based commit (`d2fa3c20a`) as a reproducible comparison point. Before making a licensing decision, obtain the official OpenProj 1.4 source archive and record its SHA-256 in the script or an equivalent signed audit record. The generated `KEEP_OPENPROJ` and `REVIEW` values are technical evidence only, not a legal conclusion.
