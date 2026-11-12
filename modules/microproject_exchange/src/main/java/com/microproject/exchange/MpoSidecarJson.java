/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.exchange;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.regex.Pattern;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.TaskSplitInterval;
import com.microproject.pm.task.Task;
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
import com.microproject.pm.snapshot.Snapshottable;
import com.microproject.pm.task.TaskSnapshot;
import com.microproject.field.Field;
import com.microproject.graphic.configuration.SpreadSheetFieldArray;
import com.microproject.pm.assignment.TimeDistributedHelper;

/**
	* Reads and writes the JSON sidecar entries of an MPOF v1.0 container: layout,
	* visibility, task splits, leveling options, text styles, native task schedules,
	* baselines and the validation of their records.
	*/
final class MpoSidecarJson {
	private static final ObjectMapper JSON = MpoFileImporter.JSON;
	private static final int MAX_OPERATION_COUNT = MpoFileImporter.MAX_OPERATION_COUNT;

	private MpoSidecarJson() {
	}

	static byte[] layoutJson(Project project) throws IOException {
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
			// Optional key, written only when wrapping is switched off so default layouts stay byte-identical.
			if (!fields.isHeaderWrap(index)) serialized.put("wrapHeader", false);
		}
		return JSON.writeValueAsBytes(root);
	}

	static void restoreLayout(Project project, byte[] bytes) throws IOException {
		JsonNode entries = JSON.readTree(bytes).path("fields");
		if (!entries.isArray() || entries.size() == 0 || entries.size() > 512) {
			throw new IOException("Invalid MPO layout field list");
		}
		SpreadSheetFieldArray fields = new SpreadSheetFieldArray();
		var widths = new java.util.ArrayList<Integer>();
		var manualWidths = new java.util.ArrayList<Boolean>();
		var headerWraps = new java.util.ArrayList<Boolean>();
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
			headerWraps.add(entry.path("wrapHeader").asBoolean(true));
		}
		fields.setWidths(widths);
		fields.setManualWidths(manualWidths);
		fields.setHeaderWraps(headerWraps);
		fields.setCategory(com.microproject.graphic.configuration.SpreadSheetCategories.taskSpreadsheetCategory);
		fields.setName(project.getName());
		project.setFieldArray(fields);
	}

	static byte[] serializeProjectXml(Project project) throws IOException {
		return serializeProject(project).xml();
	}

	static SerializedProjectXml serializeProject(Project project) throws IOException {
		ByteArrayOutputStream xml = new ByteArrayOutputStream();
		com.microproject.server.data.MSPDISerializer serializer = new com.microproject.server.data.MSPDISerializer();
		try {
			serializer.saveProjectOrThrow(project, xml);
			return new SerializedProjectXml(xml.toByteArray(), serializer.exportedAssignmentUniqueIds());
		} catch (Exception exception) {
			if (exception instanceof IOException io) throw io;
			throw new IOException("Unable to serialize mpo project snapshot", exception);
		}
	}

	static String taskIdentitiesFor(Project project, byte[] projectXml) throws IOException {
		ObjectNode root = JSON.createObjectNode();
		try {
			javax.xml.parsers.DocumentBuilderFactory factory = MpoFileImporter.secureDocumentBuilderFactory();
			org.w3c.dom.Document document = factory.newDocumentBuilder().parse(new ByteArrayInputStream(projectXml));
			org.w3c.dom.NodeList xmlTasks = document.getElementsByTagName("Task");
			java.util.Iterator<?> modelTasks = project.getTaskOutlineIterator();
			for (int i = 0; i < xmlTasks.getLength() && modelTasks.hasNext(); i++) {
				org.w3c.dom.Element xmlTask = (org.w3c.dom.Element) xmlTasks.item(i);
				String nullValue = childText(xmlTask, "IsNull");
				if (nullValue == null) nullValue = childText(xmlTask, "Null");
				if ("1".equals(nullValue == null ? null : nullValue.trim())) continue;
				String uid = childText(xmlTask, "UID");
				if (uid != null) {
					Task task = (Task) modelTasks.next();
					root.put(String.valueOf(task.getUniqueId()), Long.parseLong(uid));
				}
			}
			return JSON.writeValueAsString(root);
		} catch (Exception exception) {
			throw new IOException("Unable to build mpo task identity map", exception);
		}
	}

	/** Direct-child lookup; searching the subtree of every task is quadratic in practice. */
	static String childText(org.w3c.dom.Element parent, String name) {
		for (org.w3c.dom.Node child = parent.getFirstChild(); child != null; child = child.getNextSibling())
			if (name.equals(child.getNodeName())) return child.getTextContent();
		return null;
	}

	/** One pass over the outline, for callers that would otherwise search it per task. */
	static java.util.Map<Long, Task> tasksByUniqueId(Project project) {
		java.util.Map<Long, Task> tasks = new java.util.HashMap<>();
		for (java.util.Iterator<Task> iterator = project.getTaskOutlineIterator(); iterator.hasNext();) {
			Task task = iterator.next();
			tasks.putIfAbsent(Long.valueOf(task.getUniqueId()), task);
		}
		return tasks;
	}

	static byte[] visibilityJson(Project project, String taskIdentities) throws IOException {
		try {
			JsonNode identities = JSON.readTree(taskIdentities);
			if (!identities.isObject()) throw new IOException("Invalid mpo task identity map");
			ObjectNode root = JSON.createObjectNode();
			root.put("version", 1);
			com.fasterxml.jackson.databind.node.ArrayNode hidden = root.putArray("hiddenTaskUniqueIds");
			java.util.Map<Long, Task> tasks = tasksByUniqueId(project);
			java.util.Iterator<java.util.Map.Entry<String, JsonNode>> fields = identities.fields();
			while (fields.hasNext()) {
				java.util.Map.Entry<String, JsonNode> entry = fields.next();
				long sourceId = Long.parseLong(entry.getKey());
				Task task = tasks.get(Long.valueOf(sourceId));
				if (task != null && task.isHiddenTask())
					hidden.add(entry.getValue().longValue());
			}
			return JSON.writeValueAsBytes(root);
		} catch (NumberFormatException exception) {
			throw new IOException("Invalid mpo task identity", exception);
		}
	}

	static byte[] taskSplitsJson(Project project, String taskIdentities) throws IOException {
		try {
			JsonNode identities = JSON.readTree(taskIdentities);
			ObjectNode root = JSON.createObjectNode();
			root.put("version", 1);
			com.fasterxml.jackson.databind.node.ArrayNode tasks = root.putArray("tasks");
			for (java.util.Iterator<?> it = project.getTaskOutlineIterator(); it.hasNext();) {
				Task task = (Task) it.next();
				NormalTask normalTask = task instanceof NormalTask normal ? normal : null;
				Task.LevelingManualDateState manualDates = task.getLevelingManualDateState();
				if ((normalTask == null || normalTask.getTaskSplitIntervals().isEmpty()) && manualDates == null) continue;
				JsonNode serializedUid = identities.path(String.valueOf(task.getUniqueId()));
				if (!serializedUid.canConvertToLong()) throw new IOException("Missing serialized task identity for task split");
				ObjectNode taskNode = tasks.addObject();
				taskNode.put("taskUid", serializedUid.longValue());
				com.fasterxml.jackson.databind.node.ArrayNode intervals = taskNode.putArray("intervals");
				for (TaskSplitInterval interval : normalTask == null ? java.util.List.<TaskSplitInterval>of() : normalTask.getTaskSplitIntervals()) {
					ObjectNode intervalNode = intervals.addObject();
					intervalNode.put("startOffset", interval.startOffset());
					intervalNode.put("endOffset", interval.endOffset());
				}
				writeTaskSplitIntervals(taskNode.putArray("manualIntervals"), normalTask == null ? java.util.List.of() : normalTask.getManualTaskSplitIntervals());
				writeTaskSplitIntervals(taskNode.putArray("levelingIntervals"), normalTask == null ? java.util.List.of() : normalTask.getLevelingTaskSplitIntervals());
				if (manualDates != null) {
					ObjectNode dates = taskNode.putObject("manualLevelingDates");
					dates.put("originalStart", manualDates.originalStart());
					dates.put("originalFinish", manualDates.originalFinish());
					dates.put("generatedStart", manualDates.generatedStart());
					dates.put("generatedFinish", manualDates.generatedFinish());
				}
			}
			return JSON.writeValueAsBytes(root);
		} catch (IOException exception) {
			throw exception;
		} catch (Exception exception) {
			throw new IOException("Unable to serialize mpo task splits", exception);
		}
	}

	static byte[] taskLevelingOptionsJson(Project project, String taskIdentities) throws IOException {
		return perTaskOptionsJson(project, taskIdentities, "leveling options", "leveling options",
			// The current core model cannot encode MSPDI recurrence. Preserve an
			// imported recurring task's effective No/Yes values even when they equal
			// that source task's recurrence-sensitive defaults.
			task -> !task.isRecurringTask() && task.isLevelingCanSplit() && task.isLevelAssignments(),
			(task, node) -> {
				node.put("levelingCanSplit", task.isLevelingCanSplit());
				node.put("levelAssignments", task.isLevelAssignments());
			});
	}

	static void restoreTaskLevelingOptions(Project project, byte[] bytes) throws IOException {
		restorePerTaskOptions(project, bytes, "Invalid mpo task leveling options",
			"MPO contains too many task leveling options", "Duplicate MPO task leveling option identity",
			"MPO task leveling options reference an unknown task", node -> {
				JsonNode canSplit = node.get("levelingCanSplit");
				JsonNode levelAssignments = node.get("levelAssignments");
				if (canSplit == null || !canSplit.isBoolean() || levelAssignments == null || !levelAssignments.isBoolean())
					throw new IOException("Invalid MPO task leveling option values");
				return task -> {
					task.setLevelingCanSplit(canSplit.booleanValue());
					task.setLevelAssignments(levelAssignments.booleanValue());
				};
			});
	}

	static byte[] taskTextStylesJson(Project project, String taskIdentities) throws IOException {
		return perTaskOptionsJson(project, taskIdentities, "text style", "text styles",
			task -> task.getFontFamily() == null && task.getFontSize() == 0 && task.getFontColor() == null
				&& !task.isFontBold() && !task.isFontItalic() && !task.isFontStrikethrough(),
			(task, node) -> {
				if (task.getFontFamily() != null) node.put("fontFamily", task.getFontFamily());
				node.put("fontSize", task.getFontSize());
				if (task.getFontColor() != null) node.put("fontColor", task.getFontColor().intValue());
				node.put("bold", task.isFontBold());
				node.put("italic", task.isFontItalic());
				node.put("strikethrough", task.isFontStrikethrough());
			});
	}

	static void restoreTaskTextStyles(Project project, byte[] bytes) throws IOException {
		restorePerTaskOptions(project, bytes, "Invalid mpo task text styles", "MPO contains too many task text styles",
			"Duplicate MPO task text style identity", "MPO task text styles reference an unknown task", node -> {
				JsonNode family = node.get("fontFamily");
				JsonNode size = node.get("fontSize");
				JsonNode color = node.get("fontColor");
				JsonNode bold = node.get("bold");
				JsonNode italic = node.get("italic");
				JsonNode strikethrough = node.get("strikethrough");
				// Task.setFontSize accepts 0 (inherit) through 72 points; a larger stored value
				// is corrupted data, not a size to clamp silently.
				if ((family != null && !family.isTextual()) || size == null || !size.isIntegralNumber()
						|| !size.canConvertToInt() || size.intValue() < 0 || size.intValue() > 72
						|| (color != null && (!color.isIntegralNumber() || !color.canConvertToInt()
							|| color.intValue() < 0 || color.intValue() > 0xFFFFFF))
						|| bold == null || !bold.isBoolean() || italic == null || !italic.isBoolean()
						|| strikethrough == null || !strikethrough.isBoolean())
					throw new IOException("Invalid MPO task text style values");
				return task -> {
					task.setFontFamily(family == null ? null : family.textValue());
					task.setFontSize(size.intValue());
					task.setFontColor(color == null ? null : Integer.valueOf(color.intValue()));
					task.setFontBold(bold.booleanValue());
					task.setFontItalic(italic.booleanValue());
					task.setFontStrikethrough(strikethrough.booleanValue());
				};
			});
	}

	/** Validates one per-task record and returns the mutation to apply once its task is found. */
	@FunctionalInterface
	private interface TaskOptionReader {
		java.util.function.Consumer<Task> read(JsonNode node) throws IOException;
	}

	/**
	 * Shared frame of the per-task option entries: version 1 plus a "tasks" array of {taskUid, fields},
	 * listing only tasks whose options are not the defaults ({@code isDefault}).
	 */
	static byte[] perTaskOptionsJson(Project project, String taskIdentities, String identityLabel,
		String serializeLabel, java.util.function.Predicate<Task> isDefault,
		java.util.function.BiConsumer<Task, ObjectNode> fields) throws IOException {
		try {
			JsonNode identities = JSON.readTree(taskIdentities);
			ObjectNode root = JSON.createObjectNode();
			root.put("version", 1);
			var tasks = root.putArray("tasks");
			for (java.util.Iterator<?> iterator = project.getTaskOutlineIterator(); iterator.hasNext();) {
				Task task = (Task) iterator.next();
				if (isDefault.test(task)) continue;
				JsonNode serializedUid = identities.path(String.valueOf(task.getUniqueId()));
				if (!serializedUid.canConvertToLong())
					throw new IOException("Missing serialized task identity for " + identityLabel);
				ObjectNode node = tasks.addObject();
				node.put("taskUid", serializedUid.longValue());
				fields.accept(task, node);
			}
			return JSON.writeValueAsBytes(root);
		} catch (IOException exception) {
			throw exception;
		} catch (Exception exception) {
			throw new IOException("Unable to serialize mpo task " + serializeLabel, exception);
		}
	}

	static void restorePerTaskOptions(Project project, byte[] bytes, String invalidMessage,
		String tooManyMessage, String duplicateMessage, String unknownTaskMessage, TaskOptionReader reader)
		throws IOException {
		JsonNode root = JSON.readTree(bytes);
		JsonNode tasks = root.path("tasks");
		if (requiredInt(root, "version") != 1 || !tasks.isArray()) throw new IOException(invalidMessage);
		java.util.HashSet<Long> seen = new java.util.HashSet<>();
		int count = 0;
		for (JsonNode node : tasks) {
			if (++count > MAX_OPERATION_COUNT) throw new IOException(tooManyMessage);
			long serializedId = integralLong(node.path("taskUid"), "taskUid");
			if (!seen.add(Long.valueOf(serializedId))) throw new IOException(duplicateMessage);
			java.util.function.Consumer<Task> apply = reader.read(node);
			Task task = project.findByUniqueId(serializedId);
			if (task == null) throw new IOException(unknownTaskMessage);
			apply.accept(task);
		}
	}

	/** Older MPO snapshots predate these task flags; use the documented Yes defaults. */
	static void resetLegacyTaskLevelingDefaults(Project project) {
		for (java.util.Iterator<?> iterator = project.getTaskOutlineIterator(); iterator.hasNext();) {
			Task task = (Task) iterator.next();
			task.setLevelingCanSplit(!task.isRecurringTask());
			task.setLevelAssignments(true);
		}
	}

	static void restoreTaskSplits(Project project, byte[] bytes) throws IOException {
		JsonNode root = JSON.readTree(bytes);
		JsonNode tasks = root.path("tasks");
		if (requiredInt(root, "version") != 1 || !tasks.isArray()) throw new IOException("Invalid mpo task split data");
		int intervalCount = 0;
		for (JsonNode taskNode : tasks) {
			JsonNode uidNode = taskNode.path("taskUid");
			JsonNode intervalsNode = taskNode.path("intervals");
			long serializedId = integralLong(uidNode, "taskUid");
			if (!intervalsNode.isArray()) throw new IOException("Invalid mpo task split record");
			Task task = project.findByUniqueId(serializedId);
			if (task == null) throw new IOException("MPO task split references an unknown task");
			NormalTask normalTask = task instanceof NormalTask normal ? normal : null;
			if (normalTask == null && !intervalsNode.isEmpty()) throw new IOException("MPO task split references a non-normal task");
			java.util.ArrayList<TaskSplitInterval> intervals = new java.util.ArrayList<>();
			long previousEnd = Long.MIN_VALUE;
			for (JsonNode intervalNode : intervalsNode) {
				if (++intervalCount > MAX_OPERATION_COUNT) throw new IOException("MPO contains too many task split intervals");
				JsonNode startNode = intervalNode.path("startOffset");
				JsonNode endNode = intervalNode.path("endOffset");
				TaskSplitInterval interval;
				try {
					interval = new TaskSplitInterval(integralLong(startNode, "startOffset"),
						integralLong(endNode, "endOffset"));
				} catch (IllegalArgumentException exception) {
					throw new IOException("Invalid mpo task split interval", exception);
				} catch (ArithmeticException exception) {
					throw new IOException("Invalid mpo task split interval", exception);
				}
				if (interval.startOffset() < previousEnd) throw new IOException("MPO task split intervals overlap or are unsorted");
				previousEnd = interval.endOffset();
				intervals.add(interval);
			}
			java.util.ArrayList<TaskSplitInterval> manualIntervals = new java.util.ArrayList<>();
			java.util.ArrayList<TaskSplitInterval> levelingIntervals = new java.util.ArrayList<>();
			JsonNode manualNodes = taskNode.get("manualIntervals");
			JsonNode levelingNodes = taskNode.get("levelingIntervals");
			if (normalTask == null) {
				if ((manualNodes != null && !manualNodes.isArray()) || (levelingNodes != null && !levelingNodes.isArray()))
					throw new IOException("Invalid MPO task split provenance");
			} else if (manualNodes == null && levelingNodes == null) {
				manualIntervals.addAll(intervals);
			} else {
				readTaskSplitIntervals(manualNodes, manualIntervals, "manualIntervals");
				readTaskSplitIntervals(levelingNodes, levelingIntervals, "levelingIntervals");
				if (!NormalTask.unionTaskSplitIntervals(manualIntervals, levelingIntervals).equals(intervals))
					throw new IOException("MPO task split provenance does not match task split intervals");
			}
			if (normalTask != null) normalTask.restoreTaskSplitProvenance(manualIntervals, levelingIntervals);
			JsonNode dates = taskNode.get("manualLevelingDates");
			if (dates != null) {
				long originalStart = integralLong(dates.path("originalStart"), "originalStart");
				long originalFinish = integralLong(dates.path("originalFinish"), "originalFinish");
				long generatedStart = integralLong(dates.path("generatedStart"), "generatedStart");
				long generatedFinish = integralLong(dates.path("generatedFinish"), "generatedFinish");
				if (originalStart <= 0L || originalFinish < originalStart || generatedStart <= 0L || generatedFinish < generatedStart)
					throw new IOException("Invalid MPO manual leveling date provenance");
				task.setLevelingManualDateState(new Task.LevelingManualDateState(originalStart, originalFinish,
					generatedStart, generatedFinish));
			}
		}
	}

	static void writeTaskSplitIntervals(com.fasterxml.jackson.databind.node.ArrayNode nodes,
		java.util.List<TaskSplitInterval> intervals) {
		for (TaskSplitInterval interval : intervals) {
			ObjectNode intervalNode = nodes.addObject();
			intervalNode.put("startOffset", interval.startOffset());
			intervalNode.put("endOffset", interval.endOffset());
		}
	}

	static void readTaskSplitIntervals(JsonNode nodes, java.util.List<TaskSplitInterval> target,
		String fieldName) throws IOException {
		if (nodes == null || !nodes.isArray()) throw new IOException("Invalid MPO " + fieldName);
		long previousEnd = Long.MIN_VALUE;
		for (JsonNode node : nodes) {
			TaskSplitInterval interval;
			try {
				interval = new TaskSplitInterval(integralLong(node.path("startOffset"), "startOffset"),
					integralLong(node.path("endOffset"), "endOffset"));
			} catch (IllegalArgumentException | ArithmeticException exception) {
				throw new IOException("Invalid MPO " + fieldName, exception);
			}
			if (interval.startOffset() < previousEnd) throw new IOException("MPO " + fieldName + " overlap or unsorted");
			previousEnd = interval.endOffset();
			target.add(interval);
		}
	}

	static byte[] assignmentLevelingSplitsJson(Project project,
		java.util.Map<Assignment, Integer> serializedAssignmentUids) throws IOException {
		try {
			ObjectNode root = JSON.createObjectNode();
			root.put("version", 2);
			var assignments = root.putArray("assignments");
			for (java.util.Iterator<?> tasks = project.getTaskOutlineIterator(); tasks.hasNext();) {
				Task task = (Task) tasks.next();
				if (!(task instanceof NormalTask normalTask)) continue;
				for (Object value : normalTask.getAssignments()) {
					Assignment assignment = (Assignment) value;
					AssignmentLevelingSplitState state = assignment.getLevelingSplitState();
					if (state == null) continue;
					Integer uid = serializedAssignmentUids.get(assignment);
					if (uid == null) throw new IOException("Missing canonical MSPDI assignment identity for leveling split");
					ObjectNode node = assignments.addObject();
					node.put("assignmentUid", uid.intValue());
					node.put("beforeDuration", state.beforeDuration());
					node.put("generatedDuration", state.generatedDuration());
					node.put("sourceContourMatchesGenerated",
						state.matchesGenerated(assignment.getWorkContour(), assignment.getDurationMillis()));
					writeContour(node.putObject("beforeContour"), state.beforeContourSnapshot());
					writeContour(node.putObject("generatedContour"), state.generatedContourSnapshot());
					writeTaskSplitIntervals(node.putArray("generatedGaps"), state.generatedGaps());
				}
			}
			return JSON.writeValueAsBytes(root);
		} catch (IOException exception) {
			throw exception;
		} catch (Exception exception) {
			throw new IOException("Unable to serialize mpo assignment leveling splits", exception);
		}
	}

	static void restoreAssignmentLevelingSplits(Project project, byte[] bytes) throws IOException {
		JsonNode root = JSON.readTree(bytes);
		JsonNode assignments = root.path("assignments");
		int version = requiredInt(root, "version");
		if ((version != 1 && version != 2) || !assignments.isArray() || assignments.size() > MAX_OPERATION_COUNT)
			throw new IOException("Invalid mpo assignment leveling split data");
		java.util.ArrayList<AssignmentSplitReplacement> replacements = new java.util.ArrayList<>();
		java.util.HashSet<Long> seen = new java.util.HashSet<>();
		int gapCount = 0;
		for (JsonNode node : assignments) {
			long uid = integralLong(node.path("assignmentUid"), "assignmentUid");
			long beforeDuration = integralLong(node.path("beforeDuration"), "beforeDuration");
			long generatedDuration = integralLong(node.path("generatedDuration"), "generatedDuration");
			if (uid <= 0L || beforeDuration < 0L || generatedDuration < 0L || !seen.add(Long.valueOf(uid)))
				throw new IOException("Invalid or duplicate MPO assignment leveling identity");
			Assignment assignment = findAssignmentByUniqueId(project, uid);
			if (assignment == null) throw new IOException("MPO assignment leveling split references an unknown assignment uid=" + uid);
			BaselineAssignmentProfile.Contour before = readContour(node.path("beforeContour"));
			BaselineAssignmentProfile.Contour generated = readContour(node.path("generatedContour"));
			JsonNode gaps = node.path("generatedGaps");
			if (!gaps.isArray() || gaps.isEmpty()) throw new IOException("MPO assignment leveling split has no generated gaps");
			java.util.ArrayList<TaskSplitInterval> intervals = new java.util.ArrayList<>();
			readTaskSplitIntervals(gaps, intervals, "generatedGaps");
			gapCount += intervals.size();
			if (gapCount > MAX_OPERATION_COUNT) throw new IOException("MPO contains too many assignment leveling gaps");
			try {
				var beforeContour = BaselineAssignmentProfile.restoreContour(before);
				var generatedContour = BaselineAssignmentProfile.restoreContour(generated);
				AssignmentLevelingSplitState state = new AssignmentLevelingSplitState(
					beforeContour, beforeDuration, generatedContour, generatedDuration, intervals);
				boolean sourceContourMatchesGenerated = version >= 2
					&& node.path("sourceContourMatchesGenerated").isBoolean()
					&& node.path("sourceContourMatchesGenerated").booleanValue();
				if (sourceContourMatchesGenerated
					&& !state.matchesGenerated(assignment.getWorkContour(), assignment.getDurationMillis())
					&& assignment.importedProfilesMatchWorkTotal(generatedContour, generatedDuration)) {
					state = new AssignmentLevelingSplitState(beforeContour, beforeDuration,
						assignment.getWorkContour(), assignment.getDurationMillis(), intervals);
				}
				replacements.add(new AssignmentSplitReplacement(assignment, state));
			} catch (RuntimeException exception) {
				throw new IOException("Invalid MPO assignment leveling split for uid=" + uid, exception);
			}
		}
		for (AssignmentSplitReplacement replacement : replacements)
			replacement.assignment().restoreLevelingSplitState(replacement.state());
	}

	static Assignment findAssignmentByUniqueId(Project project, long uniqueId) {
		for (java.util.Iterator<?> tasks = project.getTaskOutlineIterator(); tasks.hasNext();) {
			Task task = (Task) tasks.next();
			if (!(task instanceof NormalTask normalTask)) continue;
			for (Object value : normalTask.getAssignments()) {
				Assignment assignment = (Assignment) value;
				if (assignment.getUniqueId() == uniqueId) return assignment;
			}
		}
		return null;
	}

	private record AssignmentSplitReplacement(Assignment assignment, AssignmentLevelingSplitState state) { }

	static byte[] baselineProfilesJson(Project project, String taskIdentities,
		java.util.Map<Long, Long> resourceIdentities) throws IOException {
		try {
			JsonNode taskUidMap = JSON.readTree(taskIdentities);
			ObjectNode root = JSON.createObjectNode();
			root.put("version", 1);
			var tasks = root.putArray("tasks");
			for (java.util.Iterator<?> iterator = project.getTaskOutlineIterator(); iterator.hasNext();) {
				Task task = (Task) iterator.next();
				JsonNode serializedTaskUid = taskUidMap.path(String.valueOf(task.getUniqueId()));
				if (!serializedTaskUid.canConvertToLong()) continue;
				for (int slot = 0; slot < Snapshottable.CURRENT.intValue(); slot++) {
					if (!(task.getSnapshot(Integer.valueOf(slot)) instanceof TaskSnapshot snapshot)) continue;
					ObjectNode taskNode = tasks.addObject();
					taskNode.put("taskUid", serializedTaskUid.longValue());
					taskNode.put("slot", slot);
					taskNode.put("fixedCostAccrual", snapshot.getFixedCostAccrual());
					com.microproject.pm.calendar.WorkCalendar fixedCostCalendar = snapshot.getFixedCostCalendar();
					if (fixedCostCalendar == null) fixedCostCalendar = task.getEffectiveWorkCalendar();
					writeCalendar(taskNode.putObject("fixedCostCalendar"), fixedCostCalendar);
					var profiles = taskNode.putArray("profiles");
					for (BaselineAssignmentProfile profile : snapshot.captureBaselineProfiles(task, slot))
						writeBaselineProfile(profiles.addObject(), profile, resourceIdentities);
					var fixedCostProfiles = taskNode.putArray("rolledUpFixedCosts");
					for (TaskSnapshot.FixedCostProfile profile : snapshot.getRolledUpFixedCostProfiles())
						writeFixedCostProfile(fixedCostProfiles.addObject(), profile);
				}
			}
			return JSON.writeValueAsBytes(root);
		} catch (IOException exception) {
			throw exception;
		} catch (Exception exception) {
			throw new IOException("Unable to serialize mpo baseline profiles", exception);
		}
	}

	static void writeBaselineProfile(ObjectNode node, BaselineAssignmentProfile profile,
		java.util.Map<Long, Long> resourceIdentities) throws IOException {
		Long resourceUid = resourceIdentities.get(Long.valueOf(profile.resourceUniqueId()));
		if (resourceUid == null) throw new IOException("Missing serialized resource identity for baseline profile");
		node.put("profileUid", profile.profileId());
		node.put("resourceUid", resourceUid.longValue());
		node.put("start", profile.start());
		node.put("finish", profile.finish());
		node.put("duration", profile.duration());
		node.put("units", profile.units());
		node.put("rate", profile.rate());
		node.put("rateUnit", profile.rateUnit());
		node.put("requestDemandType", profile.requestDemandType());
		node.put("costRateIndex", profile.costRateIndex());
		node.put("delay", profile.delay());
		node.put("levelingDelay", profile.levelingDelay());
		node.put("overtimeWork", profile.overtimeWork());
		writeContour(node.putObject("workContour"), profile.workContour());
		writeContour(node.putObject("costContour"), profile.costContour());
		writeCalendar(node.putObject("calendar"), profile.calendarCopy());
	}

	static void writeFixedCostProfile(ObjectNode node, TaskSnapshot.FixedCostProfile profile) {
		node.put("cost", profile.fixedCost());
		node.put("accrual", profile.accrualCode());
		node.put("start", profile.start());
		node.put("finish", profile.finish());
		node.put("rawDuration", profile.rawDuration());
		writeCalendar(node.putObject("calendar"), profile.calendarCopy());
	}

	static void writeContour(ObjectNode node, Contour contour) {
		node.put("type", contour.type());
		var buckets = node.putArray("buckets");
		for (Bucket bucket : contour.buckets()) {
			ObjectNode item = buckets.addObject();
			item.put("duration", bucket.duration());
			item.put("units", bucket.units());
		}
	}

	static void writeCalendar(ObjectNode node, com.microproject.pm.calendar.WorkCalendar calendar) {
		CalendarDefinition definition = calendar.getConcreteInstance();
		node.put("name", calendar.getName());
		writeCalendarBody(node, definition::getWeekDay, definition.getExceptionDays(),
			definition.getWorkWeekPeriods(), definition.getRecurringExceptions());
	}

	/**
	 * Shared week / exceptions / workWeeks / recurringExceptions block of both calendar schemas. The
	 * callers differ on purpose in what they pass: project and resource calendars write their
	 * concrete (inheritance-resolved) definition, schedule calendars write only their own differences.
	 */
	static void writeCalendarBody(ObjectNode node, java.util.function.IntFunction<WorkDay> weekDay,
		WorkDay[] exceptionDays, java.util.List<WorkWeekPeriod> workWeekPeriods,
		java.util.List<RecurringCalendarException> recurringExceptions) {
		var week = node.putArray("week");
		for (int day = 0; day < WorkWeek.DAYS_IN_WEEK; day++)
			writeCalendarDay(week.addObject(), weekDay.apply(day));
		var exceptions = node.putArray("exceptions");
		for (WorkDay day : exceptionDays) writeCalendarDay(exceptions.addObject(), day);
		var periods = node.putArray("workWeeks");
		for (WorkWeekPeriod period : workWeekPeriods) {
			ObjectNode item = periods.addObject();
			item.put("name", period.getName());
			item.put("start", period.getStart());
			item.put("end", period.getEnd());
			var days = item.putArray("week");
			for (int day = 0; day < WorkWeek.DAYS_IN_WEEK; day++)
				writeCalendarDay(days.addObject(), period.getWeekDay(day));
		}
		var recurring = node.putArray("recurringExceptions");
		for (RecurringCalendarException exception : recurringExceptions) {
			ObjectNode item = recurring.addObject();
			writeCalendarDay(item.putObject("template"), exception.getTemplate());
			writeRecurrence(item.putObject("recurrence"), exception.getRecurrence());
		}
	}

	static void writeCalendarDay(ObjectNode node, WorkDay day) {
		if (day == null) {
			node.put("defined", false);
			return;
		}
		node.put("defined", true);
		node.put("start", day.getStart());
		node.put("end", day.getEnd());
		node.put("description", day.getDescription() == null ? "" : day.getDescription());
		var ranges = node.putArray("ranges");
		java.util.List<WorkRange> intervals = day.getWorkingHours().getIntervals();
		for (int index = 0; index < intervals.size(); index++) {
			WorkRange range = intervals.get(index);
			if (range == null) continue;
			ObjectNode item = ranges.addObject();
			item.put("index", index);
			item.put("start", range.getStart());
			item.put("end", range.getEnd());
			item.put("overtime", range.isOvertime());
		}
	}

	static void writeRecurrence(ObjectNode node, CalendarRecurrence recurrence) {
		node.put("pattern", recurrence.getPattern().name());
		node.put("endMode", recurrence.getEndMode().name());
		node.put("start", recurrence.getStartDate());
		node.put("finish", recurrence.getFinishDate());
		node.put("count", recurrence.getOccurrenceCount());
		node.put("frequency", recurrence.getFrequency());
		node.put("workingDaysOnly", recurrence.isWorkingDaysOnly());
		node.put("relative", recurrence.isRelative());
		node.put("ordinal", recurrence.getOrdinal());
		node.put("weekday", recurrence.getWeekday());
		node.put("month", recurrence.getMonth());
		node.put("dayOfMonth", recurrence.getDayOfMonth());
		var weekdays = node.putArray("weekdays");
		java.util.BitSet selected = recurrence.getWeekdays();
		for (int day = selected.nextSetBit(0); day >= 0; day = selected.nextSetBit(day + 1)) weekdays.add(day);
	}

	static java.util.Map<Long, Long> resourceIdentitiesForMpo(Project project) throws IOException {
		return com.microproject.server.data.MPXConverter.exportedResourceIdentities(project);
	}

	static void restoreBaselineProfiles(Project project, byte[] bytes) throws IOException {
		JsonNode root = JSON.readTree(bytes);
		JsonNode tasks = root.path("tasks");
		int version = requiredInt(root, "version");
		if (version != 1 || !tasks.isArray() || tasks.size() > MAX_OPERATION_COUNT)
			throw new IOException("Invalid mpo baseline profile data");
		java.util.ArrayList<BaselineProfilesReplacement> replacements = new java.util.ArrayList<>();
		java.util.HashSet<String> seenSlots = new java.util.HashSet<>();
		int profileCount = 0;
		for (JsonNode taskNode : tasks) {
			JsonNode taskUidNode = taskNode.path("taskUid");
			JsonNode slotNode = taskNode.path("slot");
			JsonNode profilesNode = taskNode.path("profiles");
			if (!profilesNode.isArray() || profilesNode.size() > MAX_OPERATION_COUNT)
				throw new IOException("Invalid mpo baseline task slot");
			long taskUid = integralLong(taskUidNode, "taskUid");
			int slot = integralInt(slotNode, "slot");
			int accrualCode = requiredInt(taskNode, "fixedCostAccrual");
			try {
				com.microproject.pm.costing.Accrual.Kind.fromCode(accrualCode);
			} catch (IllegalArgumentException exception) {
				throw new IOException("Invalid mpo baseline fixed-cost accrual", exception);
			}
			Integer fixedCostAccrual = Integer.valueOf(accrualCode);
			com.microproject.pm.calendar.WorkCalendar fixedCostCalendar = taskNode.has("fixedCostCalendar")
				? readCalendar(taskNode.path("fixedCostCalendar")) : null;
			java.util.List<TaskSnapshot.FixedCostProfile> rolledUpFixedCosts = taskNode.has("rolledUpFixedCosts")
				? readFixedCostProfiles(taskNode.path("rolledUpFixedCosts")) : null;
			if (taskUid <= 0L || slot < 0 || slot >= Snapshottable.CURRENT.intValue())
				throw new IOException("MPO baseline profile references an invalid task or slot");
			String slotKey = taskUid + ":" + slot;
			if (!seenSlots.add(slotKey)) throw new IOException("Duplicate mpo baseline profile task slot");
			Task task = project.findByUniqueId(taskUid);
			if (!(task instanceof NormalTask normalTask)
					|| !(normalTask.getSnapshot(Integer.valueOf(slot)) instanceof TaskSnapshot snapshot))
				throw new IOException("MPO baseline profile references a missing task snapshot uid=" + taskUid
					+ " slot=" + slot);
			java.util.ArrayList<Assignment> assignments = new java.util.ArrayList<>();
			java.util.HashSet<Long> profileIds = new java.util.HashSet<>();
			for (JsonNode profileNode : profilesNode) {
				if (++profileCount > MAX_OPERATION_COUNT) throw new IOException("MPO contains too many baseline profiles");
				BaselineAssignmentProfile profile = readBaselineProfile(profileNode);
				if (!profileIds.add(Long.valueOf(profile.profileId())))
					throw new IOException("Duplicate profile identity in mpo baseline slot");
				Resource resource = project.getResourcePool().findById(profile.resourceUniqueId());
				if (resource == null) throw new IOException("MPO baseline profile references an unknown resource uid="
					+ profile.resourceUniqueId());
				try {
					assignments.add(profile.restore(normalTask, resource));
				} catch (RuntimeException exception) {
					throw new IOException("Invalid mpo baseline profile for task uid=" + taskUid + " slot=" + slot,
						exception);
				}
			}
			if (fixedCostCalendar == null) fixedCostCalendar = normalTask.getEffectiveWorkCalendar();
			fixedCostCalendar = TaskSnapshot.copyCalendarForSnapshot(fixedCostCalendar);
			replacements.add(new BaselineProfilesReplacement(normalTask, slot, snapshot, assignments,
				fixedCostAccrual, fixedCostCalendar, rolledUpFixedCosts));
		}
		for (BaselineProfilesReplacement replacement : replacements)
			replacement.snapshot().replaceBaselineRollupAssignments(replacement.task(), replacement.slot(),
				replacement.assignments());
		for (BaselineProfilesReplacement replacement : replacements)
			if (replacement.fixedCostAccrual() != null)
				replacement.snapshot().setFixedCostAccrual(replacement.fixedCostAccrual().intValue());
		for (BaselineProfilesReplacement replacement : replacements)
			replacement.snapshot().setPreparedFixedCostCalendar(replacement.fixedCostCalendar());
		for (BaselineProfilesReplacement replacement : replacements)
			if (replacement.rolledUpFixedCosts() != null)
				replacement.snapshot().setPreparedRolledUpFixedCostProfiles(replacement.rolledUpFixedCosts());
	}

	static java.util.List<TaskSnapshot.FixedCostProfile> readFixedCostProfiles(JsonNode node) throws IOException {
		if (!node.isArray() || node.size() > MAX_OPERATION_COUNT)
			throw new IOException("Invalid mpo rolled-up fixed-cost profiles");
		java.util.ArrayList<TaskSnapshot.FixedCostProfile> profiles = new java.util.ArrayList<>(node.size());
		for (JsonNode profile : node) {
			JsonNode cost = profile.path("cost");
			if (!cost.isNumber() || !Double.isFinite(cost.doubleValue()))
				throw new IOException("Invalid mpo rolled-up fixed cost");
			int accrual = requiredInt(profile, "accrual");
			long start = integralLong(profile.path("start"), "start");
			long finish = integralLong(profile.path("finish"), "finish");
			long rawDuration = integralLong(profile.path("rawDuration"), "rawDuration");
			com.microproject.pm.calendar.WorkCalendar calendar = readCalendar(profile.path("calendar"));
			try {
				profiles.add(new TaskSnapshot.FixedCostProfile(cost.doubleValue(), accrual, start, finish,
					rawDuration, calendar));
			} catch (RuntimeException exception) {
				throw new IOException("Invalid mpo rolled-up fixed-cost profile", exception);
			}
		}
		return java.util.List.copyOf(profiles);
	}

	static BaselineAssignmentProfile readBaselineProfile(JsonNode node) throws IOException {
		try {
			return new BaselineAssignmentProfile(requiredNonZeroLong(node, "profileUid"),
				requiredPositiveLong(node, "resourceUid"), requiredLong(node, "start"), requiredLong(node, "finish"),
				requiredLong(node, "duration"), requiredFiniteDouble(node, "units"),
				requiredFiniteDouble(node, "rate"), requiredInt(node, "rateUnit"), requiredInt(node, "requestDemandType"),
				requiredInt(node, "costRateIndex"), requiredLong(node, "delay"), requiredLong(node, "levelingDelay"),
				requiredLong(node, "overtimeWork"), readContour(node.path("workContour")),
				readContour(node.path("costContour")), readCalendar(node.path("calendar")));
		} catch (RuntimeException exception) {
			throw new IOException("Invalid mpo baseline assignment profile", exception);
		}
	}

	static Contour readContour(JsonNode node) throws IOException {
		JsonNode bucketsNode = node.path("buckets");
		if (!bucketsNode.isArray() || bucketsNode.size() > MAX_OPERATION_COUNT)
			throw new IOException("Invalid mpo baseline contour");
		int type = integralInt(node.path("type"), "type");
		java.util.ArrayList<Bucket> buckets = new java.util.ArrayList<>();
		for (JsonNode bucket : bucketsNode) {
			if (!bucket.path("units").isNumber() || !Double.isFinite(bucket.path("units").doubleValue()))
				throw new IOException("Invalid mpo baseline contour bucket");
			buckets.add(new Bucket(integralLong(bucket.path("duration"), "duration"), bucket.path("units").doubleValue()));
		}
		return new Contour(type, buckets);
	}

	static WorkingCalendar readCalendar(JsonNode node) throws IOException {
		return readCalendarDefinition(node, "baseline").copyForBaselineSnapshot();
	}

	static WorkingCalendar readCalendarDefinition(JsonNode node, String context) throws IOException {
		JsonNode weekNode = node.path("week");
		JsonNode exceptionsNode = node.path("exceptions");
		JsonNode periodsNode = node.path("workWeeks");
		JsonNode recurringNode = node.path("recurringExceptions");
		if (!node.path("name").isTextual() || !weekNode.isArray()
				|| weekNode.size() != WorkWeek.DAYS_IN_WEEK || !exceptionsNode.isArray()
				|| exceptionsNode.size() > MAX_OPERATION_COUNT || !periodsNode.isArray()
				|| periodsNode.size() > MAX_OPERATION_COUNT || !recurringNode.isArray()
				|| recurringNode.size() > MAX_OPERATION_COUNT)
			throw new IOException("Invalid mpo " + context + " calendar");
		WorkingCalendar calendar = WorkingCalendar.getInstance();
		calendar.setName(node.path("name").textValue());
		for (int day = 0; day < WorkWeek.DAYS_IN_WEEK; day++)
			calendar.setWeekDay(day, readCalendarDay(weekNode.get(day)));
		for (JsonNode exception : exceptionsNode) calendar.addOrReplaceException(readCalendarDay(exception));
		for (JsonNode periodNode : periodsNode) {
			JsonNode periodWeek = periodNode.path("week");
			if (!periodNode.path("name").isTextual() || !periodWeek.isArray()
					|| periodWeek.size() != WorkWeek.DAYS_IN_WEEK)
				throw new IOException("Invalid mpo " + context + " work-week period");
			long periodStart = integralLong(periodNode.path("start"), "start");
			long periodEnd = integralLong(periodNode.path("end"), "end");
			WorkWeek week = new WorkWeek();
			for (int day = 0; day < WorkWeek.DAYS_IN_WEEK; day++) week.setWeekDay(day, readCalendarDay(periodWeek.get(day)));
			calendar.addOrReplaceWorkWeekPeriod(new WorkWeekPeriod(periodNode.path("name").textValue(),
				periodStart, periodEnd, week));
		}
		for (JsonNode recurring : recurringNode) {
			WorkDay template = readCalendarDay(recurring.path("template"));
			CalendarRecurrence rule = readRecurrence(recurring.path("recurrence"));
			calendar.addOrReplaceRecurringException(new RecurringCalendarException(template, rule));
		}
		return calendar;
	}

	static WorkDay readCalendarDay(JsonNode node) throws IOException {
		if (!node.path("defined").isBoolean()) throw new IOException("Invalid mpo calendar day defined flag");
		if (!node.path("defined").booleanValue()) return null;
		if (!node.path("description").isTextual() || !node.path("ranges").isArray()
				|| node.path("ranges").size() > new WorkingHours().getIntervals().size())
			throw new IOException("Invalid mpo calendar day");
		WorkDay day = new WorkDay(integralLong(node.path("start"), "start"), integralLong(node.path("end"), "end"),
			node.path("description").textValue());
		WorkingHours hours = new WorkingHours();
		java.util.HashSet<Integer> indexes = new java.util.HashSet<>();
		for (JsonNode range : node.path("ranges")) {
			if (!range.path("overtime").isBoolean())
				throw new IOException("Invalid mpo calendar work range");
			int index = integralInt(range.path("index"), "index");
			if (index < 0 || index >= hours.getIntervals().size() || !indexes.add(Integer.valueOf(index)))
				throw new IOException("Duplicate or out-of-range mpo calendar work range");
			try {
				hours.setInterval(index, integralLong(range.path("start"), "start"),
					integralLong(range.path("end"), "end"));
			} catch (Exception exception) {
				throw new IOException("Invalid mpo calendar work range", exception);
			}
			WorkRange restored = hours.getInterval(index);
			if (restored != null) restored.setOvertime(range.path("overtime").booleanValue());
		}
		day.setWorkingHours(hours);
		return day;
	}

	static CalendarRecurrence readRecurrence(JsonNode node) throws IOException {
		try {
			CalendarRecurrence.Pattern pattern = CalendarRecurrence.Pattern.valueOf(requiredText(node, "pattern"));
			CalendarRecurrence.EndMode endMode = CalendarRecurrence.EndMode.valueOf(requiredText(node, "endMode"));
			long start = requiredLong(node, "start");
			long finish = requiredLong(node, "finish");
			int count = requiredInt(node, "count");
			int frequency = requiredInt(node, "frequency");
			boolean relative = requiredBoolean(node, "relative");
			int dayOfMonth = requiredInt(node, "dayOfMonth");
			int ordinal = requiredInt(node, "ordinal");
			int weekday = requiredInt(node, "weekday");
			int month = requiredInt(node, "month");
			return switch (pattern) {
				case DAILY -> CalendarRecurrence.daily(start, frequency, requiredBoolean(node, "workingDaysOnly"),
					endMode, finish, count);
				case WEEKLY -> {
					java.util.BitSet weekdays = new java.util.BitSet(8);
					JsonNode weekdayNodes = node.path("weekdays");
					if (!weekdayNodes.isArray()) throw new IOException("Invalid mpo weekly recurrence weekdays");
					for (JsonNode item : weekdayNodes) {
						int weekdayCode = integralInt(item, "weekday");
						if (weekdayCode < 1 || weekdayCode > 7)
							throw new IOException("Invalid mpo weekly recurrence weekday");
						weekdays.set(weekdayCode);
					}
					yield CalendarRecurrence.weekly(start, frequency, weekdays, endMode, finish, count);
				}
				case MONTHLY -> CalendarRecurrence.monthly(start, frequency, relative, dayOfMonth, ordinal,
					weekday, endMode, finish, count);
				case YEARLY -> CalendarRecurrence.yearly(start, frequency, relative, month, dayOfMonth,
					ordinal, weekday, endMode, finish, count);
			};
		} catch (IOException exception) {
			throw exception;
		} catch (RuntimeException exception) {
			throw new IOException("Invalid mpo baseline calendar recurrence", exception);
		}
	}

	static long requiredPositiveLong(JsonNode node, String name) throws IOException {
		long value = requiredLong(node, name);
		if (value <= 0L) throw new IOException("Invalid positive MPO baseline value: " + name);
		return value;
	}

	static long requiredNonZeroLong(JsonNode node, String name) throws IOException {
		long value = requiredLong(node, name);
		if (value == 0L) throw new IOException("Invalid nonzero MPO baseline value: " + name);
		return value;
	}

	static long requiredLong(JsonNode node, String name) throws IOException {
		return integralLong(node.path(name), name);
	}

	static int requiredInt(JsonNode node, String name) throws IOException {
		return integralInt(node.path(name), name);
	}

	static long integralLong(JsonNode value, String name) throws IOException {
		if (value == null || !value.isIntegralNumber() || !value.canConvertToLong())
			throw new IOException("Invalid MPO integer value: " + name);
		return value.longValue();
	}

	static int integralInt(JsonNode value, String name) throws IOException {
		long integral = integralLong(value, name);
		if (integral < Integer.MIN_VALUE || integral > Integer.MAX_VALUE)
			throw new IOException("Out-of-range MPO integer value: " + name);
		return (int) integral;
	}

	static double requiredFiniteDouble(JsonNode node, String name) throws IOException {
		JsonNode value = node.path(name);
		if (!value.isNumber() || !Double.isFinite(value.doubleValue()))
			throw new IOException("Invalid MPO baseline value: " + name);
		return value.doubleValue();
	}

	static String requiredText(JsonNode node, String name) throws IOException {
		JsonNode value = node.path(name);
		if (!value.isTextual()) throw new IOException("Invalid MPO baseline value: " + name);
		return value.textValue();
	}

	static boolean requiredBoolean(JsonNode node, String name) throws IOException {
		JsonNode value = node.path(name);
		if (!value.isBoolean()) throw new IOException("Invalid MPO baseline value: " + name);
		return value.booleanValue();
	}

	private record BaselineProfilesReplacement(NormalTask task, int slot, TaskSnapshot snapshot,
		java.util.List<Assignment> assignments, Integer fixedCostAccrual,
		com.microproject.pm.calendar.WorkCalendar fixedCostCalendar,
		java.util.List<TaskSnapshot.FixedCostProfile> rolledUpFixedCosts) { }

	static byte[] taskSchedulesJson(Project project, String taskIdentities) throws IOException {
		try {
			JsonNode identities = JSON.readTree(taskIdentities);
			ObjectNode root = JSON.createObjectNode();
			root.put("version", 1);
			root.set("projectCalendar", scheduleCalendarJson(
				(WorkingCalendar) project.getWorkCalendar(), null));
			com.fasterxml.jackson.databind.node.ArrayNode tasks = root.putArray("tasks");
			java.util.Set<Long> seenSerializedIds = new java.util.HashSet<>();
			java.util.Map<Long, Task> modelTasks = tasksByUniqueId(project);
			java.util.Iterator<java.util.Map.Entry<String, JsonNode>> identityIterator = identities.fields();
			while (identityIterator.hasNext()) {
				java.util.Map.Entry<String, JsonNode> identity = identityIterator.next();
				long sourceId = Long.parseLong(identity.getKey());
				JsonNode serializedUid = identity.getValue();
				if (!serializedUid.canConvertToLong()) throw new IOException("Invalid serialized task identity for schedule snapshot");
				if (!seenSerializedIds.add(serializedUid.longValue())) continue;
				if (!(modelTasks.get(Long.valueOf(sourceId)) instanceof NormalTask normalTask)) continue;
				ObjectNode taskNode = tasks.addObject();
				taskNode.put("taskUid", serializedUid.longValue());
				taskNode.put("constraintType", normalTask.getConstraintType());
				taskNode.put("constraintDate", normalTask.getConstraintDate());
				taskNode.put("start", normalTask.getStart());
				taskNode.put("finish", normalTask.getEnd());
				taskNode.put("actualStart", normalTask.getActualStart());
				if (normalTask.getWorkCalendar() != null)
					taskNode.set("calendar", scheduleCalendarJson((WorkingCalendar) normalTask.getWorkCalendar(),
						project.getWorkCalendar()));
				if (!normalTask.isWbsParent())
					taskNode.put("percentComplete", normalTask.getPercentComplete());
				if (normalTask.isNormal() && !normalTask.hasRealAssignments())
					taskNode.put("unassignedWork", normalTask.getWork(null));
			}
			return JSON.writeValueAsBytes(root);
		} catch (IOException exception) {
			throw exception;
		} catch (Exception exception) {
			throw new IOException("Unable to serialize mpo task schedules", exception);
		}
	}

	static ObjectNode scheduleCalendarJson(WorkingCalendar calendar,
		com.microproject.pm.calendar.WorkCalendar projectCalendar) {
		ObjectNode node = JSON.createObjectNode();
		node.put("name", calendar.getName());
		node.put("uniqueId", calendar.getUniqueId());
		if (calendar.getBaseCalendar() == projectCalendar && projectCalendar != null)
			node.put("baseProjectCalendar", true);
		else if (calendar.getBaseCalendar() != null)
			node.set("baseCalendar", scheduleCalendarJson((WorkingCalendar) calendar.getBaseCalendar(), projectCalendar));
		writeCalendarBody(node, calendar::getWeekDay, calendar.getExceptionDays(), calendar.getWorkWeekPeriods(),
			calendar.getRecurringExceptions());
		return node;
	}

	static WorkingCalendar readScheduleCalendar(JsonNode node,
		ScheduleCalendarReadState state, int depth) throws IOException {
		if (depth > 64) throw new IOException("MPO calendar inheritance exceeds maximum depth");
		JsonNode uniqueIdNode = node.get("uniqueId");
		Long uniqueId = null;
		if (uniqueIdNode != null) uniqueId = Long.valueOf(integralLong(uniqueIdNode, "uniqueId"));
		if (uniqueId != null && state.byId.containsKey(uniqueId)) {
			if (!state.definitions.get(uniqueId).equals(node))
				throw new IOException("Conflicting MPO schedule calendar definitions for uniqueId=" + uniqueId);
			return state.byId.get(uniqueId);
		}
		com.microproject.pm.calendar.WorkCalendar base = null;
		boolean hasProjectBase = node.has("baseProjectCalendar");
		boolean hasNestedBase = node.has("baseCalendar");
		if (hasProjectBase && hasNestedBase)
			throw new IOException("MPO schedule calendar declares multiple base calendars");
		if (hasProjectBase) {
			JsonNode projectBaseNode = node.get("baseProjectCalendar");
			if (!projectBaseNode.isBoolean() || !projectBaseNode.booleanValue() || state.projectCalendar == null)
				throw new IOException("Invalid mpo schedule calendar project base");
			base = state.projectCalendar;
		}
		WorkingCalendar calendar = readCalendarDefinition(node, "schedule");
		if (uniqueId != null) {
			calendar.setUniqueId(uniqueId.longValue());
			state.byId.put(uniqueId, calendar);
			state.definitions.put(uniqueId, node.deepCopy());
		}
		if (hasNestedBase) base = readScheduleCalendar(node.path("baseCalendar"), state, depth + 1);
		state.pendingBases.add(new PendingCalendarBase(calendar, base));
		return calendar;
	}

	static void restoreTaskSchedules(Project project, byte[] bytes) throws IOException {
		JsonNode root = JSON.readTree(bytes);
		JsonNode tasks = root.path("tasks");
		if (requiredInt(root, "version") != 1 || !tasks.isArray())
			throw new IOException("Invalid mpo task schedule data");
		ScheduleCalendarReadState calendarState = new ScheduleCalendarReadState();
		WorkingCalendar projectCalendar = root.has("projectCalendar")
			? readScheduleCalendar(root.path("projectCalendar"), calendarState, 0) : null;
		calendarState.projectCalendar = projectCalendar;
		java.util.Set<Long> seen = new java.util.HashSet<>();
		java.util.ArrayList<TaskScheduleSnapshot> snapshots = new java.util.ArrayList<>();
		for (JsonNode taskNode : tasks) {
			if (snapshots.size() >= MAX_OPERATION_COUNT) throw new IOException("MPO contains too many task schedule records");
			JsonNode uidNode = taskNode.path("taskUid");
			JsonNode constraintTypeNode = taskNode.path("constraintType");
			JsonNode constraintDateNode = taskNode.path("constraintDate");
			JsonNode startNode = taskNode.path("start");
			JsonNode finishNode = taskNode.path("finish");
			JsonNode actualStartNode = taskNode.path("actualStart");
			long serializedId = integralLong(uidNode, "taskUid");
			int constraintType = integralInt(constraintTypeNode, "constraintType");
			long constraintDate = integralLong(constraintDateNode, "constraintDate");
			long start = integralLong(startNode, "start");
			long finish = integralLong(finishNode, "finish");
			long actualStart = integralLong(actualStartNode, "actualStart");
			Double percentComplete = null;
			if (taskNode.has("percentComplete")) {
				JsonNode percentNode = taskNode.get("percentComplete");
				if (percentNode == null || !percentNode.isNumber())
					throw new IOException("Invalid mpo task schedule percent complete");
				double value = percentNode.doubleValue();
				if (!Double.isFinite(value) || value < 0D || value > 1D)
					throw new IOException("Invalid mpo task schedule percent complete");
				percentComplete = Double.valueOf(value);
			}
			Long unassignedWork = null;
			if (taskNode.has("unassignedWork")) {
				JsonNode workNode = taskNode.get("unassignedWork");
				if (workNode == null || !workNode.isIntegralNumber() || !workNode.canConvertToLong()
						|| workNode.longValue() < 0L)
					throw new IOException("Invalid mpo task schedule unassigned work");
				unassignedWork = Long.valueOf(workNode.longValue());
			}
			if (!seen.add(serializedId)) throw new IOException("Duplicate task schedule record in MPO");
			Task task = project.findByUniqueId(serializedId);
			if (!(task instanceof NormalTask normalTask))
				throw new IOException("MPO task schedule references an unknown task uid=" + serializedId
					+ " found=" + (task == null ? "null" : task.getClass().getName()));
			if (com.microproject.pm.scheduling.ConstraintType.Kind.fromCodeOrNull(constraintType) == null)
				throw new IOException("Invalid mpo task schedule constraint type");
			if (finish < start) throw new IOException("Invalid mpo task schedule boundaries");
			if (percentComplete != null && normalTask.isWbsParent())
				throw new IOException("MPO task schedule percent complete is only valid for leaf tasks");
			if (unassignedWork != null && (!normalTask.isNormal() || normalTask.hasRealAssignments()))
				throw new IOException("MPO task schedule unassigned work is only valid for unassigned leaf tasks");
			WorkingCalendar taskCalendar = taskNode.has("calendar")
				? readScheduleCalendar(taskNode.path("calendar"), calendarState, 0) : null;
			snapshots.add(new TaskScheduleSnapshot(normalTask, constraintType, constraintDate, start, finish, actualStart,
				percentComplete, unassignedWork, taskCalendar, taskNode.has("calendar") || projectCalendar != null));
		}
		validateCalendarBaseGraph(calendarState.pendingBases);
		for (PendingCalendarBase pending : calendarState.pendingBases) {
			if (pending.base() != null) {
				try {
					pending.calendar().setBaseCalendar(pending.base());
				} catch (com.microproject.configuration.CircularDependencyException exception) {
					throw new IOException("Invalid MPO schedule calendar inheritance", exception);
				}
			}
		}
		if (projectCalendar != null) project.setWorkCalendar(projectCalendar);
		for (TaskScheduleSnapshot snapshot : snapshots) {
			if (snapshot.hasCalendar()) snapshot.task().setWorkCalendar(snapshot.calendar());
			snapshot.task().restoreMpoScheduleSnapshot(snapshot.constraintType(), snapshot.constraintDate(),
				snapshot.start(), snapshot.finish(), snapshot.actualStart());
			if (snapshot.percentComplete() != null)
				snapshot.task().setImportedPercentComplete(snapshot.percentComplete().doubleValue());
			if (snapshot.unassignedWork() != null) {
				try {
					snapshot.task().restoreMpoUnassignedWork(snapshot.unassignedWork().longValue());
				} catch (IllegalArgumentException exception) {
					throw new IOException("Invalid mpo task schedule unassigned work target", exception);
				}
			}
		}
	}

	static void validateCalendarBaseGraph(java.util.List<PendingCalendarBase> pendingBases) throws IOException {
		java.util.IdentityHashMap<WorkingCalendar, WorkingCalendar> bases = new java.util.IdentityHashMap<>();
		for (PendingCalendarBase pending : pendingBases) bases.put(pending.calendar(), (WorkingCalendar) pending.base());
		java.util.Set<WorkingCalendar> complete = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
		for (WorkingCalendar start : bases.keySet()) {
			java.util.Set<WorkingCalendar> path = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
			WorkingCalendar current = start;
			while (current != null && !complete.contains(current)) {
				if (!path.add(current)) throw new IOException("Circular MPO schedule calendar inheritance");
				current = bases.get(current);
			}
			complete.addAll(path);
		}
	}

	private static final class ScheduleCalendarReadState {
		private WorkingCalendar projectCalendar;
		private final java.util.Map<Long, WorkingCalendar> byId = new java.util.HashMap<>();
		private final java.util.Map<Long, JsonNode> definitions = new java.util.HashMap<>();
		private final java.util.List<PendingCalendarBase> pendingBases = new java.util.ArrayList<>();
	}

	private record PendingCalendarBase(WorkingCalendar calendar,
		com.microproject.pm.calendar.WorkCalendar base) { }

	private record TaskScheduleSnapshot(NormalTask task, int constraintType, long constraintDate, long start,
		long finish, long actualStart, Double percentComplete, Long unassignedWork,
		WorkingCalendar calendar, boolean hasCalendar) { }

	static void restoreVisibility(Project project, byte[] bytes) throws IOException {
		JsonNode root = JSON.readTree(bytes);
		JsonNode hidden = root.path("hiddenTaskUniqueIds");
		if (requiredInt(root, "version") != 1 || !hidden.isArray())
			throw new IOException("Invalid mpo visibility state");
		for (JsonNode entry : hidden) {
			long serializedId = integralLong(entry, "taskUid");
			Task task = project.findByUniqueId(serializedId);
			if (task != null) task.setHiddenTask(true);
		}
	}

	static java.util.Map<Long, Long> readTaskIdentities(byte[] json) throws IOException {
		JsonNode root = JSON.readTree(json);
		if (!root.isObject()) throw new IOException("Invalid mpo task identity map");
		java.util.Map<Long, Long> result = new java.util.LinkedHashMap<>();
		java.util.Iterator<java.util.Map.Entry<String, JsonNode>> fields = root.fields();
		while (fields.hasNext()) {
			java.util.Map.Entry<String, JsonNode> entry = fields.next();
			try {
				result.put(Long.valueOf(entry.getKey()), Long.valueOf(integralLong(entry.getValue(), "taskUid")));
			} catch (NumberFormatException exception) {
				throw new IOException("Invalid mpo task identity", exception);
			}
		}
		return result;
	}
}
