# ADR-0002: Keep the application ribbon renderer in Swing

- Status: Accepted
- Date: 2026-10-05
- Decision owner: Project maintainers
- Related work: issue #765; rollback commit `e5ebc5631`

## Context

The ribbon must follow the current Microsoft Office ribbon appearance requested
for microProject, using the application's own green accent. The first Flamingo
renderer produced a structure and visual treatment closer to an older/classic
ribbon. Making it resemble the current reference required increasingly
specialized UI delegates and styling work, while the previous Swing ribbon was
closer to the desired layout and easier to tune against screenshots.

The migration also encouraged maintaining renderer-specific state and visual
rules alongside the existing shared command and contextual-tab contracts. That
made visual iteration slower and increased the chance that fixes would apply to
one rendering path but not another. A library being established does not by
itself make it a good fit when its component model conflicts with the target UI.

## Decision

Keep `ModernRibbonPanel` as the single application ribbon renderer, implemented
with Swing. Do not use Flamingo `JRibbon` for rendering the application ribbon,
and do not retain parallel active ribbon renderers.

Flamingo may remain where unrelated existing code needs its common or icon
APIs. Such use does not justify pulling its ribbon renderer into the application
or adding new ribbon-specific Flamingo adapters.

Reconsider this decision only if a concrete alternative demonstrates better
visual fidelity and maintainability in a rendered comparison against the
current Microsoft Office reference, while preserving the shared command,
contextual-tab, keyboard, and accessibility behavior. Record that evidence and
revise this ADR before beginning another renderer migration.

## Consequences

- Visual changes should be made in `ModernRibbonPanel` and its shared styling
  helpers; avoid a second visual source of truth.
- Keep command dispatch and contextual visibility independent of the renderer.
- Before proposing a UI toolkit migration, compare actual screenshots at the
  same dimensions and scale. Do not infer visual suitability from library
  popularity or feature lists.
- The Flamingo dependency can only be removed after its remaining non-ribbon
  callers have been migrated or removed; this ADR does not claim that the
  dependency has already been eliminated.

## Verification

When changing the renderer or its appearance, run the focused ribbon unit and
Robot acceptance tests, capture the ribbon at representative widths, and review
those images before claiming visual parity. See `docs/gui-quality-gate.md` and
the applicable cases in `TEST_PLAN.md`.
