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
		var editSupport = project.getUndoController().getEditSupport();
		var command = editSupport.beginCommand();
		var checkpoint = editSupport.checkpoint(command.group());
		boolean applied = false;
		try {
			project.getUndoController().getEditSupport().postEdit(new AssignmentDetailStateEdit(before, Map.of()));
			if (Double.compare(units, assignment.getUnits()) != 0) assignment.forceUnits(units);
			if (contour != assignment.getWorkContourTypeKind()) assignment.setWorkContourTypeKind(contour);
			if (!Objects.equals(notes, assignment.getNotes())) assignment.setNotes(notes);
			assignment.doUpdateService(source);
			Map<Assignment, Object> after = Map.of(assignment, assignment.backupDetail());
			project.getUndoController().getEditSupport().postFinalEdit(new AssignmentDetailStateEdit(Map.of(), after));
			applied = true;
			return true;
		} catch (RuntimeException | Error failure) {
			var rollbackRecording = editSupport.suppressRecording();
			try {
				assignment.restoreDetail(before.get(assignment));
				assignment.doUpdateService(source);
			} catch (RuntimeException | Error rollbackFailure) {
				failure.addSuppressed(rollbackFailure);
			} finally {
				rollbackRecording.close();
				editSupport.discardSince(command.group(), checkpoint);
			}
			throw failure;
		} finally {
			try {
				project.fireMultipleTransaction(transaction, false);
			} finally {
				try {
					if (applied) editSupport.reserveCommit(command.group());
				} finally {
					try {
						command.close();
					} finally {
						editSupport.completePublication(command.group());
					}
				}
			}
		}
	}
}
