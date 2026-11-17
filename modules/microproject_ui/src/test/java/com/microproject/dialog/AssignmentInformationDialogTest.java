/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.dialog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.swing.JTextPane;
import javax.swing.text.StyleConstants;

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
	@Test
	void richNotesRoundTripThroughEditorWithoutLosingCharacterFormatting() throws Exception {
		JTextPane editor = new JTextPane();
		editor.setText("Formatted assignment note");
		javax.swing.text.SimpleAttributeSet bold = new javax.swing.text.SimpleAttributeSet();
		StyleConstants.setBold(bold, true);
		editor.getStyledDocument().setCharacterAttributes(0, editor.getDocument().getLength(), bold, false);

		String rtf = AssignmentInformationDialog.writeNotes(editor);
		assertTrue(rtf.startsWith("{\\rtf"));
		JTextPane restored = new JTextPane();
		AssignmentInformationDialog.readNotes(restored, rtf);
		assertTrue(restored.getStyledDocument().getCharacterElement(0).getAttributes().containsAttribute(
			StyleConstants.Bold, true));
		assertTrue(restored.getText().contains("Formatted assignment note"));
	}

	@Test
	void plainNotesRemainReadableAndEditsCommitAsOneUndoableAssignmentChange() {
		Project project = createProject();
		NormalTask task = (NormalTask) project.createLocalTaskNode(null).getImpl();
		Resource resource = project.getResourcePool().newResourceInstance();
		Assignment assignment = AssignmentService.getInstance().newAssignment(task, resource, 1.0, 0L, this);
		assignment.setWorkContourTypeKind(ContourTypes.Kind.FLAT);
		assignment.setNotes("plain note\nsecond line");
		String oldNotes = assignment.getNotes();
		project.getUndoController().clear();

		JTextPane editor = new JTextPane();
		AssignmentInformationDialog.readNotes(editor, oldNotes);
		assertTrue(editor.getText().contains("plain note"));
		String formatted = "{\\rtf1\\ansi\\b new note\\b0}";
		assertTrue(AssignmentInformationEditService.apply(assignment, 1.5, ContourTypes.Kind.BACK_LOADED,
			formatted, this));
		assertEquals(1.5, assignment.getUnits());
		assertEquals(ContourTypes.Kind.BACK_LOADED, assignment.getWorkContourTypeKind());
		assertEquals(formatted, assignment.getNotes());
		assertTrue(project.getUndoController().canUndo());

		project.getUndoController().undo();
		assertEquals(1.0, assignment.getUnits());
		assertEquals(ContourTypes.Kind.FLAT, assignment.getWorkContourTypeKind());
		assertEquals(oldNotes, assignment.getNotes());
		project.getUndoController().redo();
		assertEquals(1.5, assignment.getUnits());
		assertEquals(ContourTypes.Kind.BACK_LOADED, assignment.getWorkContourTypeKind());
		assertEquals(formatted, assignment.getNotes());

		project.getUndoController().clear();
		project.setReadOnly(true);
		assertThrows(IllegalStateException.class, () -> AssignmentInformationEditService.apply(assignment,
			0.5, ContourTypes.Kind.FLAT, "changed", this));
		assertEquals(1.5, assignment.getUnits());
		assertEquals(formatted, assignment.getNotes());
		assertFalse(project.getUndoController().canUndo());
	}

	private static Project createProject() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("assignment-information-test", undo), undo);
		project.initialize(false, false);
		return project;
	}
}
