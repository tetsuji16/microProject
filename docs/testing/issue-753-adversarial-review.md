# Issue #753 report totals regression rationale

The custom report keeps every selected task row and the visible task count. Its
summary totals and Work chart use the same visible-task aggregation: when a
non-direct-assignment summary and one of its descendants are both visible, the
summary rollup is omitted so the descendant contribution is counted once. A
summary with direct assignments remains alongside its visible descendants, and
a summary without any visible descendant contributes its rollup once. The
omitted summary's own fixed cost remains in Total Cost; its Work, Actual Work,
and Remaining Work rollups contribute zero to the aggregate.

This is a product-specific custom-report rule for the Include Summary filter,
not a claim of measured Microsoft Project behavior. The model API confirms why
the split is needed: `NormalTask.cost()` and `work()` roll child values up only
for summaries without real assignments, while `getCost(null)` also includes
the summary's own fixed cost. See `NormalTask.isParentWithoutAssignments()`,
`NormalTask.cost()`, `NormalTask.work()`, and `NormalTask.getCost(FieldContext)`.

`CustomReportDialogBoxCsvTest` uses real Project task hierarchies to cover a
three-level hierarchy, nonzero summary fixed costs, leaf assignment work and
actuals, a directly assigned summary with a visible child, summary-only
visibility, and empty chart series. It checks that visible table rows remain
available while numeric totals avoid duplicate rollups.

## Rendering closure blockers and evidence

The adversarial review found two additional closure blockers in the report
renderer. Report Java2D drawing and PDF export depended on host-installed CJK
fonts, so a clean host could show missing Japanese glyphs or emit a PDF without
an embedded Japanese face. Project Details also exposed the raw field
declaration list instead of the legacy visible structure.

The reports module now bundles official Noto Sans JP static SubsetOTF Regular
and Bold resources from `notofonts/noto-cjk`, pinned to commit
`f8d157532fbfaeda587e826d4cd5b21a49186f7c`. Their combined size is 9,189,476
bytes. The full upstream OFL 1.1 text is in
`packaging/licenses/third-party/Noto-Sans-JP-OFL.txt` and is staged in
`microproject_ui.jar` under `license/third-party/`. The resource notice records
the source and SHA-256 values. Report metrics, Java2D painting, and the
report-specific PDF mapper share those font resources; the generic Gantt PDF
mapper remains on its existing path.

Project Details now retains its legacy page title and five ordered sections:
Dates (6 visible fields), Duration (5), Work (4), Costs (5), and Notes (1).
The report shows all 21 visible label/value items in English and Japanese.
Project Name remains the title; Manager and Percent Work Complete are not
visible fields. Regression coverage fixes the visible field IDs, labels,
section names, ordering, and omitted fields. No legacy baseline snapshot was
changed. The replacement layout is one portrait page for the measured fixture
under the bundled font; long Notes continue across pages with the final
Japanese sentinel intact.

Host-independent rendering coverage checks Japanese glyph support, regular and
bold faces, metrics-driven wrapping, ink from the rendered page, PDF text
extraction under English and Japanese locales, and a section heading that
moves with its first value when the section begins at a page boundary. The PDF
test inspects the actual page font dictionaries and requires non-empty
FontFile2/FontFile3 streams for both Noto Sans JP faces. The prior host-font
assumptions and skip condition have been removed.

The integrated gate passed:

```text
.\gradlew.bat build installDist stageAppDist verifyPackagedFileImports verifyDependencyAllowlist --console=plain
BUILD SUCCESSFUL (58 actionable tasks)
```

The generated distribution contains both font resources and the OFL license,
and contains no JasperReports or Eclipse Compiler jars. The MPO workspace
compatibility test ran 2 tests with no failures, errors, or skips. The physical
GUI journey `TaskInformationRibbonGuiAcceptanceTest.physicalReportPrintPreviewShowsEveryPageThroughTheSharedGraphPageableRoute`
passed after refreshing installDist; it selected the report, exported CSV, and
opened preview with a panel for every printable page. Independent review of
the completed changes reported no remaining major finding.
