/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.exchange;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import java.util.Locale;
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
import com.microproject.pm.task.DefaultSubProj;
import com.microproject.pm.task.SubProj;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Task;
import com.microproject.pm.dependency.Dependency;
import com.microproject.pm.assignment.Assignment;
import com.microproject.field.Field;
import com.microproject.graphic.configuration.SpreadSheetFieldArray;
import com.microproject.pm.assignment.TimeDistributedHelper;
import com.microproject.session.LocalSession;
import com.microproject.session.SessionFactory;
import com.microproject.strings.Messages;

/**
 * Reads and writes MPOF v1.0 containers. An mpo file is a ZIP containing a
 * UTF-8 manifest and a standards-based MSPDI XML snapshot; it never embeds a
 * Java serialized object.
 */
public class MpoFileImporter extends FileImporter {
	private static final Object EXPORT_LOCK_GUARD = new Object();
	static final String MIMETYPE_ENTRY = "mimetype";
	static final String MANIFEST_ENTRY = "META-INF/manifest.xml";
	static final String PROJECT_ENTRY = "content.xml";
	static final String META_ENTRY = "meta.xml";
	static final String SETTINGS_ENTRY = "settings.xml";
	static final String CCPM_HISTORY_ENTRY = "ccpm/history.jsonl";
	static final String LAYOUT_ENTRY = "layout.json";
	/** Native task-view state that MSPDI does not represent. */
	static final String VISIBILITY_ENTRY = "microproject/visibility.json";
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
	private static final String MIME_TYPE = "application/vnd.microproject.openproject";
	/** Marker added once MPOF snapshots use the MSPDI minute-based delay unit. */
	private static final String LEVELING_DELAY_UNIT_ATTRIBUTE = "levelingDelayUnit";
	private static final String LEVELING_DELAY_UNIT_MINUTES = "minutes";
	private static final String LEVELING_DELAY_FORMAT_MINUTES = "3";
	private static final Pattern LEVELING_DELAY = Pattern.compile("<LevelingDelay>(-?\\d+)</LevelingDelay>");
	private static final Pattern LEVELING_DELAY_FORMAT = Pattern.compile("<LevelingDelayFormat>\\d+</LevelingDelayFormat>");
	static final String OPERATIONS_ENTRY = "operations/log.jsonl";
	/** Read-only aliases for MPOF drafts written before the XML/JSONL layout was settled. */
	private static final String DRAFT_CCPM_ENTRY = "ccpm.json";
	private static final String DRAFT_OPERATIONS_ENTRY = "changes/operations.json";
	static final String TASK_IDENTITIES_ENTRY = "changes/task-identities.json";
	/** Raw linked project files carried by a portable master-project archive. */
	static final String EMBEDDED_PROJECT_PREFIX = "projects/";
	private static final int MAX_ENTRY_BYTES = 64 * 1024 * 1024;
	private static final int MAX_TOTAL_BYTES = 128 * 1024 * 1024;
	private static final int MAX_ENTRIES = 128;
	private static final ObjectMapper JSON = new ObjectMapper().enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
		.enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

	@Override
	public void importFile() throws Exception {
		if (fileInputStream == null) {
			project = loadProject(new File(fileName));
		} else {
			try (InputStream in = fileInputStream) {
				project = loadProject(in);
			}
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
		try (ZipFile zip = new ZipFile(source, StandardCharsets.UTF_8);
			 ByteArrayOutputStream normalized = new ByteArrayOutputStream()) {
			int[] totalBytes = new int[] { 0 };
			int entryCount = 0;
			try (ZipOutputStream output = new ZipOutputStream(normalized, StandardCharsets.UTF_8)) {
				java.util.Enumeration<? extends ZipEntry> entries = zip.entries();
				while (entries.hasMoreElements()) {
					ZipEntry entry = entries.nextElement();
					if (++entryCount > MAX_ENTRIES) throw new IOException("MPOF file has too many entries");
					ZipEntry copy = new ZipEntry(entry.getName());
					output.putNextEntry(copy);
					if (!entry.isDirectory()) {
						try (InputStream entryInput = zip.getInputStream(entry)) {
							output.write(readEntry(entryInput, totalBytes));
						}
					}
					output.closeEntry();
				}
			}
			return loadProject(new ByteArrayInputStream(normalized.toByteArray()));
		}
	}

	@Override
	public Project loadProject(InputStream in) throws Exception {
		byte[] manifest = null;
		byte[] projectXml = null;
		byte[] meta = null;
		byte[] settings = null;
		byte[] ccpmHistory = null;
		byte[] layout = null;
		byte[] visibility = null;
		byte[] draftCcpm = null;
		byte[] operations = null;
		byte[] draftOperations = null;
		byte[] taskIdentities = null;
		byte[] mimetype = null;
		MpoExtensions extensions = new MpoExtensions();
		int[] totalBytes = new int[] { 0 };
		int entryCount = 0;
		try (ZipInputStream zip = new ZipInputStream(in, StandardCharsets.UTF_8)) {
			ZipEntry entry;
			while ((entry = zip.getNextEntry()) != null) {
				if (++entryCount > MAX_ENTRIES) throw new IOException("MPOF file has too many entries");
				if (!entry.isDirectory() && MIMETYPE_ENTRY.equals(entry.getName())) {
					mimetype = readEntry(zip, totalBytes);
					String mime = new String(mimetype, StandardCharsets.UTF_8).trim();
					if (!MIME_TYPE.equals(mime)) throw new IOException("Invalid MPOF mimetype entry: " + mime);
					zip.closeEntry();
					continue;
				}
				if (entry.isDirectory()) {
					if (!entry.getName().endsWith("/")) throw new IOException("Invalid mpo directory entry");
				} else if (MANIFEST_ENTRY.equals(entry.getName())) {
					if (manifest != null) throw new IOException("Duplicate manifest entry");
					manifest = readEntry(zip, totalBytes);
				} else if (PROJECT_ENTRY.equals(entry.getName())) {
					if (projectXml != null) throw new IOException("Duplicate project snapshot entry");
					projectXml = readEntry(zip, totalBytes);
				} else if (META_ENTRY.equals(entry.getName())) {
					if (meta != null) throw new IOException("Duplicate mpo entry: " + META_ENTRY);
					meta = readEntry(zip, totalBytes);
				} else if (SETTINGS_ENTRY.equals(entry.getName())) {
					if (settings != null) throw new IOException("Duplicate mpo entry: " + SETTINGS_ENTRY);
					settings = readEntry(zip, totalBytes);
				} else if (CCPM_HISTORY_ENTRY.equals(entry.getName())) {
					if (ccpmHistory != null) throw new IOException("Duplicate mpo entry: " + CCPM_HISTORY_ENTRY);
					ccpmHistory = readEntry(zip, totalBytes);
				} else if (LAYOUT_ENTRY.equals(entry.getName())) {
					if (layout != null) throw new IOException("Duplicate mpo entry: " + LAYOUT_ENTRY);
					layout = readEntry(zip, totalBytes);
				} else if (VISIBILITY_ENTRY.equals(entry.getName())) {
					if (visibility != null) throw new IOException("Duplicate mpo entry: " + VISIBILITY_ENTRY);
					visibility = readEntry(zip, totalBytes);
				} else if (DRAFT_CCPM_ENTRY.equals(entry.getName())) {
					if (draftCcpm != null) throw new IOException("Duplicate draft mpo entry: " + DRAFT_CCPM_ENTRY);
					draftCcpm = readEntry(zip, totalBytes);
				} else if (OPERATIONS_ENTRY.equals(entry.getName())) {
					if (operations != null) throw new IOException("Duplicate mpo entry: " + OPERATIONS_ENTRY);
					operations = readEntry(zip, totalBytes);
				} else if (DRAFT_OPERATIONS_ENTRY.equals(entry.getName())) {
					if (draftOperations != null) throw new IOException("Duplicate draft mpo entry: " + DRAFT_OPERATIONS_ENTRY);
					draftOperations = readEntry(zip, totalBytes);
				} else if (TASK_IDENTITIES_ENTRY.equals(entry.getName())) {
					if (taskIdentities != null) throw new IOException("Duplicate mpo entry: " + TASK_IDENTITIES_ENTRY);
					taskIdentities = readEntry(zip, totalBytes);
				} else {
					validateExtensionName(entry.getName());
					if (extensions.entries.containsKey(entry.getName())) throw new IOException("Duplicate mpo extension: " + entry.getName());
					extensions.entries.put(entry.getName(), readEntry(zip, totalBytes));
				}
				zip.closeEntry();
			}
		}
		if (manifest == null || projectXml == null) {
			throw new IOException("An MPOF file must contain " + MANIFEST_ENTRY + " and " + PROJECT_ENTRY);
		}
		if (settings != null && draftCcpm != null) {
			throw new IOException("MPOF contains both current and draft CCPM settings");
		}
		boolean legacyMicroprojectLevelingDelay = meta != null && isLegacyMicroprojectLevelingDelay(meta);
		if (meta != null) validateMeta(meta);
		ManifestData manifestData = readManifest(manifest, projectXml);
		validateArchiveChecksums(manifestData, mimetype, meta, settings, ccpmHistory, layout, visibility,
				draftCcpm, operations, draftOperations, taskIdentities, projectXml, extensions.entries);
		java.util.Map<String, SubProj.LoadStatus> embeddedProjectFailures = validateEmbeddedProjects(manifestData, extensions.entries);
		if (legacyMicroprojectLevelingDelay) projectXml = migrateLegacyLevelingDelays(projectXml);
		MicrosoftImporter delegate = new MicrosoftImporter();
		delegate.setFileName(PROJECT_ENTRY);
		delegate.setProjectFactory(projectFactory);
		project = delegate.loadProject(new ByteArrayInputStream(projectXml));
		if (manifestData.documentId() != null) try {
			project.setDocumentId(manifestData.documentId());
		} catch (IllegalArgumentException exception) {
			throw new IOException("Invalid MPOF document identity", exception);
		}
		if (manifestData.projectUniqueId() != null && manifestData.projectUniqueId().longValue() > 0L)
			project.setUniqueId(manifestData.projectUniqueId().longValue());
		if (manifestData.sharedResourcePoolPath() != null && !manifestData.sharedResourcePoolPath().isBlank())
			project.setSharedResourcePoolFile(manifestData.sharedResourcePoolPath());
		if (manifestData.sharedResourcePoolProjectId() != null && manifestData.sharedResourcePoolProjectId().longValue() > 0L)
			project.setSharedResourcePoolProjectId(manifestData.sharedResourcePoolProjectId().longValue());
		if (settings != null) {
			restoreSettings(project, settings);
		} else if (draftCcpm != null) {
			restoreCcpm(project, new String(draftCcpm, StandardCharsets.UTF_8));
		}
		if (ccpmHistory != null) restoreCcpmHistory(project, ccpmHistory);
		if (layout != null) restoreLayout(project, layout);
		if (visibility != null) restoreVisibility(project, visibility);
		if (operations != null && draftOperations != null) throw new IOException("MPOF contains both current and draft operation logs");
		if (operations == null) operations = draftOperations;
		if (operations != null) {
			OperationLog.DocumentLog operationLog = draftOperations == operations ? new OperationLog().readDocument(operations) : new OperationLog().readJsonl(operations);
			if (manifestData.documentId() != null && !manifestData.documentId().equals(operationLog.documentId()))
				throw new IOException("MPOF operation log document identity does not match its manifest");
			if (manifestData.documentId() == null) project.setDocumentId(operationLog.documentId());
			java.util.List<OperationLog.Operation> normalized = taskIdentities == null ? operationLog.operations()
				: remapTaskOperations(operationLog.operations(), readTaskIdentities(taskIdentities));
			// The snapshot is authoritative: it already represents the advertised
			// document state.  The operation log is retained as collaboration history
			// for a later explicit merge, but must not be replayed during an ordinary
			// open or it can duplicate an already-materialized change.
			MpoOperationState state = project.getOrCreateTransientDocumentState(MpoOperationState.class, MpoOperationState::new);
			state.json = new OperationLog().writeJsonl(project.getDocumentId(), normalized); state.documentId = project.getDocumentId(); state.operations.addAll(normalized); state.capture(project);
		}
		if (!extensions.entries.isEmpty()) project.getOrCreateTransientDocumentState(MpoExtensions.class, MpoExtensions::new).entries.putAll(extensions.entries);
		restoreEmbeddedProjectReferences(project, manifestData, extensions.entries, embeddedProjectFailures);
		return project;
	}

	@Override
	public void exportFile() throws Exception {
		File target = new File(fileName);
		Path lockPath = target.toPath().toAbsolutePath().resolveSibling(target.getName() + ".lock");
		// OneDrive can start two desktop writers at nearly the same time.  Lock a
		// stable sidecar (rather than the atomically replaced mpo inode) so the
		// read/merge/write transaction is serialized across JVMs as well.
		synchronized (EXPORT_LOCK_GUARD) {
			try (FileChannel lockChannel = FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
				 FileLock ignored = lockChannel.lock()) {
				exportFileLocked(target);
			}
		}
	}

	private void exportFileLocked(File target) throws Exception {
		MpoOperationState operationState = operationStateFor(project);
		operationState.appendChanges(project);
		if (target.isFile() && target.length() > 0L) mergeExternalOperations(target, project, operationState);
		// Merging may apply operations from a concurrent editor.  Serialize only
		// after that merge so content.xml and the journal describe the same state.
		byte[] snapshot = serializeProjectXml(project);
		operationState.remapTaskIds(readTaskIdentities(taskIdentitiesFor(project, snapshot).getBytes(StandardCharsets.UTF_8)));
		File temporary = createTemporaryFile(target);
		boolean completed = false;
		try (OutputStream out = new FileOutputStream(temporary)) {
			writeMpo(project, out, snapshot, operationState);
			completed = true;
		} finally {
			if (!completed) {
				Files.deleteIfExists(temporary.toPath());
			}
		}
		moveTemporary(temporary.toPath(), target.toPath());
	}

	/** Test seam for proving that a failed atomic replacement is non-destructive. */
	protected File createTemporaryFile(File target) throws IOException {
		return File.createTempFile(target.getName() + ".", ".tmp", target.getAbsoluteFile().getParentFile());
	}

	/** Replaces the destination only after the complete archive has been written. */
	protected void moveTemporary(Path temporary, Path target) throws IOException {
		try {
			Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (java.nio.file.AtomicMoveNotSupportedException e) {
			Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	@Override
	public boolean saveProject(Project project, OutputStream out) throws Exception {
		byte[] projectXml = serializeProjectXml(project);
		MpoOperationState operationState = operationStateFor(project);
		operationState.appendChanges(project);
		String taskIdentities = taskIdentitiesFor(project, projectXml);
		operationState.remapTaskIds(readTaskIdentities(taskIdentities.getBytes(StandardCharsets.UTF_8)));
		return writeMpo(project, out, projectXml, operationState);
	}

	private boolean writeMpo(Project project, OutputStream out, byte[] projectXml, MpoOperationState operationState) throws Exception {
		java.util.List<EmbeddedProject> embeddedProjects = embeddedProjectsFor(project);
		java.util.LinkedHashMap<String, byte[]> archiveEntries = new java.util.LinkedHashMap<>();
		archiveEntries.put(MIMETYPE_ENTRY, MIME_TYPE.getBytes(StandardCharsets.US_ASCII));
		archiveEntries.put(META_ENTRY, metaXml().getBytes(StandardCharsets.UTF_8));
		archiveEntries.put(PROJECT_ENTRY, projectXml);
		for (EmbeddedProject embedded : embeddedProjects)
			archiveEntries.put(embedded.entryName(), embedded.contents());
		CriticalChainService.Settings ccpm = existingCcpm(project);
		archiveEntries.put(SETTINGS_ENTRY, settingsXml(ccpm, ccpm == null ? null : new CriticalChainService().findBaseline(project)).getBytes(StandardCharsets.UTF_8));
		byte[] layout = layoutJson(project);
		if (layout != null) archiveEntries.put(LAYOUT_ENTRY, layout);
		archiveEntries.put(OPERATIONS_ENTRY, operationState.json);
		String taskIdentities = taskIdentitiesFor(project, projectXml);
		archiveEntries.put(TASK_IDENTITIES_ENTRY, taskIdentities.getBytes(StandardCharsets.UTF_8));
		archiveEntries.put(VISIBILITY_ENTRY, visibilityJson(project, taskIdentities));
		archiveEntries.put(CCPM_HISTORY_ENTRY, ccpmHistoryJson(project));
		MpoExtensions extensions = project.findTransientDocumentState(MpoExtensions.class);
		if (extensions != null) for (java.util.Map.Entry<String, byte[]> extension : extensions.entries.entrySet()) {
			if (MIMETYPE_ENTRY.equals(extension.getKey()) || MANIFEST_ENTRY.equals(extension.getKey()) || META_ENTRY.equals(extension.getKey()) || SETTINGS_ENTRY.equals(extension.getKey()) || CCPM_HISTORY_ENTRY.equals(extension.getKey()) || LAYOUT_ENTRY.equals(extension.getKey()) || VISIBILITY_ENTRY.equals(extension.getKey()) || PROJECT_ENTRY.equals(extension.getKey()) || OPERATIONS_ENTRY.equals(extension.getKey()) || TASK_IDENTITIES_ENTRY.equals(extension.getKey()) || extension.getKey().startsWith(EMBEDDED_PROJECT_PREFIX)) continue;
			archiveEntries.put(extension.getKey(), extension.getValue());
		}
		try (ZipOutputStream zip = new ZipOutputStream(out, StandardCharsets.UTF_8)) {
			writeMimetypeEntry(zip);
			writeEntry(zip, MANIFEST_ENTRY, manifestFor(projectXml, project.getDocumentId(), project.getUniqueId(),
					project.getSharedResourcePoolFile(), embeddedProjects, archiveEntries,
					project.getSharedResourcePoolProjectId()).getBytes(StandardCharsets.UTF_8));
			for (java.util.Map.Entry<String, byte[]> entry : archiveEntries.entrySet()) {
				if (!MANIFEST_ENTRY.equals(entry.getKey()) && !MIMETYPE_ENTRY.equals(entry.getKey()))
					writeEntry(zip, entry.getKey(), entry.getValue());
			}
		}
		return true;
	}

	private static MpoOperationState operationStateFor(Project project) throws IOException {
		MpoOperationState operationState = project.findTransientDocumentState(MpoOperationState.class);
		if (operationState == null) {
			operationState = project.getOrCreateTransientDocumentState(MpoOperationState.class, MpoOperationState::new);
			operationState.documentId = project.getDocumentId(); operationState.json = new OperationLog().writeJsonl(operationState.documentId, java.util.List.of()); operationState.capture(project);
		} else if (operationState.documentId == null || operationState.documentId.isBlank()) {
			operationState.documentId = project.getDocumentId();
			operationState.json = new OperationLog().writeJsonl(operationState.documentId, operationState.operations);
		} else if (!operationState.documentId.equals(project.getDocumentId())) {
			project.setDocumentId(operationState.documentId);
		}
		return operationState;
	}

	private static byte[] layoutJson(Project project) throws IOException {
		SpreadSheetFieldArray fields = project.getFieldArray();
		if (fields == null || fields.isEmpty()) return null;
		ObjectNode root = JSON.createObjectNode();
		var serializedFields = root.putArray("fields");
		for (int index = 0; index < fields.size(); index++) {
			Field field = fields.get(index);
			ObjectNode serialized = serializedFields.addObject();
			serialized.put("id", TimeDistributedHelper.getIdForObject(field));
			serialized.put("width", fields.getWidth(index));
			serialized.put("manual", fields.isManualWidth(index));
		}
		return JSON.writeValueAsBytes(root);
	}

	private static void restoreLayout(Project project, byte[] bytes) throws IOException {
		JsonNode entries = JSON.readTree(bytes).path("fields");
		if (!entries.isArray() || entries.size() == 0 || entries.size() > 512) {
			throw new IOException("Invalid MPO layout field list");
		}
		SpreadSheetFieldArray fields = new SpreadSheetFieldArray();
		var widths = new java.util.ArrayList<Integer>();
		var manualWidths = new java.util.ArrayList<Boolean>();
		for (JsonNode entry : entries) {
			String id = entry.path("id").asText(null);
			if (id == null || id.isBlank()) throw new IOException("Invalid MPO layout field id");
			Object field = TimeDistributedHelper.getObjectFromId(id);
			if (!(field instanceof Field)) throw new IOException("Unknown MPO layout field: " + id);
			int width = entry.path("width").asInt(-1);
			if (width < -1 || width > 100000) throw new IOException("Invalid MPO layout width");
			fields.add((Field) field);
			widths.add(width);
			manualWidths.add(entry.path("manual").asBoolean(false));
		}
		fields.setWidths(widths);
		fields.setManualWidths(manualWidths);
		fields.setCategory(com.microproject.graphic.configuration.SpreadSheetCategories.taskSpreadsheetCategory);
		fields.setName(project.getName());
		project.setFieldArray(fields);
	}

	private static byte[] serializeProjectXml(Project project) throws IOException {
		ByteArrayOutputStream xml = new ByteArrayOutputStream();
		MicrosoftImporter delegate = new MicrosoftImporter();
		delegate.setFileName(PROJECT_ENTRY);
		try {
			if (!delegate.saveProject(project, xml)) throw new IOException("Unable to serialize mpo project snapshot");
		} catch (Exception exception) {
			if (exception instanceof IOException io) throw io;
			throw new IOException("Unable to serialize mpo project snapshot", exception);
		}
		return xml.toByteArray();
	}

	private static String taskIdentitiesFor(Project project, byte[] projectXml) throws IOException {
		ObjectNode root = JSON.createObjectNode();
		try {
			javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
			factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
			factory.setXIncludeAware(false);
			factory.setExpandEntityReferences(false);
			factory.setAttribute(javax.xml.XMLConstants.ACCESS_EXTERNAL_DTD, "");
			factory.setAttribute(javax.xml.XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
			org.w3c.dom.Document document = factory.newDocumentBuilder().parse(new ByteArrayInputStream(projectXml));
			org.w3c.dom.NodeList xmlTasks = document.getElementsByTagName("Task");
			java.util.Iterator<?> modelTasks = project.getTaskOutlineIterator();
			for (int i = 0; i < xmlTasks.getLength() && modelTasks.hasNext(); i++) {
				Task task = (Task) modelTasks.next();
				org.w3c.dom.NodeList uids = ((org.w3c.dom.Element) xmlTasks.item(i)).getElementsByTagName("UID");
				if (uids.getLength() > 0) root.put(String.valueOf(task.getUniqueId()), Long.parseLong(uids.item(0).getTextContent()));
			}
			return JSON.writeValueAsString(root);
		} catch (Exception exception) {
			throw new IOException("Unable to build mpo task identity map", exception);
		}
	}

	private static byte[] visibilityJson(Project project, String taskIdentities) throws IOException {
		try {
			JsonNode identities = JSON.readTree(taskIdentities);
			if (!identities.isObject()) throw new IOException("Invalid mpo task identity map");
			ObjectNode root = JSON.createObjectNode();
			root.put("version", 1);
			com.fasterxml.jackson.databind.node.ArrayNode hidden = root.putArray("hiddenTaskUniqueIds");
			java.util.Iterator<java.util.Map.Entry<String, JsonNode>> fields = identities.fields();
			while (fields.hasNext()) {
				java.util.Map.Entry<String, JsonNode> entry = fields.next();
				long sourceId = Long.parseLong(entry.getKey());
				if (project.findByUniqueId(sourceId) instanceof Task task && task.isHiddenTask())
					hidden.add(entry.getValue().longValue());
			}
			return JSON.writeValueAsBytes(root);
		} catch (NumberFormatException exception) {
			throw new IOException("Invalid mpo task identity", exception);
		}
	}

	private static void restoreVisibility(Project project, byte[] bytes) throws IOException {
		JsonNode root = JSON.readTree(bytes);
		JsonNode hidden = root.path("hiddenTaskUniqueIds");
		if (!root.path("version").canConvertToInt() || root.path("version").intValue() != 1 || !hidden.isArray())
			throw new IOException("Invalid mpo visibility state");
		for (JsonNode entry : hidden) {
			if (!entry.canConvertToLong()) throw new IOException("Invalid mpo hidden task identity");
			long serializedId = entry.longValue();
			Task task = project.findByUniqueId(serializedId);
			if (task != null) task.setHiddenTask(true);
		}
	}

	private static java.util.Map<Long, Long> readTaskIdentities(byte[] json) throws IOException {
		JsonNode root = JSON.readTree(json);
		if (!root.isObject()) throw new IOException("Invalid mpo task identity map");
		java.util.Map<Long, Long> result = new java.util.LinkedHashMap<>();
		java.util.Iterator<java.util.Map.Entry<String, JsonNode>> fields = root.fields();
		while (fields.hasNext()) {
			java.util.Map.Entry<String, JsonNode> entry = fields.next();
			try {
				if (!entry.getValue().canConvertToLong()) throw new NumberFormatException();
				result.put(Long.valueOf(entry.getKey()), Long.valueOf(entry.getValue().longValue()));
			} catch (NumberFormatException exception) {
				throw new IOException("Invalid mpo task identity", exception);
			}
		}
		return result;
	}

	private static java.util.List<OperationLog.Operation> remapTaskOperations(java.util.List<OperationLog.Operation> operations, java.util.Map<Long, Long> identities) {
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

	private static void mergeExternalOperations(File target, Project project, MpoOperationState local) throws IOException {
		ExternalMpo external = externalOperations(target);
		if (!local.documentId.equals(external.document.documentId())) throw new IOException("Cannot merge mpo files with different document IDs");
		if (!local.documentId.equals(external.manifestDocumentId)) throw new IOException("Cannot merge mpo with mismatched manifest document ID");
		// The numeric Project unique id is an in-memory/importer identifier and is
		// intentionally not used as a collaboration identity: MSPDI reloads may
		// allocate a different value for the same document.  The stable mpo
		// documentId above is the authoritative identity at this boundary.
		java.util.List<OperationLog.Operation> all = new java.util.ArrayList<OperationLog.Operation>(local.operations);
		java.util.Set<String> locallyAppliedOperationIds = new java.util.LinkedHashSet<String>();
		for (OperationLog.Operation operation : local.operations)
			locallyAppliedOperationIds.add(operation.id());
		all.addAll(external.taskIdentities == null ? external.document.operations() : remapTaskOperations(external.document.operations(), readTaskIdentities(external.taskIdentities)));
		try {
			OperationLog.MergeResult merged = new OperationLog().merge(all);
			// The snapshot already contains every local operation.  Replaying that
			// history is not idempotent for moves (the original parent may no longer
			// exist), so apply only operations introduced by the external archive.
		// A causally dependent external operation still sees its local parent in
			// the current snapshot.
			java.util.List<OperationLog.Operation> externalReady = merged.ready().stream()
					.filter(operation -> !locallyAppliedOperationIds.contains(operation.id())).toList();
			applyMergedOperationsOnEdt(project, externalReady);
			local.operations.clear(); local.operations.addAll(merged.ready()); local.operations.addAll(merged.pending());
			local.json = new OperationLog().writeJsonl(local.documentId, local.operations);
			local.capture(project);
			MpoExtensions localExtensions = project.getOrCreateTransientDocumentState(MpoExtensions.class, MpoExtensions::new);
			for (java.util.Map.Entry<String, byte[]> entry : external.extensions.entries.entrySet()) localExtensions.entries.putIfAbsent(entry.getKey(), entry.getValue().clone());
		} catch (IllegalArgumentException exception) {
			throw new IOException("Cannot merge conflicting mpo operation logs", exception);
		}
	}

	private static ExternalMpo externalOperations(File target) throws IOException {
		byte[] mimetype = null; byte[] meta = null; byte[] settings = null; byte[] history = null;
		byte[] layout = null; byte[] visibility = null; byte[] draftCcpm = null; byte[] operations = null; byte[] draftOperations = null;
		byte[] manifest = null; byte[] projectXml = null; byte[] taskIdentities = null;
		int[] totalBytes = new int[] { 0 }; int entryCount = 0; MpoExtensions extensions = new MpoExtensions();
		try (InputStream in = new FileInputStream(target); ZipInputStream zip = new ZipInputStream(in, StandardCharsets.UTF_8)) {
			ZipEntry entry;
			while ((entry = zip.getNextEntry()) != null) {
				if (++entryCount > MAX_ENTRIES) throw new IOException("mpo has too many entries");
				if (!entry.isDirectory() && MIMETYPE_ENTRY.equals(entry.getName())) { if (mimetype != null) throw new IOException("Duplicate mpo entry: " + MIMETYPE_ENTRY); mimetype = readEntry(zip, totalBytes); }
				else if (!entry.isDirectory() && META_ENTRY.equals(entry.getName())) { if (meta != null) throw new IOException("Duplicate mpo entry: " + META_ENTRY); meta = readEntry(zip, totalBytes); }
				else if (!entry.isDirectory() && SETTINGS_ENTRY.equals(entry.getName())) { if (settings != null) throw new IOException("Duplicate mpo entry: " + SETTINGS_ENTRY); settings = readEntry(zip, totalBytes); }
				else if (!entry.isDirectory() && CCPM_HISTORY_ENTRY.equals(entry.getName())) { if (history != null) throw new IOException("Duplicate mpo entry: " + CCPM_HISTORY_ENTRY); history = readEntry(zip, totalBytes); }
				else if (!entry.isDirectory() && LAYOUT_ENTRY.equals(entry.getName())) { if (layout != null) throw new IOException("Duplicate mpo entry: " + LAYOUT_ENTRY); layout = readEntry(zip, totalBytes); }
				else if (!entry.isDirectory() && VISIBILITY_ENTRY.equals(entry.getName())) { if (visibility != null) throw new IOException("Duplicate mpo entry: " + VISIBILITY_ENTRY); visibility = readEntry(zip, totalBytes); }
				else if (!entry.isDirectory() && DRAFT_CCPM_ENTRY.equals(entry.getName())) { if (draftCcpm != null) throw new IOException("Duplicate draft mpo entry: " + DRAFT_CCPM_ENTRY); draftCcpm = readEntry(zip, totalBytes); }
				else if (!entry.isDirectory() && OPERATIONS_ENTRY.equals(entry.getName())) { if (operations != null) throw new IOException("Duplicate mpo entry: " + OPERATIONS_ENTRY); operations = readEntry(zip, totalBytes); }
				else if (!entry.isDirectory() && DRAFT_OPERATIONS_ENTRY.equals(entry.getName())) { if (draftOperations != null) throw new IOException("Duplicate draft mpo entry: " + DRAFT_OPERATIONS_ENTRY); draftOperations = readEntry(zip, totalBytes); }
				else if (!entry.isDirectory() && MANIFEST_ENTRY.equals(entry.getName())) { if (manifest != null) throw new IOException("Duplicate manifest entry"); manifest = readEntry(zip, totalBytes); }
				else if (!entry.isDirectory() && PROJECT_ENTRY.equals(entry.getName())) { if (projectXml != null) throw new IOException("Duplicate project snapshot entry"); projectXml = readEntry(zip, totalBytes); }
				else if (!entry.isDirectory() && TASK_IDENTITIES_ENTRY.equals(entry.getName())) { if (taskIdentities != null) throw new IOException("Duplicate mpo entry: " + TASK_IDENTITIES_ENTRY); taskIdentities = readEntry(zip, totalBytes); }
				else if (!entry.isDirectory()) { validateExtensionName(entry.getName()); if (extensions.entries.containsKey(entry.getName())) throw new IOException("Duplicate mpo extension: " + entry.getName()); extensions.entries.put(entry.getName(), readEntry(zip, totalBytes)); }
				zip.closeEntry();
			}
		}
		if (manifest == null || projectXml == null) throw new IOException("Cannot merge MPOF without its manifest and project snapshot");
		if (mimetype != null && !MIME_TYPE.equals(new String(mimetype, StandardCharsets.UTF_8).trim()))
			throw new IOException("Invalid MPOF mimetype entry");
		ManifestData manifestData = readManifest(manifest, projectXml);
		if (meta != null) validateMeta(meta);
		validateArchiveChecksums(manifestData, mimetype, meta, settings, history, layout, visibility, draftCcpm,
				operations, draftOperations, taskIdentities, projectXml, extensions.entries);
		if (settings != null && draftCcpm != null) throw new IOException("MPOF contains both current and draft CCPM settings");
		if (operations != null && draftOperations != null) throw new IOException("MPOF contains both current and draft operation logs");
		if (operations == null) operations = draftOperations;
		if (operations == null) throw new IOException("Cannot merge MPOF without an operation log");
		OperationLog.DocumentLog document = draftOperations == operations ? new OperationLog().readDocument(operations) : new OperationLog().readJsonl(operations);
		String manifestDocumentId = manifestData.documentId();
		Long manifestProjectId = manifestData.projectUniqueId();
		return new ExternalMpo(document, extensions, manifestDocumentId, manifestProjectId, taskIdentities);
	}

	private record EmbeddedProject(String sourcePath, String entryName, byte[] contents, String referenceId) { }
	private record EmbeddedProjectReference(String sourcePath, String entryName, String sha256, String referenceId) { }
	private record ManifestData(String documentId, Long projectUniqueId, Long sharedResourcePoolProjectId, String sharedResourcePoolPath,
			java.util.List<EmbeddedProjectReference> embeddedProjects, java.util.Map<String, String> checksums) { }
	private record ExternalMpo(OperationLog.DocumentLog document, MpoExtensions extensions, String manifestDocumentId, Long manifestProjectId, byte[] taskIdentities) { }

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

	private static byte[] readEntry(InputStream in, int[] totalBytes) throws IOException {
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

	private static void writeEntry(ZipOutputStream zip, String name, byte[] data) throws IOException {
		ZipEntry entry = new ZipEntry(name);
		entry.setTime(0L);
		zip.putNextEntry(entry);
		zip.write(data);
		zip.closeEntry();
	}

	/** ODF convention: the first ZIP entry is an uncompressed {@code mimetype}. */
	private static void writeMimetypeEntry(ZipOutputStream zip) throws IOException {
		ZipEntry entry = new ZipEntry(MIMETYPE_ENTRY);
		entry.setTime(0L);
		zip.putNextEntry(entry);
		zip.write(MIME_TYPE.getBytes(StandardCharsets.US_ASCII));
		zip.closeEntry();
	}

	static String manifestFor(byte[] projectXml) {
		return manifestFor(projectXml, null, null);
	}

	static String manifestFor(byte[] projectXml, String documentId, Long projectUniqueId) {
		return manifestFor(projectXml, documentId, projectUniqueId, null, java.util.List.of());
	}

	private static String manifestFor(byte[] projectXml, String documentId, Long projectUniqueId,
			String sharedResourcePoolPath, java.util.List<EmbeddedProject> embeddedProjects) {
		return manifestFor(projectXml, documentId, projectUniqueId, sharedResourcePoolPath, embeddedProjects, null);
	}

	private static String manifestFor(byte[] projectXml, String documentId, Long projectUniqueId,
			String sharedResourcePoolPath, java.util.List<EmbeddedProject> embeddedProjects,
			java.util.Map<String, byte[]> archiveEntries) {
		return manifestFor(projectXml, documentId, projectUniqueId, sharedResourcePoolPath, embeddedProjects,
				archiveEntries, 0L);
	}

	private static String manifestFor(byte[] projectXml, String documentId, Long projectUniqueId,
			String sharedResourcePoolPath, java.util.List<EmbeddedProject> embeddedProjects,
			java.util.Map<String, byte[]> archiveEntries, long sharedResourcePoolProjectId) {
		StringBuilder manifest = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<manifest format=\"mpof\" formatVersion=\"")
			.append(FORMAT_VERSION).append("\" projectEntry=\"").append(PROJECT_ENTRY).append("\" projectSha256=\"").append(sha256(projectXml)).append("\"");
		if (documentId != null) manifest.append(" documentId=\"").append(xmlEscape(documentId)).append("\"");
		if (documentId != null && projectUniqueId != null) manifest.append(" projectUniqueId=\"").append(projectUniqueId.longValue()).append("\"");
		if (sharedResourcePoolPath != null && !sharedResourcePoolPath.isBlank())
			manifest.append(" sharedResourcePoolPath=\"").append(xmlEscape(sharedResourcePoolPath)).append("\"");
		if (sharedResourcePoolProjectId > 0L)
			manifest.append(" sharedResourcePoolProjectId=\"").append(sharedResourcePoolProjectId).append("\"");
		boolean hasArchiveChecksums = archiveEntries != null && !archiveEntries.isEmpty();
		if ((embeddedProjects == null || embeddedProjects.isEmpty()) && !hasArchiveChecksums) return manifest.append("/>\n").toString();
		manifest.append(">\n");
		if (embeddedProjects != null) for (EmbeddedProject embedded : embeddedProjects) {
			manifest.append("  <embeddedProject entry=\"").append(xmlEscape(embedded.entryName())).append("\" sourcePath=\"")
				.append(xmlEscape(embedded.sourcePath())).append("\" sha256=\"").append(sha256(embedded.contents())).append("\"");
			if (embedded.referenceId() != null && !embedded.referenceId().isBlank())
				manifest.append(" referenceId=\"").append(xmlEscape(embedded.referenceId())).append("\"");
			manifest.append("/>\n");
		}
		if (archiveEntries != null) for (java.util.Map.Entry<String, byte[]> entry : archiveEntries.entrySet()) {
			if (MANIFEST_ENTRY.equals(entry.getKey())) continue;
			manifest.append("  <entry path=\"").append(xmlEscape(entry.getKey())).append("\" sha256=\"")
				.append(sha256(entry.getValue())).append("\"/>\n");
		}
		return manifest.append("</manifest>\n").toString();
	}

	/**
	 * Accepts any MPOF container whose major version equals {@link #SUPPORTED_FORMAT_MAJOR}.
	 * Files written by an older or newer minor revision load unchanged and are rewritten at
	 * {@link #FORMAT_VERSION} the next time they are saved, so opening and saving upgrades a
	 * file to the current layout (issue #356). A different major version cannot be read
	 * safely and is reported as such instead of as a generic invalid-manifest error.
	 */
	static void requireReadableFormatVersion(String value) throws IOException {
		if (value == null || value.isBlank()) throw new IOException("MPOF manifest is missing formatVersion");
		MpoFormatVersion version = MpoFormatVersion.parse(value);
		if (!version.isReadableBy(MpoFormatVersion.CURRENT)) {
			throw new IOException("Unsupported MPOF format version " + value + "; this build reads version "
				+ SUPPORTED_FORMAT_MAJOR + ".x and writes " + FORMAT_VERSION);
		}
	}

	static void validateManifest(String manifest, byte[] projectXml) throws IOException {
		readManifest(manifest.getBytes(StandardCharsets.UTF_8), projectXml);
	}

	private static java.util.List<EmbeddedProject> embeddedProjectsFor(Project master) throws IOException {
		if (master == null)
			return java.util.List.of();
		java.util.List<EmbeddedProject> embedded = new java.util.ArrayList<EmbeddedProject>();
		java.util.Map<String, String> sourcePaths = new java.util.LinkedHashMap<String, String>();
		if (master.getSharedResourcePoolFile() != null && !master.getSharedResourcePoolFile().isBlank())
			sourcePaths.put(master.getSharedResourcePoolFile(), null);
		int index = 0;
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
		java.util.Set<String> seen = new java.util.LinkedHashSet<String>();
		for (java.util.Map.Entry<String, String> sourceEntry : sourcePaths.entrySet()) {
			String sourcePath = sourceEntry.getKey();
			File source = new File(sourcePath).getCanonicalFile();
			if (!source.isFile())
				throw new IOException("Cannot embed linked project or resource pool because its file is unavailable: " + sourcePath);
			if (!seen.add(source.getPath()))
				continue;
			byte[] contents = Files.readAllBytes(source.toPath());
			if (contents.length > MAX_ENTRY_BYTES)
				throw new IOException("Linked subproject exceeds MPOF entry limit: " + sourcePath);
			String name = source.getName().replaceAll("[^A-Za-z0-9._-]", "_");
			if (name.isBlank()) name = "project.mpo";
			embedded.add(new EmbeddedProject(source.getPath(), EMBEDDED_PROJECT_PREFIX + (++index) + "-" + name, contents,
					sourceEntry.getValue()));
		}
		return embedded;
	}

	/**
	 * The master snapshot remains authoritative when one optional embedded child
	 * is damaged.  Structural manifest defects still fail the archive, while
	 * absent or tampered child payloads are reported on their corresponding
	 * master reference so every other project can be opened and repaired.
	 */
	private static java.util.Map<String, SubProj.LoadStatus> validateEmbeddedProjects(ManifestData manifest,
			java.util.Map<String, byte[]> entries) throws IOException {
		if (manifest.embeddedProjects().size() > MAX_ENTRIES)
			throw new IOException("MPOF manifest has too many embedded projects");
		java.util.Set<String> seen = new java.util.HashSet<String>();
		java.util.Map<String, SubProj.LoadStatus> failures = new java.util.LinkedHashMap<String, SubProj.LoadStatus>();
		for (EmbeddedProjectReference reference : manifest.embeddedProjects()) {
			if (!seen.add(reference.entryName()))
				throw new IOException("MPOF manifest has a duplicate embedded project entry: " + reference.entryName());
			byte[] contents = entries.get(reference.entryName());
			if (contents == null)
				failures.put(reference.sourcePath(), SubProj.LoadStatus.MISSING);
			else if (!sha256(contents).equals(reference.sha256()))
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

	private static void validateEmbeddedProjectPayload(byte[] contents) throws IOException {
		byte[] mimetype = null; byte[] manifest = null; byte[] projectXml = null; byte[] meta = null;
		byte[] settings = null; byte[] history = null; byte[] layout = null; byte[] visibility = null; byte[] draftCcpm = null;
		byte[] operations = null; byte[] draftOperations = null; byte[] taskIdentities = null;
		MpoExtensions extensions = new MpoExtensions();
		try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(contents), StandardCharsets.UTF_8)) {
			ZipEntry entry;
			int count = 0; int[] totalBytes = new int[] { 0 };
			while ((entry = zip.getNextEntry()) != null) {
				if (++count > MAX_ENTRIES) throw new IOException("Embedded MPOF has too many entries");
				if (entry.isDirectory()) continue;
				if (MIMETYPE_ENTRY.equals(entry.getName())) { if (mimetype != null) throw new IOException("Embedded MPOF has duplicate mimetype"); mimetype = readEntry(zip, totalBytes); }
				else if (MANIFEST_ENTRY.equals(entry.getName())) { if (manifest != null) throw new IOException("Embedded MPOF has duplicate manifest"); manifest = readEntry(zip, totalBytes); }
				else if (PROJECT_ENTRY.equals(entry.getName())) { if (projectXml != null) throw new IOException("Embedded MPOF has duplicate project payload"); projectXml = readEntry(zip, totalBytes); }
				else if (META_ENTRY.equals(entry.getName())) { if (meta != null) throw new IOException("Embedded MPOF has duplicate meta"); meta = readEntry(zip, totalBytes); }
				else if (SETTINGS_ENTRY.equals(entry.getName())) { if (settings != null) throw new IOException("Embedded MPOF has duplicate settings"); settings = readEntry(zip, totalBytes); }
				else if (CCPM_HISTORY_ENTRY.equals(entry.getName())) { if (history != null) throw new IOException("Embedded MPOF has duplicate CCPM history"); history = readEntry(zip, totalBytes); }
				else if (LAYOUT_ENTRY.equals(entry.getName())) { if (layout != null) throw new IOException("Embedded MPOF has duplicate layout"); layout = readEntry(zip, totalBytes); }
				else if (VISIBILITY_ENTRY.equals(entry.getName())) { if (visibility != null) throw new IOException("Embedded MPOF has duplicate visibility"); visibility = readEntry(zip, totalBytes); }
				else if (DRAFT_CCPM_ENTRY.equals(entry.getName())) { if (draftCcpm != null) throw new IOException("Embedded MPOF has duplicate draft CCPM settings"); draftCcpm = readEntry(zip, totalBytes); }
				else if (OPERATIONS_ENTRY.equals(entry.getName())) { if (operations != null) throw new IOException("Embedded MPOF has duplicate operations"); operations = readEntry(zip, totalBytes); }
				else if (DRAFT_OPERATIONS_ENTRY.equals(entry.getName())) { if (draftOperations != null) throw new IOException("Embedded MPOF has duplicate draft operations"); draftOperations = readEntry(zip, totalBytes); }
				else if (TASK_IDENTITIES_ENTRY.equals(entry.getName())) { if (taskIdentities != null) throw new IOException("Embedded MPOF has duplicate task identities"); taskIdentities = readEntry(zip, totalBytes); }
				else { validateExtensionName(entry.getName()); if (extensions.entries.put(entry.getName(), readEntry(zip, totalBytes)) != null) throw new IOException("Embedded MPOF has duplicate extension: " + entry.getName()); }
			}
		}
		if (manifest == null || projectXml == null)
			throw new IOException("Embedded MPOF is missing its manifest or project payload");
		if (mimetype != null && !MIME_TYPE.equals(new String(mimetype, StandardCharsets.UTF_8).trim()))
			throw new IOException("Embedded MPOF has an invalid mimetype");
		ManifestData embeddedManifest = readManifest(manifest, projectXml);
		if (meta != null) validateMeta(meta);
		validateArchiveChecksums(embeddedManifest, mimetype, meta, settings, history, layout, visibility, draftCcpm,
				operations, draftOperations, taskIdentities, projectXml, extensions.entries);
		if (settings != null && draftCcpm != null) throw new IOException("Embedded MPOF has both current and draft CCPM settings");
		if (operations != null && draftOperations != null) throw new IOException("Embedded MPOF has both current and draft operations");
		if (operations != null) new OperationLog().readJsonl(operations);
		else if (draftOperations != null) new OperationLog().readDocument(draftOperations);
		try {
			javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
			factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
			factory.setXIncludeAware(false);
			factory.setExpandEntityReferences(false);
			factory.newDocumentBuilder().parse(new ByteArrayInputStream(projectXml));
		} catch (Exception invalidXml) {
			throw new IOException("Embedded MPOF project payload is not valid XML", invalidXml);
		}
	}

	/** Operation replay mutates tasks and can repaint an open spreadsheet, so it must use the EDT. */
	private static void applyMergedOperationsOnEdt(Project project, java.util.List<OperationLog.Operation> operations) throws IOException {
		Runnable apply = () -> {
			try {
				new MpoTaskOperationService().apply(project, operations);
			} catch (IOException exception) {
				throw new UncheckedIOException(exception);
			}
		};
		if (SwingUtilities.isEventDispatchThread()) {
			try {
				apply.run();
			} catch (UncheckedIOException exception) {
				throw exception.getCause();
			}
			return;
		}
		try {
			SwingUtilities.invokeAndWait(apply);
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new IOException("Interrupted while applying merged MPO operations", exception);
		} catch (java.lang.reflect.InvocationTargetException exception) {
			if (exception.getCause() instanceof UncheckedIOException ioException)
				throw ioException.getCause();
			throw new IOException("Could not apply merged MPO operations", exception.getCause());
		}
	}

	private static void restoreEmbeddedProjectReferences(Project master, ManifestData manifest,
			java.util.Map<String, byte[]> entries, java.util.Map<String, SubProj.LoadStatus> failures) throws IOException {
		if (manifest.embeddedProjects().isEmpty())
			return;
		Path directory = Files.createTempDirectory("microproject-mpof-");
		directory.toFile().deleteOnExit();
		java.util.Map<String, String> extractedBySource = new java.util.LinkedHashMap<String, String>();
		for (EmbeddedProjectReference reference : manifest.embeddedProjects()) {
			if (failures.containsKey(reference.sourcePath()))
				continue;
			byte[] contents = entries.remove(reference.entryName());
			Path target = directory.resolve(new File(reference.entryName()).getName());
			Files.write(target, contents, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
			target.toFile().deleteOnExit();
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
	}

	private static long rewriteExtractedProjectReferences(Path extractedFile,
			java.util.Map<String, String> extractedBySource) throws IOException {
		java.util.Map<String, byte[]> entries = new java.util.LinkedHashMap<String, byte[]>();
		try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(extractedFile), StandardCharsets.UTF_8)) {
			ZipEntry entry;
			while ((entry = zip.getNextEntry()) != null) {
				if (entry.isDirectory()) continue;
				entries.put(entry.getName(), zip.readAllBytes());
			}
		}
		byte[] projectXml = entries.get(PROJECT_ENTRY);
		byte[] manifestXml = entries.get(MANIFEST_ENTRY);
		if (projectXml == null || manifestXml == null)
			throw new IOException("Embedded project is not an MPOF archive: " + extractedFile.getFileName());
		ManifestData originalManifest = readManifest(manifestXml, projectXml);
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
			String escapedEntry = java.util.regex.Pattern.quote(xmlEscape(nested.entryName()));
			manifestText = manifestText.replaceAll("(?m)^\\s*<embeddedProject\\b[^>]*entry=\\\""
					+ escapedEntry + "\\\"[^>]*/>\\s*\\r?\\n?", "");
			manifestText = manifestText.replaceAll("(?m)^\\s*<entry\\s+path=\\\"" + escapedEntry
					+ "\\\"[^>]*/>\\s*\\r?\\n?", "");
		}
		manifestText = manifestText.replaceFirst("projectSha256=\\\"[^\\\"]*\\\"",
				"projectSha256=\\\"" + sha256(rewrittenProject) + "\\\"");
		// New MPOF manifests also checksum every payload entry.  Keep the
		// content.xml checksum in sync after portable path rewriting.
		manifestText = manifestText.replaceFirst("(path=\\\"" + java.util.regex.Pattern.quote(PROJECT_ENTRY)
				+ "\\\" sha256=\\\")[^\\\"]*(\\\")",
				"$1" + sha256(rewrittenProject) + "$2");
		entries.put(PROJECT_ENTRY, rewrittenProject);
		entries.put(MANIFEST_ENTRY, manifestText.getBytes(StandardCharsets.UTF_8));
		Path temporary = Files.createTempFile(extractedFile.getParent(), extractedFile.getFileName().toString(), ".tmp");
		try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(temporary, StandardOpenOption.TRUNCATE_EXISTING), StandardCharsets.UTF_8)) {
			for (java.util.Map.Entry<String, byte[]> entry : entries.entrySet())
				writeEntry(zip, entry.getKey(), entry.getValue());
		}
		try {
			Files.move(temporary, extractedFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
		} catch (java.nio.file.AtomicMoveNotSupportedException unsupported) {
			Files.move(temporary, extractedFile, StandardCopyOption.REPLACE_EXISTING);
		}
		return originalManifest.projectUniqueId() == null ? 0L : originalManifest.projectUniqueId().longValue();
	}

	private static byte[] rewriteEmbeddedXml(byte[] xml, java.util.Map<String, String> extractedBySource) {
		String rewritten = new String(xml, StandardCharsets.UTF_8);
		for (java.util.Map.Entry<String, String> entry : extractedBySource.entrySet()) {
			String source = entry.getKey();
			String target = xmlEscape(entry.getValue());
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
				String escaped = xmlEscape(path);
				rewritten = java.util.regex.Pattern.compile(java.util.regex.Pattern.quote(escaped),
						java.util.regex.Pattern.CASE_INSENSITIVE)
					.matcher(rewritten)
					.replaceAll(java.util.regex.Matcher.quoteReplacement(target));
			}
		}
		return rewritten.getBytes(StandardCharsets.UTF_8);
	}

	private static SubProj.LoadStatus embeddedFailureFor(String sourcePath,
			java.util.Map<String, SubProj.LoadStatus> failures) {
		for (java.util.Map.Entry<String, SubProj.LoadStatus> entry : failures.entrySet())
			if (sameCanonicalPath(sourcePath, entry.getKey()))
				return entry.getValue();
		return null;
	}

	private static boolean sameCanonicalPath(String first, String second) {
		if (first == null || second == null)
			return false;
		try {
			return new File(first).getCanonicalFile().equals(new File(second).getCanonicalFile());
		} catch (IOException exception) {
			return new File(first).getAbsoluteFile().equals(new File(second).getAbsoluteFile());
		}
	}

	private static void validateEmbeddedEntryName(String name) throws IOException {
		validateExtensionName(name);
		if (!name.startsWith(EMBEDDED_PROJECT_PREFIX) || name.length() <= EMBEDDED_PROJECT_PREFIX.length())
			throw new IOException("Invalid MPOF embedded project entry");
	}

	private static ManifestData readManifest(byte[] manifestBytes, byte[] projectXml) throws IOException {
		String document = new String(manifestBytes, StandardCharsets.UTF_8).trim();
		if (document.startsWith("{")) return readDraftJsonManifest(document, projectXml);
		try {
			javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
			factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
			factory.setXIncludeAware(false); factory.setExpandEntityReferences(false);
			org.w3c.dom.Element root = factory.newDocumentBuilder().parse(new ByteArrayInputStream(manifestBytes)).getDocumentElement();
			if (!"manifest".equals(root.getTagName()) || !FORMAT_ID.equals(root.getAttribute("format")) || !PROJECT_ENTRY.equals(root.getAttribute("projectEntry"))) throw new IOException("Unsupported or invalid MPOF manifest");
			requireReadableFormatVersion(root.getAttribute("formatVersion"));
			if (!sha256(projectXml).equals(requiredAttribute(root, "projectSha256"))) throw new IOException("content.xml checksum does not match its MPOF manifest");
			String documentId = root.getAttribute("documentId");
			Long projectUniqueId = root.hasAttribute("projectUniqueId") ? Long.valueOf(root.getAttribute("projectUniqueId")) : null;
			Long sharedResourcePoolProjectId = root.hasAttribute("sharedResourcePoolProjectId")
					? Long.valueOf(root.getAttribute("sharedResourcePoolProjectId")) : null;
			java.util.List<EmbeddedProjectReference> embeddedProjects = new java.util.ArrayList<EmbeddedProjectReference>();
			java.util.Map<String, String> checksums = new java.util.LinkedHashMap<>();
			org.w3c.dom.NodeList children = root.getChildNodes();
			for (int index = 0; index < children.getLength(); index++) {
				if (!(children.item(index) instanceof org.w3c.dom.Element child))
					continue;
				if ("embeddedProject".equals(child.getTagName())) {
					String entry = requiredAttribute(child, "entry");
					validateEmbeddedEntryName(entry);
					String referenceId = child.hasAttribute("referenceId") ? child.getAttribute("referenceId") : null;
					if (referenceId != null && !referenceId.isBlank()) java.util.UUID.fromString(referenceId);
					embeddedProjects.add(new EmbeddedProjectReference(requiredAttribute(child, "sourcePath"), entry,
						requiredAttribute(child, "sha256"), referenceId));
				} else if ("entry".equals(child.getTagName())) {
					String path = requiredAttribute(child, "path");
					if (MANIFEST_ENTRY.equals(path) || checksums.put(path, requiredAttribute(child, "sha256")) != null)
						throw new IOException("Duplicate MPOF checksum entry: " + path);
				} else throw new IOException("Unsupported MPOF manifest entry: " + child.getTagName());
			}
			String sharedResourcePoolPath = root.getAttribute("sharedResourcePoolPath");
			return new ManifestData(documentId.isBlank() ? null : documentId, projectUniqueId,
				sharedResourcePoolProjectId,
				sharedResourcePoolPath.isBlank() ? null : sharedResourcePoolPath, embeddedProjects, checksums);
		} catch (IOException exception) { throw exception; }
		catch (Exception exception) { throw new IOException("Invalid MPOF manifest", exception); }
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
			CriticalChainService service = new CriticalChainService();
			CriticalChainService.Settings target = service.settings(project);
			target.setEnabled(settings.isEnabled());
			target.setBufferFraction(settings.getBufferFraction());
			target.setLevelingOrder(settings.getLevelingOrder());
			target.setOnlyWithinAvailableSlack(settings.isOnlyWithinAvailableSlack());
			target.setAllowTaskSplits(settings.isAllowTaskSplits());
			JsonNode baseline = root.get("baseline");
			if (baseline != null) service.restoreBaseline(project, baseline(baseline));
		} catch (RuntimeException e) {
			throw new IOException("Invalid ccpm.json", e);
		}
	}

	private static ManifestData readDraftJsonManifest(String manifest, byte[] projectXml) throws IOException {
		JsonNode root = object(manifest, MANIFEST_ENTRY);
		String format = root.path("format").isTextual() ? root.path("format").textValue() : null;
		if (!FORMAT_ID.equals(format) || !PROJECT_ENTRY.equals(text(root, "projectEntry"))) throw new IOException("Unsupported or invalid draft MPOF manifest: format=" + format);
		requireReadableFormatVersion(text(root, "formatVersion"));
		if (!sha256(projectXml).equals(text(root, "projectSha256"))) throw new IOException("content.xml checksum does not match its draft MPOF manifest");
		String documentId = root.path("documentId").isTextual() ? root.path("documentId").textValue() : null;
		Long projectUniqueId = root.path("projectUniqueId").canConvertToLong() ? Long.valueOf(root.path("projectUniqueId").longValue()) : null;
		return new ManifestData(documentId, projectUniqueId, null, null, java.util.List.of(), java.util.Map.of());
	}

	/** Validates checksums for the complete archive when written by a checksum-aware MPOF writer. */
	private static void validateArchiveChecksums(ManifestData manifest, byte[] mimetype, byte[] meta,
			byte[] settings, byte[] history, byte[] layout, byte[] visibility, byte[] draftCcpm, byte[] operations,
			byte[] draftOperations, byte[] taskIdentities, byte[] projectXml,
			java.util.Map<String, byte[]> extensions) throws IOException {
		if (manifest.checksums().isEmpty()) return; // Older MPOF versions only checksum content.xml and children.
		java.util.Map<String, byte[]> actual = new java.util.LinkedHashMap<>();
		if (mimetype != null) actual.put(MIMETYPE_ENTRY, mimetype);
		if (meta != null) actual.put(META_ENTRY, meta);
		if (settings != null) actual.put(SETTINGS_ENTRY, settings);
		if (history != null) actual.put(CCPM_HISTORY_ENTRY, history);
		if (layout != null) actual.put(LAYOUT_ENTRY, layout);
		if (visibility != null) actual.put(VISIBILITY_ENTRY, visibility);
		if (draftCcpm != null) actual.put(DRAFT_CCPM_ENTRY, draftCcpm);
		if (operations != null) actual.put(OPERATIONS_ENTRY, operations);
		if (draftOperations != null) actual.put(DRAFT_OPERATIONS_ENTRY, draftOperations);
		if (taskIdentities != null) actual.put(TASK_IDENTITIES_ENTRY, taskIdentities);
		actual.put(PROJECT_ENTRY, projectXml);
		actual.putAll(extensions);
		for (java.util.Map.Entry<String, String> checksum : manifest.checksums().entrySet()) {
			// Embedded children are recoverable units: a bad child must be reported on
			// its SubProj reference while the valid siblings and master remain open.
			if (checksum.getKey().startsWith(EMBEDDED_PROJECT_PREFIX)) continue;
			byte[] bytes = actual.get(checksum.getKey());
			if (bytes == null)
				throw new IOException("MPOF manifest checksum entry is missing from archive: " + checksum.getKey());
			if (!sha256(bytes).equals(checksum.getValue()))
				throw new IOException("MPOF entry checksum does not match: " + checksum.getKey());
		}
	}

	private static String metaXml() {
		String version = com.microproject.util.VersionUtils.getVersion();
		return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<meta formatVersion=\"1.0\" generator=\"microProject\"" +
			" " + LEVELING_DELAY_UNIT_ATTRIBUTE + "=\"" + LEVELING_DELAY_UNIT_MINUTES + "\"" +
			(version == null ? "" : " applicationVersion=\"" + xmlEscape(version) + "\"") + "/>\n";
	}

	/**
	 * MPOF snapshots written before the unit marker stored the engine's raw
	 * millisecond value in MSPDI's serialized delay field. Restrict conversion to these
	 * microProject files: ordinary MSPDI imports remain standards-compliant.
	 */
	private static boolean isLegacyMicroprojectLevelingDelay(byte[] xmlBytes) throws IOException {
		try {
			javax.xml.parsers.DocumentBuilderFactory factory = secureDocumentBuilderFactory();
			org.w3c.dom.Element root = factory.newDocumentBuilder().parse(new ByteArrayInputStream(xmlBytes)).getDocumentElement();
			return "microProject".equals(root.getAttribute("generator"))
				&& !LEVELING_DELAY_UNIT_MINUTES.equals(root.getAttribute(LEVELING_DELAY_UNIT_ATTRIBUTE));
		} catch (Exception exception) { throw new IOException("Invalid meta.xml", exception); }
	}

	private static byte[] migrateLegacyLevelingDelays(byte[] xmlBytes) throws IOException {
		String xml = new String(xmlBytes, StandardCharsets.UTF_8);
		Matcher matcher = LEVELING_DELAY.matcher(xml);
		StringBuffer migrated = new StringBuffer(xml.length());
		while (matcher.find()) {
			try {
				// MPXJ serializes a minute-format leveling delay in tenths of a
				// minute. Normalize both the value and its format rather than relying
				// on the stale format written by the legacy serializer.
				long serializedMinutes = Long.parseLong(matcher.group(1)) / 6000L;
				matcher.appendReplacement(migrated, Matcher.quoteReplacement("<LevelingDelay>" + serializedMinutes + "</LevelingDelay>"));
			} catch (NumberFormatException exception) {
				throw new IOException("Invalid legacy LevelingDelay", exception);
			}
		}
		matcher.appendTail(migrated);
		return LEVELING_DELAY_FORMAT.matcher(migrated).replaceAll("<LevelingDelayFormat>" + LEVELING_DELAY_FORMAT_MINUTES + "</LevelingDelayFormat>")
			.getBytes(StandardCharsets.UTF_8);
	}

	private static javax.xml.parsers.DocumentBuilderFactory secureDocumentBuilderFactory() throws javax.xml.parsers.ParserConfigurationException {
		javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
		factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
		factory.setXIncludeAware(false); factory.setExpandEntityReferences(false);
		return factory;
	}

	private static void validateMeta(byte[] xmlBytes) throws IOException {
		try {
			javax.xml.parsers.DocumentBuilderFactory factory = secureDocumentBuilderFactory();
			org.w3c.dom.Element root = factory.newDocumentBuilder().parse(new ByteArrayInputStream(xmlBytes)).getDocumentElement();
			if (!"meta".equals(root.getTagName()) || root.getAttribute("generator").isBlank()) throw new IOException("Invalid meta.xml");
			requireReadableFormatVersion(root.getAttribute("formatVersion"));
		} catch (IOException exception) { throw exception; }
		catch (Exception exception) { throw new IOException("Invalid meta.xml", exception); }
	}

	private static String settingsXml(CriticalChainService.Settings settings, CriticalChainService.Baseline baseline) {
		StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<settings schemaVersion=\"1\">");
		if (settings != null) {
			xml.append("<ccpm enabled=\"").append(settings.isEnabled()).append("\" bufferFraction=\"").append(settings.getBufferFraction())
				.append("\" levelingOrder=\"").append(settings.getLevelingOrder()).append("\" onlyWithinAvailableSlack=\"").append(settings.isOnlyWithinAvailableSlack())
				.append("\" allowTaskSplits=\"").append(settings.isAllowTaskSplits()).append("\">");
			if (baseline != null) {
				xml.append("<baseline projectFinishMillis=\"").append(baseline.projectFinishMillis()).append("\" projectBufferMillis=\"").append(baseline.projectBufferMillis())
					.append("\" bufferFraction=\"").append(baseline.bufferFraction()).append("\" allResources=\"").append(baseline.allResources()).append("\"><criticalTaskIds>");
				for (Long id : baseline.criticalTaskIds()) xml.append("<id>").append(id).append("</id>");
				xml.append("</criticalTaskIds><feedingTaskStartMillis>");
				for (java.util.Map.Entry<Long, Long> entry : baseline.feedingTaskStartMillis().entrySet()) xml.append("<value taskId=\"").append(entry.getKey()).append("\">").append(entry.getValue()).append("</value>");
				xml.append("</feedingTaskStartMillis><feedingBufferMillis>");
				for (java.util.Map.Entry<Long, Long> entry : baseline.feedingBufferMillis().entrySet()) xml.append("<value taskId=\"").append(entry.getKey()).append("\">").append(entry.getValue()).append("</value>");
				xml.append("</feedingBufferMillis><resourceIds>");
				for (Long id : baseline.resourceIds()) xml.append("<id>").append(id).append("</id>");
				xml.append("</resourceIds></baseline>");
			}
			xml.append("</ccpm>");
		}
		return xml.append("</settings>\n").toString();
	}

	private static void restoreSettings(Project project, byte[] xmlBytes) throws IOException {
		try {
			javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
			factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
			factory.setXIncludeAware(false); factory.setExpandEntityReferences(false);
			org.w3c.dom.Element root = factory.newDocumentBuilder().parse(new ByteArrayInputStream(xmlBytes)).getDocumentElement();
			if (!"settings".equals(root.getTagName()) || !"1".equals(root.getAttribute("schemaVersion"))) throw new IOException("Unsupported settings.xml schemaVersion");
			org.w3c.dom.NodeList nodes = root.getElementsByTagName("ccpm");
			if (nodes.getLength() == 0) return;
			org.w3c.dom.Element value = (org.w3c.dom.Element) nodes.item(0);
			CriticalChainService.Settings settings = new CriticalChainService.Settings();
			settings.setEnabled(Boolean.parseBoolean(requiredAttribute(value, "enabled")));
			settings.setBufferFraction(Double.parseDouble(requiredAttribute(value, "bufferFraction")));
			settings.setLevelingOrder(com.microproject.pm.resource.ResourceLevelingService.Order.valueOf(requiredAttribute(value, "levelingOrder")));
			settings.setOnlyWithinAvailableSlack(Boolean.parseBoolean(requiredAttribute(value, "onlyWithinAvailableSlack")));
			settings.setAllowTaskSplits(Boolean.parseBoolean(requiredAttribute(value, "allowTaskSplits")));
			CriticalChainService service = new CriticalChainService(); CriticalChainService.Settings target = service.settings(project);
			target.setEnabled(settings.isEnabled()); target.setBufferFraction(settings.getBufferFraction()); target.setLevelingOrder(settings.getLevelingOrder());
			target.setOnlyWithinAvailableSlack(settings.isOnlyWithinAvailableSlack()); target.setAllowTaskSplits(settings.isAllowTaskSplits());
			org.w3c.dom.NodeList baselines = value.getElementsByTagName("baseline");
			if (baselines.getLength() > 0) {
				org.w3c.dom.Element baseline = (org.w3c.dom.Element) baselines.item(0);
				java.util.List<Long> critical = new java.util.ArrayList<>(); org.w3c.dom.NodeList ids = baseline.getElementsByTagName("id");
				for (int i = 0; i < ids.getLength(); i++) critical.add(Long.valueOf(ids.item(i).getTextContent()));
				java.util.List<Long> resources = xmlLongList(baseline, "resourceIds");
				boolean allResources = !baseline.hasAttribute("allResources") || Boolean.parseBoolean(requiredAttribute(baseline, "allResources"));
				service.restoreBaseline(project, new CriticalChainService.Baseline(Long.parseLong(requiredAttribute(baseline, "projectFinishMillis")), Long.parseLong(requiredAttribute(baseline, "projectBufferMillis")), Double.parseDouble(requiredAttribute(baseline, "bufferFraction")), critical, xmlLongMap(baseline, "feedingTaskStartMillis"), xmlLongMap(baseline, "feedingBufferMillis"), allResources, resources));
			}
		} catch (Exception exception) { throw new IOException("Invalid settings.xml", exception); }
	}

	private static java.util.Map<Long, Long> xmlLongMap(org.w3c.dom.Element parent, String name) {
		java.util.Map<Long, Long> result = new java.util.LinkedHashMap<>(); org.w3c.dom.NodeList values = ((org.w3c.dom.Element) parent.getElementsByTagName(name).item(0)).getElementsByTagName("value");
		for (int i = 0; i < values.getLength(); i++) { org.w3c.dom.Element value = (org.w3c.dom.Element) values.item(i); result.put(Long.valueOf(value.getAttribute("taskId")), Long.valueOf(value.getTextContent())); }
		return result;
	}
	private static java.util.List<Long> xmlLongList(org.w3c.dom.Element parent, String name) {
		java.util.List<Long> result = new java.util.ArrayList<>(); org.w3c.dom.NodeList parents = parent.getElementsByTagName(name);
		if (parents.getLength() == 0) return result;
		org.w3c.dom.NodeList ids = ((org.w3c.dom.Element) parents.item(0)).getElementsByTagName("id");
		for (int i = 0; i < ids.getLength(); i++) result.add(Long.valueOf(ids.item(i).getTextContent()));
		return result;
	}

	private static String requiredAttribute(org.w3c.dom.Element element, String name) throws IOException {
		String value = element.getAttribute(name); if (value == null || value.isBlank()) throw new IOException("Missing settings.xml attribute: " + name); return value;
	}
	private static String xmlEscape(String value) { return value.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;"); }

	private static byte[] ccpmHistoryJson(Project project) throws IOException {
		CriticalChainBufferHistory history = project.findTransientDocumentState(CriticalChainBufferHistory.class);
		if (history == null || history.points().isEmpty()) return new byte[0];
		StringBuilder jsonl = new StringBuilder();
		for (CriticalChainBufferHistory.Point point : history.points()) {
			ObjectNode value = JSON.createObjectNode();
			value.put("observedAt", point.observedAt().toString());
			value.put("actorId", point.actorId()); value.put("actorName", point.actorName());
			value.put("progressPercent", point.progressPercent()); value.put("consumptionPercent", point.consumptionPercent());
			value.put("zone", point.zone()); value.put("baselineId", point.baselineId());
			jsonl.append(JSON.writeValueAsString(value)).append('\n');
		}
		return jsonl.toString().getBytes(StandardCharsets.UTF_8);
	}

	private static void restoreCcpmHistory(Project project, byte[] bytes) throws IOException {
		CriticalChainBufferHistory history = project.getOrCreateTransientDocumentState(CriticalChainBufferHistory.class, CriticalChainBufferHistory::new);
		String content = new String(bytes, StandardCharsets.UTF_8);
		for (String line : content.split("\\R")) {
			if (line.isBlank()) continue;
			try {
				JsonNode value = JSON.readTree(line);
				history.add(new CriticalChainBufferHistory.Point(Instant.parse(value.path("observedAt").asText()),
					value.path("actorId").asText("unknown"), value.path("actorName").asText("unknown"),
					value.path("progressPercent").asDouble(), value.path("consumptionPercent").asDouble(),
					value.path("zone").asText("UNKNOWN"), value.path("baselineId").asText("")));
			} catch (RuntimeException | IOException exception) {
				throw new IOException("Invalid CCPM history entry", exception);
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

	private static JsonNode object(String json, String name) throws IOException {
		JsonNode root = JSON.readTree(json);
		if (root == null || !root.isObject()) throw new IOException(name + " must be a JSON object");
		return root;
	}

	private static String text(JsonNode object, String field) throws IOException {
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

	private static void validateExtensionName(String name) throws IOException {
		if (name == null || name.isEmpty() || name.startsWith("/") || name.indexOf('\\') >= 0 || name.contains(".."))
			throw new IOException("Unsafe mpo extension name");
	}

	private static final class MpoExtensions {
		private final java.util.SortedMap<String, byte[]> entries = new java.util.TreeMap<String, byte[]>();
	}
	private static final class MpoOperationState {
		private byte[] json; private String documentId; private final String actorId = java.util.UUID.randomUUID().toString();
		private final java.util.List<OperationLog.Operation> operations = new java.util.ArrayList<OperationLog.Operation>();
		private final java.util.Map<Long, String> snapshots = new java.util.LinkedHashMap<Long, String>();
		private final java.util.Map<Long, Long> parentSnapshots = new java.util.LinkedHashMap<Long, Long>();
		private final java.util.Set<String> dependencySnapshots = new java.util.LinkedHashSet<String>();
		private final java.util.Map<String, String> assignmentSnapshots = new java.util.LinkedHashMap<String, String>();
		private void capture(Project project) { snapshots.clear(); parentSnapshots.clear(); dependencySnapshots.clear(); assignmentSnapshots.clear(); for (java.util.Iterator<?> it = project.getTaskOutlineIterator(); it.hasNext();) { Task task = (Task) it.next(); Long id = Long.valueOf(task.getUniqueId()); snapshots.put(id, signature(task)); parentSnapshots.put(id, parentId(task)); for (java.util.Iterator<?> links = task.getSuccessorList().iterator(); links.hasNext();) { Dependency dependency = (Dependency) links.next(); dependencySnapshots.add(dependencyKey(dependency)); } if (task instanceof NormalTask) for (java.util.Iterator<?> assignments = ((NormalTask) task).getAssignments().iterator(); assignments.hasNext();) { Assignment assignment = (Assignment) assignments.next(); if (!assignment.isDefault()) assignmentSnapshots.put(assignmentKey(assignment), assignmentValue(assignment)); } } }
		private void remapTaskIds(java.util.Map<Long, Long> identities) throws IOException {
			java.util.List<OperationLog.Operation> normalized = remapTaskOperations(operations, identities);
			operations.clear(); operations.addAll(normalized); json = new OperationLog().writeJsonl(documentId, operations);
		}
		private void appendChanges(Project project) throws IOException {
			boolean changed = false; long sequence = operations.stream().filter(value -> actorId.equals(value.actorId())).mapToLong(OperationLog.Operation::sequence).max().orElse(0L);
			java.util.Set<Long> seen = new java.util.LinkedHashSet<Long>();
			for (java.util.Iterator<?> it = project.getTaskOutlineIterator(); it.hasNext();) { com.microproject.pm.task.Task task = (com.microproject.pm.task.Task) it.next(); Long id = Long.valueOf(task.getUniqueId()); seen.add(id); String signature = signature(task); Long parentId = parentId(task); String kind = snapshots.containsKey(id) ? "task.update" : "task.create"; if (!snapshots.containsKey(id) || !signature.equals(snapshots.get(id)) || !java.util.Objects.equals(parentId, parentSnapshots.get(id))) { java.util.Map<String,Object> payload = new java.util.LinkedHashMap<String,Object>(); payload.put("legacyUniqueId", id); if (snapshots.containsKey(id) && !java.util.Objects.equals(parentId, parentSnapshots.get(id))) { kind = "task.move"; if (parentId != null) payload.put("parentLegacyUniqueId", parentId); } else { payload.put("name", task.getName()); payload.put("notes", task.getNotes()); payload.put("percentComplete", Double.valueOf(task.getPercentComplete())); } operations.add(new OperationLog.Operation(java.util.UUID.randomUUID().toString(), actorId, ++sequence, java.util.Set.of(), kind, java.util.UUID.nameUUIDFromBytes((documentId + ":" + task.getUniqueId()).getBytes(StandardCharsets.UTF_8)).toString(), payload)); changed = true; } }
			for (Long id : snapshots.keySet()) if (!seen.contains(id)) { operations.add(new OperationLog.Operation(java.util.UUID.randomUUID().toString(), actorId, ++sequence, java.util.Set.of(), "task.delete", java.util.UUID.nameUUIDFromBytes((documentId + ":" + id).getBytes(StandardCharsets.UTF_8)).toString(), java.util.Map.of("legacyUniqueId", id))); changed = true; }
			java.util.Set<String> dependencies = new java.util.LinkedHashSet<String>(); java.util.Map<String, String> assignments = new java.util.LinkedHashMap<String, String>();
			for (java.util.Iterator<?> it = project.getTaskOutlineIterator(); it.hasNext();) { Task task = (Task) it.next(); for (java.util.Iterator<?> links = task.getSuccessorList().iterator(); links.hasNext();) dependencies.add(dependencyKey((Dependency) links.next())); if (task instanceof NormalTask) for (java.util.Iterator<?> values = ((NormalTask) task).getAssignments().iterator(); values.hasNext();) { Assignment assignment = (Assignment) values.next(); if (!assignment.isDefault()) assignments.put(assignmentKey(assignment), assignmentValue(assignment)); } }
			for (String key : dependencies) if (!dependencySnapshots.contains(key)) { String[] parts = key.split(":", -1); java.util.Map<String,Object> payload = new java.util.LinkedHashMap<String,Object>(); payload.put("predecessorLegacyUniqueId", Long.valueOf(parts[0])); payload.put("successorLegacyUniqueId", Long.valueOf(parts[1])); payload.put("dependencyType", Integer.valueOf(parts[2])); payload.put("lag", Long.valueOf(parts[3])); addOperation("dependency.add", key, payload, ++sequence); changed = true; }
			for (String key : dependencySnapshots) if (!dependencies.contains(key)) { String[] parts = key.split(":", -1); java.util.Map<String,Object> payload = new java.util.LinkedHashMap<String,Object>(); payload.put("predecessorLegacyUniqueId", Long.valueOf(parts[0])); payload.put("successorLegacyUniqueId", Long.valueOf(parts[1])); payload.put("dependencyType", Integer.valueOf(parts[2])); payload.put("lag", Long.valueOf(parts[3])); addOperation("dependency.delete", key, payload, ++sequence); changed = true; }
			for (java.util.Map.Entry<String,String> entry : assignments.entrySet()) if (!assignmentSnapshots.containsKey(entry.getKey())) { String[] ids = entry.getKey().split(":", -1); String[] values = entry.getValue().split(":", -1); Assignment assignment = findAssignment(project, Long.parseLong(ids[0]), Long.parseLong(ids[1])); java.util.Map<String,Object> payload = new java.util.LinkedHashMap<String,Object>(); payload.put("taskLegacyUniqueId", Long.valueOf(ids[0])); payload.put("resourceUniqueId", Long.valueOf(ids[1])); payload.put("resourceName", assignment.getResource().getName()); payload.put("units", Double.valueOf(values[0])); payload.put("delay", Long.valueOf(values[1])); addOperation("assignment.add", entry.getKey(), payload, ++sequence); changed = true; }
			for (String key : assignmentSnapshots.keySet()) if (!assignments.containsKey(key)) { String[] ids = key.split(":", -1); java.util.Map<String,Object> payload = new java.util.LinkedHashMap<String,Object>(); payload.put("taskLegacyUniqueId", Long.valueOf(ids[0])); payload.put("resourceUniqueId", Long.valueOf(ids[1])); addOperation("assignment.delete", key, payload, ++sequence); changed = true; }
			if (changed) { json = new OperationLog().writeJsonl(documentId, operations); capture(project); }
		}
		private void addOperation(String kind, String key, java.util.Map<String,Object> payload, long sequence) { operations.add(new OperationLog.Operation(java.util.UUID.randomUUID().toString(), actorId, sequence, java.util.Set.of(), kind, java.util.UUID.nameUUIDFromBytes((documentId + ":" + kind + ":" + key).getBytes(StandardCharsets.UTF_8)).toString(), payload)); }
		private static String signature(com.microproject.pm.task.Task task) { return String.valueOf(task.getName()) + "\u0000" + String.valueOf(task.getNotes()) + "\u0000" + task.getPercentComplete(); }
		private static Long parentId(com.microproject.pm.task.Task task) { return task.getWbsParentTask() == null ? null : Long.valueOf(task.getWbsParentTask().getUniqueId()); }
		private static String dependencyKey(Dependency dependency) { return dependency.getPredecessorId() + ":" + dependency.getSuccessorId() + ":" + dependency.getDependencyType() + ":" + dependency.getLag(); }
		private static String assignmentKey(Assignment assignment) { return assignment.getTask().getUniqueId() + ":" + assignment.getResource().getUniqueId(); }
		private static String assignmentValue(Assignment assignment) { return assignment.getUnits() + ":" + assignment.getDelay(); }
		private static Assignment findAssignment(Project project, long taskId, long resourceId) { Task task = project.findByUniqueId(taskId); if (task instanceof NormalTask) for (java.util.Iterator<?> it = ((NormalTask) task).getAssignments().iterator(); it.hasNext();) { Assignment assignment = (Assignment) it.next(); if (!assignment.isDefault() && assignment.getResource().getUniqueId() == resourceId) return assignment; } throw new IllegalStateException("Assignment snapshot disappeared"); }
	}

	private static String sha256(byte[] data) {
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
