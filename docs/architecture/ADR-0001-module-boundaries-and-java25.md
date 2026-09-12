# ADR-0001: Eight-module boundary and Java 25 baseline

- Status: Accepted
- Date: 2026-09-12

## Context

The repository has accumulated documentation that described an earlier
six-module layout and a Java 21 packaging baseline. The current Gradle build
uses eight `micrproject_*` subprojects, and the root build compiles against the
Java 25 API level. Documentation drift makes dependency-boundary reviews and
release troubleshooting unreliable.

## Decision

`settings.gradle.kts` is the source of truth for the eight production modules:

1. `micrproject_contrib`
2. `micrproject_core`
3. `micrproject_application`
4. `micrproject_ui`
5. `micrproject_exchange`
6. `micrproject_reports`
7. `micrproject_bootstrap`
8. `micrproject_ribbon`

The root `build.gradle.kts` applies a Java toolchain to every subproject and
sets `JavaCompile.options.release` to 25. Java 25 is therefore the minimum
supported compile/API baseline. CI and release workflows use Temurin JDK 25;
newer local JDKs may run Gradle, but code must remain valid for `--release 25`.

The reports and exchange modules remain independent adapters around the core
model. Their prohibited UI/application dependencies are checked by
`verifyArchitectureBoundaries`.

## Consequences

- Module additions or renames must update `settings.gradle.kts`, the README,
  and this ADR/architecture documentation together.
- A Java dependency that requires a newer API level is not acceptable without
  an explicit baseline decision and corresponding CI/toolchain update.
- The architecture check remains a lightweight source-boundary guard; it does
  not replace module tests or format round-trip verification.

## Verification

```powershell
.\gradlew.bat verifyArchitectureBoundaries --console=plain
```
