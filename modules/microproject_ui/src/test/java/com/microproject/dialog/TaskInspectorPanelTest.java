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
