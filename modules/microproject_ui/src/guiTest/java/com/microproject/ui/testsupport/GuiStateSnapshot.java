/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.testsupport;

import java.awt.Dialog;
import java.awt.Frame;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Window;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JViewport;

import com.microproject.model.task.Project;
import com.microproject.model.task.Task;
import com.microproject.ui.frame.DocumentFrame;
import com.microproject.ui.frame.GraphicManager;
import com.microproject.ui.frame.MainRibbonFrame;
import com.microproject.ui.view.gantt.Gantt;
import com.microproject.ui.view.spreadsheet.SpreadSheet;
import com.microproject.ui.view.timescale.CoordinatesConverter;
import com.microproject.ui.view.views.BaseView;
import com.microproject.ui.view.views.GanttView;

/** Reads the visible state of the main window and the project model into a JSON-ready map. */
public final class GuiStateSnapshot {
	private GuiStateSnapshot() {
	}

	/** Finds the manager of a visible main window, or {@code null}. */
	public static GraphicManager discover() {
		for (Window window : Window.getWindows()) {
			if (window instanceof MainRibbonFrame frame && frame.isShowing() && frame.getGraphicManager() != null) {
				return frame.getGraphicManager();
			}
		}
		return null;
	}

	/** Captures the state on the EDT. {@code manager} may be {@code null}. */
	public static Map<String, Object> capture(GraphicManager manager) throws Exception {
		Map<String, Object> result = new LinkedHashMap<>();
		GuiAcceptanceSupport.runOnEdtWithTimeout(() -> fill(manager, result), "GUI state snapshot");
		return result;
	}

	private static void fill(GraphicManager manager, Map<String, Object> result) {
		DocumentFrame frame = manager == null ? null : manager.getCurrentFrame();
		result.put("gantt", frame == null ? null : gantt(frame));
		result.put("view", view(frame));
		Project project = frame == null ? null : frame.getProject();
		result.put("project", project == null ? null : project(project));
		result.put("tasks", project == null ? null : ProjectStateSnapshot.tasks(project));
	}

	/** The left and right edge of the visible Gantt chart, or {@code null} when it is not shown. */
	public static Map<String, Object> gantt(DocumentFrame frame) {
		BaseView top = frame.getActiveTopView();
		if (!(top instanceof GanttView ganttView)) return null;
		Gantt gantt = ganttView.getGantt();
		if (gantt == null || gantt.getCoord() == null || !(gantt.getParent() instanceof JViewport viewport)) return null;
		CoordinatesConverter coord = gantt.getCoord();
		Point position = viewport.getViewPosition();
		int width = viewport.getExtentSize().width;
		long left = (long) coord.toTime(position.x);
		long right = (long) coord.toTime(position.x + width);
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("leftEdge", ProjectStateSnapshot.isoUtc(left));
		map.put("leftEdgeMillis", left);
		map.put("rightEdge", ProjectStateSnapshot.isoUtc(right));
		map.put("rightEdgeMillis", right);
		map.put("originMillis", coord.getOrigin());
		map.put("viewportX", position.x);
		map.put("viewportWidth", width);
		return map;
	}

	private static Map<String, Object> view(DocumentFrame frame) {
		Map<String, Object> map = new LinkedHashMap<>();
		BaseView top = frame == null ? null : frame.getActiveTopView();
		map.put("name", top == null ? null : top.getViewName());
		List<Integer> rows = new ArrayList<>();
		List<Long> ids = new ArrayList<>();
		SpreadSheet sheet = frame == null ? null : frame.getActiveSpreadSheet();
		if (sheet != null) {
			for (int row : sheet.getSelectedRows()) rows.add(row);
			for (Object node : sheet.getSelectedNodesImpl()) {
				if (node instanceof Task task) ids.add(task.getUniqueId());
			}
		}
		map.put("selectedRows", rows);
		map.put("selectedTaskIds", ids);
		Window active = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
		map.put("focusedWindow", active == null ? null : title(active));
		String modal = null;
		for (Window window : Window.getWindows()) {
			if (window instanceof Dialog dialog && dialog.isShowing() && dialog.isModal()) modal = dialog.getTitle();
		}
		map.put("modalDialog", modal);
		return map;
	}

	private static String title(Window window) {
		if (window instanceof Frame frame) return frame.getTitle();
		if (window instanceof Dialog dialog) return dialog.getTitle();
		return window.getClass().getName();
	}

	private static Map<String, Object> project(Project project) {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("fileName", project.getFileName());
		map.put("start", ProjectStateSnapshot.isoUtc(project.getStart()));
		map.put("end", ProjectStateSnapshot.isoUtc(project.getEnd()));
		map.put("dirty", project.isDirty());
		Map<String, Object> undo = new LinkedHashMap<>();
		var controller = project.getUndoController();
		undo.put("canUndo", controller.canUndo());
		undo.put("canRedo", controller.canRedo());
		undo.put("undoNames", controller.getUndoNames(20));
		map.put("undo", undo);
		return map;
	}
}
