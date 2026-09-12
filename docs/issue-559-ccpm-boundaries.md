# Issue #559 — CCPM boundary contract

The existing `CriticalChainService` remains the single analysis source. The
following boundaries are covered without a second CCPM implementation:

| Boundary | Contract | Evidence |
|---|---|---|
| empty project | typed empty task/edge projection and non-negative project buffer | `CriticalChainServiceTest.emptyAndBoundaryTasksProduceTypedSafeBuffers` |
| dependency chain | dependency edges preserve task IDs | `CriticalChainServiceTest`, `CriticalChainApplyAndRenderTest` |
| resource-constrained chain | resource edges and selected resource scope are retained | `previewFindsResourceConstraintWithoutMutatingSchedule`, `applyLevelsResourceConflictsOnlyAfterCcpMIsEnabled` |
| feeding/project/resource buffers | immutable typed `Buffer` values with `BufferKind.PROJECT/FEEDING/RESOURCE`; remaining never exceeds planned | `CriticalChainServiceTest.emptyAndBoundaryTasksProduceTypedSafeBuffers`, `CriticalChainServiceTest.typedBufferRejectsInvalidValuesAndPreservesEdgeKinds`, `MpoFileImporterTest.mpoRoundTripPreservesAppliedCcpmAndCanReanalyzeTheChain` |
| zero-duration milestone / 100% complete | milestone remains zero duration and complete after apply | `emptyAndBoundaryTasksProduceTypedSafeBuffers` |
| Analyze / Apply / Refresh and asynchronous stale result | the service exposes one typed preview/apply path; document generation tokens discard old load/save completion before refresh can publish it | `CriticalChainServiceTest`, `DocumentGenerationTest.realFileSaveAndLoadCompletionsDiscardAnOlderDocumentGeneration` |
| Apply → Save → Reload → Clear | applied CCPM state survives MPO round-trip, can be re-analyzed, and clearing removes only CCPM state while retaining the task model | `MpoFileImporterTest.mpoRoundTripPreservesAppliedCcpmAndCanReanalyzeTheChain` |
| stale Analyze / Apply / Refresh callback | monotonic `CriticalChainService.Generation` accepts only the latest token | `CriticalChainServiceTest.analyzeApplyRefreshDiscardStaleGenerations`; document I/O integration is covered by `DocumentGenerationTest.realFileSaveAndLoadCompletionsDiscardAnOlderDocumentGeneration` |
| edited baseline on Clear | clear returns a conflict-aware `ClearResult`; the existing undo transaction restores the edited baseline and schedule atomically | `CriticalChainServiceTest.clearReportsEditedBaselineAndUndoRestoresIt` |
| multiple feeding / fixed constraints | feeding and resource-predecessor projections remain separate immutable maps, with typed graph edges | `CriticalChainServiceTest.multipleFeedingBranchesRemainSeparateFromFixedDependencies` |
| resource buffer thresholds | remaining-work consumption is bounded and uses GREEN/AMBER/RED thresholds | `CriticalChainServiceTest.resourceBufferRemainingWorkUsesGreenAmberRedThresholds` |
| supported view/report projection | graph scene exposes project, feeding, and resource buffer nodes from the same analysis snapshot | `CriticalChainGraphScene`, `CriticalChainReportService`, `CriticalChainService.Analysis.resourceBuffers()` |

The current implementation now has typed buffer kinds, resource-buffer projections,
generation guards, and conflict-aware clear results.  The issue remains open until
the acceptance matrix also has explicit multiple-feeding-chain/fixed-constraint
coverage, threshold/remaining-work assertions for resource buffers, and a
projection-level proof that every supported CCPM view/report renders the typed
resource buffers without mutating the ordinary WBS task list.

The network UI consumes `CriticalChainGraphScene`; it does not recalculate or
mutate the domain analysis. Immutable snapshots prevent callers from mutating an
analysis after it has been published. The integration test uses the real MPO
reader/writer and exercises the full Apply → Save → Reload → Clear lifecycle.
