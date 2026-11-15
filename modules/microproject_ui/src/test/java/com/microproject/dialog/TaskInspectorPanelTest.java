/*
 * MIT License
 * Copyright (c) 2026 microProject
 */
package com.microproject.dialog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.strings.Messages;

class TaskInspectorPanelTest {
	@Test
	void predecessorProjectionFeedbackHasEnglishAndJapaneseResourceValues() {
		ResourceBundle english = ResourceBundle.getBundle("com.microproject.strings.client", Locale.ROOT);
		ResourceBundle japanese = ResourceBundle.getBundle("com.microproject.strings.client", Locale.JAPANESE);
		String key = "TaskInspector.PredecessorHidden";

		assertEquals("The predecessor is hidden by the current filter or grouping.", english.getString(key));
		assertEquals("先行タスクは現在のフィルターまたはグループ化で非表示です。", japanese.getString(key));
	}

	@Test
	void followsSelectedTaskAndRendersScheduleCategoriesWithoutChangingModel() throws Exception {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("inspector", undo), undo);
		project.initialize(false, false);
		NormalTask first = new NormalTask(project);
		first.setName("First task");
		first.setManuallyScheduled(true);
		project.connectTask(first);
		NormalTask second = new NormalTask(project);
		second.setName("Second task");
		project.connectTask(second);
		long firstStart = first.getStart();
		long firstEnd = first.getEnd();
		long secondStart = second.getStart();
		long secondEnd = second.getEnd();
		boolean dirty = project.needsSaving();
		boolean canUndo = undo.canUndo();

		SwingUtilities.invokeAndWait(() -> {
			TaskInspectorPanel panel = new TaskInspectorPanel(task -> { }, task -> { }, task -> false);
			panel.showTask(first);
			assertSame(first, panel.getTask());
			assertTrue(findButton(panel, Messages.getString("TaskInspector.Automatic")) != null);
			assertTrue(!findButton(panel, Messages.getString("TaskInspector.Automatic")).isEnabled(),
				"the mutation route must reflect the command's unavailable state");
			String rendered = renderedText(panel);
			assertTrue(rendered.contains(Messages.getString("TaskInspector.ActualStartAssignments")), rendered);
			assertTrue(rendered.contains(Messages.getString("TaskInspector.LevelingDelay")));
			assertTrue(rendered.contains(Messages.getString("TaskInspector.Constraint")));
			assertTrue(rendered.contains(Messages.getString("TaskInspector.Predecessors")));
			assertTrue(rendered.contains(Messages.getString("TaskInspector.Subtasks")));
			assertTrue(rendered.contains(Messages.getString("TaskInspector.Calendar")));
			panel.showTask(second);
			assertSame(second, panel.getTask());
			assertTrue(renderedText(panel).contains("Second task"));
		});
		assertEquals(firstStart, first.getStart());
		assertEquals(firstEnd, first.getEnd());
		assertEquals(secondStart, second.getStart());
		assertEquals(secondEnd, second.getEnd());
		assertEquals(dirty, project.needsSaving());
		assertEquals(canUndo, undo.canUndo());
	}

	private static String renderedText(Container container) {
		StringBuilder text = new StringBuilder();
		for (Component component : container.getComponents()) {
			if (component instanceof JLabel label) text.append(label.getText()).append('\n');
			if (component instanceof JTextArea area) text.append(area.getText()).append('\n');
			if (component instanceof Container nested) text.append(renderedText(nested));
		}
		return text.toString();
	}

	@Test
	void indicatorCheckBoxReflectsAndAppliesIgnoreWarnings() throws Exception {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("inspector-ignore", undo), undo);
		project.initialize(false, false);
		NormalTask task = new NormalTask(project);
		task.setName("Checked task");
		project.connectTask(task);
		java.util.List<Object> applied = new java.util.ArrayList<>();

		SwingUtilities.invokeAndWait(() -> {
			TaskInspectorPanel panel = new TaskInspectorPanel(t -> { }, t -> { }, t -> false,
				(t, ignore) -> { applied.add(t); applied.add(ignore); }, t -> true);
			panel.showTask(task);
			javax.swing.JCheckBox box = findCheckBox(panel);
			assertEquals(Messages.getString("TaskInspector.ShowIndicators"), box.getText());
			assertTrue(box.isSelected());
			assertTrue(box.isEnabled());
			box.doClick();
			assertEquals(java.util.List.of(task, Boolean.TRUE), applied);
			task.setIgnoreWarnings(true);
			panel.showTask(task);
			assertTrue(!findCheckBox(panel).isSelected());
			TaskInspectorPanel readOnly = new TaskInspectorPanel(t -> { }, t -> { }, t -> false);
			readOnly.showTask(task);
			assertTrue(!findCheckBox(readOnly).isEnabled());
		});
	}

	@Test
	void indicatorCheckBoxLabelHasEnglishAndJapaneseValues() {
		assertEquals("Show warning and suggestion indicators for this task",
			ResourceBundle.getBundle("com.microproject.strings.client", Locale.ROOT).getString("TaskInspector.ShowIndicators"));
		assertEquals("このタスクの警告と提案のインジケーターを表示する",
			ResourceBundle.getBundle("com.microproject.strings.client", Locale.JAPANESE).getString("TaskInspector.ShowIndicators"));
	}

	private static javax.swing.JCheckBox findCheckBox(Container root) {
		for (Component child : root.getComponents()) {
			if (child instanceof javax.swing.JCheckBox box) return box;
			if (child instanceof Container container) {
				javax.swing.JCheckBox nested = findCheckBox(container);
				if (nested != null) return nested;
			}
		}
		return null;
	}

	private static JButton findButton(Container container, String text) {
		for (Component component : container.getComponents()) {
			if (component instanceof JButton button && text.equals(button.getText())) return button;
			if (component instanceof Container nested) {
				JButton found = findButton(nested, text);
				if (found != null) return found;
			}
		}
		return null;
	}
}
