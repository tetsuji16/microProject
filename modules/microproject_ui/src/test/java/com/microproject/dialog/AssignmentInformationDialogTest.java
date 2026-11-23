/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.dialog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;

import javax.swing.JComboBox;
import javax.swing.JSpinner;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.assignment.AssignmentService;
import com.microproject.pm.assignment.contour.ContourTypes;
import com.microproject.pm.resource.Resource;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;

class AssignmentInformationDialogTest {

	private static Project newProject() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("test", undo), undo);
		project.setStart(1699142400000L); // Fixed date: 2026-11-16 08:00 JST
		project.initialize(false, false);
		project.setUndoController(undo);
		return project;
	}

	private static NormalTask newTask(Project project, String name) {
		NormalTask task = project.createScriptedTask();
		task.setName(name);
		project.connectTask(task);
		return task;
	}

	private static Assignment newAssignment(NormalTask task, Resource resource) {
		return AssignmentService.getInstance().newAssignment(task, resource, 1.0, 0L, null);
	}

	@Test
	void loadsAssignmentValuesIntoDialog() {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "Dialog test requires a display");
		Project project = newProject();
		Resource resource = project.getResourcePool().newResourceInstance();
		resource.setName("R1");
		NormalTask task = newTask(project, "Task 1");
		Assignment assignment = newAssignment(task, resource);
		assignment.forceUnits(1.5);
		assignment.setWorkContourTypeKind(ContourTypes.Kind.BACK_LOADED);
		assignment.setNotes("Test notes");

		AssignmentInformationDialog dialog = new AssignmentInformationDialog((Frame) null, assignment);

		JSpinner unitsSpinner = findComponent(dialog, "assignmentUnits", JSpinner.class);
		JComboBox<ContourTypes.Kind> contourCombo = findComponent(dialog, "assignmentWorkContour", JComboBox.class);
		assertEquals(150.0, unitsSpinner.getValue());
		assertEquals(ContourTypes.Kind.BACK_LOADED, contourCombo.getSelectedItem());
		// Notes are loaded via RTFEditorKit; plain text should be present
		dialog.dispose();
	}

	@Test
	void okAppliesChangesAndCreatesSingleUndoEdit() {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "Dialog test requires a display");
		Project project = newProject();
		Resource resource = project.getResourcePool().newResourceInstance();
		resource.setName("R1");
		NormalTask task = newTask(project, "Task 1");
		Assignment assignment = newAssignment(task, resource);
		assignment.forceUnits(1.0);
		assignment.setWorkContourTypeKind(ContourTypes.Kind.FLAT);
		assignment.setNotes("Original notes");

		AssignmentInformationDialog dialog = new AssignmentInformationDialog((Frame) null, assignment);
		JSpinner unitsSpinner = findComponent(dialog, "assignmentUnits", JSpinner.class);
		JComboBox<ContourTypes.Kind> contourCombo = findComponent(dialog, "assignmentWorkContour", JComboBox.class);

		unitsSpinner.setValue(200.0); // 200%
		contourCombo.setSelectedItem(ContourTypes.Kind.FRONT_LOADED);

		// Simulate OK by calling onOk() via reflection or direct call
		dialog.onOk();
		dialog.dispose();

		// Verify model changes
		assertEquals(2.0, assignment.getUnits(), 0.001);
		assertEquals(ContourTypes.Kind.FRONT_LOADED, assignment.getWorkContourTypeKind());

		// Verify single Undo edit
		assertTrue(project.getUndoController().canUndo());
		project.getUndoController().undo();
		assertEquals(1.0, assignment.getUnits(), 0.001);
		assertEquals(ContourTypes.Kind.FLAT, assignment.getWorkContourTypeKind());
		assertEquals("Original notes", assignment.getNotes());
	}

	@Test
	void cancelLeavesModelUnchanged() {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "Dialog test requires a display");
		Project project = newProject();
		Resource resource = project.getResourcePool().newResourceInstance();
		resource.setName("R1");
		NormalTask task = newTask(project, "Task 1");
		Assignment assignment = newAssignment(task, resource);
		assignment.forceUnits(1.0);
		assignment.setWorkContourTypeKind(ContourTypes.Kind.FLAT);
		assignment.setNotes("Original notes");

		AssignmentInformationDialog dialog = new AssignmentInformationDialog((Frame) null, assignment);
		JSpinner unitsSpinner = findComponent(dialog, "assignmentUnits", JSpinner.class);
		unitsSpinner.setValue(200.0);

		// Simulate Cancel by calling onCancel() or dispose without onOk
		dialog.onCancel();
		dialog.dispose();

		assertEquals(1.0, assignment.getUnits(), 0.001);
		assertFalse(project.getUndoController().canUndo());
	}

	@Test
	void readOnlyProjectDisablesEditing() {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "Dialog test requires a display");
		Project project = newProject();
		project.setReadOnly(true);
		Resource resource = project.getResourcePool().newResourceInstance();
		resource.setName("R1");
		NormalTask task = newTask(project, "Task 1");
		Assignment assignment = newAssignment(task, resource);

		AssignmentInformationDialog dialog = new AssignmentInformationDialog((Frame) null, assignment);
		JSpinner unitsSpinner = findComponent(dialog, "assignmentUnits", JSpinner.class);
		JComboBox<?> contourCombo = findComponent(dialog, "assignmentWorkContour", JComboBox.class);

		assertFalse(unitsSpinner.isEnabled());
		assertFalse(contourCombo.isEnabled());
		dialog.dispose();
	}

	// Helper to find component by name
	private static <T extends Component> T findComponent(Container root, String name, Class<T> type) {
		for (Component c : root.getComponents()) {
			if (name.equals(c.getName()) && type.isInstance(c)) {
				return type.cast(c);
			}
			if (c instanceof Container container) {
				T found = findComponent(container, name, type);
				if (found != null) return found;
			}
		}
		return null;
	}
}
