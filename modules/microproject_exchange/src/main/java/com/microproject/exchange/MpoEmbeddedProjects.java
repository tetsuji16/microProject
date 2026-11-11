/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.exchange;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.DefaultSubProj;
import com.microproject.pm.task.SubProj;
import com.microproject.pm.task.Task;
import com.microproject.temporary.TemporaryCleanupQueue;
import com.microproject.util.MarkupEscape;
import com.microproject.util.SafeFileReplace;

/**
	* Reads, validates and rewrites the embedded and linked project entries of an
	* MPOF v1.0 master archive, including their extraction into a workspace and the
	* rewrite of cross-project references inside extracted child archives.
	*/
final class MpoEmbeddedProjects {

	private MpoEmbeddedProjects() {
	}

	static java.util.Map<String, String> embeddedSourcesFor(Project master) {
		if (master == null)
			return java.util.Map.of();
		java.util.Map<String, String> sourcePaths = new java.util.LinkedHashMap<String, String>();
		if (master.getSharedResourcePoolFile() != null && !master.getSharedResourcePoolFile().isBlank())
			sourcePaths.put(master.getSharedResourcePoolFile(), null);
		if (master.isMaster()) for (java.util.Iterator<?> tasks = master.getTaskOutlineIterator(); tasks.hasNext();) {
			Object value = tasks.next();
			if (value instanceof SubProj reference) {
				if (reference.getSubprojectFile() != null && !reference.getSubprojectFile().isBlank())
					sourcePaths.putIfAbsent(reference.getSubprojectFile(), reference.getReferenceId());
				Project child = reference.getSubproject();
				if (child != null && child.getSharedResourcePoolFile() != null && !child.getSharedResourcePoolFile().isBlank())
					sourcePaths.putIfAbsent(child.getSharedResourcePoolFile(), null);
			}
		}
		return sourcePaths;
	}
	static java.util.List<EmbeddedProject> readEmbeddedProjects(java.util.Map<String, String> sourcePaths) throws IOException {
		java.util.List<EmbeddedProject> embedded = new java.util.ArrayList<EmbeddedProject>();
		int index = 0;
		java.util.Set<String> seen = new java.util.LinkedHashSet<String>();
		for (java.util.Map.Entry<String, String> sourceEntry : sourcePaths.entrySet()) {
			String sourcePath = sourceEntry.getKey();
			File source = new File(sourcePath).getCanonicalFile();
			if (!source.isFile())
				throw new IOException("Cannot embed linked project or resource pool because its file is unavailable: " + sourcePath);
			if (!seen.add(source.getPath()))
				continue;
			byte[] contents = Files.readAllBytes(source.toPath());
			if (contents.length > MpoFileImporter.MAX_ENTRY_BYTES)
				throw new IOException("Linked subproject exceeds MPOF entry limit: " + sourcePath);
			String name = source.getName().replaceAll("[^A-Za-z0-9._-]", "_");
			if (name.isBlank()) name = "project.mpo";
			embedded.add(new EmbeddedProject(source.getPath(), MpoFileImporter.EMBEDDED_PROJECT_PREFIX + (++index) + "-" + name, contents,
					sourceEntry.getValue()));
		}
		return embedded;
	}
	static java.util.Map<String, SubProj.LoadStatus> validateEmbeddedProjects(ManifestData manifest,
			java.util.Map<String, byte[]> entries) throws IOException {
		if (manifest.embeddedProjects().size() > MpoFileImporter.MAX_ENTRIES)
			throw new IOException("MPOF manifest has too many embedded projects");
		java.util.Set<String> seen = new java.util.HashSet<String>();
		java.util.Map<String, SubProj.LoadStatus> failures = new java.util.LinkedHashMap<String, SubProj.LoadStatus>();
		for (EmbeddedProjectReference reference : manifest.embeddedProjects()) {
			if (!seen.add(reference.entryName()))
				throw new IOException("MPOF manifest has a duplicate embedded project entry: " + reference.entryName());
			byte[] contents = entries.get(reference.entryName());
			if (contents == null)
				failures.put(reference.sourcePath(), SubProj.LoadStatus.MISSING);
			else if (!MpoFileImporter.sha256(contents).equals(reference.sha256()))
				failures.put(reference.sourcePath(), SubProj.LoadStatus.INVALID);
			else {
				try {
					validateEmbeddedProjectPayload(contents);
				} catch (IOException invalidPayload) {
					failures.put(reference.sourcePath(), SubProj.LoadStatus.INVALID);
				}
			}
		}
		return failures;
	}
	static void validateEmbeddedProjectPayload(byte[] contents) throws IOException {
		MpoArchiveBudgetValidator.Result budget = MpoArchiveBudgetValidator.validate(contents,
			MpoArchiveBudgetValidator.DEFAULT_BUDGET);
		if (!budget.valid())
			throw new IOException("Embedded MPOF archive budget rejected: " + budget.reason());
		byte[] mimetype = null; byte[] manifest = null; byte[] projectXml = null; byte[] meta = null;
		byte[] settings = null; byte[] history = null; byte[] layout = null; byte[] visibility = null; byte[] draftCcpm = null;
		byte[] operations = null; byte[] draftOperations = null; byte[] taskIdentities = null;
		MpoExtensions extensions = new MpoExtensions();
		try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(contents), StandardCharsets.UTF_8)) {
			ZipEntry entry;
			int count = 0; int[] totalBytes = new int[] { 0 };
			while ((entry = zip.getNextEntry()) != null) {
				MpoArchiveBudgetValidator.validateEntryName(entry.getName(), MpoArchiveBudgetValidator.DEFAULT_BUDGET);
				if (++count > MpoFileImporter.MAX_ENTRIES) throw new IOException("Embedded MPOF has too many entries");
				if (entry.isDirectory()) continue;
				if (MpoFileImporter.MIMETYPE_ENTRY.equals(entry.getName())) { if (mimetype != null) throw new IOException("Embedded MPOF has duplicate mimetype"); mimetype = MpoFileImporter.readEntry(zip, totalBytes); }
				else if (MpoFileImporter.MANIFEST_ENTRY.equals(entry.getName())) { if (manifest != null) throw new IOException("Embedded MPOF has duplicate manifest"); manifest = MpoFileImporter.readEntry(zip, totalBytes); }
				else if (MpoFileImporter.PROJECT_ENTRY.equals(entry.getName())) { if (projectXml != null) throw new IOException("Embedded MPOF has duplicate project payload"); projectXml = MpoFileImporter.readEntry(zip, totalBytes); }
				else if (MpoFileImporter.META_ENTRY.equals(entry.getName())) { if (meta != null) throw new IOException("Embedded MPOF has duplicate meta"); meta = MpoFileImporter.readEntry(zip, totalBytes); }
				else if (MpoFileImporter.SETTINGS_ENTRY.equals(entry.getName())) { if (settings != null) throw new IOException("Embedded MPOF has duplicate settings"); settings = MpoFileImporter.readEntry(zip, totalBytes); }
				else if (MpoFileImporter.CCPM_HISTORY_ENTRY.equals(entry.getName())) { if (history != null) throw new IOException("Embedded MPOF has duplicate CCPM history"); history = MpoFileImporter.readEntry(zip, totalBytes); }
				else if (MpoFileImporter.LAYOUT_ENTRY.equals(entry.getName())) { if (layout != null) throw new IOException("Embedded MPOF has duplicate layout"); layout = MpoFileImporter.readEntry(zip, totalBytes); }
				else if (MpoFileImporter.VISIBILITY_ENTRY.equals(entry.getName())) { if (visibility != null) throw new IOException("Embedded MPOF has duplicate visibility"); visibility = MpoFileImporter.readEntry(zip, totalBytes); }
				else if (MpoFileImporter.DRAFT_CCPM_ENTRY.equals(entry.getName())) { if (draftCcpm != null) throw new IOException("Embedded MPOF has duplicate draft CCPM settings"); draftCcpm = MpoFileImporter.readEntry(zip, totalBytes); }
				else if (MpoFileImporter.OPERATIONS_ENTRY.equals(entry.getName())) { if (operations != null) throw new IOException("Embedded MPOF has duplicate operations"); operations = MpoFileImporter.readEntry(zip, totalBytes); }
				else if (MpoFileImporter.DRAFT_OPERATIONS_ENTRY.equals(entry.getName())) { if (draftOperations != null) throw new IOException("Embedded MPOF has duplicate draft operations"); draftOperations = MpoFileImporter.readEntry(zip, totalBytes); }
				else if (MpoFileImporter.TASK_IDENTITIES_ENTRY.equals(entry.getName())) { if (taskIdentities != null) throw new IOException("Embedded MPOF has duplicate task identities"); taskIdentities = MpoFileImporter.readEntry(zip, totalBytes); }
				else { MpoFileImporter.validateExtensionName(entry.getName()); if (extensions.entries.put(entry.getName(), MpoFileImporter.readEntry(zip, totalBytes)) != null) throw new IOException("Embedded MPOF has duplicate extension: " + entry.getName()); }
			}
		}
		if (manifest == null || projectXml == null)
			throw new IOException("Embedded MPOF is missing its manifest or project payload");
		if (mimetype != null && !MpoFormatIdentifiers.WRITE_AND_READ_MIME_TYPE.equals(new String(mimetype, StandardCharsets.UTF_8).trim()))
			throw new IOException("Embedded MPOF has an invalid mimetype");
		ManifestData embeddedManifest = MpoFileImporter.readManifest(manifest, projectXml);
		if (meta != null) MpoFileImporter.validateMeta(meta);
		MpoFileImporter.validateArchiveChecksums(embeddedManifest, mimetype, meta, settings, history, layout, visibility, draftCcpm,
				operations, draftOperations, taskIdentities, projectXml, extensions.entries);
		if (settings != null && draftCcpm != null) throw new IOException("Embedded MPOF has both current and draft CCPM settings");
		if (operations != null && draftOperations != null) throw new IOException("Embedded MPOF has both current and draft operations");
		if (history != null) MpoFileImporter.validateCcpmHistory(history);
		if (operations != null) MpoFileImporter.readBoundedOperationLog(operations, false);
		else if (draftOperations != null) MpoFileImporter.readBoundedOperationLog(draftOperations, true);
		try {
			MpoFileImporter.secureDocumentBuilderFactory().newDocumentBuilder().parse(new ByteArrayInputStream(projectXml));
		} catch (Exception invalidXml) {
			throw new IOException("Embedded MPOF project payload is not valid XML", invalidXml);
		}
	}
	static void restoreEmbeddedProjectReferences(Project master, ManifestData manifest,
			java.util.Map<String, byte[]> entries, java.util.Map<String, SubProj.LoadStatus> failures) throws IOException {
		if (manifest.embeddedProjects().isEmpty())
			return;
		MpoExtractionSession session = MpoExtractionSession.open(MpoFileImporter.extractionWorkspaceRoot == null
				? MpoExtractionSession.defaultWorkspaceRoot() : MpoFileImporter.extractionWorkspaceRoot);
		try {
		java.util.Map<String, String> extractedBySource = new java.util.LinkedHashMap<String, String>();
		for (EmbeddedProjectReference reference : manifest.embeddedProjects()) {
			if (failures.containsKey(reference.sourcePath()))
				continue;
			byte[] contents = entries.remove(reference.entryName());
			if (contents == null)
				throw new IOException("Missing embedded MPOF entry: " + reference.entryName());
			Path target = session.extract(reference.entryName(), contents);
			extractedBySource.put(reference.sourcePath(), target.toString());
		}
		// Embedded projects can themselves contain cross-project task placeholders
		// or shared-pool references.  Rewrite those references to the extracted
		// archive members before a child is opened, otherwise moving a complete
		// MPO bundle still leaves its internal links pointing at the old machine.
		java.util.Map<String, Long> embeddedProjectIdsBySource = new java.util.LinkedHashMap<String, Long>();
		java.util.Map<String, String> embeddedReferenceIdsBySource = new java.util.LinkedHashMap<String, String>();
		for (EmbeddedProjectReference embedded : manifest.embeddedProjects())
			if (embedded.referenceId() != null && !embedded.referenceId().isBlank())
				embeddedReferenceIdsBySource.put(embedded.sourcePath(), embedded.referenceId());
		for (java.util.Map.Entry<String, String> extracted : extractedBySource.entrySet())
			embeddedProjectIdsBySource.put(extracted.getKey(),
					rewriteExtractedProjectReferences(Path.of(extracted.getValue()), extractedBySource));
		// The master snapshot also contains external-task placeholders.  Those
		// references do not belong to an embedded child archive, so rewrite them
		// on the already-loaded master model as well.
		for (java.util.Iterator<?> tasks = master.getTaskOutlineIterator(); tasks.hasNext();) {
			Object value = tasks.next();
			if (!(value instanceof Task task) || task.getExternalProjectFile() == null)
				continue;
			for (java.util.Map.Entry<String, String> extracted : extractedBySource.entrySet())
				if (sameCanonicalPath(task.getExternalProjectFile(), extracted.getKey())) {
					task.setExternalProjectFile(extracted.getValue());
					break;
				}
		}
		for (java.util.Iterator<?> tasks = master.getTaskOutlineIterator(); tasks.hasNext();) {
			Object value = tasks.next();
			if (!(value instanceof DefaultSubProj reference))
				continue;
			SubProj.LoadStatus failure = embeddedFailureFor(reference.getSubprojectFile(), failures);
			if (failure != null) {
				reference.setLoadStatus(failure);
				continue;
			}
			for (java.util.Map.Entry<String, String> extracted : extractedBySource.entrySet()) {
				if (sameCanonicalPath(reference.getSubprojectFile(), extracted.getKey())) {
					reference.setSubprojectFile(extracted.getValue());
					String referenceId = embeddedReferenceIdsBySource.get(extracted.getKey());
					if (referenceId != null) reference.setReferenceId(referenceId);
					Long subprojectId = embeddedProjectIdsBySource.get(extracted.getKey());
					if (subprojectId != null && subprojectId.longValue() > 0L)
						reference.setSubprojectUniqueId(subprojectId.longValue());
					break;
				}
			}
		}
		for (java.util.Map.Entry<String, String> extracted : extractedBySource.entrySet()) {
			if (sameCanonicalPath(manifest.sharedResourcePoolPath(), extracted.getKey())) {
				master.setSharedResourcePoolFile(extracted.getValue());
				break;
			}
		}
		if (extractedBySource.isEmpty()) {
			session.close();
		} else {
			MpoExtractionOwnershipRegistry.attach(master, session);
			session = null;
		}
		} finally {
			if (session != null)
				session.close();
		}
	}
	static long rewriteExtractedProjectReferences(Path extractedFile,
			java.util.Map<String, String> extractedBySource) throws IOException {
		java.util.Map<String, byte[]> entries = new java.util.LinkedHashMap<String, byte[]>();
		int[] totalBytes = new int[1];
		int entryCount = 0;
		try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(extractedFile), StandardCharsets.UTF_8)) {
			ZipEntry entry;
			while ((entry = zip.getNextEntry()) != null) {
				if (++entryCount > MpoArchiveBudgetValidator.DEFAULT_BUDGET.maxEntries())
					throw new IOException("Embedded project exceeds " + MpoArchiveBudgetValidator.DEFAULT_BUDGET.maxEntries() + " entries");
				MpoArchiveBudgetValidator.validateEntryName(entry.getName(), MpoArchiveBudgetValidator.DEFAULT_BUDGET);
				if (entry.isDirectory()) continue;
				entries.put(entry.getName(), MpoFileImporter.readEntry(zip, totalBytes));
			}
		}
		byte[] projectXml = entries.get(MpoFileImporter.PROJECT_ENTRY);
		byte[] manifestXml = entries.get(MpoFileImporter.MANIFEST_ENTRY);
		if (projectXml == null || manifestXml == null)
			throw new IOException("Embedded project is not an MPOF archive: " + extractedFile.getFileName());
		ManifestData originalManifest = MpoFileImporter.readManifest(manifestXml, projectXml);
		byte[] rewrittenProject = rewriteEmbeddedXml(projectXml, extractedBySource);
		byte[] rewrittenManifest = rewriteEmbeddedXml(manifestXml, extractedBySource);
		if (java.util.Arrays.equals(projectXml, rewrittenProject) && java.util.Arrays.equals(manifestXml, rewrittenManifest))
			return originalManifest.projectUniqueId() == null ? 0L : originalManifest.projectUniqueId().longValue();
		String manifestText = new String(rewrittenManifest, StandardCharsets.UTF_8);
		// A master archive owns the canonical extracted copy of every linked
		// project.  Do not leave nested embedded copies inside a child archive:
		// reopening that child would extract a second copy and its links would no
		// longer point at the master's shared project instance.
		for (EmbeddedProjectReference nested : originalManifest.embeddedProjects()) {
			entries.remove(nested.entryName());
			String escapedEntry = java.util.regex.Pattern.quote(MarkupEscape.escape(nested.entryName()));
			manifestText = manifestText.replaceAll("(?m)^\\s*<embeddedProject\\b[^>]*entry=\\\""
					+ escapedEntry + "\\\"[^>]*/>\\s*\\r?\\n?", "");
			manifestText = manifestText.replaceAll("(?m)^\\s*<entry\\s+path=\\\"" + escapedEntry
					+ "\\\"[^>]*/>\\s*\\r?\\n?", "");
		}
		manifestText = manifestText.replaceFirst("projectSha256=\\\"[^\\\"]*\\\"",
				"projectSha256=\\\"" + MpoFileImporter.sha256(rewrittenProject) + "\\\"");
		// New MPOF manifests also checksum every payload entry.  Keep the
		// content.xml checksum in sync after portable path rewriting.
		manifestText = manifestText.replaceFirst("(path=\\\"" + java.util.regex.Pattern.quote(MpoFileImporter.PROJECT_ENTRY)
				+ "\\\" sha256=\\\")[^\\\"]*(\\\")",
				"$1" + MpoFileImporter.sha256(rewrittenProject) + "$2");
		entries.put(MpoFileImporter.PROJECT_ENTRY, rewrittenProject);
		entries.put(MpoFileImporter.MANIFEST_ENTRY, manifestText.getBytes(StandardCharsets.UTF_8));
		Path temporary = Files.createTempFile(extractedFile.getParent(), extractedFile.getFileName().toString(), ".tmp");
		boolean moved = false;
		try {
			try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(temporary, StandardOpenOption.TRUNCATE_EXISTING), StandardCharsets.UTF_8)) {
				for (java.util.Map.Entry<String, byte[]> entry : entries.entrySet())
					MpoFileImporter.writeEntry(zip, entry.getKey(), entry.getValue());
			}
			SafeFileReplace.replaceDurably(temporary, extractedFile, true);
			moved = true;
		} finally {
			if (!moved) TemporaryCleanupQueue.deleteOrEnqueue(temporary);
		}
		return originalManifest.projectUniqueId() == null ? 0L : originalManifest.projectUniqueId().longValue();
	}
	static byte[] rewriteEmbeddedXml(byte[] xml, java.util.Map<String, String> extractedBySource) {
		String rewritten = new String(xml, StandardCharsets.UTF_8);
		for (java.util.Map.Entry<String, String> entry : extractedBySource.entrySet()) {
			String source = entry.getKey();
			String target = MarkupEscape.escape(entry.getValue());
			// XML produced by different Windows/JDK combinations can use either
			// slash direction and may normalize drive/path casing differently.
			// Match the complete escaped path case-insensitively while accepting
			// both separator forms, so portable extraction never leaves a link
			// pointing back to the original machine.
			java.util.Set<String> pathVariants = new java.util.LinkedHashSet<String>();
			pathVariants.add(source);
			pathVariants.add(source.replace('\\', '/'));
			pathVariants.add(source.replace('/', '\\'));
			pathVariants.add(source.replace("\\", "&#92;"));
			pathVariants.add(source.replace("\\", "&#x5c;"));
			pathVariants.add(source.replace(":", "%3A").replace("\\", "%5C"));
			pathVariants.add(source.replace(":", "%3a").replace("\\", "%5c"));
			try {
				String uri = new File(source).toURI().toString();
				pathVariants.add(uri);
				if (uri.startsWith("file:/")) {
					pathVariants.add("file:///" + uri.substring("file:/".length()));
					pathVariants.add("file://" + uri.substring("file:/".length()));
				}
			} catch (RuntimeException ignored) {
				// Keep the raw forms above when a malformed legacy path is encountered.
			}
			for (String path : pathVariants) {
				String escaped = MarkupEscape.escape(path);
				rewritten = java.util.regex.Pattern.compile(java.util.regex.Pattern.quote(escaped),
						java.util.regex.Pattern.CASE_INSENSITIVE)
					.matcher(rewritten)
					.replaceAll(java.util.regex.Matcher.quoteReplacement(target));
			}
		}
		return rewritten.getBytes(StandardCharsets.UTF_8);
	}
	static SubProj.LoadStatus embeddedFailureFor(String sourcePath,
			java.util.Map<String, SubProj.LoadStatus> failures) {
		for (java.util.Map.Entry<String, SubProj.LoadStatus> entry : failures.entrySet())
			if (sameCanonicalPath(sourcePath, entry.getKey()))
				return entry.getValue();
		return null;
	}
	static boolean sameCanonicalPath(String first, String second) {
		if (first == null || second == null)
			return false;
		try {
			return new File(first).getCanonicalFile().equals(new File(second).getCanonicalFile());
		} catch (IOException exception) {
			return new File(first).getAbsoluteFile().equals(new File(second).getAbsoluteFile());
		}
	}
	static void validateEmbeddedEntryName(String name) throws IOException {
		MpoFileImporter.validateExtensionName(name);
		if (!name.startsWith(MpoFileImporter.EMBEDDED_PROJECT_PREFIX) || name.length() <= MpoFileImporter.EMBEDDED_PROJECT_PREFIX.length())
			throw new IOException("Invalid MPOF embedded project entry");
	}
}
