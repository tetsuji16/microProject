# ProjectLibre Java 25 modernization progress

Status: in progress; this document tracks ProjectLibre-origin code separately
from OpenProj-origin work in issue [#595](https://github.com/tetsuji16/ProjectLibre/issues/595).
Plan: [#727](https://github.com/tetsuji16/ProjectLibre/issues/727).

## Scope and provenance rule

The target is active production code whose ProjectLibre origin is established
at the changed hunk by comparison with the OpenProj baseline and ProjectLibre
history/source. The file name, old copyright notice, or absence of an OpenProj
path match is not sufficient evidence. `docs/legal/license-provenance.csv`
currently classifies no Java source row as ProjectLibre-origin; unresolved
mixed files remain `REVIEW`. Existing hunk-level findings below are recorded
from `docs/audit/openproj-java25-modernization.md` and must not be counted as
OpenProj progress.

## Plan

- [ ] Phase 0: review provenance for active Java hunks and record verified
  ProjectLibre-origin candidates, callers, and compatibility boundaries.
- [ ] Phase 1: apply focused type-safety and Java language improvements where
  behavior and API erasure remain stable.
- [ ] Phase 2: modernize higher-risk APIs or redundant responsibilities only
  after caller, reflection, persistence, and UI-route review.
- [ ] Phase 3: verify the relevant module/callers and update this record and
  the issue checklist for each completed tranche.

## Verified modernization candidates

| ProjectLibre-origin responsibility | Evidence and change | Verification |
|---|---|---|
| `FieldUtil.getCategories` | The `FieldUtil` class exists in the OpenProj baseline, but this method does not; it first appears in ProjectLibre 1.9.8 (`0530be227f4a10c5545cce8d3db20ac5a4d76a66`). Replaced its sized-array `toArray` call with `categories.toArray(String[]::new)` and applied diamond inference to its local collections; result type, order, and contents are unchanged. | `:microproject_core:test --tests "com.microproject.core.fields.FieldUtilTest" --console=plain` passed 2026-09-27; regression asserts hierarchy order and contents. |
| `Dictionary.add` and `Dictionary.getClassesAsArray` | `Dictionary` is absent from the OpenProj baseline and exists in ProjectLibre 1.9.8 as `org.projectlibre.core.dictionary.Dictionary`. Bound `HasCategories` in the guard, replaced the sized-array conversion with `classes.toArray(Class<?>[]::new)`, and applied diamond inference to collection construction and anonymous iterators. Category/ALL indexing and class results are unchanged. | `:microproject_core:test --tests "com.microproject.core.dictionary.DictionaryTest" --console=plain` passed 2026-09-27; regression checks category and ALL indexes plus class-array contents. |
| ProjectLibre JAXB map adapters | `DictionaryAdapter`, `DictionaryAdapterList`, `MapAdapter`, `MapAdapterEntry`, and `MapAdapterList` exist in ProjectLibre 1.9.8 and are absent from the OpenProj baseline. Replaced explicit generic constructor type arguments with diamond inference; collection types, capacities, and JAXB method signatures are unchanged. `MapAdapterEntry` remains a bean with its no-argument constructor and XML annotations. `DictionaryAdapter` retains its cast because its unconstrained public `T` type and JAXB adapter boundary need compatibility review before changing the generic bound. | Full `:microproject_core:test --console=plain` passed 2026-09-27. |
| `ResourceUtil.createObject` | `ResourceUtil` exists in ProjectLibre 1.9.8 and is absent from the OpenProj baseline. Replaced deprecated `Class.newInstance()` with `getDeclaredConstructor().newInstance()` while preserving package fallback and rethrowing the underlying constructor exception. | `:microproject_core:test --tests "com.microproject.core.util.ResourceUtilTest" --console=plain` passed 2026-09-27; covers successful construction and original exception identity. |
| `FieldUtil.convertField` Boolean skip condition | The Boolean `instanceof` plus cast condition matches ProjectLibre 1.9.8 `projectlibre_core/src/com/projectlibre/core/fields/FieldUtil.java`; it is a ProjectLibre-modified responsibility in a mixed-origin class. Replaced the redundant cast with a Boolean pattern binding; null and false still skip the setter, while true continues through conversion and reflection. | `:microproject_core:test --tests "com.microproject.core.fields.FieldUtilTest" --console=plain` verifies false skip and true setter behavior. |
| `AbstractNode.fieldValues` | This class and its string-keyed field store match ProjectLibre 1.9.8 `com.projectlibre.core.nodes.AbstractNode`; there is no corresponding OpenProj `AbstractNode` class. Applied diamond inference without changing the `Map<String, Object>` field type or field lookup behavior. | `:microproject_core:test --tests "com.microproject.core.nodes.AbstractNodeTest" --console=plain` passed; verifies property and direct-field access. |
| `FieldUtil.getFields` | This method is absent from the compared OpenProj baseline and present in ProjectLibre 1.9.8 `com.projectlibre.core.fields.FieldUtil`. Applied diamond inference to its result map without changing keys, values, category precedence, or return type. | `:microproject_core:test --tests "com.microproject.core.fields.FieldUtilTest" --console=plain` verifies a registered field is returned from its category. |
| `FieldUtil.getFields` category traversal | Same ProjectLibre-added method as above. Replaced `keySet()` plus `get(key)` with typed `entrySet()` traversal; category order, first-category-wins precedence, value cast, and returned map remain unchanged while avoiding a second lookup for each key. | `:microproject_core:test --tests "com.microproject.core.fields.FieldUtilTest" --console=plain` passed 2026-09-27. |
| `Dictionary.iterator(DictionaryCategory)` empty result | The missing-category branch matches ProjectLibre 1.9.8 `org.projectlibre.core.dictionary.Dictionary`. Repository search found no production callers. Replaced its custom empty iterator with `Collections.emptyIterator()`, so `hasNext` is false and `next` follows the `Iterator` contract by throwing `NoSuchElementException`; removal follows the JDK empty iterator's `IllegalStateException` behavior. | `:microproject_core:test --tests "com.microproject.core.dictionary.DictionaryTest" --console=plain` verifies empty, exhausted-next, and remove behavior. |
| `LocaleDialog` collection construction and `Country.equals` | `LocaleDialog` is absent from OpenProj 1.4 and present in ProjectLibre 1.9.8. Replaced four explicit generic collection constructors and two raw/explicit `TreeSet` constructors with diamond inference. Replaced `instanceof` plus cast in `Country.equals` with a pattern binding; null, type, subclass, and country-code equality semantics are unchanged. | `:microproject_ui:test --tests "com.microproject.dialog.LocaleDialogTest" --console=plain` passed 2026-09-27; covers equal/different codes, null, and a different type. |
| `ProjectLibrePrintServiceImpl` spreadsheet parameter handling | The active class matches ProjectLibre 1.9.8 and is absent from OpenProj 1.4. Replaced two `instanceof` plus cast sequences in width/height ratio calculation with pattern variables; fallback calculations and invalid-parameter result remain unchanged. | `:microproject_ui:test --tests "com.microproject.print.ProjectLibrePrintServiceImplTest" --console=plain` passed 2026-09-27; covers non-spreadsheet width fallback and null/non-spreadsheet height rejection. |
| ProjectLibre-added `Serializer.saveTasks` and `deserializeProject` collections | The methods are recorded as ProjectLibre-added in the hunk ledger and match the ProjectLibre 1.9.8 source. Replaced explicit constructor type arguments for unchanged-ID lists and the subproject-node map with diamond inference. Existing capacity hints, element types, and serialization behavior are unchanged. The similarly named referring-subproject block is fork-owned and was not touched. | `:microproject_exchange:test --tests "com.microproject.exchange.PodRoundTripTest" --console=plain` passed 2026-09-27; covers native project save/load and subproject round trips. |
| `MPXConverter.projity2mpxTimeUnit` | The hunk ledger records this method as ProjectLibre-added. Replaced the mutating switch statement with a switch expression; all 14 engine time-unit constants retain their MPXJ index and the default still maps to elapsed percent. | `:microproject_exchange:test --tests "com.microproject.server.data.MpxConverterTimeUnitTest" --console=plain` passed 2026-09-27; checks all 14 source/output unit pairs through `toMPXDuration`. |
| `ScrollPaneSynchronizer` mouse-wheel target tracking | The target lists and register/unregister helpers are present in ProjectLibre 1.9.8 and absent from the OpenProj baseline. Typed the bounded target lists and private helpers as `ArrayList<Component>`, then replaced the indexed loop and cast with enhanced-for. Registration/removal order, capacity 6, and final clear are unchanged; protected field erasures remain `ArrayList`. | `:microproject_ui:test --tests "com.microproject.pm.graphic.views.synchro.ScrollPaneSynchronizerTest" --console=plain` passed 2026-09-27; the new EDT test checks installation/removal across the pane, view, row header and column header for both panes. |

## Explicit exclusions

- `Field.toTaskSheetScheduleValue` is absent from the ProjectLibre 1.9.8
  source and OpenProj baseline; its first appearance is this repository's
  `54e5480390` refactor. It is microProject fork code, not ProjectLibre-origin
  code.
- `Field.getGroupDuration` is absent from the ProjectLibre 1.9.8 source and
  OpenProj baseline; it is a later microProject fork addition.
- `com.microproject.configuration.Dictionary.getAll`'s typed `NamedItem[]`
  conversion was introduced by local commit `54e5480390`; ProjectLibre 1.9.8
  and the OpenProj baseline both use an `Object[]` from `values().toArray()`.
  Do not count a modernization of that typed hunk as ProjectLibre-origin work.
- `TreeView.getSelectedNodes` and its one-element list construction were added
  by microProject commit `c3745988a` for RBS selection fallback; the method is
  absent from both the OpenProj baseline and ProjectLibre 1.9.8 source. Keep it
  outside #727 provenance counts (and do not misclassify it as OpenProj simply
  because the containing `TreeView` type is OpenProj-derived).
- These exclusions are not counted toward #727 or #595.

## ProjectLibre-added core type inventory

Compared additions between the OpenProj baseline (`d2fa3c20a`) and the
ProjectLibre 1.9.8 source (`0530be227f4a10c5545cce8d3db20ac5a4d76a66`) with
the active `modules/` sources:

- `FieldList` is active and carries JAXB names/identity references. Its current
  class has no remaining generic construction, obsolete reflection API, or
  cast-after-`instanceof` candidate; its XML annotations remain a compatibility
  boundary.
- ProjectLibre additions `FieldManager`, `VersionComparator`,
  `ScheduleChangedEvent`, `Scheduler`, and `SnapshotList` have no active source
  counterpart under `modules/` and are not current modernization targets.
- The upstream-added `com.projectlibre.pm.tasks.TaskSnapshot` has no active
  counterpart. The similarly named active `com.microproject.pm.task.TaskSnapshot`
  corresponds to the separate `com.projectlibre1.pm.task.TaskSnapshot` path,
  which maps to OpenProj's `com.projity.pm.task.TaskSnapshot`; do not conflate
  these classes during provenance review.
- The active `DictionaryAdapter` still casts its unconstrained `T` to
  `HasStringId` at the JAXB conversion boundary. No production or XML-resource
  callers were found; changing the generic bound would alter its public source
  contract, so it remains a compatibility review item rather than an automatic
  cleanup. `ArrayUtil` retains `StringTokenizer` because replacing its token
  semantics would change how empty comma-separated coordinates are handled.

## ProjectLibre-added UI type inventory

- `LocaleDialog` is absent from OpenProj 1.4 and present in ProjectLibre 1.9.8.
  It remains active under `modules/microproject_ui`; its collection constructors
  and `Country.equals` are covered by the verified modernization entry above.
  Its locale parsing and sorting behavior remain unchanged.
- A basename comparison of active non-contrib production Java files with the
  OpenProj and ProjectLibre 1.9.8 source trees found 15 active ProjectLibre
  additions by path/name. This is a candidate inventory, not a complete hunk
  provenance audit; mixed-origin files and renamed/moved classes still need
  direct source/history comparison. The existing additions include the five
  JAXB adapters, dictionary types, `FieldList`, utility types, `LocaleDialog`,
  `ImageExport`, and `ProjectLibrePrintServiceImpl`.
- `ImageExport` is a path/name false positive for ProjectLibre provenance: its
  type exists in the OpenProj baseline (`openproj_ui/src/org/projectlibre/export/ImageExport.java`,
  introduced in `60d3081295`) and is marked `KEEP_OPENPROJ` in the delta ledger.
  The current injectable file chooser and asynchronous export facade are
  microProject changes; do not count them toward #727.
- `ExtRibbonFactory` is confirmed ProjectLibre-added by the delta ledger. Its
  active implementation already uses `Map<String, List<AbstractCommandButton>>`,
  `computeIfAbsent`, and typed return values; no remaining raw-collection or
  cast-after-`instanceof` cleanup candidate was found in this type. Repository
  source/resource searches found no current caller of `ExtRibbonFactory` after
  the app moved to `SwingRibbonFactory`. Since the class is public and
  ProjectLibre-added, removal also requires an external extension compatibility
  check; do not treat the internal no-caller result alone as proof of dead code.

## ProjectLibre-added method crosswalk

The retained hunk ledger in `docs/audit/projectlibre-delta-items.csv` uses the
pre-rename `modules/projectlibre_*` paths. Matching its `KEEP_PROJECTLIBRE`
method symbols to active non-contrib production Java files by type name yields
33 current source candidates. This is a crosswalk for further review, not proof
that every current hunk in each file still has upstream provenance; the CSV
path predates module and package renames, and each target change still requires
line-level history/source comparison.

Candidate type names: `AssignmentData`, `ByteArrayObject`, `CalendarData`,
`CompanyData`, `Context`, `CustomFieldsMapper`, `DataObjectImpl`,
`EnterpriseResourceData`, `ExtRibbonFactory`, `GroupData`,
`ImportedCalendarService`, `IncrementalData`, `LinkData`, `Linker`,
`MPXConverter`, `MSPDISerializer`, `MenuManager`, `MicrosoftImporter`,
`ProjectData`, `ProjectSerializer`, `ResourceData`, `ResourceLinker`,
`RoleData`, `SerializeOptions`, `SerializeUtil`, `Serializer`,
`ServerFileImporter`, `ServerLocalFileImporter`, `TaskData`, `TaskLinker`,
`TypeSystemConverter`, `TypeSystemConverterFactory`, and `UserData`.

Spot checks against ProjectLibre 1.9.8 and `git blame` rejected several
apparent modernization hits as fork-owned or OpenProj-owned: for example,
`Serializer`'s referring-subproject serialization block is local fork code;
`MSPDISerializer`'s opened-subproject export guard and calendar collector are
also local additions; and the remaining `MSPDISerializer` `Assignment` check
comes from the OpenProj baseline. These are excluded from #727. The ledger's
ProjectLibre-added methods in `Serializer` and `MSPDISerializer` use typed
iterators where removal is required; replacing those with enhanced loops would
change mutation behavior or fail to preserve removals.

`TaskLinker` is a ProjectLibre-added type, but its raw `flatAssignments`
property is exposed through protected state and public getter/setter methods.
The only in-repository setter caller is `Serializer`, whose pipeline currently
passes `Collection<DataObject>` and inserts `AssignmentData`; no external
subclass inventory is available here. Its `PreparedAttributes` raw
`Collection`/`List` fields are explicitly documented in source as a legacy
subclass compatibility holder and have no in-repository callers. Defer changing
these generic signatures until extension/source compatibility can be assessed;
do not count their containing type's ProjectLibre provenance as blanket
authorization to narrow the public surface.

## Initial inventory finding

The previous audit classified the two `Field` responsibilities above as
ProjectLibre fork additions based on commits in this repository. The general
provenance CSV remains file-based and conservative; the verified hunk-level
finding above is recorded here rather than promoting a whole mixed file. Phase
0 continues for additional source; no Java file is promoted from `REVIEW` based
on naming or current implementation alone.

