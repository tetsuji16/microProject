/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.dialog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import javax.swing.JComboBox;
import javax.swing.JSpinner;
import javax.swing.JTextArea;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.model.assignment.Assignment;
import com.microproject.model.assignment.AssignmentService;
import com.microproject.model.assignment.contour.ContourTypes;
import com.microproject.exchange.MpoFileImporter;
import com.microproject.model.resource.Resource;
import com.microproject.model.resource.ResourcePool;
import com.microproject.model.task.NormalTask;
import com.microproject.model.task.Project;
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
		assertEquals("Test notes", findComponent(dialog, "assignmentNotes", JTextArea.class).getText());
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
		JTextArea notes = findComponent(dialog, "assignmentNotes", JTextArea.class);

		unitsSpinner.setValue(200.0); // 200%
		contourCombo.setSelectedItem(ContourTypes.Kind.FRONT_LOADED);
		notes.setText("Changed notes");

		// Simulate OK by calling onOk() via reflection or direct call
		dialog.onOk();
		dialog.dispose();

		// Verify model changes
		assertEquals(2.0, assignment.getUnits(), 0.001);
		assertEquals(ContourTypes.Kind.FRONT_LOADED, assignment.getWorkContourTypeKind());
		assertEquals("Changed notes", assignment.getNotes());

		// Verify single Undo edit
		assertTrue(project.getUndoController().canUndo());
		project.getUndoController().undo();
		assertEquals(1.0, assignment.getUnits(), 0.001);
		assertEquals(ContourTypes.Kind.FLAT, assignment.getWorkContourTypeKind());
		assertEquals("Original notes", assignment.getNotes());
	}

	@Test
	void headlessEditServiceAppliesAllFieldsAsOneUndoableEdit() throws Exception {
		Project project = newProject();
		Resource resource = project.getResourcePool().newResourceInstance();
		NormalTask task = newTask(project, "Task 1");
		Assignment assignment = newAssignment(task, resource);
		assignment.forceUnits(1.0);
		assignment.setWorkContourTypeKind(ContourTypes.Kind.FLAT);
		assignment.setNotes("Before");
		project.getUndoController().clear();

		assertTrue(com.microproject.application.AssignmentInformationEditService.apply(assignment,
			2.0, ContourTypes.Kind.FRONT_LOADED, "After", this));
		assertEquals(2.0, assignment.getUnits(), 0.001);
		assertEquals(ContourTypes.Kind.FRONT_LOADED, assignment.getWorkContourTypeKind());
		assertEquals("After", assignment.getNotes());
		ByteArrayOutputStream encoded = new ByteArrayOutputStream();
		org.junit.jupiter.api.Assertions.assertTrue(new MpoFileImporter().saveProject(project, encoded));
		Project reloaded = new MpoFileImporter().loadProject(new ByteArrayInputStream(encoded.toByteArray()));
		NormalTask reloadedTask = (NormalTask) reloaded.getTaskList().stream()
			.filter(candidate -> "Task 1".equals(candidate.getName())).findFirst().orElseThrow();
		Assignment reloadedAssignment = (Assignment) reloadedTask.getAssignments().iterator().next();
		assertEquals(2.0, reloadedAssignment.getUnits(), 0.001);
		assertEquals("After", reloadedAssignment.getNotes());
		assertTrue(project.getUndoController().canUndo());
		project.getUndoController().undo();
		assertEquals(1.0, assignment.getUnits(), 0.001);
		assertEquals(ContourTypes.Kind.FLAT, assignment.getWorkContourTypeKind());
		assertEquals("Before", assignment.getNotes());
	}

	@Test
	void headlessEditServiceRestoresAllFieldsWhenAFieldMutationFails() {
		Project project = newProject();
		Resource resource = project.getResourcePool().newResourceInstance();
		NormalTask task = newTask(project, "Task 1");
		Assignment assignment = newAssignment(task, resource);
		assignment.forceUnits(1.0);
		assignment.setWorkContourTypeKind(ContourTypes.Kind.FLAT);
		assignment.setNotes("Before");
		project.getUndoController().clear();
		boolean[] priorEditUndone = { false };
		project.getUndoController().getEditSupport().postEdit(new javax.swing.undo.AbstractUndoableEdit() {
			private static final long serialVersionUID = 1L;
			@Override public void undo() {
				super.undo();
				priorEditUndone[0] = true;
			}
		});
		assertTrue(project.getUndoController().canUndo(), "pre-existing history is available before the failed edit");

		org.junit.jupiter.api.Assertions.assertThrows(NullPointerException.class, () ->
			com.microproject.application.AssignmentInformationEditService.apply(assignment,
				2.0, null, "After", this));
		assertEquals(1.0, assignment.getUnits(), 0.001);
		assertEquals(ContourTypes.Kind.FLAT, assignment.getWorkContourTypeKind());
		assertEquals("Before", assignment.getNotes());
		assertTrue(project.getUndoController().canUndo(), "failure must retain the earlier history without a broken edit");
		project.getUndoController().undo();
		assertTrue(priorEditUndone[0], "the earlier undo entry remains the top history item");
		assertFalse(project.getUndoController().canUndo(), "the failed assignment edit must not be added to history");
		assertEquals(1.0, assignment.getUnits(), 0.001);
		assertEquals(ContourTypes.Kind.FLAT, assignment.getWorkContourTypeKind());
		assertEquals("Before", assignment.getNotes());
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
