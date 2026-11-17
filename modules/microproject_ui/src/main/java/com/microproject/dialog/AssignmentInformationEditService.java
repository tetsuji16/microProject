/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.dialog;

import java.util.LinkedHashMap;
import java.util.Map;

import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.assignment.contour.ContourTypes;
import com.microproject.pm.task.Project;
import com.microproject.strings.Messages;
import com.microproject.undo.AssignmentDetailStateEdit;

/** Applies the editable assignment information fields as one reversible model edit. */
final class AssignmentInformationEditService {
	private AssignmentInformationEditService() { }

	static boolean apply(Assignment assignment, double units, ContourTypes.Kind contour, String notes, Object source) {
		if (assignment.isReadOnly()) throw new IllegalStateException(Messages.getString("AssignmentInformationDialog.readOnly"));
		boolean changed = Double.compare(units, assignment.getUnits()) != 0
			|| contour != assignment.getWorkContourTypeKind()
			|| !java.util.Objects.equals(notes, assignment.getNotes());
		if (!changed) return false;
		Project project = assignment.getTask().getProject();
		if (project == null) throw new IllegalStateException(Messages.getString("AssignmentInformationDialog.readOnly"));
		Map<Assignment, Object> before = new LinkedHashMap<>();
		before.put(assignment, assignment.backupDetail());
		int transaction = project.fireMultipleTransaction(0, true);
		project.beginUndoUpdate();
		try {
			project.getUndoController().getEditSupport().postEdit(new AssignmentDetailStateEdit(before, Map.of()));
			assignment.forceUnits(units);
			assignment.setWorkContourTypeKind(contour);
			assignment.setNotes(notes);
			assignment.doUpdateService(source);
			Map<Assignment, Object> after = Map.of(assignment, assignment.backupDetail());
			project.getUndoController().getEditSupport().postFinalEdit(new AssignmentDetailStateEdit(Map.of(), after));
			return true;
		} catch (RuntimeException | Error failure) {
			assignment.restoreDetail(before.get(assignment));
			assignment.doUpdateService(source);
			throw failure;
		} finally {
			project.fireMultipleTransaction(transaction, false);
			project.endUndoUpdate();
		}
	}
}
