/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.exchange;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.fasterxml.jackson.databind.JsonNode;
import com.microproject.pm.ccpm.CriticalChainService;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.SubProj;
import com.microproject.pm.assignment.Assignment;
import com.microproject.util.MarkupEscape;

/**
	* Reads, writes and validates the MPOF v1.0 container manifest, its meta and
	* settings documents, the archive checksums and the legacy metadata migrations.
	*/
final class MpoManifest {

	private MpoManifest() {
	}

	static String manifestFor(byte[] projectXml) {
		return manifestFor(projectXml, null, null);
	}

	static String manifestFor(byte[] projectXml, String documentId, Long projectUniqueId) {
		return manifestFor(projectXml, documentId, projectUniqueId, null, java.util.List.of());
	}

	static String manifestFor(byte[] projectXml, String documentId, Long projectUniqueId,
			String sharedResourcePoolPath, java.util.List<EmbeddedProject> embeddedProjects) {
		return manifestFor(projectXml, documentId, projectUniqueId, sharedResourcePoolPath, embeddedProjects, null);
	}

	static String manifestFor(byte[] projectXml, String documentId, Long projectUniqueId,
			String sharedResourcePoolPath, java.util.List<EmbeddedProject> embeddedProjects,
			java.util.Map<String, byte[]> archiveEntries) {
		return manifestFor(projectXml, documentId, projectUniqueId, sharedResourcePoolPath, embeddedProjects,
				archiveEntries, 0L);
	}

	static String manifestFor(byte[] projectXml, String documentId, Long projectUniqueId,
			String sharedResourcePoolPath, java.util.List<EmbeddedProject> embeddedProjects,
			java.util.Map<String, byte[]> archiveEntries, long sharedResourcePoolProjectId) {
		return manifestFor(projectXml, documentId, projectUniqueId, sharedResourcePoolPath, embeddedProjects,
				archiveEntries, sharedResourcePoolProjectId, 0L);
	}

	static String manifestFor(byte[] projectXml, String documentId, Long projectUniqueId,
			String sharedResourcePoolPath, java.util.List<EmbeddedProject> embeddedProjects,
			java.util.Map<String, byte[]> archiveEntries, long sharedResourcePoolProjectId, long statusDate) {
		StringBuilder manifest = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<manifest format=\"mpof\" formatVersion=\"")
			.append(MpoFileImporter.FORMAT_VERSION).append("\" projectEntry=\"").append(MpoFileImporter.PROJECT_ENTRY).append("\" projectSha256=\"").append(MpoFileImporter.sha256(projectXml)).append("\"");
		if (documentId != null) manifest.append(" documentId=\"").append(MarkupEscape.escape(documentId)).append("\"");
		if (documentId != null && projectUniqueId != null) manifest.append(" projectUniqueId=\"").append(projectUniqueId.longValue()).append("\"");
		if (sharedResourcePoolPath != null && !sharedResourcePoolPath.isBlank())
			manifest.append(" sharedResourcePoolPath=\"").append(MarkupEscape.escape(sharedResourcePoolPath)).append("\"");
		if (sharedResourcePoolProjectId != 0L)
			manifest.append(" sharedResourcePoolProjectId=\"").append(sharedResourcePoolProjectId).append("\"");
		if (statusDate > 0L)
			manifest.append(" statusDate=\"").append(statusDate).append("\"");
		boolean hasArchiveChecksums = archiveEntries != null && !archiveEntries.isEmpty();
		if ((embeddedProjects == null || embeddedProjects.isEmpty()) && !hasArchiveChecksums) return manifest.append("/>\n").toString();
		manifest.append(">\n");
		if (embeddedProjects != null) for (EmbeddedProject embedded : embeddedProjects) {
			manifest.append("  <embeddedProject entry=\"").append(MarkupEscape.escape(embedded.entryName())).append("\" sourcePath=\"")
				.append(MarkupEscape.escape(embedded.sourcePath())).append("\" sha256=\"").append(MpoFileImporter.sha256(embedded.contents())).append("\"");
			if (embedded.referenceId() != null && !embedded.referenceId().isBlank())
				manifest.append(" referenceId=\"").append(MarkupEscape.escape(embedded.referenceId())).append("\"");
			manifest.append("/>\n");
		}
		if (archiveEntries != null) for (java.util.Map.Entry<String, byte[]> entry : archiveEntries.entrySet()) {
			if (MpoFileImporter.MANIFEST_ENTRY.equals(entry.getKey())) continue;
			manifest.append("  <entry path=\"").append(MarkupEscape.escape(entry.getKey())).append("\" sha256=\"")
				.append(MpoFileImporter.sha256(entry.getValue())).append("\"/>\n");
		}
		return manifest.append("</manifest>\n").toString();
	}
	static void requireReadableFormatVersion(String value) throws IOException {
		if (value == null || value.isBlank()) throw new IOException("MPOF manifest is missing formatVersion");
		MpoFormatVersion version = MpoFormatVersion.parse(value);
		if (!version.isReadableBy(MpoFormatVersion.CURRENT)) {
			throw new IOException("Unsupported MPOF format version " + value + "; this build reads version "
				+ MpoFileImporter.SUPPORTED_FORMAT_MAJOR + ".x and writes " + MpoFileImporter.FORMAT_VERSION);
		}
	}
	static void validateManifest(String manifest, byte[] projectXml) throws IOException {
		readManifest(manifest.getBytes(StandardCharsets.UTF_8), projectXml);
	}
	static ManifestData readManifest(byte[] manifestBytes, byte[] projectXml) throws IOException {
		String document = new String(manifestBytes, StandardCharsets.UTF_8).trim();
		if (document.startsWith("{")) return readDraftJsonManifest(document, projectXml);
		try {
			javax.xml.parsers.DocumentBuilderFactory factory = MpoFileImporter.secureDocumentBuilderFactory();
			org.w3c.dom.Element root = factory.newDocumentBuilder().parse(new ByteArrayInputStream(manifestBytes)).getDocumentElement();
			if (!"manifest".equals(root.getTagName()) || !MpoFileImporter.FORMAT_ID.equals(root.getAttribute("format")) || !MpoFileImporter.PROJECT_ENTRY.equals(root.getAttribute("projectEntry"))) throw new IOException("Unsupported or invalid MPOF manifest");
			requireReadableFormatVersion(root.getAttribute("formatVersion"));
			if (!MpoFileImporter.sha256(projectXml).equals(MpoFileImporter.requiredAttribute(root, "projectSha256"))) throw new IOException("content.xml checksum does not match its MPOF manifest");
			String documentId = root.getAttribute("documentId");
			Long projectUniqueId = root.hasAttribute("projectUniqueId") ? Long.valueOf(root.getAttribute("projectUniqueId")) : null;
			Long statusDate = root.hasAttribute("statusDate") ? Long.valueOf(root.getAttribute("statusDate")) : null;
			Project.validatePersistedStatusDate(statusDate == null ? 0L : statusDate.longValue());
			Long sharedResourcePoolProjectId = root.hasAttribute("sharedResourcePoolProjectId")
					? Long.valueOf(root.getAttribute("sharedResourcePoolProjectId")) : null;
			java.util.List<EmbeddedProjectReference> embeddedProjects = new java.util.ArrayList<EmbeddedProjectReference>();
			java.util.Map<String, String> checksums = new java.util.LinkedHashMap<>();
			org.w3c.dom.NodeList children = root.getChildNodes();
			for (int index = 0; index < children.getLength(); index++) {
				if (!(children.item(index) instanceof org.w3c.dom.Element child))
					continue;
				if ("embeddedProject".equals(child.getTagName())) {
					String entry = MpoFileImporter.requiredAttribute(child, "entry");
					MpoEmbeddedProjects.validateEmbeddedEntryName(entry);
					String referenceId = child.hasAttribute("referenceId") ? child.getAttribute("referenceId") : null;
					if (referenceId != null && !referenceId.isBlank()) java.util.UUID.fromString(referenceId);
					embeddedProjects.add(new EmbeddedProjectReference(MpoFileImporter.requiredAttribute(child, "sourcePath"), entry,
						MpoFileImporter.requiredAttribute(child, "sha256"), referenceId));
				} else if ("entry".equals(child.getTagName())) {
					String path = MpoFileImporter.requiredAttribute(child, "path");
					if (MpoFileImporter.MANIFEST_ENTRY.equals(path) || checksums.put(path, MpoFileImporter.requiredAttribute(child, "sha256")) != null)
						throw new IOException("Duplicate MPOF checksum entry: " + path);
				} else throw new IOException("Unsupported MPOF manifest entry: " + child.getTagName());
			}
			String sharedResourcePoolPath = root.getAttribute("sharedResourcePoolPath");
			return new ManifestData(documentId.isBlank() ? null : documentId, projectUniqueId, statusDate,
				sharedResourcePoolProjectId,
				sharedResourcePoolPath.isBlank() ? null : sharedResourcePoolPath, embeddedProjects, checksums);
		} catch (IOException exception) { throw exception; }
		catch (Exception exception) { throw new IOException("Invalid MPOF manifest", exception); }
	}
	static ManifestData readDraftJsonManifest(String manifest, byte[] projectXml) throws IOException {
		JsonNode root = MpoFileImporter.object(manifest, MpoFileImporter.MANIFEST_ENTRY);
		String format = root.path("format").isTextual() ? root.path("format").textValue() : null;
		if (!MpoFileImporter.FORMAT_ID.equals(format) || !MpoFileImporter.PROJECT_ENTRY.equals(MpoFileImporter.text(root, "projectEntry"))) throw new IOException("Unsupported or invalid draft MPOF manifest: format=" + format);
		requireReadableFormatVersion(MpoFileImporter.text(root, "formatVersion"));
		if (!MpoFileImporter.sha256(projectXml).equals(MpoFileImporter.text(root, "projectSha256"))) throw new IOException("content.xml checksum does not match its draft MPOF manifest");
		String documentId = root.path("documentId").isTextual() ? root.path("documentId").textValue() : null;
		Long projectUniqueId = root.path("projectUniqueId").canConvertToLong() ? Long.valueOf(root.path("projectUniqueId").longValue()) : null;
		return new ManifestData(documentId, projectUniqueId, null, null, null, java.util.List.of(), java.util.Map.of());
	}
	static void validateArchiveChecksums(ManifestData manifest, byte[] mimetype, byte[] meta,
			byte[] settings, byte[] history, byte[] layout, byte[] visibility, byte[] draftCcpm, byte[] operations,
			byte[] draftOperations, byte[] taskIdentities, byte[] projectXml,
			java.util.Map<String, byte[]> extensions) throws IOException {
		if (manifest.checksums().isEmpty()) return; // Older MPOF versions only checksum content.xml and children.
		java.util.Map<String, byte[]> actual = new java.util.LinkedHashMap<>();
		if (mimetype != null) actual.put(MpoFileImporter.MIMETYPE_ENTRY, mimetype);
		if (meta != null) actual.put(MpoFileImporter.META_ENTRY, meta);
		if (settings != null) actual.put(MpoFileImporter.SETTINGS_ENTRY, settings);
		if (history != null) actual.put(MpoFileImporter.CCPM_HISTORY_ENTRY, history);
		if (layout != null) actual.put(MpoFileImporter.LAYOUT_ENTRY, layout);
		if (visibility != null) actual.put(MpoFileImporter.VISIBILITY_ENTRY, visibility);
		if (draftCcpm != null) actual.put(MpoFileImporter.DRAFT_CCPM_ENTRY, draftCcpm);
		if (operations != null) actual.put(MpoFileImporter.OPERATIONS_ENTRY, operations);
		if (draftOperations != null) actual.put(MpoFileImporter.DRAFT_OPERATIONS_ENTRY, draftOperations);
		if (taskIdentities != null) actual.put(MpoFileImporter.TASK_IDENTITIES_ENTRY, taskIdentities);
		actual.put(MpoFileImporter.PROJECT_ENTRY, projectXml);
		actual.putAll(extensions);
		for (java.util.Map.Entry<String, String> checksum : manifest.checksums().entrySet()) {
			// Embedded children are recoverable units: a bad child must be reported on
			// its SubProj reference while the valid siblings and master remain open.
			if (checksum.getKey().startsWith(MpoFileImporter.EMBEDDED_PROJECT_PREFIX)) continue;
			byte[] bytes = actual.get(checksum.getKey());
			if (bytes == null)
				throw new IOException("MPOF manifest checksum entry is missing from archive: " + checksum.getKey());
			if (!MpoFileImporter.sha256(bytes).equals(checksum.getValue()))
				throw new IOException("MPOF entry checksum does not match: " + checksum.getKey());
		}
	}
	static String metaXml() {
		String version = com.microproject.util.VersionUtils.getVersion();
		return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<meta formatVersion=\"1.0\" generator=\"microProject\"" +
			" " + MpoFileImporter.LEVELING_DELAY_UNIT_ATTRIBUTE + "=\"" + MpoFileImporter.LEVELING_DELAY_UNIT_MINUTES + "\"" +
			" " + MpoFileImporter.ASSIGNMENT_BASELINE_COST_UNIT_ATTRIBUTE + "=\"" + MpoFileImporter.ASSIGNMENT_BASELINE_COST_UNIT_HUNDREDTHS + "\"" +
			(version == null ? "" : " applicationVersion=\"" + MarkupEscape.escape(version) + "\"") + "/>\n";
	}
	static boolean isLegacyMicroprojectAssignmentBaselineCost(byte[] xmlBytes) throws IOException {
		try {
			javax.xml.parsers.DocumentBuilderFactory factory = MpoFileImporter.secureDocumentBuilderFactory();
			org.w3c.dom.Element root = factory.newDocumentBuilder().parse(new ByteArrayInputStream(xmlBytes)).getDocumentElement();
			return "microProject".equals(root.getAttribute("generator"))
				&& !MpoFileImporter.ASSIGNMENT_BASELINE_COST_UNIT_HUNDREDTHS.equals(
					root.getAttribute(MpoFileImporter.ASSIGNMENT_BASELINE_COST_UNIT_ATTRIBUTE));
		} catch (Exception exception) { throw new IOException("Invalid meta.xml", exception); }
	}
	static byte[] migrateLegacyAssignmentBaselineCosts(byte[] xmlBytes) throws IOException {
		String xml = new String(xmlBytes, StandardCharsets.UTF_8);
		Pattern assignment = Pattern.compile("(<Assignment(?:\\s[^>]*)?>)([\\s\\S]*?)(</Assignment>)");
		Matcher matcher = assignment.matcher(xml);
		StringBuffer migrated = new StringBuffer(xml.length());
		while (matcher.find()) {
			String assignmentBody = matcher.group(2);
			Pattern row = Pattern.compile("(<TimephasedData(?:\\s[^>]*)?>[\\s\\S]*?<Type>)(\\d+)(</Type>[\\s\\S]*?<Value>)([^<]+)(</Value>[\\s\\S]*?</TimephasedData>)");
			Matcher rows = row.matcher(assignmentBody);
			StringBuffer migratedRows = new StringBuffer(assignmentBody.length());
			while (rows.find()) {
				int type;
				try { type = Integer.parseInt(rows.group(2)); }
				catch (NumberFormatException exception) { throw new IOException("Invalid legacy assignment baseline cost type", exception); }
				boolean baselineCost = false;
				for (int slot = 0; slot <= 10; slot++)
					if (type == com.microproject.core.pm.exchange.converters.mpx.MpxImportState
							.assignmentBaselineCostType(slot)) { baselineCost = true; break; }
				if (!baselineCost) continue;
				String value;
				try {
					value = new java.math.BigDecimal(rows.group(4)).multiply(java.math.BigDecimal.valueOf(100L))
						.stripTrailingZeros().toPlainString();
				} catch (NumberFormatException exception) {
					throw new IOException("Invalid legacy assignment baseline cost value", exception);
				}
				rows.appendReplacement(migratedRows, Matcher.quoteReplacement(rows.group(1) + rows.group(2)
					+ rows.group(3) + value + rows.group(5)));
			}
			rows.appendTail(migratedRows);
			matcher.appendReplacement(migrated, Matcher.quoteReplacement(matcher.group(1)
				+ migratedRows + matcher.group(3)));
		}
		matcher.appendTail(migrated);
		return migrated.toString().getBytes(StandardCharsets.UTF_8);
	}
	static boolean isLegacyMicroprojectLevelingDelay(byte[] xmlBytes) throws IOException {
		try {
			javax.xml.parsers.DocumentBuilderFactory factory = MpoFileImporter.secureDocumentBuilderFactory();
			org.w3c.dom.Element root = factory.newDocumentBuilder().parse(new ByteArrayInputStream(xmlBytes)).getDocumentElement();
			return "microProject".equals(root.getAttribute("generator"))
				&& !MpoFileImporter.LEVELING_DELAY_UNIT_MINUTES.equals(root.getAttribute(MpoFileImporter.LEVELING_DELAY_UNIT_ATTRIBUTE));
		} catch (Exception exception) { throw new IOException("Invalid meta.xml", exception); }
	}
	static byte[] migrateLegacyLevelingDelays(byte[] xmlBytes) throws IOException {
		String xml = new String(xmlBytes, StandardCharsets.UTF_8);
		Matcher matcher = MpoFileImporter.LEVELING_DELAY.matcher(xml);
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
		return MpoFileImporter.LEVELING_DELAY_FORMAT.matcher(migrated).replaceAll("<LevelingDelayFormat>" + MpoFileImporter.LEVELING_DELAY_FORMAT_MINUTES + "</LevelingDelayFormat>")
			.getBytes(StandardCharsets.UTF_8);
	}
	static void validateMeta(byte[] xmlBytes) throws IOException {
		try {
			javax.xml.parsers.DocumentBuilderFactory factory = MpoFileImporter.secureDocumentBuilderFactory();
			org.w3c.dom.Element root = factory.newDocumentBuilder().parse(new ByteArrayInputStream(xmlBytes)).getDocumentElement();
			if (!"meta".equals(root.getTagName()) || root.getAttribute("generator").isBlank()) throw new IOException("Invalid meta.xml");
			requireReadableFormatVersion(root.getAttribute("formatVersion"));
			String baselineCostUnit = root.getAttribute(MpoFileImporter.ASSIGNMENT_BASELINE_COST_UNIT_ATTRIBUTE);
			if (!baselineCostUnit.isEmpty()
					&& !MpoFileImporter.ASSIGNMENT_BASELINE_COST_UNIT_HUNDREDTHS.equals(baselineCostUnit))
				throw new IOException("Unsupported MPO assignment baseline cost unit: " + baselineCostUnit);
		} catch (IOException exception) { throw exception; }
		catch (Exception exception) { throw new IOException("Invalid meta.xml", exception); }
	}
	static String settingsXml(CriticalChainService.Settings settings, CriticalChainService.Baseline baseline) {
		StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<settings schemaVersion=\"1\">");
		if (settings != null) {
			xml.append("<ccpm enabled=\"").append(settings.isEnabled()).append("\" bufferFraction=\"").append(settings.getBufferFraction())
				.append("\" levelingOrder=\"").append(settings.getLevelingOrder()).append("\" onlyWithinAvailableSlack=\"").append(settings.isOnlyWithinAvailableSlack())
				.append("\" allowTaskSplits=\"").append(settings.isAllowTaskSplits())
				.append("\" levelAssignments=\"").append(settings.isLevelAssignments())
				.append("\" includeProposedResources=\"").append(settings.isIncludeProposedResources())
				.append("\" levelManuallyScheduledTasks=\"").append(settings.isLevelManuallyScheduledTasks())
				.append("\" sensitivity=\"").append(settings.getSensitivity()).append("\">");
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
	static void applySettings(CriticalChainService.Settings target, CriticalChainService.Settings settings) {
		target.setEnabled(settings.isEnabled());
		target.setBufferFraction(settings.getBufferFraction());
		target.setLevelingOrder(settings.getLevelingOrder());
		target.setOnlyWithinAvailableSlack(settings.isOnlyWithinAvailableSlack());
		target.setAllowTaskSplits(settings.isAllowTaskSplits());
		target.setLevelAssignments(settings.isLevelAssignments());
		target.setIncludeProposedResources(settings.isIncludeProposedResources());
		target.setLevelManuallyScheduledTasks(settings.isLevelManuallyScheduledTasks());
		target.setSensitivity(settings.getSensitivity());
	}
	static void restoreSettings(Project project, byte[] xmlBytes) throws IOException {
		try {
			javax.xml.parsers.DocumentBuilderFactory factory = MpoFileImporter.secureDocumentBuilderFactory();
			org.w3c.dom.Element root = factory.newDocumentBuilder().parse(new ByteArrayInputStream(xmlBytes)).getDocumentElement();
			if (!"settings".equals(root.getTagName()) || !"1".equals(root.getAttribute("schemaVersion"))) throw new IOException("Unsupported settings.xml schemaVersion");
			org.w3c.dom.NodeList nodes = root.getElementsByTagName("ccpm");
			if (nodes.getLength() == 0) return;
			org.w3c.dom.Element value = (org.w3c.dom.Element) nodes.item(0);
			CriticalChainService.Settings settings = new CriticalChainService.Settings();
			settings.setEnabled(Boolean.parseBoolean(MpoFileImporter.requiredAttribute(value, "enabled")));
			settings.setBufferFraction(Double.parseDouble(MpoFileImporter.requiredAttribute(value, "bufferFraction")));
			settings.setLevelingOrder(com.microproject.pm.resource.ResourceLevelingService.Order.valueOf(MpoFileImporter.requiredAttribute(value, "levelingOrder")));
			settings.setOnlyWithinAvailableSlack(Boolean.parseBoolean(MpoFileImporter.requiredAttribute(value, "onlyWithinAvailableSlack")));
			settings.setAllowTaskSplits(Boolean.parseBoolean(MpoFileImporter.requiredAttribute(value, "allowTaskSplits")));
			settings.setLevelAssignments(value.hasAttribute("levelAssignments")
				? Boolean.parseBoolean(value.getAttribute("levelAssignments")) : true);
			settings.setIncludeProposedResources(value.hasAttribute("includeProposedResources")
				&& Boolean.parseBoolean(value.getAttribute("includeProposedResources")));
			settings.setLevelManuallyScheduledTasks(value.hasAttribute("levelManuallyScheduledTasks")
				&& Boolean.parseBoolean(value.getAttribute("levelManuallyScheduledTasks")));
			settings.setSensitivity(value.hasAttribute("sensitivity")
				? com.microproject.pm.resource.ResourceLevelingService.Sensitivity.valueOf(value.getAttribute("sensitivity"))
				: com.microproject.pm.resource.ResourceLevelingService.Sensitivity.DAY);
			CriticalChainService service = new CriticalChainService();
			applySettings(service.settings(project), settings);
			org.w3c.dom.NodeList baselines = value.getElementsByTagName("baseline");
			if (baselines.getLength() > 0) {
				org.w3c.dom.Element baseline = (org.w3c.dom.Element) baselines.item(0);
				java.util.List<Long> critical = new java.util.ArrayList<>(); org.w3c.dom.NodeList ids = baseline.getElementsByTagName("id");
				for (int i = 0; i < ids.getLength(); i++) critical.add(Long.valueOf(ids.item(i).getTextContent()));
				java.util.List<Long> resources = MpoFileImporter.xmlLongList(baseline, "resourceIds");
				boolean allResources = !baseline.hasAttribute("allResources") || Boolean.parseBoolean(MpoFileImporter.requiredAttribute(baseline, "allResources"));
				service.restoreBaseline(project, new CriticalChainService.Baseline(Long.parseLong(MpoFileImporter.requiredAttribute(baseline, "projectFinishMillis")), Long.parseLong(MpoFileImporter.requiredAttribute(baseline, "projectBufferMillis")), Double.parseDouble(MpoFileImporter.requiredAttribute(baseline, "bufferFraction")), critical, MpoFileImporter.xmlLongMap(baseline, "feedingTaskStartMillis"), MpoFileImporter.xmlLongMap(baseline, "feedingBufferMillis"), allResources, resources));
			}
		} catch (Exception exception) { throw new IOException("Invalid settings.xml", exception); }
	}
}
