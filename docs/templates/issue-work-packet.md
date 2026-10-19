# Issue #<number> — <short title>

State: <OPEN / IN PROGRESS / BLOCKED>
Issue: <URL>
Contract frozen at: <date, issue revision or link>
Baseline HEAD: `<sha>`
Baseline working tree: <clean / relevant pre-existing changes>

## Goal and scope

<One concise statement.>

Out of scope: <explicit exclusions>
Blockers: <none or precise dependency>

## Acceptance

| ID | Required result/evidence | State | Evidence (SHA + link) |
|---|---|---|---|
| <ID> | <observable criterion> | <open/pass> | <link or pending> |

## Current slice

**Next slice:** <exactly one bounded action>
**Done when:** <observable result and evidence>
**Then:** <next unresolved criterion, or integration/close>

## Remaining blockers

- <criterion/evidence still missing; use “none” when empty>

## Latest evidence

- Tests/build: <command, result, SHA, link>
- Physical GUI/manual: <scenario, fixture, environment/bounds, result or pending>
- CI: <required check and exact candidate SHA/link>

Close and report in the same work once every criterion, evidence item, and
required child is resolved within scope. Otherwise list only unmet criterion
IDs and the evidence still needed. For reused evidence, record its tested SHA
and the scoped-diff basis for reuse.

<about 100 lines maximum; older detailed evidence stays in linked history.>
