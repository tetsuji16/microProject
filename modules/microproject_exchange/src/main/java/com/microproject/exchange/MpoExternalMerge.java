/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.exchange;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import java.util.Map;
import com.microproject.collaboration.OperationLog;
import com.microproject.pm.task.Project;

/**
	* Merges externally changed MPO archives with the local document and owns the
	* operation-state records that a capture writes and an export commits.
	*/
final class MpoExternalMerge {

	private MpoExternalMerge() {
	}

	static void writeCapture(MpoCapture capture, OutputStream out, MpoOperationState operationState) throws Exception {
		MpoFileImporter.ensureOperationCount(operationState.operations);
		MpoDocumentMetadata metadata = capture.metadata();
		byte[] projectXml = capture.serialized().xml();
		java.util.List<EmbeddedProject> embeddedProjects = MpoEmbeddedProjects.readEmbeddedProjects(metadata.embeddedSources());
		java.util.LinkedHashMap<String, byte[]> archiveEntries = new java.util.LinkedHashMap<>();
		archiveEntries.put(MpoFileImporter.MIMETYPE_ENTRY, MpoFormatIdentifiers.WRITE_AND_READ_MIME_TYPE.getBytes(StandardCharsets.US_ASCII));
		archiveEntries.put(MpoFileImporter.META_ENTRY, MpoManifest.metaXml().getBytes(StandardCharsets.UTF_8));
		archiveEntries.put(MpoFileImporter.PROJECT_ENTRY, projectXml);
		for (EmbeddedProject embedded : embeddedProjects)
			archiveEntries.put(embedded.entryName(), embedded.contents());
		archiveEntries.putAll(capture.entries());
		archiveEntries.put(MpoFileImporter.OPERATIONS_ENTRY, operationState.json);
		MpoArchiveSnapshot snapshot = snapshotForWrite(metadata, projectXml, archiveEntries, embeddedProjects);
		java.util.List<MpoMergePlan.Operation> writeOperations = archiveEntries.keySet().stream()
				.map(name -> new MpoMergePlan.Operation(name, MpoMergePlan.Action.KEEP)).toList();
		validateMergePlan(planFor(snapshot, writeOperations, java.util.List.of()));
		try (ZipOutputStream zip = new ZipOutputStream(out, StandardCharsets.UTF_8)) {
			MpoFileImporter.writeMimetypeEntry(zip);
			MpoFileImporter.writeEntry(zip, MpoFileImporter.MANIFEST_ENTRY, MpoManifest.manifestFor(projectXml, metadata.documentId(), metadata.uniqueId(),
					metadata.sharedResourcePoolFile(), embeddedProjects, archiveEntries,
					metadata.sharedResourcePoolProjectId(), metadata.statusDate()).getBytes(StandardCharsets.UTF_8));
			for (java.util.Map.Entry<String, byte[]> entry : archiveEntries.entrySet()) {
				if (!MpoFileImporter.MANIFEST_ENTRY.equals(entry.getKey()) && !MpoFileImporter.MIMETYPE_ENTRY.equals(entry.getKey()))
					MpoFileImporter.writeEntry(zip, entry.getKey(), entry.getValue());
			}
		}
	}
	static MpoArchiveSnapshot snapshotForWrite(MpoDocumentMetadata metadata, byte[] projectXml,
			java.util.Map<String, byte[]> archiveEntries, java.util.List<EmbeddedProject> embeddedProjects) {
		String documentId = metadata.documentId() == null ? "" : metadata.documentId();
		java.util.List<String> embedded = embeddedProjects.stream().map(EmbeddedProject::sourcePath).toList();
		return new MpoArchiveSnapshot(archiveEntries,
				new MpoArchiveSnapshot.MspProjection(documentId, metadata.name(),
						new String(projectXml, StandardCharsets.UTF_8)),
				new MpoArchiveSnapshot.MpofMetadata(MpoFileImporter.FORMAT_VERSION, documentId, embedded));
	}
	static MpoOperationState workingOperationStateFor(Project project) throws IOException {
		MpoOperationState current = project.findTransientDocumentState(MpoOperationState.class);
		if (current == null) {
			MpoOperationState created = new MpoOperationState();
			created.documentId = project.getDocumentId();
			created.json = new OperationLog().writeJsonl(created.documentId, java.util.List.of());
			created.capture(project);
			return created;
		}
		return current.copy();
	}
	static void commitOperationState(Project project, MpoOperationState plan) throws IOException {
		MpoOperationState current = project.findTransientDocumentState(MpoOperationState.class);
		if (current == null)
			current = project.getOrCreateTransientDocumentState(MpoOperationState.class, MpoOperationState::new);
		current.commitFrom(plan);
	}
	static ExternalMpo externalOperations(File target) throws IOException {
		MpoArchiveBudgetValidator.Result budget = MpoArchiveBudgetValidator.validate(target.toPath(),
			MpoArchiveBudgetValidator.DEFAULT_BUDGET);
		if (!budget.valid())
			throw new IOException("MPOF archive budget rejected: " + budget.reason());
		byte[] mimetype = null; byte[] meta = null; byte[] settings = null; byte[] history = null;
		byte[] layout = null; byte[] visibility = null; byte[] draftCcpm = null; byte[] operations = null; byte[] draftOperations = null;
		byte[] manifest = null; byte[] projectXml = null; byte[] taskIdentities = null;
		int[] totalBytes = new int[] { 0 }; int entryCount = 0; MpoExtensions extensions = new MpoExtensions();
		try (InputStream in = new FileInputStream(target); ZipInputStream zip = new ZipInputStream(in, StandardCharsets.UTF_8)) {
			ZipEntry entry;
			while ((entry = zip.getNextEntry()) != null) {
				MpoArchiveBudgetValidator.validateEntryName(entry.getName(), MpoArchiveBudgetValidator.DEFAULT_BUDGET);
				if (++entryCount > MpoFileImporter.MAX_ENTRIES) throw new IOException("mpo has too many entries");
				if (!entry.isDirectory() && MpoFileImporter.MIMETYPE_ENTRY.equals(entry.getName())) { if (mimetype != null) throw new IOException("Duplicate mpo entry: " + MpoFileImporter.MIMETYPE_ENTRY); mimetype = MpoFileImporter.readEntry(zip, totalBytes); }
				else if (!entry.isDirectory() && MpoFileImporter.META_ENTRY.equals(entry.getName())) { if (meta != null) throw new IOException("Duplicate mpo entry: " + MpoFileImporter.META_ENTRY); meta = MpoFileImporter.readEntry(zip, totalBytes); }
				else if (!entry.isDirectory() && MpoFileImporter.SETTINGS_ENTRY.equals(entry.getName())) { if (settings != null) throw new IOException("Duplicate mpo entry: " + MpoFileImporter.SETTINGS_ENTRY); settings = MpoFileImporter.readEntry(zip, totalBytes); }
				else if (!entry.isDirectory() && MpoFileImporter.CCPM_HISTORY_ENTRY.equals(entry.getName())) { if (history != null) throw new IOException("Duplicate mpo entry: " + MpoFileImporter.CCPM_HISTORY_ENTRY); history = MpoFileImporter.readEntry(zip, totalBytes); }
				else if (!entry.isDirectory() && MpoFileImporter.LAYOUT_ENTRY.equals(entry.getName())) { if (layout != null) throw new IOException("Duplicate mpo entry: " + MpoFileImporter.LAYOUT_ENTRY); layout = MpoFileImporter.readEntry(zip, totalBytes); }
				else if (!entry.isDirectory() && MpoFileImporter.VISIBILITY_ENTRY.equals(entry.getName())) { if (visibility != null) throw new IOException("Duplicate mpo entry: " + MpoFileImporter.VISIBILITY_ENTRY); visibility = MpoFileImporter.readEntry(zip, totalBytes); }
				else if (!entry.isDirectory() && MpoFileImporter.DRAFT_CCPM_ENTRY.equals(entry.getName())) { if (draftCcpm != null) throw new IOException("Duplicate draft mpo entry: " + MpoFileImporter.DRAFT_CCPM_ENTRY); draftCcpm = MpoFileImporter.readEntry(zip, totalBytes); }
				else if (!entry.isDirectory() && MpoFileImporter.OPERATIONS_ENTRY.equals(entry.getName())) { if (operations != null) throw new IOException("Duplicate mpo entry: " + MpoFileImporter.OPERATIONS_ENTRY); operations = MpoFileImporter.readEntry(zip, totalBytes); }
				else if (!entry.isDirectory() && MpoFileImporter.DRAFT_OPERATIONS_ENTRY.equals(entry.getName())) { if (draftOperations != null) throw new IOException("Duplicate draft mpo entry: " + MpoFileImporter.DRAFT_OPERATIONS_ENTRY); draftOperations = MpoFileImporter.readEntry(zip, totalBytes); }
				else if (!entry.isDirectory() && MpoFileImporter.MANIFEST_ENTRY.equals(entry.getName())) { if (manifest != null) throw new IOException("Duplicate manifest entry"); manifest = MpoFileImporter.readEntry(zip, totalBytes); }
				else if (!entry.isDirectory() && MpoFileImporter.PROJECT_ENTRY.equals(entry.getName())) { if (projectXml != null) throw new IOException("Duplicate project snapshot entry"); projectXml = MpoFileImporter.readEntry(zip, totalBytes); }
				else if (!entry.isDirectory() && MpoFileImporter.TASK_IDENTITIES_ENTRY.equals(entry.getName())) { if (taskIdentities != null) throw new IOException("Duplicate mpo entry: " + MpoFileImporter.TASK_IDENTITIES_ENTRY); taskIdentities = MpoFileImporter.readEntry(zip, totalBytes); }
				else if (!entry.isDirectory()) { MpoFileImporter.validateExtensionName(entry.getName()); if (extensions.entries.containsKey(entry.getName())) throw new IOException("Duplicate mpo extension: " + entry.getName()); extensions.entries.put(entry.getName(), MpoFileImporter.readEntry(zip, totalBytes)); }
				zip.closeEntry();
			}
		}
		if (manifest == null || projectXml == null) throw new IOException("Cannot merge MPOF without its manifest and project snapshot");
		if (mimetype != null && !MpoFormatIdentifiers.WRITE_AND_READ_MIME_TYPE.equals(new String(mimetype, StandardCharsets.UTF_8).trim()))
			throw new IOException("Invalid MPOF mimetype entry");
		ManifestData manifestData = MpoManifest.readManifest(manifest, projectXml);
		if (meta != null) MpoManifest.validateMeta(meta);
		MpoManifest.validateArchiveChecksums(manifestData, mimetype, meta, settings, history, layout, visibility, draftCcpm,
				operations, draftOperations, taskIdentities, projectXml, extensions.entries);
		if (settings != null && draftCcpm != null) throw new IOException("MPOF contains both current and draft CCPM settings");
		if (history != null) MpoFileImporter.validateCcpmHistory(history);
		if (operations != null && draftOperations != null) throw new IOException("MPOF contains both current and draft operation logs");
		if (operations == null) operations = draftOperations;
		if (operations == null) throw new IOException("Cannot merge MPOF without an operation log");
		OperationLog.DocumentLog document = MpoFileImporter.readBoundedOperationLog(operations, draftOperations == operations);
		String manifestDocumentId = manifestData.documentId();
		Long manifestProjectId = manifestData.projectUniqueId();
		return new ExternalMpo(document, extensions, manifestDocumentId, manifestProjectId, taskIdentities,
				snapshotOf(manifestData, projectXml, extensions.entries));
	}
	static MpoArchiveSnapshot snapshotOf(ManifestData manifest, byte[] projectXml,
			java.util.Map<String, byte[]> extensions) {
		java.util.LinkedHashMap<String, byte[]> entries = new java.util.LinkedHashMap<>();
		entries.put(MpoFileImporter.PROJECT_ENTRY, projectXml);
		if (extensions != null) entries.putAll(extensions);
		String documentId = manifest.documentId() == null ? "" : manifest.documentId();
		java.util.List<String> embedded = manifest.embeddedProjects().stream()
				.map(EmbeddedProjectReference::sourcePath).toList();
		return new MpoArchiveSnapshot(entries,
				new MpoArchiveSnapshot.MspProjection(documentId, "", new String(projectXml, StandardCharsets.UTF_8)),
				new MpoArchiveSnapshot.MpofMetadata(MpoFileImporter.FORMAT_VERSION, documentId, embedded));
	}
	static MpoMergePlan planFor(MpoArchiveSnapshot snapshot,
			java.util.List<MpoMergePlan.Operation> operations,
			java.util.List<MpoMergePlan.Conflict> conflicts) {
		return new MpoMergePlan(snapshot.mspProjection(), snapshot.metadata(), operations, conflicts);
	}
	static void validateMergePlan(MpoMergePlan plan) throws IOException {
		java.util.List<MpoValidationDiagnostic> diagnostics = new java.util.ArrayList<>();
		if (!plan.mspProjection().scheduleXml().contains("<"))
			diagnostics.add(new MpoValidationDiagnostic(MpoValidationDiagnostic.Severity.ERROR,
				"MPO_PROJECT_XML", "MPOF project snapshot is empty", MpoFileImporter.PROJECT_ENTRY));
		for (MpoMergePlan.Operation operation : plan.operations()) {
			try {
				MpoArchiveBudgetValidator.validateEntryName(operation.entryName(), MpoArchiveBudgetValidator.DEFAULT_BUDGET);
			} catch (IOException exception) {
				diagnostics.add(new MpoValidationDiagnostic(MpoValidationDiagnostic.Severity.ERROR,
						"MPO_ENTRY_NAME", exception.getMessage(), operation.entryName()));
			}
		}
		for (MpoMergePlan.Conflict conflict : plan.conflicts())
			diagnostics.add(new MpoValidationDiagnostic(MpoValidationDiagnostic.Severity.ERROR,
				"MPO_CONFLICT", conflict.reason(), conflict.entryName()));
		if (diagnostics.stream().anyMatch(diagnostic -> diagnostic.severity() == MpoValidationDiagnostic.Severity.ERROR)) {
			MpoValidationDiagnostic diagnostic = diagnostics.stream()
					.filter(value -> value.severity() == MpoValidationDiagnostic.Severity.ERROR).findFirst().orElseThrow();
			throw new IOException("MPOF validation failed [" + diagnostic.code() + "]: " + diagnostic.message());
		}
	}
}
