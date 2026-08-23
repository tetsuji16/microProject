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
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import java.util.Locale;

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
import com.microproject.pm.task.Project;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Task;
import com.microproject.pm.dependency.Dependency;
import com.microproject.pm.assignment.Assignment;
import com.microproject.session.LocalSession;
import com.microproject.session.SessionFactory;
import com.microproject.strings.Messages;

/**
 * Reads and writes MPOF v1.0 containers. An mpo file is a ZIP containing a
 * UTF-8 manifest and a standards-based MSPDI XML snapshot; it never embeds a
 * Java serialized object.
 */
public final class MpoFileImporter extends FileImporter {
	private static final Object EXPORT_LOCK_GUARD = new Object();
	static final String MIMETYPE_ENTRY = "mimetype";
	static final String MANIFEST_ENTRY = "META-INF/manifest.xml";
	static final String PROJECT_ENTRY = "content.xml";
	static final String META_ENTRY = "meta.xml";
	static final String SETTINGS_ENTRY = "settings.xml";
	/** MPOF v1.0 container layout (ODF conventions). */
	static final String FORMAT_ID = "mpof";
	static final String FORMAT_VERSION = "1.0";
	private static final String MIME_TYPE = "application/vnd.microproject.openproject";
	static final String OPERATIONS_ENTRY = "operations/log.jsonl";
	static final String TASK_IDENTITIES_ENTRY = "changes/task-identities.json";
	private static final int MAX_ENTRY_BYTES = 64 * 1024 * 1024;
	private static final int MAX_TOTAL_BYTES = 128 * 1024 * 1024;
	private static final int MAX_ENTRIES = 128;
	private static final ObjectMapper JSON = new ObjectMapper().enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
		.enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

	@Override
	public void importFile() throws Exception {
		try (InputStream in = fileInputStream == null ? new FileInputStream(fileName) : fileInputStream) {
			project = loadProject(in);
		}
	}

	@Override
	public Project loadProject(InputStream in) throws Exception {
		byte[] manifest = null;
		byte[] projectXml = null;
		byte[] meta = null;
		byte[] settings = null;
		byte[] operations = null;
		byte[] taskIdentities = null;
		MpoExtensions extensions = new MpoExtensions();
		int[] totalBytes = new int[] { 0 };
		int entryCount = 0;
		try (ZipInputStream zip = new ZipInputStream(in, StandardCharsets.UTF_8)) {
			ZipEntry entry;
			while ((entry = zip.getNextEntry()) != null) {
				if (++entryCount > MAX_ENTRIES) throw new IOException("MPOF file has too many entries");
				if (!entry.isDirectory() && MIMETYPE_ENTRY.equals(entry.getName())) {
					String mime = new String(readEntry(zip, totalBytes), StandardCharsets.UTF_8).trim();
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
				} else if (OPERATIONS_ENTRY.equals(entry.getName())) {
					if (operations != null) throw new IOException("Duplicate mpo entry: " + OPERATIONS_ENTRY);
					operations = readEntry(zip, totalBytes);
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
		if (manifest == null || projectXml == null || meta == null || settings == null) {
			throw new IOException("An MPOF file must contain " + MANIFEST_ENTRY + ", " + META_ENTRY + ", " + SETTINGS_ENTRY + " and " + PROJECT_ENTRY);
		}
		validateMeta(meta);
		validateManifest(new String(manifest, StandardCharsets.UTF_8), projectXml);
		MicrosoftImporter delegate = new MicrosoftImporter();
		delegate.setFileName(PROJECT_ENTRY);
		delegate.setProjectFactory(projectFactory);
		project = delegate.loadProject(new ByteArrayInputStream(projectXml));
		if (settings != null) {
			restoreSettings(project, settings);
		}
		if (operations != null) {
			OperationLog.DocumentLog operationLog = new OperationLog().readJsonl(operations);
			java.util.List<OperationLog.Operation> normalized = taskIdentities == null ? operationLog.operations()
				: remapTaskOperations(operationLog.operations(), readTaskIdentities(taskIdentities));
			new MpoTaskOperationService().apply(project, normalized);
			MpoOperationState state = project.getOrCreateTransientDocumentState(MpoOperationState.class, MpoOperationState::new);
			state.json = new OperationLog().writeJsonl(operationLog.documentId(), normalized); state.documentId = operationLog.documentId(); state.operations.addAll(normalized); state.capture(project);
		}
		if (!extensions.entries.isEmpty()) project.getOrCreateTransientDocumentState(MpoExtensions.class, MpoExtensions::new).entries.putAll(extensions.entries);
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
		byte[] snapshot = mspdiSnapshot(project);
		operationState.remapTaskIds(readTaskIdentities(taskIdentitiesFor(project, snapshot).getBytes(StandardCharsets.UTF_8)));
		if (target.isFile() && target.length() > 0L) mergeExternalOperations(target, project, operationState);
		File temporary = File.createTempFile(target.getName() + ".", ".tmp", target.getAbsoluteFile().getParentFile());
		boolean completed = false;
		try (OutputStream out = new FileOutputStream(temporary)) {
			saveProject(project, out);
			completed = true;
		} finally {
			if (!completed) {
				Files.deleteIfExists(temporary.toPath());
			}
		}
		try {
			Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (java.nio.file.AtomicMoveNotSupportedException e) {
			Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
	}

	@Override
	public boolean saveProject(Project project, OutputStream out) throws Exception {
		ByteArrayOutputStream xml = new ByteArrayOutputStream();
		MicrosoftImporter delegate = new MicrosoftImporter();
		delegate.setFileName(PROJECT_ENTRY);
		if (!delegate.saveProject(project, xml)) {
			return false;
		}
		byte[] projectXml = xml.toByteArray();
		MpoOperationState operationState = operationStateFor(project);
		operationState.appendChanges(project);
		operationState.remapTaskIds(readTaskIdentities(taskIdentitiesFor(project, projectXml).getBytes(StandardCharsets.UTF_8)));
		try (ZipOutputStream zip = new ZipOutputStream(out, StandardCharsets.UTF_8)) {
			writeMimetypeEntry(zip);
			writeEntry(zip, META_ENTRY, metaXml().getBytes(StandardCharsets.UTF_8));
			writeEntry(zip, MANIFEST_ENTRY, manifestFor(projectXml, operationState.documentId, project.getUniqueId()).getBytes(StandardCharsets.UTF_8));
			writeEntry(zip, PROJECT_ENTRY, projectXml);
			CriticalChainService.Settings ccpm = existingCcpm(project);
			if (ccpm != null) {
				writeEntry(zip, SETTINGS_ENTRY, settingsXml(ccpm, new CriticalChainService().findBaseline(project)).getBytes(StandardCharsets.UTF_8));
			}
			if (ccpm == null) writeEntry(zip, SETTINGS_ENTRY, settingsXml(null, null).getBytes(StandardCharsets.UTF_8));
			writeEntry(zip, OPERATIONS_ENTRY, operationState.json);
			writeEntry(zip, TASK_IDENTITIES_ENTRY, taskIdentitiesFor(project, projectXml).getBytes(StandardCharsets.UTF_8));
			MpoExtensions extensions = project.findTransientDocumentState(MpoExtensions.class);
			if (extensions != null) for (java.util.Map.Entry<String, byte[]> extension : extensions.entries.entrySet()) {
				if (MIMETYPE_ENTRY.equals(extension.getKey()) || MANIFEST_ENTRY.equals(extension.getKey()) || META_ENTRY.equals(extension.getKey()) || SETTINGS_ENTRY.equals(extension.getKey()) || PROJECT_ENTRY.equals(extension.getKey()) || OPERATIONS_ENTRY.equals(extension.getKey()) || TASK_IDENTITIES_ENTRY.equals(extension.getKey())) continue;
				writeEntry(zip, extension.getKey(), extension.getValue());
			}
		}
		return true;
	}

	private static MpoOperationState operationStateFor(Project project) throws IOException {
		MpoOperationState operationState = project.findTransientDocumentState(MpoOperationState.class);
		if (operationState == null) {
			operationState = project.getOrCreateTransientDocumentState(MpoOperationState.class, MpoOperationState::new);
			operationState.documentId = java.util.UUID.randomUUID().toString(); operationState.json = new OperationLog().writeJsonl(operationState.documentId, java.util.List.of()); operationState.capture(project);
		}
		return operationState;
	}

	private static byte[] mspdiSnapshot(Project project) throws IOException {
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
		all.addAll(external.taskIdentities == null ? external.document.operations() : remapTaskOperations(external.document.operations(), readTaskIdentities(external.taskIdentities)));
		try {
			OperationLog.MergeResult merged = new OperationLog().merge(all);
			// Bring non-local operations into the in-memory snapshot before writing
			// the replacement file.  Otherwise the operation log and project.xml
			// temporarily describe different projects until the next reload.
			// Replaying the complete ready set is intentional: the operation service
			// is idempotent and this preserves parent-before-child ordering when an
			// external operation depends on a local operation.
			new MpoTaskOperationService().apply(project, merged.ready());
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
		byte[] operations = null; byte[] manifest = null; byte[] projectXml = null; byte[] taskIdentities = null;
		int[] totalBytes = new int[] { 0 }; int entryCount = 0; MpoExtensions extensions = new MpoExtensions(); boolean settingsSeen = false;
		try (InputStream in = new FileInputStream(target); ZipInputStream zip = new ZipInputStream(in, StandardCharsets.UTF_8)) {
			ZipEntry entry;
			while ((entry = zip.getNextEntry()) != null) {
				if (++entryCount > MAX_ENTRIES) throw new IOException("mpo has too many entries");
				if (!entry.isDirectory() && OPERATIONS_ENTRY.equals(entry.getName())) { if (operations != null) throw new IOException("Duplicate mpo entry: " + OPERATIONS_ENTRY); operations = readEntry(zip, totalBytes); }
				else if (!entry.isDirectory() && MIMETYPE_ENTRY.equals(entry.getName())) { readEntry(zip, totalBytes); }
				else if (!entry.isDirectory() && MANIFEST_ENTRY.equals(entry.getName())) { if (manifest != null) throw new IOException("Duplicate manifest entry"); manifest = readEntry(zip, totalBytes); }
				else if (!entry.isDirectory() && PROJECT_ENTRY.equals(entry.getName())) { if (projectXml != null) throw new IOException("Duplicate project snapshot entry"); projectXml = readEntry(zip, totalBytes); }
				else if (!entry.isDirectory() && SETTINGS_ENTRY.equals(entry.getName())) { if (settingsSeen) throw new IOException("Duplicate mpo entry: " + SETTINGS_ENTRY); settingsSeen = true; readEntry(zip, totalBytes); }
				else if (!entry.isDirectory() && TASK_IDENTITIES_ENTRY.equals(entry.getName())) { if (taskIdentities != null) throw new IOException("Duplicate mpo entry: " + TASK_IDENTITIES_ENTRY); taskIdentities = readEntry(zip, totalBytes); }
				else if (!entry.isDirectory()) { validateExtensionName(entry.getName()); if (extensions.entries.containsKey(entry.getName())) throw new IOException("Duplicate mpo extension: " + entry.getName()); extensions.entries.put(entry.getName(), readEntry(zip, totalBytes)); }
				zip.closeEntry();
			}
		}
		if (manifest == null || projectXml == null) throw new IOException("Cannot merge MPOF without its manifest and project snapshot");
		ManifestData manifestData = readManifest(manifest, projectXml);
		if (operations == null) throw new IOException("Cannot merge MPOF without an operation log");
		OperationLog.DocumentLog document = new OperationLog().readJsonl(operations);
		String manifestDocumentId = manifestData.documentId();
		Long manifestProjectId = manifestData.projectUniqueId();
		return new ExternalMpo(document, extensions, manifestDocumentId, manifestProjectId, taskIdentities);
	}

	private record ManifestData(String documentId, Long projectUniqueId) { }
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
		StringBuilder manifest = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<manifest format=\"mpof\" formatVersion=\"")
			.append(FORMAT_VERSION).append("\" projectEntry=\"").append(PROJECT_ENTRY).append("\" projectSha256=\"").append(sha256(projectXml)).append("\"");
		if (documentId != null) manifest.append(" documentId=\"").append(xmlEscape(documentId)).append("\"");
		if (documentId != null && projectUniqueId != null) manifest.append(" projectUniqueId=\"").append(projectUniqueId.longValue()).append("\"");
		return manifest.append("/>\n").toString();
	}

	static void validateManifest(String manifest, byte[] projectXml) throws IOException {
		readManifest(manifest.getBytes(StandardCharsets.UTF_8), projectXml);
	}

	private static ManifestData readManifest(byte[] manifestBytes, byte[] projectXml) throws IOException {
		try {
			javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
			factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
			factory.setXIncludeAware(false); factory.setExpandEntityReferences(false);
			org.w3c.dom.Element root = factory.newDocumentBuilder().parse(new ByteArrayInputStream(manifestBytes)).getDocumentElement();
			if (!"manifest".equals(root.getTagName()) || !FORMAT_ID.equals(root.getAttribute("format")) || !FORMAT_VERSION.equals(root.getAttribute("formatVersion")) || !PROJECT_ENTRY.equals(root.getAttribute("projectEntry"))) throw new IOException("Unsupported or invalid MPOF manifest");
			if (!sha256(projectXml).equals(requiredAttribute(root, "projectSha256"))) throw new IOException("content.xml checksum does not match its MPOF manifest");
			String documentId = root.getAttribute("documentId");
			Long projectUniqueId = root.hasAttribute("projectUniqueId") ? Long.valueOf(root.getAttribute("projectUniqueId")) : null;
			return new ManifestData(documentId.isBlank() ? null : documentId, projectUniqueId);
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

	private static String metaXml() {
		String version = com.microproject.util.VersionUtils.getVersion();
		return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<meta formatVersion=\"1.0\" generator=\"microProject\"" +
			(version == null ? "" : " applicationVersion=\"" + xmlEscape(version) + "\"") + "/>\n";
	}

	private static void validateMeta(byte[] xmlBytes) throws IOException {
		try {
			javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
			factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
			factory.setXIncludeAware(false); factory.setExpandEntityReferences(false);
			org.w3c.dom.Element root = factory.newDocumentBuilder().parse(new ByteArrayInputStream(xmlBytes)).getDocumentElement();
			if (!"meta".equals(root.getTagName()) || !"1.0".equals(root.getAttribute("formatVersion")) || root.getAttribute("generator").isBlank()) throw new IOException("Invalid meta.xml");
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
					.append("\" bufferFraction=\"").append(baseline.bufferFraction()).append("\"><criticalTaskIds>");
				for (Long id : baseline.criticalTaskIds()) xml.append("<id>").append(id).append("</id>");
				xml.append("</criticalTaskIds><feedingTaskStartMillis>");
				for (java.util.Map.Entry<Long, Long> entry : baseline.feedingTaskStartMillis().entrySet()) xml.append("<value taskId=\"").append(entry.getKey()).append("\">").append(entry.getValue()).append("</value>");
				xml.append("</feedingTaskStartMillis><feedingBufferMillis>");
				for (java.util.Map.Entry<Long, Long> entry : baseline.feedingBufferMillis().entrySet()) xml.append("<value taskId=\"").append(entry.getKey()).append("\">").append(entry.getValue()).append("</value>");
				xml.append("</feedingBufferMillis></baseline>");
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
				service.restoreBaseline(project, new CriticalChainService.Baseline(Long.parseLong(requiredAttribute(baseline, "projectFinishMillis")), Long.parseLong(requiredAttribute(baseline, "projectBufferMillis")), Double.parseDouble(requiredAttribute(baseline, "bufferFraction")), critical, xmlLongMap(baseline, "feedingTaskStartMillis"), xmlLongMap(baseline, "feedingBufferMillis")));
			}
		} catch (Exception exception) { throw new IOException("Invalid settings.xml", exception); }
	}

	private static java.util.Map<Long, Long> xmlLongMap(org.w3c.dom.Element parent, String name) {
		java.util.Map<Long, Long> result = new java.util.LinkedHashMap<>(); org.w3c.dom.NodeList values = ((org.w3c.dom.Element) parent.getElementsByTagName(name).item(0)).getElementsByTagName("value");
		for (int i = 0; i < values.getLength(); i++) { org.w3c.dom.Element value = (org.w3c.dom.Element) values.item(i); result.put(Long.valueOf(value.getAttribute("taskId")), Long.valueOf(value.getTextContent())); }
		return result;
	}

	private static String requiredAttribute(org.w3c.dom.Element element, String name) throws IOException {
		String value = element.getAttribute(name); if (value == null || value.isBlank()) throw new IOException("Missing settings.xml attribute: " + name); return value;
	}
	private static String xmlEscape(String value) { return value.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;"); }

	private static CriticalChainService.Baseline baseline(JsonNode value) throws IOException {
		if (!value.isObject()) throw new IOException("Invalid CCPM baseline");
		long projectFinish = wholeNumberField(value, "projectFinishMillis");
		long projectBuffer = wholeNumberField(value, "projectBufferMillis");
		double fraction = number(value, "bufferFraction");
		if (!value.path("criticalTaskIds").isArray() || !value.path("feedingTaskStartMillis").isObject() || !value.path("feedingBufferMillis").isObject()) throw new IOException("Invalid CCPM baseline");
		java.util.List<Long> critical = new java.util.ArrayList<Long>();
		for (JsonNode id : value.path("criticalTaskIds")) critical.add(Long.valueOf(wholeNumber(id, "critical task ID")));
		return new CriticalChainService.Baseline(projectFinish, projectBuffer, fraction, critical,
			longMap(value.path("feedingTaskStartMillis")), longMap(value.path("feedingBufferMillis")));
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
