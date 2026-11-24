/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.application;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import com.microproject.model.assignment.Assignment;
import com.microproject.model.assignment.contour.ContourTypes;
import com.microproject.model.task.Project;
import com.microproject.undo.AssignmentDetailStateEdit;

/** Applies assignment information fields as one reversible model edit. */
public final class AssignmentInformationEditService {
	private AssignmentInformationEditService() { }

	public static final class AssignmentReadOnlyException extends IllegalStateException {
		private static final long serialVersionUID = 1L;
		public AssignmentReadOnlyException() { super("Assignment information cannot be edited"); }
	}

	public static boolean apply(Assignment assignment, double units, ContourTypes.Kind contour,
			String notes, Object source) {
		if (assignment.isReadOnly()) throw new AssignmentReadOnlyException();
		boolean changed = Double.compare(units, assignment.getUnits()) != 0
			|| contour != assignment.getWorkContourTypeKind()
			|| !Objects.equals(notes, assignment.getNotes());
		if (!changed) return false;
		Project project = assignment.getTask().getProject();
		if (project == null) throw new AssignmentReadOnlyException();
		Map<Assignment, Object> before = new LinkedHashMap<>();
		before.put(assignment, assignment.backupDetail());
		int transaction = project.fireMultipleTransaction(0, true);
		project.beginUndoUpdate();
		try {
			project.getUndoController().getEditSupport().postEdit(new AssignmentDetailStateEdit(before, Map.of()));
			if (Double.compare(units, assignment.getUnits()) != 0) assignment.forceUnits(units);
			if (contour != assignment.getWorkContourTypeKind()) assignment.setWorkContourTypeKind(contour);
			if (!Objects.equals(notes, assignment.getNotes())) assignment.setNotes(notes);
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
