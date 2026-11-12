/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.exchange;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.SwingUtilities;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.microproject.job.Job;
import com.microproject.collaboration.OperationLog;
import com.microproject.collaboration.MpoTaskOperationService;
import com.microproject.job.JobRunnable;
import com.microproject.pm.ccpm.CriticalChainService;
import com.microproject.pm.ccpm.CriticalChainBufferHistory;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.ProjectFactory;
import com.microproject.pm.task.DefaultSubProj;
import com.microproject.pm.task.SubProj;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.TaskSplitInterval;
import com.microproject.pm.task.Task;
import com.microproject.pm.dependency.Dependency;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.assignment.BaselineAssignmentProfile;
import com.microproject.pm.assignment.AssignmentLevelingSplitState;
import com.microproject.pm.assignment.BaselineAssignmentProfile.Bucket;
import com.microproject.pm.assignment.BaselineAssignmentProfile.Contour;
import com.microproject.pm.calendar.CalendarDefinition;
import com.microproject.pm.calendar.CalendarRecurrence;
import com.microproject.pm.calendar.RecurringCalendarException;
import com.microproject.pm.calendar.WorkDay;
import com.microproject.pm.calendar.WorkRange;
import com.microproject.pm.calendar.WorkWeek;
import com.microproject.pm.calendar.WorkWeekPeriod;
import com.microproject.pm.calendar.WorkingCalendar;
import com.microproject.pm.calendar.WorkingHours;
import com.microproject.pm.resource.Resource;
import com.microproject.pm.resource.ResourceImpl;
import com.microproject.pm.snapshot.Snapshottable;
import com.microproject.pm.task.TaskSnapshot;
import com.microproject.field.Field;
import com.microproject.graphic.configuration.SpreadSheetFieldArray;
import com.microproject.pm.assignment.TimeDistributedHelper;
import com.microproject.transaction.ProjectMutationState;
import com.microproject.session.LocalSession;
import com.microproject.session.SessionFactory;
import com.microproject.strings.Messages;
import com.microproject.temporary.TemporaryCleanupQueue;
import com.microproject.util.MarkupEscape;
import com.microproject.util.RetirableFileLock;
import com.microproject.util.SafeFileReplace;

/**
 * Reads and writes MPOF v1.0 containers. An mpo file is a ZIP containing a
 * UTF-8 manifest and a standards-based MSPDI XML snapshot; it never embeds a
 * Java serialized object.
 */
public class MpoFileImporter extends FileImporter {
	private static final Object EXPORT_LOCK_GUARD = new Object();
	private static final int CAPTURE_ATTEMPTS = 3;
	private static final Pattern SERIALIZER_CLOCK = Pattern.compile("<CurrentDate>[^<]*</CurrentDate>");
	private boolean replaceDifferentDocument;
	private volatile boolean committed;
	static volatile Path extractionWorkspaceRoot;
	static final String MIMETYPE_ENTRY = "mimetype";
	static final String MANIFEST_ENTRY = "META-INF/manifest.xml";
	static final String PROJECT_ENTRY = "content.xml";
	static final String META_ENTRY = "meta.xml";
	static final String SETTINGS_ENTRY = "settings.xml";
	static final String CCPM_HISTORY_ENTRY = "ccpm/history.jsonl";
	static final String LAYOUT_ENTRY = "layout.json";
	/** Native task-view state that MSPDI does not represent. */
	static final String VISIBILITY_ENTRY = "microproject/visibility.json";
	/** Task-owned Gantt split intervals not represented by the MSPDI schema. */
	static final String TASK_SPLITS_ENTRY = "microproject/task-splits.json";
	static final String TASK_LEVELING_OPTIONS_ENTRY = "microproject/task-leveling-options.json";
	/** Per-task text style (Text Styles, Format Painter); MSPDI has no task-level font fields. */
	static final String TASK_TEXT_STYLES_ENTRY = "microproject/task-text-styles.json";
	/** Exact native schedule values needed when MSPDI import recalculates assignments. */
	static final String TASK_SCHEDULES_ENTRY = "microproject/task-schedules.json";
	/** Detached saved-baseline assignment profiles not representable in MSPDI assignments. */
	static final String BASELINE_PROFILES_ENTRY = "microproject/baseline-profiles.json";
	/** Assignment-level generated split ownership not represented by MSPDI. */
	static final String ASSIGNMENT_LEVELING_SPLITS_ENTRY = "microproject/assignment-leveling-splits.json";
	/** MPOF v1.0 container layout (ODF conventions). */
	static final String FORMAT_ID = "mpof";
	/** Container version this build writes; every save rewrites the file at this version. */
	static final String FORMAT_VERSION = MpoFormatVersion.CURRENT.toString();
	/**
	 * A container is readable when its major version matches. Minor revisions are additive
	 * (unknown entries are carried through as extensions), so an older or newer minor
	 * revision still opens and is upgraded to {@link #FORMAT_VERSION} on the next save
	 * (issue #356). A different major version is rejected with an explicit message.
	 */
	static final int SUPPORTED_FORMAT_MAJOR = MpoFormatVersion.CURRENT.major();
	/** Marker added once MPOF snapshots use the MSPDI minute-based delay unit. */
	static final String LEVELING_DELAY_UNIT_ATTRIBUTE = "levelingDelayUnit";
	static final String LEVELING_DELAY_UNIT_MINUTES = "minutes";
	static final String ASSIGNMENT_BASELINE_COST_UNIT_ATTRIBUTE = "assignmentBaselineCostUnit";
	static final String ASSIGNMENT_BASELINE_COST_UNIT_HUNDREDTHS = "mspdi-hundredths";
	static final String LEVELING_DELAY_FORMAT_MINUTES = "3";
	static final Pattern LEVELING_DELAY = Pattern.compile("<LevelingDelay>(-?\\d+)</LevelingDelay>");
	static final Pattern LEVELING_DELAY_FORMAT = Pattern.compile("<LevelingDelayFormat>\\d+</LevelingDelayFormat>");
	static final String OPERATIONS_ENTRY = "operations/log.jsonl";
	/** Read-only aliases for MPOF drafts written before the XML/JSONL layout was settled. */
	static final String DRAFT_CCPM_ENTRY = "ccpm.json";
	static final String DRAFT_OPERATIONS_ENTRY = "changes/operations.json";
	static final String TASK_IDENTITIES_ENTRY = "changes/task-identities.json";
	/** Raw linked project files carried by a portable master-project archive. */
	static final String EMBEDDED_PROJECT_PREFIX = "projects/";
	static final int MAX_ENTRY_BYTES = 64 * 1024 * 1024;
	private static final int MAX_TOTAL_BYTES = 128 * 1024 * 1024;
	static final int MAX_ENTRIES = 128;
	/** Semantic limits are independent of the compressed/archive byte budget. */
	static final int MAX_OPERATION_COUNT = 100_000;
	static final int MAX_OPERATION_PARENTS = 256;
	static final int MAX_OPERATION_PAYLOAD_FIELDS = 256;
	// Keep normal task notes/actor labels compatible while bounding pathological
	// single-field allocations independently from the 64 MiB entry budget.
	static final int MAX_JSON_STRING_CHARS = 1_048_576;
	static final int MAX_JSON_NESTING = 32;
	static final int MAX_CCPM_HISTORY_ENTRIES = 100_000;
	static final int MAX_CCPM_HISTORY_LINE_CHARS = 64 * 1024;
	static final ObjectMapper JSON = new ObjectMapper().enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
		.enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

	/**
	 * Configures the application-owned root used for extracted embedded projects.
	 * The path is copied as a normalized absolute path; passing {@code null}
	 * restores the platform default.  This bridge keeps exchange independent of
	 * the application TemporaryWorkspace implementation.
	 */
	public static void setExtractionWorkspaceRoot(Path root) {
		extractionWorkspaceRoot = root == null ? null : root.toAbsolutePath().normalize();
	}

	@Override
	public void importFile() throws Exception {
		if (fileInputStream == null) {
			MpoValidationResult preflight = MpoArchiveValidator.validate(fileName == null ? null : Path.of(fileName));
			if (!preflight.valid()) {
				throw new IOException("MPO preflight validation failed: " + preflight.reason());
			}
			project = loadProject(new File(fileName));
		} else {
			// The stream is supplied by the session/application and remains owned by
			// that caller.  loadProject(InputStream) follows the same non-closing
			// contract, so closing a ZIP parser must never close the session stream.
			project = loadProject(fileInputStream);
		}
	}

	/**
	 * Reads a file-backed MPO through its central directory before applying the
	 * stream parser.  Some valid ZIP writers use data descriptors whose local
	 * header sizes are unreliable to {@link ZipInputStream}; {@link ZipFile}
	 * resolves those entries from the authoritative central directory.  The
	 * normalized bytes still pass through {@link #loadProject(InputStream)}, so
	 * duplicate-name, mimetype, checksum and expansion-limit validation remains
	 * identical for stream and file imports.
	 */
	private Project loadProject(File source) throws Exception {
		MpoArchiveBudgetValidator.Result budget = MpoArchiveBudgetValidator.validate(source.toPath(),
			MpoArchiveBudgetValidator.DEFAULT_BUDGET);
		if (!budget.valid())
			throw new IOException("MPOF archive budget rejected: " + budget.reason());
		ArchiveParts parts = new ArchiveParts();
		try (ZipFile zip = new ZipFile(source, StandardCharsets.UTF_8)) {
			java.util.Enumeration<? extends ZipEntry> entries = zip.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				MpoArchiveBudgetValidator.validateEntryName(entry.getName(), MpoArchiveBudgetValidator.DEFAULT_BUDGET);
				if (entry.isDirectory()) {
					parts.accept(entry.getName(), true, null);
				} else {
					try (InputStream entryInput = zip.getInputStream(entry)) {
						parts.accept(entry.getName(), false, entryInput);
					}
				}
			}
		}
		return finishArchive(parts);
	}

	@Override
	public Project loadProject(InputStream in) throws Exception {
		Objects.requireNonNull(in, "in");
		ArchiveParts parts = new ArchiveParts();
		// ZipInputStream.close() also closes its source.  Shield the public input
		// stream so callers can reuse/close it according to their own lifecycle.
		try (ZipInputStream zip = new ZipInputStream(new NonClosingInputStream(in), StandardCharsets.UTF_8)) {
			ZipEntry entry;
			while ((entry = zip.getNextEntry()) != null) {
				MpoArchiveBudgetValidator.validateEntryName(entry.getName(), MpoArchiveBudgetValidator.DEFAULT_BUDGET);
				parts.accept(entry.getName(), entry.isDirectory(), zip);
				zip.closeEntry();
			}
		}
		return finishArchive(parts);
	}

	private Project finishArchive(ArchiveParts parts) throws Exception {
		byte[] manifest = parts.manifest;
		byte[] projectXml = parts.projectXml;
		byte[] meta = parts.meta;
		byte[] settings = parts.settings;
		byte[] ccpmHistory = parts.ccpmHistory;
		byte[] layout = parts.layout;
		byte[] visibility = parts.visibility;
		byte[] draftCcpm = parts.draftCcpm;
		byte[] operations = parts.operations;
		byte[] draftOperations = parts.draftOperations;
		byte[] taskIdentities = parts.taskIdentities;
		byte[] mimetype = parts.mimetype;
		MpoExtensions extensions = parts.extensions;
		if (manifest == null || projectXml == null) {
			throw new IOException("An MPOF file must contain " + MANIFEST_ENTRY + " and " + PROJECT_ENTRY);
		}
		if (settings != null && draftCcpm != null) {
			throw new IOException("MPOF contains both current and draft CCPM settings");
		}
		boolean legacyMicroprojectLevelingDelay = meta != null && MpoManifest.isLegacyMicroprojectLevelingDelay(meta);
		boolean legacyMicroprojectAssignmentBaselineCost = meta != null
			&& MpoManifest.isLegacyMicroprojectAssignmentBaselineCost(meta);
		if (meta != null) MpoManifest.validateMeta(meta);
		ManifestData manifestData = MpoManifest.readManifest(manifest, projectXml);
		MpoManifest.validateArchiveChecksums(manifestData, mimetype, meta, settings, ccpmHistory, layout, visibility,
				draftCcpm, operations, draftOperations, taskIdentities, projectXml, extensions.entries);
		if (operations != null && draftOperations != null)
			throw new IOException("MPOF contains both current and draft operation logs");
		if (operations == null) operations = draftOperations;
		// Parse and bound untrusted semantic records before creating or mutating a
		// Project.  OperationLog itself predates these archive-level limits.
		OperationLog.DocumentLog operationLog = operations == null ? null
			: readBoundedOperationLog(operations, draftOperations == operations);
		if (operationLog != null && manifestData.documentId() != null
				&& !manifestData.documentId().equals(operationLog.documentId()))
			throw new IOException("MPOF operation log document identity does not match its manifest");
		java.util.Map<Long, Long> taskIdentityMap = taskIdentities == null ? null : MpoSidecarJson.readTaskIdentities(taskIdentities);
		if (ccpmHistory != null) validateCcpmHistory(ccpmHistory);
		// Freeze the validated read boundary before constructing the mutable model.
		// The plan is deliberately built from immutable snapshot data so malformed
		// archives fail before any Project state is changed.
		MpoArchiveSnapshot snapshot = MpoExternalMerge.snapshotOf(manifestData, projectXml, extensions.entries);
		MpoExternalMerge.validateMergePlan(MpoExternalMerge.planFor(snapshot, java.util.List.of(), java.util.List.of()));
		java.util.Map<String, SubProj.LoadStatus> embeddedProjectFailures = MpoEmbeddedProjects.validateEmbeddedProjects(manifestData, extensions.entries);
		if (legacyMicroprojectLevelingDelay) projectXml = MpoManifest.migrateLegacyLevelingDelays(projectXml);
		if (legacyMicroprojectAssignmentBaselineCost)
			projectXml = MpoManifest.migrateLegacyAssignmentBaselineCosts(projectXml);
		MicrosoftImporter delegate = new MicrosoftImporter();
		delegate.setFileName(PROJECT_ENTRY);
		delegate.setProjectFactory(projectFactory);
		project = delegate.loadProject(new ByteArrayInputStream(projectXml));
		if (manifestData.documentId() != null) try {
			project.setDocumentId(manifestData.documentId());
		} catch (IllegalArgumentException exception) {
			throw new IOException("Invalid MPOF document identity", exception);
		}
		if (manifestData.projectUniqueId() != null && manifestData.projectUniqueId().longValue() != 0L)
			project.setUniqueId(manifestData.projectUniqueId().longValue());
		// The MPO manifest is authoritative for the explicit/NA distinction.
		// The embedded MSP project XML may contain an effective date even when the
		// user selected NA, so clear the delegate's value before applying it.
		project.restoreStatusDate(manifestData.statusDate() == null ? 0L : manifestData.statusDate().longValue());
		if (manifestData.sharedResourcePoolPath() != null && !manifestData.sharedResourcePoolPath().isBlank())
			project.setSharedResourcePoolFile(manifestData.sharedResourcePoolPath());
		if (manifestData.sharedResourcePoolProjectId() != null && manifestData.sharedResourcePoolProjectId().longValue() != 0L)
			project.setSharedResourcePoolProjectId(manifestData.sharedResourcePoolProjectId().longValue());
		if (settings != null) {
			MpoManifest.restoreSettings(project, settings);
		} else if (draftCcpm != null) {
			restoreCcpm(project, new String(draftCcpm, StandardCharsets.UTF_8));
		}
		if (ccpmHistory != null) restoreCcpmHistory(project, ccpmHistory);
		if (layout != null) MpoSidecarJson.restoreLayout(project, layout);
		if (visibility != null) MpoSidecarJson.restoreVisibility(project, visibility);
		if (operationLog != null) {
			if (manifestData.documentId() == null) project.setDocumentId(operationLog.documentId());
			java.util.List<OperationLog.Operation> normalized = taskIdentities == null ? operationLog.operations()
				: remapTaskOperations(operationLog.operations(), taskIdentityMap);
			// The snapshot is authoritative: it already represents the advertised
			// document state.  The operation log is retained as collaboration history
			// for a later explicit merge, but must not be replayed during an ordinary
			// open or it can duplicate an already-materialized change.
			MpoOperationState state = project.getOrCreateTransientDocumentState(MpoOperationState.class, MpoOperationState::new);
			state.documentId = project.getDocumentId();
			state.operations.addAll(normalized);
			state.appliedOperationIds.addAll(operationLog.appliedOperationIds());
			normalized.forEach(operation -> state.loadedOperationIds.add(operation.id()));
			state.json = new OperationLog().writeJsonl(state.documentId, normalized, state.appliedOperationIds);
			state.capture(project);
		}
		if (!extensions.entries.isEmpty()) project.getOrCreateTransientDocumentState(MpoExtensions.class, MpoExtensions::new).entries.putAll(extensions.entries);
		MpoEmbeddedProjects.restoreEmbeddedProjectReferences(project, manifestData, extensions.entries, embeddedProjectFailures);
		restoreTaskExtensions(project, extensions.entries);
		return project;
	}

	/** Applies the task data MSPDI cannot hold; opening a file and copying a model must agree on it. */
	private static void restoreTaskExtensions(Project project, java.util.Map<String, byte[]> entries) throws IOException {
		if (entries.containsKey(TASK_SPLITS_ENTRY))
			MpoSidecarJson.restoreTaskSplits(project, entries.get(TASK_SPLITS_ENTRY));
		if (entries.containsKey(TASK_LEVELING_OPTIONS_ENTRY))
			MpoSidecarJson.restoreTaskLevelingOptions(project, entries.get(TASK_LEVELING_OPTIONS_ENTRY));
		else
			MpoSidecarJson.resetLegacyTaskLevelingDefaults(project);
		if (entries.containsKey(TASK_TEXT_STYLES_ENTRY))
			MpoSidecarJson.restoreTaskTextStyles(project, entries.get(TASK_TEXT_STYLES_ENTRY));
		if (entries.containsKey(TASK_SCHEDULES_ENTRY))
			MpoSidecarJson.restoreTaskSchedules(project, entries.get(TASK_SCHEDULES_ENTRY));
		if (entries.containsKey(BASELINE_PROFILES_ENTRY))
			MpoSidecarJson.restoreBaselineProfiles(project, entries.get(BASELINE_PROFILES_ENTRY));
		if (entries.containsKey(ASSIGNMENT_LEVELING_SPLITS_ENTRY))
			MpoSidecarJson.restoreAssignmentLevelingSplits(project, entries.get(ASSIGNMENT_LEVELING_SPLITS_ENTRY));
	}

	@Override
	public void exportFile() throws Exception {
		File target = new File(fileName);
		Path parent = target.toPath().toAbsolutePath().getParent();
		if (parent == null) {
			throw new IOException("MPO save target has no parent directory: " + target);
		}
		// Save As can target a newly created subfolder.  Create it before opening
		// the transaction lock; otherwise FileChannel.open reports only the
		// generic save error and the archive is never attempted.
		Files.createDirectories(parent);
		Path lockPath = target.toPath().toAbsolutePath().resolveSibling(target.getName() + ".lock");
		// OneDrive can start two desktop writers at nearly the same time.  Lock a
		// sidecar (rather than the atomically replaced mpo inode) so the
		// read/merge/write transaction is serialized across JVMs as well.  The
		// sidecar exists only for that transaction and is removed at its end.
		synchronized (EXPORT_LOCK_GUARD) {
			RetirableFileLock transactionLock;
			try {
				transactionLock = RetirableFileLock.acquire(lockPath, FileChannel::lock);
			} catch (IOException lockFailure) {
				throw persistLockFailureRecovery(target, lockFailure);
			}
			try (transactionLock) {
				try {
					cleanupAbandonedTemporaryFiles(target);
					exportFileLocked(target);
				} finally {
					transactionLock.retire();
				}
			}
		}
	}

	/** Best-effort cleanup of this MPO's generated stages left by a crashed writer. */
	private void cleanupAbandonedTemporaryFiles(File target) {
		Path absoluteTarget = target.toPath().toAbsolutePath();
		Path parent = absoluteTarget.getParent();
		if (parent == null) return;
		String prefix = target.getName() + ".";
		long staleBefore = System.currentTimeMillis() - java.time.Duration.ofDays(1).toMillis();
		try (java.util.stream.Stream<Path> siblings = Files.list(parent)) {
			for (Path sibling : siblings.toList()) {
				String name = sibling.getFileName().toString();
				if (!name.startsWith(prefix) || !name.endsWith(".tmp")) continue;
				String generatedSuffix = name.substring(prefix.length(), name.length() - ".tmp".length());
				if (generatedSuffix.length() < 3
						|| !generatedSuffix.chars().allMatch(Character::isLetterOrDigit)) continue;
				try {
					if (Files.isRegularFile(sibling, LinkOption.NOFOLLOW_LINKS)
							&& Files.getLastModifiedTime(sibling, LinkOption.NOFOLLOW_LINKS).toMillis() < staleBefore)
						Files.deleteIfExists(sibling);
				} catch (IOException ignored) {
					// A stale artifact must not prevent a valid save when cleanup is denied.
				}
			}
		} catch (IOException ignored) {
			// Directory listing is best-effort; the transaction itself remains authoritative.
		}
	}

	private MpoConflictRecoveryException persistLockFailureRecovery(File target, IOException lockFailure) throws IOException {
		MpoCapture captured = captureLiveModel(project);
		MpoOperationState operationState = captured.operationState();
		Path recoveryCopy = persistRecoveryCopy(target, captured, operationState,
				new java.util.ArrayList<>(operationState.operations), null);
		return MpoConflictRecoveryException.lockUnavailable(recoveryCopy, lockFailure);
	}

	/**
	 * The one save protocol shared by ordinary saves, recovery snapshots and
	 * merges: capture the live model, build and replace the archive from that
	 * detached capture with no model barrier held, then commit only if the live
	 * model still equals what was captured.
	 */
	private void exportFileLocked(File target) throws Exception {
		committed = false;
		MpoCapture captured = captureLiveModel(project);
		MpoOperationState operationState = captured.operationState();
		MpoMergePreparation merge = target.isFile() && target.length() > 0L
				? prepareExternalMerge(target, captured, operationState) : null;
		MpoCapture output = captured;
		if (merge != null) {
			// Apply concurrent operations only to an isolated candidate.  The live
			// Project and its operation state are committed after the archive replace.
			Project outputProject = copyOfCapture(captured);
			applyMergedOperationsOnEdt(outputProject, merge.replayOperations(), merge.alreadyAppliedOperationIds());
			merge.applyExtensions(outputProject);
			operationState.operations.clear();
			operationState.operations.addAll(merge.mergedOperations());
			operationState.appliedOperationIds.clear();
			operationState.appliedOperationIds.addAll(merge.appliedOperationIds());
			operationState.json = new OperationLog().writeJsonl(operationState.documentId, operationState.operations,
					operationState.appliedOperationIds);
			// Merging may apply operations from a concurrent editor.  Serialize only
			// after that merge so content.xml and the journal describe the same state.
			output = captureModel(outputProject, MpoSidecarJson.serializeProject(outputProject));
			operationState.remapTaskIds(MpoSidecarJson.readTaskIdentities(output.entries().get(TASK_IDENTITIES_ENTRY)));
		}
		File temporary = createTemporaryFile(target);
		boolean completed = false;
		try (OutputStream out = new FileOutputStream(temporary)) {
			MpoExternalMerge.writeCapture(output, out, operationState);
			completed = true;
		} finally {
			if (!completed) {
				TemporaryCleanupQueue.deleteOrEnqueue(temporary.toPath());
			}
		}
		boolean moved = false;
		try {
			if (merge != null) {
				// Validate before replacement; failures must also clean up the stage.
				verifyMergeApplicability(project, merge.replayOperations(), merge.alreadyAppliedOperationIds());
			}
			moveTemporary(temporary.toPath(), target.toPath());
			moved = true;
		} finally {
			// A preflight or replacement failure must not leave a staged archive.
			// The destination was never touched when moveTemporary fails.
			if (!moved) TemporaryCleanupQueue.deleteOrEnqueue(temporary.toPath());
		}
		boolean appliesExternalOperations = merge != null && merge.replayOperations().stream()
				.anyMatch(operation -> !merge.alreadyAppliedOperationIds().contains(operation.id()));
		// Serialization can take seconds, so the full comparison stays off the EDT.
		verifyLiveModelUnchanged(target, captured);
		IoAction commit = () -> commitReplacedArchive(target, merge, operationState, appliesExternalOperations);
		if (appliesExternalOperations) runOnEdt(commit);
		else commit.run();
	}

	private void commitReplacedArchive(File target, MpoMergePreparation merge,
			MpoOperationState plan, boolean appliesExternalOperations) throws IOException {
		// Remote operations only touch fields the journal tracks, so this cheap check
		// on the same EDT step keeps them from overwriting an edit made just before.
		if (hasUnjournaledChanges(project))
			throw new MpoPartialApplyException(target.toPath(),
					new IOException("The project changed while it was being saved"));
		if (merge != null) {
			// This is deterministic after the isolated candidate succeeded.  No
			// mutation occurs on any read/write/move failure above.  If the final
			// in-memory phase fails, preserve the detached operation state and
			// expose a retryable, explicit partial-apply result.
			try {
				if (appliesExternalOperations)
					applyMergedOperationsOnEdt(project, merge.replayOperations(), merge.alreadyAppliedOperationIds());
				merge.applyExtensions(project);
			} catch (RuntimeException | IOException exception) {
				throw new MpoPartialApplyException(target.toPath(), exception);
			}
		}
		// The baseline describes the live model, never the MSPDI copy, whose values
		// can round differently and would then look like an edit on the next save.
		if (appliesExternalOperations) plan.capture(project);
		MpoExternalMerge.commitOperationState(project, plan);
		committed = true;
	}

	/** Compares the model with the journal baseline; cheap enough for the EDT. */
	private static boolean hasUnjournaledChanges(Project project) throws IOException {
		MpoOperationState probe = MpoExternalMerge.workingOperationStateFor(project);
		int before = probe.operations.size();
		probe.appendChanges(project);
		return probe.operations.size() != before;
	}

	private void verifyLiveModelUnchanged(File target, MpoCapture captured) throws IOException {
		String current;
		try {
			current = fingerprintOf(project);
		} catch (RuntimeException | IOException unreadable) {
			// A structural edit can invalidate the read itself.
			throw new MpoPartialApplyException(target.toPath(), unreadable);
		}
		if (!captured.fingerprint().equals(current))
			throw new MpoPartialApplyException(target.toPath(),
					new IOException("The project changed while it was being saved"));
	}

	/**
	 * Reads one stable generation of the live model and reserves its operation
	 * IDs.  No lock is held while reading, because serialization can take seconds
	 * and edits on the EDT must not wait for it.  The archive content itself is
	 * read twice and compared instead, so an edit through any setter is detected
	 * and no list of writers has to be kept complete.
	 */
	private static MpoCapture captureLiveModel(Project project) throws IOException {
		Exception last = null;
		for (int attempt = 0; attempt < CAPTURE_ATTEMPTS; attempt++) {
			try {
				MpoCapture captured = captureOnce(project);
				if (captured != null) return captured;
			} catch (UncheckedIOException exception) {
				throw exception.getCause();
			} catch (RuntimeException concurrentEdit) {
				// Reading a list while the EDT restructures it fails rather than tears.
				last = concurrentEdit;
			}
		}
		throw new IOException("The project kept changing while it was being saved; save again", last);
	}

	/** @return the capture, or {@code null} when the model changed while it was read */
	private static MpoCapture captureOnce(Project project) throws IOException {
		MpoCapture captured = captureModel(project, MpoSidecarJson.serializeProject(project));
		java.util.Map<Long, Long> taskIdentities = MpoSidecarJson.readTaskIdentities(captured.entries().get(TASK_IDENTITIES_ENTRY));
		MpoOperationState reservation = ProjectMutationState.coordinated(() -> {
			try {
				MpoOperationState working = MpoExternalMerge.workingOperationStateFor(project);
				working.appendChanges(project);
				ensureOperationCount(working.operations);
				return working;
			} catch (IOException exception) {
				throw new UncheckedIOException(exception);
			}
		});
		if (!captured.fingerprint().equals(fingerprintOf(project))) return null;
		reservation.remapTaskIds(taskIdentities);
		// Reserved on the live journal so a failed write or replacement retries with
		// the same operation IDs, and a later edit becomes a separate operation.
		MpoExternalMerge.commitOperationState(project, reservation);
		return new MpoCapture(captured.serialized(), captured.entries(), captured.metadata(), captured.fingerprint(),
				reservation.copy());
	}

	@FunctionalInterface
	private interface IoAction {
		void run() throws IOException;
	}

	private static void runOnEdt(IoAction action) throws IOException {
		if (SwingUtilities.isEventDispatchThread()) {
			action.run();
			return;
		}
		try {
			SwingUtilities.invokeAndWait(() -> {
				try {
					action.run();
				} catch (IOException exception) {
					throw new UncheckedIOException(exception);
				}
			});
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new IOException("Interrupted while applying merged MPO operations", exception);
		} catch (java.lang.reflect.InvocationTargetException exception) {
			if (exception.getCause() instanceof UncheckedIOException ioException)
				throw ioException.getCause();
			throw new IOException("Could not apply merged MPO operations", exception.getCause());
		}
	}

	@Override
	public void setReplaceDifferentDocument(boolean replaceDifferentDocument) {
		this.replaceDifferentDocument = replaceDifferentDocument;
	}

	/** Runs on the EDT with the dirty-flag update, so it uses only the cheap check. */
	@Override
	public boolean confirmSavedState() {
		if (!committed) return false;
		try {
			return !hasUnjournaledChanges(project);
		} catch (RuntimeException | IOException exception) {
			return false;
		}
	}

	protected void verifyMergeApplicability(Project source, java.util.List<OperationLog.Operation> operations,
			java.util.Set<String> alreadyAppliedOperationIds) throws IOException {
		Project preflight = copyProjectForTransaction(source, MpoSidecarJson.serializeProjectXml(source));
		// Preflight is an internal validation pass.  Keep it independent from
		// the overridable commit-stage seam so an injected commit failure cannot
		// accidentally make the archive replacement happen later than intended.
		applyMergedOperationsOnEdtInternal(preflight, operations, alreadyAppliedOperationIds);
	}

	/** Test seam for proving that a failed atomic replacement is non-destructive. */
	protected File createTemporaryFile(File target) throws IOException {
		return File.createTempFile(target.getName() + ".", ".tmp", target.getAbsoluteFile().getParentFile());
	}

	/** Replaces the destination only after the complete archive has been written. */
	protected void moveTemporary(Path temporary, Path target) throws IOException {
		SafeFileReplace.replaceDurably(temporary, target, true);
	}

	@Override
	public boolean saveProject(Project project, OutputStream out) throws Exception {
		MpoCapture captured = captureLiveModel(project);
		MpoExternalMerge.writeCapture(captured, out, captured.operationState());
		if (!captured.fingerprint().equals(fingerprintOf(project)))
			throw new IOException("The project changed while it was being saved");
		return true;
	}

	private static String fingerprintOf(Project project) throws IOException {
		return captureModel(project, MpoSidecarJson.serializeProject(project)).fingerprint();
	}

	/** Reads everything an archive takes from the model, into detached bytes. */
	private static MpoCapture captureModel(Project project, SerializedProjectXml serialized) throws IOException {
		byte[] projectXml = serialized.xml();
		java.util.LinkedHashMap<String, byte[]> entries = new java.util.LinkedHashMap<>();
		CriticalChainService.Settings ccpm = existingCcpm(project);
		entries.put(SETTINGS_ENTRY, MpoManifest.settingsXml(ccpm, ccpm == null ? null : new CriticalChainService().findBaseline(project)).getBytes(StandardCharsets.UTF_8));
		byte[] layout = MpoSidecarJson.layoutJson(project);
		if (layout != null) entries.put(LAYOUT_ENTRY, layout);
		// Filled from the operation plan when written; the journal is not model content.
		entries.put(OPERATIONS_ENTRY, new byte[0]);
		String taskIdentities = MpoSidecarJson.taskIdentitiesFor(project, projectXml);
		entries.put(TASK_IDENTITIES_ENTRY, taskIdentities.getBytes(StandardCharsets.UTF_8));
		entries.put(VISIBILITY_ENTRY, MpoSidecarJson.visibilityJson(project, taskIdentities));
		entries.put(TASK_SPLITS_ENTRY, MpoSidecarJson.taskSplitsJson(project, taskIdentities));
		entries.put(TASK_LEVELING_OPTIONS_ENTRY, MpoSidecarJson.taskLevelingOptionsJson(project, taskIdentities));
		entries.put(TASK_TEXT_STYLES_ENTRY, MpoSidecarJson.taskTextStylesJson(project, taskIdentities));
		entries.put(TASK_SCHEDULES_ENTRY, MpoSidecarJson.taskSchedulesJson(project, taskIdentities));
		entries.put(BASELINE_PROFILES_ENTRY, MpoSidecarJson.baselineProfilesJson(project, taskIdentities,
			MpoSidecarJson.resourceIdentitiesForMpo(project)));
		entries.put(ASSIGNMENT_LEVELING_SPLITS_ENTRY, MpoSidecarJson.assignmentLevelingSplitsJson(project, serialized.assignmentUids()));
		entries.put(CCPM_HISTORY_ENTRY, ccpmHistoryJson(project));
		MpoExtensions extensions = project.findTransientDocumentState(MpoExtensions.class);
		if (extensions != null) for (java.util.Map.Entry<String, byte[]> extension : extensions.entries.entrySet()) {
			if (MIMETYPE_ENTRY.equals(extension.getKey()) || MANIFEST_ENTRY.equals(extension.getKey()) || META_ENTRY.equals(extension.getKey()) || SETTINGS_ENTRY.equals(extension.getKey()) || CCPM_HISTORY_ENTRY.equals(extension.getKey()) || LAYOUT_ENTRY.equals(extension.getKey()) || VISIBILITY_ENTRY.equals(extension.getKey()) || TASK_SPLITS_ENTRY.equals(extension.getKey()) || TASK_LEVELING_OPTIONS_ENTRY.equals(extension.getKey()) || TASK_TEXT_STYLES_ENTRY.equals(extension.getKey()) || TASK_SCHEDULES_ENTRY.equals(extension.getKey()) || BASELINE_PROFILES_ENTRY.equals(extension.getKey()) || ASSIGNMENT_LEVELING_SPLITS_ENTRY.equals(extension.getKey()) || PROJECT_ENTRY.equals(extension.getKey()) || OPERATIONS_ENTRY.equals(extension.getKey()) || TASK_IDENTITIES_ENTRY.equals(extension.getKey()) || extension.getKey().startsWith(EMBEDDED_PROJECT_PREFIX)) continue;
			entries.put(extension.getKey(), extension.getValue().clone());
		}
		MpoDocumentMetadata metadata = new MpoDocumentMetadata(project.getDocumentId(), project.getUniqueId(),
				String.valueOf(project.getName()), project.getSharedResourcePoolFile(),
				project.getSharedResourcePoolProjectId(), project.isStatusDateSet() ? project.getStatusDate() : 0L,
				MpoEmbeddedProjects.embeddedSourcesFor(project));
		return new MpoCapture(serialized, entries, metadata, fingerprint(projectXml, entries, metadata), null);
	}

	/** Digest of all model content an archive holds; the journal is excluded. */
	private static String fingerprint(byte[] projectXml, java.util.Map<String, byte[]> entries, MpoDocumentMetadata metadata) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			digest.update(metadata.toString().getBytes(StandardCharsets.UTF_8));
			// The serializer stamps the wall clock here; that is not model content.
			digest.update(SERIALIZER_CLOCK.matcher(new String(projectXml, StandardCharsets.UTF_8)).replaceAll("")
				.getBytes(StandardCharsets.UTF_8));
			for (java.util.Map.Entry<String, byte[]> entry : entries.entrySet()) {
				digest.update((byte) 0);
				digest.update(entry.getKey().getBytes(StandardCharsets.UTF_8));
				digest.update((byte) 0);
				digest.update(entry.getValue());
			}
			return java.util.HexFormat.of().formatHex(digest.digest());
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is required by the Java runtime", exception);
		}
	}

	/** Writes a detached capture; only linked files are read, and no model state. */
	static void ensureOperationCount(java.util.Collection<OperationLog.Operation> operations) throws IOException {
		if (operations != null && operations.size() > MAX_OPERATION_COUNT)
			throw new IOException("MPOF operation log exceeds " + MAX_OPERATION_COUNT + " operations");
	}

	/** Creates a detached operation-state plan.  Callers commit it only after
	 * their output transaction has completed successfully. */
	/** The candidate written to an existing file: the document as reopening the capture would show it. */
	private Project copyOfCapture(MpoCapture captured) throws IOException {
		Project copy = copyProjectForTransaction(project, captured.serialized().xml());
		MpoSidecarJson.restoreVisibility(copy, captured.entries().get(VISIBILITY_ENTRY));
		restoreTaskExtensions(copy, captured.entries());
		return copy;
	}

	private Project copyProjectForTransaction(Project source, byte[] xml) throws IOException {
		MicrosoftImporter delegate = new MicrosoftImporter();
		delegate.setFileName(PROJECT_ENTRY);
		delegate.setProjectFactory(projectFactory == null ? ProjectFactory.getInstance() : projectFactory);
		try (InputStream in = new ByteArrayInputStream(xml)) {
			Project copy = delegate.loadProject(in);
			copy.setDocumentId(source.getDocumentId());
			copy.setUniqueId(source.getUniqueId());
			copy.setFileName(source.getFileName());
			copy.setMaster(source.isMaster());
			copy.setLocal(source.isLocal());
			copy.setReadOnly(source.isReadOnly());
			copy.setSharedResourcePoolFile(source.getSharedResourcePoolFile());
			copy.setSharedResourcePoolProjectId(source.getSharedResourcePoolProjectId());
			copy.setFieldArray(source.getFieldArray());
			CriticalChainService service = new CriticalChainService();
			CriticalChainService.Settings settings = service.findSettings(source);
			if (settings != null)
				copy.getOrCreateTransientDocumentState(CriticalChainService.Settings.class, settings::copy);
			CriticalChainService.Baseline baseline = service.findBaseline(source);
			if (baseline != null)
				copy.getOrCreateTransientDocumentState(CriticalChainService.Baseline.class,
					() -> new CriticalChainService.Baseline(baseline.projectFinishMillis(), baseline.projectBufferMillis(),
						baseline.bufferFraction(), baseline.criticalTaskIds(), baseline.feedingTaskStartMillis(),
						baseline.feedingBufferMillis(), baseline.allResources(), baseline.resourceIds()));
			CriticalChainBufferHistory history = source.findTransientDocumentState(CriticalChainBufferHistory.class);
			if (history != null) {
				CriticalChainBufferHistory clonedHistory = copy.getOrCreateTransientDocumentState(
					CriticalChainBufferHistory.class, CriticalChainBufferHistory::new);
				for (CriticalChainBufferHistory.Point point : history.points()) clonedHistory.add(point);
				for (CriticalChainBufferHistory.Retraction retraction : history.retractions()) clonedHistory.recordRetraction(retraction);
			}
			MpoExtensions extensions = source.findTransientDocumentState(MpoExtensions.class);
			if (extensions != null) {
				MpoExtensions cloned = copy.getOrCreateTransientDocumentState(MpoExtensions.class, MpoExtensions::new);
				for (java.util.Map.Entry<String, byte[]> entry : extensions.entries.entrySet())
					cloned.entries.put(entry.getKey(), entry.getValue().clone());
			}
			return copy;
		} catch (Exception exception) {
			if (exception instanceof IOException io) throw io;
			throw new IOException("Unable to create isolated MPO transaction project", exception);
		}
	}

	static java.util.List<OperationLog.Operation> remapTaskOperations(java.util.List<OperationLog.Operation> operations, java.util.Map<Long, Long> identities) {
		java.util.List<OperationLog.Operation> result = new java.util.ArrayList<>(operations.size());
		for (OperationLog.Operation operation : operations) {
			java.util.Map<String, Object> payload = new java.util.LinkedHashMap<>(operation.payload());
			for (String key : java.util.List.of("legacyUniqueId", "parentLegacyUniqueId", "predecessorLegacyUniqueId", "successorLegacyUniqueId", "taskLegacyUniqueId")) {
				Object raw = payload.get(key);
				if (raw instanceof Number) {
					Long mapped = identities.get(Long.valueOf(((Number) raw).longValue()));
					if (mapped != null) payload.put(key, mapped);
				}
			}
			result.add(new OperationLog.Operation(operation.id(), operation.actorId(), operation.sequence(), operation.parents(), operation.kind(), operation.entityId(), payload));
		}
		return result;
	}

	private MpoMergePreparation prepareExternalMerge(File target, MpoCapture captured, MpoOperationState local) throws IOException {
		ExternalMpo external = MpoExternalMerge.externalOperations(target);
		// An explicit Save As over another document replaces it; there is nothing to merge.
		if (replaceDifferentDocument && !(local.documentId.equals(external.document().documentId())
				&& local.documentId.equals(external.manifestDocumentId()))) return null;
		if (!local.documentId.equals(external.document().documentId())) throw new IOException("Cannot merge mpo files with different document IDs");
		if (!local.documentId.equals(external.manifestDocumentId())) throw new IOException("Cannot merge mpo with mismatched manifest document ID");
		// The numeric Project unique id is an in-memory/importer identifier and is
		// intentionally not used as a collaboration identity: MSPDI reloads may
		// allocate a different value for the same document.  The stable mpo
		// documentId above is the authoritative identity at this boundary.
		java.util.List<OperationLog.Operation> all = new java.util.ArrayList<OperationLog.Operation>(local.operations);
		java.util.Set<String> locallyAppliedOperationIds = new java.util.LinkedHashSet<String>(local.appliedOperationIds);
		all.addAll(external.taskIdentities() == null ? external.document().operations() : remapTaskOperations(external.document().operations(), MpoSidecarJson.readTaskIdentities(external.taskIdentities())));
		if (all.size() > MAX_OPERATION_COUNT)
			throw new IOException("MPOF merged operation log exceeds " + MAX_OPERATION_COUNT + " operations");
		try {
			OperationLog.MergeResult merged = new OperationLog().merge(all);
			// New operations carry no parents and each session has its own actor, so the log alone
			// reports an edit as concurrent with the history it was made on top of.
			java.util.List<OperationLog.Conflict> conflicts = merged.conflicts().stream()
					.filter(conflict -> conflict.operationIds().stream().noneMatch(local.loadedOperationIds::contains))
					.toList();
			java.util.List<MpoMergePlan.Conflict> planConflicts = conflicts.stream()
					.map(conflict -> new MpoMergePlan.Conflict(conflict.entityId(),
							"Concurrent " + conflict.kind() + " operations: " + conflict.operationIds()))
					.toList();
			java.util.List<MpoMergePlan.Operation> plannedOperations = merged.ready().stream()
					.filter(operation -> external.document().appliedOperationIds().contains(operation.id()))
					.filter(operation -> !locallyAppliedOperationIds.contains(operation.id()))
					.map(operation -> new MpoMergePlan.Operation(operation.id(), MpoMergePlan.Action.UPDATE))
					.toList();
			MpoMergePlan plan = MpoExternalMerge.planFor(external.snapshot(), plannedOperations, planConflicts);
			if (!conflicts.isEmpty()) {
				java.util.List<OperationLog.Operation> conflictOperations = new java.util.ArrayList<>(merged.ready());
				conflictOperations.addAll(merged.pending());
				Path recoveryCopy = persistRecoveryCopy(target, captured, local, conflictOperations,
						external.extensions());
				throw new MpoConflictRecoveryException(recoveryCopy, conflicts);
			}
			MpoExternalMerge.validateMergePlan(plan);
			// The snapshot already contains every local operation.  Replaying that
			// history is not idempotent for moves (the original parent may no longer
			// exist), so apply only operations introduced by the external archive.
		// A causally dependent external operation still sees its local parent in
			// the current snapshot.
			java.util.Set<String> mergedAppliedIds = new java.util.LinkedHashSet<String>(locallyAppliedOperationIds);
			mergedAppliedIds.addAll(external.document().appliedOperationIds());
			java.util.List<OperationLog.Operation> replayOperations = merged.ready().stream()
					.filter(operation -> mergedAppliedIds.contains(operation.id()))
					.toList();
			java.util.List<OperationLog.Operation> mergedOperations = new java.util.ArrayList<>(merged.ready());
			mergedOperations.addAll(merged.pending());
			return new MpoMergePreparation(replayOperations, mergedOperations, locallyAppliedOperationIds,
					mergedAppliedIds, external.extensions(), plan);
		} catch (IllegalArgumentException exception) {
			throw new IOException("Cannot merge conflicting mpo operation logs", exception);
		}
	}

	private Path persistRecoveryCopy(File target, MpoCapture captured, MpoOperationState local,
			java.util.List<OperationLog.Operation> mergedOperations, MpoExtensions externalExtensions) throws IOException {
		Project recoveryProject = copyOfCapture(captured);
		if (externalExtensions != null && !externalExtensions.entries.isEmpty()) {
			MpoExtensions recoveryExtensions = recoveryProject.getOrCreateTransientDocumentState(MpoExtensions.class,
					MpoExtensions::new);
			for (java.util.Map.Entry<String, byte[]> entry : externalExtensions.entries.entrySet())
				recoveryExtensions.entries.putIfAbsent(entry.getKey(), entry.getValue().clone());
		}
		MpoOperationState recoveryState = local.copy();
		recoveryState.operations.clear();
		recoveryState.operations.addAll(mergedOperations);
		try {
			recoveryState.json = new OperationLog().writeJsonl(recoveryState.documentId,
				recoveryState.operations, recoveryState.appliedOperationIds);
			MpoCapture recoveryCapture = captureModel(recoveryProject, MpoSidecarJson.serializeProject(recoveryProject));
			recoveryState.remapTaskIds(MpoSidecarJson.readTaskIdentities(recoveryCapture.entries().get(TASK_IDENTITIES_ENTRY)));
			File temporary = createTemporaryFile(target);
			boolean moved = false;
			try {
				try (OutputStream out = new FileOutputStream(temporary)) {
					MpoExternalMerge.writeCapture(recoveryCapture, out, recoveryState);
				}
				Path recovery = target.toPath().toAbsolutePath().resolveSibling(target.getName()
					+ ".conflict-" + java.util.UUID.randomUUID() + ".mpo");
				SafeFileReplace.replaceDurably(temporary.toPath(), recovery, false);
				moved = true;
				return recovery;
			} finally {
				if (!moved) TemporaryCleanupQueue.deleteOrEnqueue(temporary.toPath());
			}
		} catch (Exception exception) {
			if (exception instanceof IOException ioException) throw ioException;
			throw new IOException("Could not create MPOF conflict recovery copy", exception);
		}
	}

	@Override
	public Job getImportFileJob() {
		return job("importFile", Messages.getString("MicrosoftImporter.Importing"), new JobRunnable("Import mpo", 1.0f) {
			public Object run() throws Exception {
				importFile();
				setProgress(1.0f);
				return null;
			}
		});
	}

	@Override
	public Job getExportFileJob() {
		return job("exportFile", Messages.getString("LocalFileImporter.Exporting"), new JobRunnable("Export mpo", 1.0f) {
			public Object run() throws Exception {
				exportFile();
				setProgress(1.0f);
				return null;
			}
		});
	}

	private Job job(String name, String title, JobRunnable runnable) {
		Job job = new Job(SessionFactory.getInstance().getLocalSession().getJobQueue(), name, title, true);
		job.addRunnable(runnable);
		return job;
	}

	/** Reads an operation log and enforces archive-specific semantic limits. */
	static OperationLog.DocumentLog readBoundedOperationLog(byte[] bytes, boolean draft)
			throws IOException {
		Objects.requireNonNull(bytes, "operation log");
		if (!draft) validateJsonlOperationShape(bytes);
		OperationLog.DocumentLog result = draft ? new OperationLog().readDocument(bytes)
			: new OperationLog().readJsonl(bytes);
		if (result.operations().size() > MAX_OPERATION_COUNT)
			throw new IOException("MPOF operation log exceeds " + MAX_OPERATION_COUNT + " operations");
		for (OperationLog.Operation operation : result.operations()) {
			if (operation.parents().size() > MAX_OPERATION_PARENTS)
				throw new IOException("MPOF operation has too many parents");
			if (operation.payload().size() > MAX_OPERATION_PAYLOAD_FIELDS)
				throw new IOException("MPOF operation payload has too many fields");
			validateStringLength(operation.id(), "operation id");
			validateStringLength(operation.actorId(), "operation actor");
			validateStringLength(operation.entityId(), "operation entity");
			validateStringLength(operation.kind(), "operation kind");
			for (String parent : operation.parents()) validateStringLength(parent, "operation parent");
			validateJsonValue(operation.payload(), 0, "operation payload");
		}
		return result;
	}

	/** Performs a bounded line-by-line pass before OperationLog allocates its list. */
	private static void validateJsonlOperationShape(byte[] bytes) throws IOException {
		int count = 0;
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(
				new ByteArrayInputStream(bytes), StandardCharsets.UTF_8))) {
			String line;
			boolean first = true;
			while ((line = reader.readLine()) != null) {
				if (line.length() > MAX_ENTRY_BYTES)
					throw new IOException("MPOF operation record is too large");
				if (first) {
					first = false;
					continue;
				}
				if (line.isBlank()) throw new IOException("Invalid blank JSONL operation record");
				if (++count > MAX_OPERATION_COUNT)
					throw new IOException("MPOF operation log exceeds " + MAX_OPERATION_COUNT + " operations");
				JsonNode value = JSON.readTree(line);
				if (value == null || !value.isObject() || value.has("type"))
					throw new IOException("Invalid JSONL operation record");
				validateOperationNodeShape(value);
			}
			if (first) throw new IOException("Invalid JSONL operation log header");
		} catch (IOException exception) {
			throw exception;
		}
	}

	private static void validateOperationNodeShape(JsonNode value) throws IOException {
		JsonNode parents = value.get("parents");
		if (parents != null && (!parents.isArray() || parents.size() > MAX_OPERATION_PARENTS))
			throw new IOException("MPOF operation has too many parents");
		JsonNode payload = value.get("payload");
		if (payload != null && (!payload.isObject() || payload.size() > MAX_OPERATION_PAYLOAD_FIELDS))
			throw new IOException("MPOF operation payload has too many fields");
		validateJsonNode(value, 0, "operation");
	}

	private static void validateJsonNode(JsonNode value, int depth, String description) throws IOException {
		if (value == null) return;
		if (depth > MAX_JSON_NESTING) throw new IOException("MPOF " + description + " is too deeply nested");
		if (value.isTextual()) {
			validateStringLength(value.textValue(), description);
		} else if (value.isObject()) {
			if (value.size() > MAX_OPERATION_PAYLOAD_FIELDS)
				throw new IOException("MPOF " + description + " has too many fields");
			java.util.Iterator<Map.Entry<String, JsonNode>> fields = value.fields();
			while (fields.hasNext()) {
				Map.Entry<String, JsonNode> field = fields.next();
				validateStringLength(field.getKey(), description + " field name");
				validateJsonNode(field.getValue(), depth + 1, description + "." + field.getKey());
			}
		} else if (value.isArray()) {
			if (value.size() > MAX_OPERATION_COUNT)
				throw new IOException("MPOF " + description + " array is too large");
			for (JsonNode child : value) validateJsonNode(child, depth + 1, description + "[]");
		}
	}

	private static void validateJsonValue(Object value, int depth, String description) throws IOException {
		if (value == null) return;
		if (depth > MAX_JSON_NESTING) throw new IOException("MPOF " + description + " is too deeply nested");
		if (value instanceof String string) {
			validateStringLength(string, description);
		} else if (value instanceof Map<?, ?> map) {
			if (map.size() > MAX_OPERATION_PAYLOAD_FIELDS)
				throw new IOException("MPOF " + description + " has too many fields");
			for (Map.Entry<?, ?> entry : map.entrySet()) {
				if (entry.getKey() != null) validateStringLength(String.valueOf(entry.getKey()), description + " field name");
				validateJsonValue(entry.getValue(), depth + 1, description);
			}
		} else if (value instanceof Iterable<?> iterable) {
			int count = 0;
			for (Object child : iterable) {
				if (++count > MAX_OPERATION_COUNT) throw new IOException("MPOF " + description + " array is too large");
				validateJsonValue(child, depth + 1, description + "[]");
			}
		}
	}

	private static void validateStringLength(String value, String description) throws IOException {
		if (value != null && value.length() > MAX_JSON_STRING_CHARS)
			throw new IOException("MPOF " + description + " is too long");
	}

	static void validateCcpmHistory(byte[] bytes) throws IOException {
		int count = 0;
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(
				new ByteArrayInputStream(bytes), StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.isBlank()) continue;
				if (line.length() > MAX_CCPM_HISTORY_LINE_CHARS)
					throw new IOException("MPOF CCPM history entry is too large");
				if (++count > MAX_CCPM_HISTORY_ENTRIES)
					throw new IOException("MPOF CCPM history exceeds " + MAX_CCPM_HISTORY_ENTRIES + " entries");
				JsonNode value = JSON.readTree(line);
				if (value == null || !value.isObject()) throw new IOException("Invalid CCPM history entry");
				validateJsonNode(value, 0, "CCPM history");
				validateCcpmHistoryEntry(value, line);
			}
		}
	}

	private static java.util.UUID historyEntryId(JsonNode value, String source) {
		return value.hasNonNull("id") ? java.util.UUID.fromString(value.path("id").asText())
			: java.util.UUID.nameUUIDFromBytes(source.getBytes(StandardCharsets.UTF_8));
	}

	private static CriticalChainBufferHistory.Retraction retractionFrom(JsonNode value, java.util.UUID id) {
		return new CriticalChainBufferHistory.Retraction(id,
			Instant.parse(value.path("retractedAt").asText()), value.path("actorId").asText("unknown"),
			value.path("actorName").asText("unknown"), value.path("reason").asText());
	}

	private static CriticalChainBufferHistory.Point pointFrom(JsonNode value, java.util.UUID id) {
		return new CriticalChainBufferHistory.Point(id, Instant.parse(value.path("observedAt").asText()),
			value.path("actorId").asText("unknown"), value.path("actorName").asText("unknown"),
			value.path("progressPercent").asDouble(), value.path("consumptionPercent").asDouble(),
			value.path("zone").asText("UNKNOWN"), value.path("baselineId").asText(""));
	}

	private static void validateCcpmHistoryEntry(JsonNode value, String source) throws IOException {
		try {
			java.util.UUID id = historyEntryId(value, source);
			if ("retraction".equals(value.path("kind").asText())) retractionFrom(value, id);
			else pointFrom(value, id);
		} catch (RuntimeException exception) {
			throw new IOException("Invalid CCPM history entry", exception);
		}
	}

	static byte[] readEntry(InputStream in, int[] totalBytes) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buffer = new byte[8192];
		int total = 0;
		int read;
		while ((read = in.read(buffer)) != -1) {
			total += read;
			if (total > MAX_ENTRY_BYTES) {
				throw new IOException("mpo entry exceeds " + MAX_ENTRY_BYTES + " bytes");
			}
			totalBytes[0] += read;
			if (totalBytes[0] > MAX_TOTAL_BYTES) throw new IOException("mpo exceeds " + MAX_TOTAL_BYTES + " bytes");
			out.write(buffer, 0, read);
		}
		return out.toByteArray();
	}

	static void writeEntry(ZipOutputStream zip, String name, byte[] data) throws IOException {
		ZipEntry entry = new ZipEntry(name);
		entry.setTime(0L);
		zip.putNextEntry(entry);
		zip.write(data);
		zip.closeEntry();
	}

	/** ODF convention: the first ZIP entry is an uncompressed {@code mimetype}. */
	static void writeMimetypeEntry(ZipOutputStream zip) throws IOException {
		ZipEntry entry = new ZipEntry(MIMETYPE_ENTRY);
		entry.setTime(0L);
		zip.putNextEntry(entry);
		zip.write(MpoFormatIdentifiers.WRITE_AND_READ_MIME_TYPE.getBytes(StandardCharsets.US_ASCII));
		zip.closeEntry();
	}

	/**
	 * Accepts any MPOF container whose major version equals {@link #SUPPORTED_FORMAT_MAJOR}.
	 * Files written by an older or newer minor revision load unchanged and are rewritten at
	 * {@link #FORMAT_VERSION} the next time they are saved, so opening and saving upgrades a
	 * file to the current layout (issue #356). A different major version cannot be read
	 * safely and is reported as such instead of as a generic invalid-manifest error.
	 */
	/** Linked files to embed, read from the model without touching the disk. */
	/**
	 * The master snapshot remains authoritative when one optional embedded child
	 * is damaged.  Structural manifest defects still fail the archive, while
	 * absent or tampered child payloads are reported on their corresponding
	 * master reference so every other project can be opened and repaired.
	 */
	/** Operation replay mutates tasks and can repaint an open spreadsheet, so it must use the EDT. */
	/**
	 * Applies the already validated domain phase of an MPOF transaction.  This
	 * is deliberately a protected stage seam: callers cannot pass a mutable
	 * archive into it, while transaction tests can inject a transient failure
	 * and prove that a subsequent export retries the apply phase.
	 */
	/** Applies a merged history while skipping operations already present in the project snapshot. */
	protected void applyMergedOperationsOnEdt(Project project,
			java.util.List<OperationLog.Operation> operations,
			java.util.Set<String> alreadyAppliedOperationIds) throws IOException {
		applyMergedOperationsOnEdtInternal(project, operations, alreadyAppliedOperationIds);
	}

	private static void applyMergedOperationsOnEdtInternal(Project project,
			java.util.List<OperationLog.Operation> operations,
			java.util.Set<String> alreadyAppliedOperationIds) throws IOException {
		runOnEdt(() -> new MpoTaskOperationService().apply(project, operations, alreadyAppliedOperationIds));
	}

	private static CriticalChainService.Settings existingCcpm(Project project) {
		return new CriticalChainService().findSettings(project);
	}

	private static String ccpmJson(CriticalChainService.Settings settings, CriticalChainService.Baseline baseline) {
		ObjectNode ccpm = JSON.createObjectNode();
		ccpm.put("schemaVersion", 1);
		ccpm.put("enabled", settings.isEnabled());
		ccpm.put("bufferFraction", settings.getBufferFraction());
		ccpm.put("levelingOrder", settings.getLevelingOrder().name());
		ccpm.put("onlyWithinAvailableSlack", settings.isOnlyWithinAvailableSlack());
		ccpm.put("allowTaskSplits", settings.isAllowTaskSplits());
		ccpm.put("levelManuallyScheduledTasks", settings.isLevelManuallyScheduledTasks());
		ccpm.put("levelAssignments", settings.isLevelAssignments());
		ccpm.put("sensitivity", settings.getSensitivity().name());
		if (baseline != null) {
			ObjectNode value = ccpm.putObject("baseline");
			value.put("projectFinishMillis", baseline.projectFinishMillis());
			value.put("projectBufferMillis", baseline.projectBufferMillis());
			value.put("bufferFraction", baseline.bufferFraction());
			com.fasterxml.jackson.databind.node.ArrayNode critical = value.putArray("criticalTaskIds");
			for (Long id : baseline.criticalTaskIds()) critical.add(id.longValue());
			longMap(value.putObject("feedingTaskStartMillis"), baseline.feedingTaskStartMillis());
			longMap(value.putObject("feedingBufferMillis"), baseline.feedingBufferMillis());
			value.put("allResources", baseline.allResources());
			com.fasterxml.jackson.databind.node.ArrayNode resources = value.putArray("resourceIds");
			for (Long id : baseline.resourceIds()) resources.add(id.longValue());
		}
		return json(ccpm);
	}

	private static void longMap(ObjectNode target, java.util.Map<Long, Long> source) {
		for (java.util.Map.Entry<Long, Long> entry : source.entrySet()) target.put(Long.toString(entry.getKey().longValue()), entry.getValue().longValue());
	}

	private static void restoreCcpm(Project project, String json) throws IOException {
		try {
			JsonNode root = object(json, "ccpm.json");
			if (!root.path("schemaVersion").canConvertToInt() || root.path("schemaVersion").intValue() != 1) throw new IOException("Unsupported ccpm.json schemaVersion");
			CriticalChainService.Settings settings = new CriticalChainService.Settings();
			settings.setEnabled(bool(root, "enabled"));
			settings.setBufferFraction(number(root, "bufferFraction"));
			settings.setLevelingOrder(com.microproject.pm.resource.ResourceLevelingService.Order.valueOf(text(root, "levelingOrder")));
			settings.setOnlyWithinAvailableSlack(bool(root, "onlyWithinAvailableSlack"));
			settings.setAllowTaskSplits(bool(root, "allowTaskSplits"));
			settings.setLevelAssignments(root.has("levelAssignments") ? bool(root, "levelAssignments") : true);
			settings.setIncludeProposedResources(root.has("includeProposedResources")
				? bool(root, "includeProposedResources") : false);
			settings.setLevelManuallyScheduledTasks(root.has("levelManuallyScheduledTasks")
				? bool(root, "levelManuallyScheduledTasks") : false);
			settings.setSensitivity(root.has("sensitivity")
				? com.microproject.pm.resource.ResourceLevelingService.Sensitivity.valueOf(text(root, "sensitivity"))
				: com.microproject.pm.resource.ResourceLevelingService.Sensitivity.DAY);
			CriticalChainService service = new CriticalChainService();
			MpoManifest.applySettings(service.settings(project), settings);
			JsonNode baseline = root.get("baseline");
			if (baseline != null) service.restoreBaseline(project, baseline(baseline));
		} catch (RuntimeException e) {
			throw new IOException("Invalid ccpm.json", e);
		}
	}

	/** Validates checksums for the complete archive when written by a checksum-aware MPOF writer. */
	/**
	 * MPOF snapshots written before the unit marker stored the engine's raw
	 * millisecond value in MSPDI's serialized delay field. Restrict conversion to these
	 * microProject files: ordinary MSPDI imports remain standards-compliant.
	 */
	static javax.xml.parsers.DocumentBuilderFactory secureDocumentBuilderFactory() throws javax.xml.parsers.ParserConfigurationException {
		return MspdiProjectBoundary.secureDocumentBuilderFactory(false);
	}

	static java.util.Map<Long, Long> xmlLongMap(org.w3c.dom.Element parent, String name) {
		java.util.Map<Long, Long> result = new java.util.LinkedHashMap<>(); org.w3c.dom.NodeList values = ((org.w3c.dom.Element) parent.getElementsByTagName(name).item(0)).getElementsByTagName("value");
		for (int i = 0; i < values.getLength(); i++) { org.w3c.dom.Element value = (org.w3c.dom.Element) values.item(i); result.put(Long.valueOf(value.getAttribute("taskId")), Long.valueOf(value.getTextContent())); }
		return result;
	}
	static java.util.List<Long> xmlLongList(org.w3c.dom.Element parent, String name) {
		java.util.List<Long> result = new java.util.ArrayList<>(); org.w3c.dom.NodeList parents = parent.getElementsByTagName(name);
		if (parents.getLength() == 0) return result;
		org.w3c.dom.NodeList ids = ((org.w3c.dom.Element) parents.item(0)).getElementsByTagName("id");
		for (int i = 0; i < ids.getLength(); i++) result.add(Long.valueOf(ids.item(i).getTextContent()));
		return result;
	}

	static String requiredAttribute(org.w3c.dom.Element element, String name) throws IOException {
		String value = element.getAttribute(name); if (value == null || value.isBlank()) throw new IOException("Missing settings.xml attribute: " + name); return value;
	}
	private static byte[] ccpmHistoryJson(Project project) throws IOException {
		CriticalChainBufferHistory history = project.findTransientDocumentState(CriticalChainBufferHistory.class);
		if (history == null || (history.points().isEmpty() && history.retractions().isEmpty())) return new byte[0];
		StringBuilder jsonl = new StringBuilder();
		for (CriticalChainBufferHistory.Point point : history.points()) {
			ObjectNode value = JSON.createObjectNode();
			value.put("kind", "observation");
			value.put("id", point.observationId().toString());
			value.put("observedAt", point.observedAt().toString());
			value.put("actorId", point.actorId()); value.put("actorName", point.actorName());
			value.put("progressPercent", point.progressPercent()); value.put("consumptionPercent", point.consumptionPercent());
			value.put("zone", point.zone()); value.put("baselineId", point.baselineId());
			jsonl.append(JSON.writeValueAsString(value)).append('\n');
		}
		for (CriticalChainBufferHistory.Retraction retraction : history.retractions()) {
			ObjectNode value = JSON.createObjectNode();
			value.put("kind", "retraction");
			value.put("id", retraction.observationId().toString());
			value.put("retractedAt", retraction.retractedAt().toString());
			value.put("actorId", retraction.actorId()); value.put("actorName", retraction.actorName());
			value.put("reason", retraction.reason());
			jsonl.append(JSON.writeValueAsString(value)).append('\n');
		}
		return jsonl.toString().getBytes(StandardCharsets.UTF_8);
	}

	private static void restoreCcpmHistory(Project project, byte[] bytes) throws IOException {
		CriticalChainBufferHistory history = project.getOrCreateTransientDocumentState(CriticalChainBufferHistory.class, CriticalChainBufferHistory::new);
		int count = 0;
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(
				new ByteArrayInputStream(bytes), StandardCharsets.UTF_8))) {
		String line;
		while ((line = reader.readLine()) != null) {
			if (line.isBlank()) continue;
			if (++count > MAX_CCPM_HISTORY_ENTRIES)
				throw new IOException("MPOF CCPM history exceeds " + MAX_CCPM_HISTORY_ENTRIES + " entries");
			try {
				JsonNode value = JSON.readTree(line);
				java.util.UUID id = historyEntryId(value, line);
				if ("retraction".equals(value.path("kind").asText())) history.recordRetraction(retractionFrom(value, id));
				else history.add(pointFrom(value, id));
			} catch (RuntimeException | IOException exception) {
				throw new IOException("Invalid CCPM history entry", exception);
			}
		}
		}
	}

	private static CriticalChainService.Baseline baseline(JsonNode value) throws IOException {
		if (!value.isObject()) throw new IOException("Invalid CCPM baseline");
		long projectFinish = wholeNumberField(value, "projectFinishMillis");
		long projectBuffer = wholeNumberField(value, "projectBufferMillis");
		double fraction = number(value, "bufferFraction");
		if (!value.path("criticalTaskIds").isArray() || !value.path("feedingTaskStartMillis").isObject() || !value.path("feedingBufferMillis").isObject()) throw new IOException("Invalid CCPM baseline");
		java.util.List<Long> critical = new java.util.ArrayList<Long>();
		for (JsonNode id : value.path("criticalTaskIds")) critical.add(Long.valueOf(wholeNumber(id, "critical task ID")));
		java.util.List<Long> resources = new java.util.ArrayList<>();
		JsonNode resourceIds = value.get("resourceIds");
		if (resourceIds != null) { if (!resourceIds.isArray()) throw new IOException("Invalid CCPM resource scope"); for (JsonNode id : resourceIds) resources.add(Long.valueOf(wholeNumber(id, "resource ID"))); }
		boolean allResources = !value.has("allResources") || bool(value, "allResources");
		return new CriticalChainService.Baseline(projectFinish, projectBuffer, fraction, critical,
			longMap(value.path("feedingTaskStartMillis")), longMap(value.path("feedingBufferMillis")), allResources, resources);
	}

	private static java.util.Map<Long, Long> longMap(JsonNode value) throws IOException {
		java.util.Map<Long, Long> result = new java.util.LinkedHashMap<Long, Long>();
		java.util.Iterator<java.util.Map.Entry<String, JsonNode>> fields = value.fields();
		while (fields.hasNext()) {
			java.util.Map.Entry<String, JsonNode> field = fields.next();
			try { result.put(Long.valueOf(Long.parseLong(field.getKey())), Long.valueOf(wholeNumber(field.getValue(), "baseline value"))); }
			catch (NumberFormatException exception) { throw new IOException("Invalid CCPM baseline task ID", exception); }
		}
		return result;
	}

	private static long wholeNumberField(JsonNode object, String field) throws IOException {
		return wholeNumber(object.get(field), field);
	}

	private static long wholeNumber(JsonNode value, String field) throws IOException {
		if (value == null || !value.isIntegralNumber() || !value.canConvertToLong()) throw new IOException("Missing or invalid " + field);
		return value.longValue();
	}

	private static String json(ObjectNode value) {
		try {
			return JSON.writeValueAsString(value) + "\n";
		} catch (IOException e) {
			throw new IllegalStateException("Unable to write mpo JSON", e);
		}
	}

	static JsonNode object(String json, String name) throws IOException {
		JsonNode root = JSON.readTree(json);
		if (root == null || !root.isObject()) throw new IOException(name + " must be a JSON object");
		return root;
	}

	static String text(JsonNode object, String field) throws IOException {
		JsonNode value = object.get(field);
		if (value == null || !value.isTextual()) throw new IOException("Missing or invalid " + field);
		return value.textValue();
	}

	private static boolean bool(JsonNode object, String field) throws IOException {
		JsonNode value = object.get(field);
		if (value == null || !value.isBoolean()) throw new IOException("Missing or invalid " + field);
		return value.booleanValue();
	}

	private static double number(JsonNode object, String field) throws IOException {
		JsonNode value = object.get(field);
		if (value == null || !value.isNumber() || !Double.isFinite(value.doubleValue())) throw new IOException("Missing or invalid " + field);
		return value.doubleValue();
	}

	static void validateExtensionName(String name) throws IOException {
		if (name == null || name.isEmpty() || name.startsWith("/") || name.indexOf('\\') >= 0 || name.contains(".."))
			throw new IOException("Unsafe mpo extension name");
	}

	/** Mutable only while an archive is being collected; handed off once complete. */
	private static final class ArchiveParts {
		private byte[] manifest;
		private byte[] projectXml;
		private byte[] meta;
		private byte[] settings;
		private byte[] ccpmHistory;
		private byte[] layout;
		private byte[] visibility;
		private byte[] draftCcpm;
		private byte[] operations;
		private byte[] draftOperations;
		private byte[] taskIdentities;
		private byte[] mimetype;
		private final MpoExtensions extensions = new MpoExtensions();
		private final int[] totalBytes = new int[] { 0 };
		private int entryCount;

		private void accept(String name, boolean directory, InputStream input) throws IOException {
			if (++entryCount > MAX_ENTRIES) throw new IOException("MPOF file has too many entries");
			if (directory) {
				if (!name.endsWith("/")) throw new IOException("Invalid mpo directory entry");
				return;
			}
			// Reject traversal/absolute extension names before reading their bytes.
			// This keeps malformed entries from consuming the per-entry memory budget.
			if (!isStandardEntry(name)) validateExtensionName(name);
			byte[] value = readEntry(Objects.requireNonNull(input, "input"), totalBytes);
			if (MIMETYPE_ENTRY.equals(name)) {
				if (mimetype != null) throw new IOException("Duplicate mpo entry: " + MIMETYPE_ENTRY);
				mimetype = value;
				String mime = new String(value, StandardCharsets.UTF_8).trim();
				if (!MpoFormatIdentifiers.WRITE_AND_READ_MIME_TYPE.equals(mime)) throw new IOException("Invalid MPOF mimetype entry: " + mime);
			} else if (MANIFEST_ENTRY.equals(name)) {
				if (manifest != null) throw new IOException("Duplicate manifest entry");
				manifest = value;
			} else if (PROJECT_ENTRY.equals(name)) {
				if (projectXml != null) throw new IOException("Duplicate project snapshot entry");
				projectXml = value;
			} else if (META_ENTRY.equals(name)) {
				if (meta != null) throw new IOException("Duplicate mpo entry: " + META_ENTRY);
				meta = value;
			} else if (SETTINGS_ENTRY.equals(name)) {
				if (settings != null) throw new IOException("Duplicate mpo entry: " + SETTINGS_ENTRY);
				settings = value;
			} else if (CCPM_HISTORY_ENTRY.equals(name)) {
				if (ccpmHistory != null) throw new IOException("Duplicate mpo entry: " + CCPM_HISTORY_ENTRY);
				ccpmHistory = value;
			} else if (LAYOUT_ENTRY.equals(name)) {
				if (layout != null) throw new IOException("Duplicate mpo entry: " + LAYOUT_ENTRY);
				layout = value;
			} else if (VISIBILITY_ENTRY.equals(name)) {
				if (visibility != null) throw new IOException("Duplicate mpo entry: " + VISIBILITY_ENTRY);
				visibility = value;
			} else if (DRAFT_CCPM_ENTRY.equals(name)) {
				if (draftCcpm != null) throw new IOException("Duplicate draft mpo entry: " + DRAFT_CCPM_ENTRY);
				draftCcpm = value;
			} else if (OPERATIONS_ENTRY.equals(name)) {
				if (operations != null) throw new IOException("Duplicate mpo entry: " + OPERATIONS_ENTRY);
				operations = value;
			} else if (DRAFT_OPERATIONS_ENTRY.equals(name)) {
				if (draftOperations != null) throw new IOException("Duplicate draft mpo entry: " + DRAFT_OPERATIONS_ENTRY);
				draftOperations = value;
			} else if (TASK_IDENTITIES_ENTRY.equals(name)) {
				if (taskIdentities != null) throw new IOException("Duplicate mpo entry: " + TASK_IDENTITIES_ENTRY);
				taskIdentities = value;
			} else {
				validateExtensionName(name);
				if (extensions.entries.put(name, value) != null) throw new IOException("Duplicate mpo extension: " + name);
			}
		}

		private static boolean isStandardEntry(String name) {
			return MIMETYPE_ENTRY.equals(name) || MANIFEST_ENTRY.equals(name) || PROJECT_ENTRY.equals(name)
				|| META_ENTRY.equals(name) || SETTINGS_ENTRY.equals(name) || CCPM_HISTORY_ENTRY.equals(name)
				|| LAYOUT_ENTRY.equals(name) || VISIBILITY_ENTRY.equals(name) || DRAFT_CCPM_ENTRY.equals(name)
				|| OPERATIONS_ENTRY.equals(name) || DRAFT_OPERATIONS_ENTRY.equals(name)
				|| TASK_IDENTITIES_ENTRY.equals(name);
		}
	}

	private static final class NonClosingInputStream extends FilterInputStream {
		private NonClosingInputStream(InputStream delegate) { super(delegate); }
		@Override public void close() { /* caller owns the wrapped stream */ }
	}

	static String sha256(byte[] data) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(data);
			StringBuilder text = new StringBuilder(digest.length * 2);
			for (byte value : digest) {
				text.append(String.format(Locale.ROOT, "%02x", Byte.valueOf(value)));
			}
			return text.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 is required by the Java runtime", e);
		}
	}
}
