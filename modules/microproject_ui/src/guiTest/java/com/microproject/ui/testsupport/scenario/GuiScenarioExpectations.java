/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.testsupport.scenario;

import java.awt.Dialog;
import java.awt.Window;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.microproject.association.Association;
import com.microproject.exchange.MpoFileImporter;
import com.microproject.model.dependency.Dependency;
import com.microproject.model.task.Project;
import com.microproject.model.task.Task;
import com.microproject.options.CalendarOption;
import com.microproject.ui.frame.DocumentFrame;
import com.microproject.ui.testsupport.GuiAcceptanceSupport;
import com.microproject.ui.testsupport.GuiProjectWindow;
import com.microproject.ui.testsupport.GuiRunRecorder;
import com.microproject.ui.testsupport.GuiStateSnapshot;
import com.microproject.ui.testsupport.ProjectStateSnapshot;
import com.microproject.ui.view.spreadsheet.SpreadSheet;
import com.microproject.ui.view.spreadsheet.SpreadSheetModel;

/** Evaluates the {@code expect} paths of a scenario step and the {@code saveReload} comparison. */
final class GuiScenarioExpectations {
	/** Project-level values {@code saveReload} does not compare: they differ by design after a reload. */
	static final Set<String> SAVE_RELOAD_IGNORED = Set.of("fileName", "dirty", "undo");

	private static final ObjectMapper MAPPER = new ObjectMapper();
	private static final JsonNodeFactory NODES = JsonNodeFactory.instance;
	private static final Pattern BY_NAME = Pattern.compile("^task\\[(.+)]\\.(\\w+)$");
	private static final Pattern BY_ROW = Pattern.compile("^task#(\\d+)\\.(\\w+)$");
	private static final double NUMBER_TOLERANCE = 1e-6;

	private GuiScenarioExpectations() {
	}

	/** One {@code expect} entry that did not hold. */
	record Mismatch(String path, JsonNode expected, JsonNode actual) {
	}

	/** Checks every entry of {@code expect} and returns all that did not hold. */
	static List<Mismatch> check(GuiProjectWindow window, JsonNode expect) throws Exception {
		List<Mismatch> mismatches = new ArrayList<>();
		GuiAcceptanceSupport.runOnEdtWithTimeout(() -> {
			for (Iterator<Map.Entry<String, JsonNode>> it = expect.fields(); it.hasNext();) {
				Map.Entry<String, JsonNode> entry = it.next();
				JsonNode actual = actual(window, entry.getKey());
				if (!matches(entry.getKey(), entry.getValue(), actual)) {
					mismatches.add(new Mismatch(entry.getKey(), entry.getValue(), actual));
				}
			}
		}, "scenario expectation read");
		return mismatches;
	}

	private static boolean matches(String path, JsonNode expected, JsonNode actual) {
		if (expected.isNumber() && actual.isNumber()) {
			return Math.abs(expected.asDouble() - actual.asDouble()) < NUMBER_TOLERANCE;
		}
		// Dates are compared from the front, so "2026-01-05" matches "2026-01-05T08:00".
		if (isDatePath(path) && expected.isTextual() && actual.isTextual()) {
			return actual.asText().startsWith(expected.asText());
		}
		return expected.equals(actual);
	}

	private static boolean isDatePath(String path) {
		return path.equals("gantt.leftEdge") || path.endsWith(".start") || path.endsWith(".end");
	}

	private static JsonNode actual(GuiProjectWindow window, String path) {
		Project project = window.project();
		DocumentFrame frame = window.manager().getCurrentFrame();
		Matcher byName = BY_NAME.matcher(path);
		if (byName.matches()) {
			Task task = taskNamed(project, byName.group(1));
			return task == null ? NODES.textNode("<no task named " + byName.group(1) + ">") : field(task, byName.group(2));
		}
		Matcher byRow = BY_ROW.matcher(path);
		if (byRow.matches()) {
			int row = Integer.parseInt(byRow.group(1));
			List<Task> rows = tableTasks(frame);
			Task task = row >= 1 && row <= rows.size() ? rows.get(row - 1) : null;
			return task == null ? NODES.textNode("<no task in row " + row + ">") : field(task, byRow.group(2));
		}
		return switch (path) {
			case "project.taskCount" -> NODES.numberNode(allTasks(project).size());
			case "undo.canUndo" -> NODES.booleanNode(project.getUndoController().canUndo());
			case "undo.canRedo" -> NODES.booleanNode(project.getUndoController().canRedo());
			case "gantt.leftEdge" -> {
				Map<String, Object> gantt = frame == null ? null : GuiStateSnapshot.gantt(frame);
				yield gantt == null ? NODES.nullNode() : NODES.textNode(String.valueOf(gantt.get("leftEdge")));
			}
			case "view.selectedTasks" -> names(selectedTasks(frame));
			case "dialog" -> {
				String title = modalDialogTitle();
				yield title == null ? NODES.nullNode() : NODES.textNode(title);
			}
			case "errors" -> NODES.numberNode(GuiRunRecorder.current().count("uncaught")
				+ GuiRunRecorder.current().count("log-severe"));
			default -> throw new IllegalArgumentException("unknown expect path: " + path);
		};
	}

	private static JsonNode field(Task task, String field) {
		return switch (field) {
			case "name" -> NODES.textNode(task.getName());
			// The model counts a top-level task as level 0; scenarios use the Microsoft Project numbering that starts at 1.
			case "outlineLevel" -> NODES.numberNode(task.getOutlineLevel() + 1);
			case "summary" -> NODES.booleanNode(task.isSummary());
			case "start" -> NODES.textNode(ProjectStateSnapshot.isoUtc(task.getStart()));
			case "end" -> NODES.textNode(ProjectStateSnapshot.isoUtc(task.getEnd()));
			case "durationDays" -> NODES.numberNode(
				(double) task.getDurationMillis() / CalendarOption.getInstance().getMillisPerDay());
			case "parent" -> {
				Task parent = task.getWbsParentTask();
				yield parent == null ? NODES.nullNode() : NODES.textNode(parent.getName());
			}
			case "predecessors" -> {
				List<Task> predecessors = new ArrayList<>();
				for (Association association : task.getPredecessorList()) {
					if (association instanceof Dependency dependency && dependency.getPredecessor() instanceof Task predecessor) {
						predecessors.add(predecessor);
					}
				}
				yield names(predecessors);
			}
			default -> throw new IllegalArgumentException("unknown task field in expect: " + field);
		};
	}

	private static JsonNode names(List<Task> tasks) {
		var array = NODES.arrayNode();
		for (Task task : tasks) array.add(task.getName());
		return array;
	}

	static List<Task> allTasks(Project project) {
		List<Task> tasks = new ArrayList<>();
		Iterator<Task> iterator = project.getTaskOutlineIterator();
		while (iterator.hasNext()) tasks.add(iterator.next());
		return tasks;
	}

	static Task taskNamed(Project project, String name) {
		for (Task task : allTasks(project)) {
			if (name.equals(task.getName())) return task;
		}
		return null;
	}

	/** The tasks in the visible table, in display order (one entry per row; {@code null} for a non-task row). */
	static List<Task> tableTasks(DocumentFrame frame) {
		List<Task> rows = new ArrayList<>();
		SpreadSheet sheet = frame == null ? null : frame.getActiveSpreadSheet();
		if (sheet == null || !(sheet.getModel() instanceof SpreadSheetModel model)) return rows;
		for (int row = 0; row < sheet.getRowCount(); row++) {
			var node = model.getNodeForDisplayRow(row);
			rows.add(node != null && node.getImpl() instanceof Task task ? task : null);
		}
		return rows;
	}

	private static List<Task> selectedTasks(DocumentFrame frame) {
		List<Task> tasks = new ArrayList<>();
		SpreadSheet sheet = frame == null ? null : frame.getActiveSpreadSheet();
		if (sheet == null) return tasks;
		for (Object node : sheet.getSelectedNodesImpl()) {
			if (node instanceof Task task) tasks.add(task);
		}
		return tasks;
	}

	/** The title of the showing modal dialog, or {@code null}. Call on the EDT. */
	static String modalDialogTitle() {
		String title = null;
		for (Window window : Window.getWindows()) {
			if (window instanceof Dialog dialog && dialog.isShowing() && dialog.isModal()) title = dialog.getTitle();
		}
		return title;
	}

	// ---- saveReload ----

	/**
	 * Saves {@code project} as MPO in {@code directory}, reads it back without the GUI, and returns a
	 * description of the first difference, or {@code null} when the two states match.
	 */
	static String saveAndReloadDifference(Project project, Path directory) throws Exception {
		Path file = directory.resolve("save-reload.mpo");
		try (OutputStream out = Files.newOutputStream(file)) {
			new MpoFileImporter().saveProject(project, out);
		}
		Project reloaded;
		try (InputStream in = Files.newInputStream(file)) {
			reloaded = new MpoFileImporter().loadProject(in);
		}
		JsonNode before = state(project);
		JsonNode after = state(reloaded);
		if (before.equals(after)) return null;
		for (Iterator<String> it = before.fieldNames(); it.hasNext();) {
			String key = it.next();
			if (key.equals("tasks")) {
				JsonNode left = before.get(key);
				JsonNode right = after.get(key);
				for (int index = 0; index < Math.max(left.size(), right.size()); index++) {
					if (!left.path(index).equals(right.path(index))) {
						return "tasks[" + index + "]: before " + left.path(index) + " after " + right.path(index);
					}
				}
			} else if (!before.get(key).equals(after.get(key))) {
				return key + ": before " + before.get(key) + " after " + after.get(key);
			}
		}
		return "states differ: before " + before + " after " + after;
	}

	private static JsonNode state(Project project) {
		Map<String, Object> state = new LinkedHashMap<>();
		state.put("fileName", project.getFileName());
		state.put("dirty", project.isDirty());
		Map<String, Object> undo = new LinkedHashMap<>();
		undo.put("canUndo", project.getUndoController().canUndo());
		undo.put("canRedo", project.getUndoController().canRedo());
		undo.put("names", project.getUndoController().getUndoNames(20));
		state.put("undo", undo);
		state.put("tasks", ProjectStateSnapshot.tasks(project));
		ObjectNode node = MAPPER.valueToTree(state);
		node.remove(SAVE_RELOAD_IGNORED);
		return node;
	}
}
