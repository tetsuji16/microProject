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
| `ProjectLibreXlsxWriter` ProjectLibre-specific workbook sections | This writer and its ProjectLibre-specific calendar, assignment, and dependency sections were introduced in ProjectLibre's exchange module; history traces the checked casts to that import. Replaced `ResourceImpl`, `NormalTask`, `Assignment`, and `Dependency` `instanceof` plus casts with Java pattern bindings. Output conditions, ordering, row indices, and serialized values are unchanged; unrelated MPXJ workbook paths were not modified. | `:microproject_exchange:test --tests "test.com.microproject.exchange.XlsxSupportTest" --console=plain` passed 2026-09-27, including generated XLSX import/export coverage. |
| `ServerLocalFileImporter` collection construction | The importer is present in ProjectLibre 1.9.8; the same `ArrayList`/`HashMap` responsibilities and explicit constructor type arguments are present in that upstream source. Applied diamond inference while preserving concrete generic types and each existing capacity, including the capacity added by the local fork. | `:microproject_exchange:test --tests "com.microproject.exchange.ServerLocalFileImporterTest" --console=plain` passed 2026-09-27. |
| `MSPDISerializer.collectProjectCalendars` set construction | The method and both set responsibilities are present in ProjectLibre 1.9.8 and absent from the OpenProj baseline. Replaced explicit constructor type arguments with diamond inference; `LinkedHashSet` encounter order, visited-set equality semantics, and method signature are unchanged. | `:microproject_exchange:test --tests "com.microproject.core.pm.exchange.converters.mpx.MpxProjectConverterTest" --console=plain` passed 2026-09-27. |
| `Serializer` ordering comparators | The comparators in `printTaskDataHierarchy`, `sortResourcesByChildPosition`, and `sortTasksByChildPosition` match the corresponding ProjectLibre 1.9.8 source responsibilities. Replaced anonymous comparator classes with lambdas and `Comparator.comparingLong`; retained external-task ordering and the duplicate-position diagnostic side effect used by the `TreeSet`. | `:microproject_exchange:test --tests "com.microproject.exchange.PodRoundTripTest" --console=plain` passed 2026-09-27, including moved-task order through native save/reload. |
| `LocalSession.loadLocalProjectDescriptors` ordering | The descending modification-date and case-insensitive name comparator matches ProjectLibre 1.9.8. Replaced its anonymous `Comparator<ProjectData>` with `List.sort` and a lambda; date tie handling, null-date fallback, null-name order, and case-insensitive comparison are unchanged. | Full `:microproject_core:test --console=plain` passed 2026-09-27. |

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
- `ScrollPaneSynchronizer.ganttSynchronizers` and its invalidation/zoom-restore
  path were introduced by microProject commit `2cc236603` and are absent from
  the OpenProj baseline and ProjectLibre 1.9.8. The attempted raw-map typing was
  rejected after this provenance check; do not count that hunk toward #727.
- `CalendarViewDialogBox` was introduced wholesale by local commit
  `2b3ff5faa5` ("Complete MS Project usability roadmap"); the file is absent
  from that commit's parent. Its old `modules/projectlibre_ui` path and
  `com.projectlibre1` package reflect the temporary checkout layout, not
  upstream provenance. Keep this UI extension outside #727 and #595.
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

Reconciliation against every distinct non-empty `KEEP_PROJECTLIBRE.current_path`
in the retained ledger found 43 pre-rename paths: 36 map to existing files
(35 production Java files and one test file), while the seven MSPDI paths below
are removed or replaced. The 33 named production candidates above are the
behavior-bearing types screened for modernization. The other two current
production paths are accounted for separately: `LockException` contains only
the `serialVersionUID` and four direct superclass-constructor delegations, so
changing it offers no Java 25 improvement and risks altering exception
serialization/API behavior; `MenuActionConstants` is a public interface of
named string constants, whose explicit modifiers make its exposed API clear and
are not a meaningful modernization target. `MicrosoftImporterTest` is test
code rather than a production candidate. This accounts for every mapped path;
it does not by itself close the per-symbol provenance review of all ledger
rows.

Further idiom screening of active exchange/UI candidates found one safe
ProjectLibre-origin tranche in `ServerLocalFileImporter` (recorded above).
Other apparent hits were excluded at the hunk level:

- `Serializer`'s referring-subproject type check and `LocalSession` seed reset
  were added by microProject commits `86e89bfe37` and `a14fe81977`; they are
  fork-owned, not #727 candidates.
- `Linker.addOutline`'s `instanceof Assignment` conditions match the OpenProj
  baseline (`d2fa3c20a`); this belongs to #595, not #727.
- `ImportedCalendarService` exists in ProjectLibre 1.9.8, but its two explicit
  generic `HashMap` constructor arguments were added by local commit
  `49eb8dd329`; the upstream class used raw maps. These constructor hunks do
  not establish ProjectLibre-origin modernization work.
- The remaining `MSPDISerializer` `Assignment` check matches the OpenProj
  baseline. Other visible `VoidNodeImpl` and `WorkingCalendar` checks are
  associated with local serialization changes and were not promoted without
  exact upstream hunk matches.
- `CustomFieldsMapper`'s reflection cast is required at the `Class<?>`
  reflection boundary; the declared-field result is only known at runtime.

This is a targeted screen of these named candidates, not a declaration that
the full Phase 0 inventory is complete. The remaining delta-ledger candidates
still require method/hunk review before Phase 0 can be closed.

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

## Active crosswalk residual screen (2026-09-27)

Re-ran the syntax/API screen over all 33 active Java source paths named by the
retained ProjectLibre hunk crosswalk. The search covered explicit collection
constructor arguments, cast-after-`instanceof`, old `Collections.sort`,
anonymous Comparator/Iterator/functional-interface construction, reflective
`Class.newInstance`, and sized-array `toArray` calls. Every match was then
checked at the hunk and caller level; basename and copyright matches were not
used as provenance evidence.

| Residual match | Disposition |
|---|---|
| `Serializer`'s `referringSubprojectTasks` list and `LocalSession` seed reset | Fork-owned changes (`86e89bfe37`, `a14fe81977`), excluded from #727. |
| `Serializer`'s old commented dirty-task loop | OpenProj-origin dead comment; removed under #84. The active outline-based dirty-task path remains. |
| `MSPDISerializer.externalTasks` map and `MPXConverter.ExportIdAllocator` map | Local fork additions (`ab550c36db`, `bdac3f3851`), excluded from #727. |
| `MSPDISerializer`'s opened-subproject export checks | Local serialization changes, excluded from #727. The ProjectLibre-added `collectProjectCalendars` sets were modernized above. |
| `ImportedCalendarService`'s two explicit generic map constructors | Constructors are local edits in an upstream-added type; ProjectLibre 1.9.8 used raw maps. Do not attribute these exact hunk edits to ProjectLibre. |
| `Linker.addOutline` Assignment checks | Match OpenProj baseline `d2fa3c20a`; tracked under #595. The mutable state in its anonymous traversal callback is not replaced with captured-array state. |
| `SerializeUtil`'s three `instanceof` checks | They only choose the ZIP serialization branch and do not feed casts or repeated type-specific work; a binding provides no value. |
| `MSPDISerializer`'s `VoidNodeImpl`/`Assignment` checks and `ServerLocalFileImporter`'s `String` check | Type-only guards without a corresponding cast at the check site; no safe binding improvement. |
| `TypeSystemConverterFactory` reflection | Already uses `getDeclaredConstructor().newInstance()`. |
| `CustomFieldsMapper` reflection cast | Runtime `FieldType` comes from `Class.getDeclaredField`; retain the cast at this reflective boundary. |
| `TaskLinker` public/protected raw collection API and `DictionaryAdapter<T>` cast | Compatibility-sensitive boundaries already described above; no internal-only caller search justifies changing public generic source contracts. |

This closes the targeted idiom screen for the 33 crosswalk source candidates,
not the full Phase 0 ledger review: the retained path-based delta ledger still
needs one-to-one current-hunk/caller reconciliation before Phase 0 can be marked
complete. No additional safe modernization change was identified by this screen.

### Stale paths found during Phase 0 reconciliation (2026-09-27)

The retained ledger also contains seven pre-rename MSPDI paths marked as active
ProjectLibre additions even though those files are no longer present in the
current source tree. Six were removed by `c8084bd7f` as duplicate/obsolete MPXJ
implementations: `ModifiedMSPDIWriter`, `ProjectContentHandler`,
`TimeDistributedTypeMapper`, `TimephasedConsumer`, `TimephasedGetter`, and
`TimephasedService`. The remaining `XsdDuration` was replaced by the JDK
`javax.xml.datatype.Duration` implementation in `01aab5cc0` as part of issue
#43. The seven files therefore are historical ledger entries, not seven
unreviewed active modernization candidates. Keep their provenance history, but
correct the ledger's current-tree status/evidence and link each removal or
replacement commit before closing Phase 0. The surviving MSPDI behavior remains
subject to import/export compatibility review through the current serializer
and MPXJ paths.

Reconciliation recorded on 2026-09-28: the ledger contains 778 distinct
`KEEP_PROJECTLIBRE` entries across 43 pre-rename current paths (446 methods,
248 fields, 60 types, 22 constructors, and two hunks). Mapping module and
package names to the active layout finds 36 existing files (35 production
files and one test) and the seven historical MSPDI paths above. The 71
ProjectLibre ledger entries for those paths, plus the separate third-party
`XsdDuration#<init>(net.sf.mpxj.Duration)` row, retain their existing
`VERIFIED` work status because that shared field tracks progress in the
broader rename ledger. Their evidence records the removal or JDK replacement
commits. The issue-specific current-tree status is recorded here rather than
overloading that shared field. This closes the stale-path reconciliation, but
not the remaining per-symbol declaration/caller review for the 36 extant
files, so Phase 0 remains open.

## Initial inventory finding

The previous audit classified the two `Field` responsibilities above as
ProjectLibre fork additions based on commits in this repository. The general
provenance CSV remains file-based and conservative; the verified hunk-level
finding above is recorded here rather than promoting a whole mixed file. Phase
0 continues for additional source; no Java file is promoted from `REVIEW` based
on naming or current implementation alone.

