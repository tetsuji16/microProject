/*******************************************************************************
 * The contents of this file are subject to the Common Public Attribution License
 * Version 1.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.projectlibre.com/license . The License is based on the Mozilla Public
 * License Version 1.1 but Sections 14 and 15 have been added to cover use of
 * software over a computer network and provide for limited attribution for the
 * Original Developer. In addition, Exhibit A has been modified to be consistent
 * with Exhibit B.
 *******************************************************************************/
package com.microproject.dialog.assignment;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.event.ChangeEvent;

import com.microproject.association.Association;
import com.microproject.field.Field;
import com.microproject.graphic.configuration.GraphicConfiguration;
import com.microproject.graphic.configuration.SpreadSheetFieldArray;
import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.model.NodeModelDataFactory;
import com.microproject.options.TimesheetOption;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.assignment.timesheet.TimesheetAssignment;
import com.microproject.pm.assignment.timesheet.TimesheetStatus;
import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetModel;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetUtils;
import com.microproject.pm.resource.Resource;
import com.microproject.pm.snapshot.Snapshottable;
import com.microproject.pm.task.Project;

public class TimesheetEntryPane extends JScrollPane {
	private static final long serialVersionUID = 1L;

	public static final String spreadsheetCategory = "timesheetSpreadsheet";

	private final CommonAssignmentDialog dialog;
	private final Project project;
	private final List<Resource> selectedResources = new ArrayList<>();
	private final List<TimesheetAssignment> timesheetAssignments = new ArrayList<>();
	private SpreadSheet spreadSheet;
	private final Collection editableTimesheetFields;

	private class TimesheetSpreadSheet extends SpreadSheet {
		private static final long serialVersionUID = 1L;

		private TimesheetAssignment getEntryInRow(int row) {
			Node node = ((SpreadSheetModel) getModel()).getNode(row).getNode();
			if (node != null && !node.isVirtual()) {
				return (TimesheetAssignment) node.getImpl();
			}
			return null;
		}

		public void setValueAt(Object aValue, int row, int column) {
			super.setValueAt(aValue, row, column);
			TimesheetAssignment entry = getEntryInRow(row);
			if (entry != null) {
				entry.getAssignment().setTimesheetStatus(TimesheetStatus.ENTERED);
				entry.setDirty(true);
			}
		}

		public java.awt.Component prepareEditor(javax.swing.table.TableCellEditor editor, int row, int column) {
			dialog.setEditorButtonsVisible(true);
			return super.prepareEditor(editor, row, column);
		}

		public void editingCanceled(ChangeEvent e) {
			dialog.setEditorButtonsVisible(false);
			super.editingCanceled(e);
		}

		public void editingStopped(ChangeEvent e) {
			dialog.setEditorButtonsVisible(false);
			super.editingStopped(e);
		}
	}

	public TimesheetEntryPane(CommonAssignmentDialog dialog, Project project) {
		super();
		this.dialog = dialog;
		this.project = project;
		this.editableTimesheetFields = createEditableTimesheetFields();
	}

	public void setSelectedResources(List<? extends Resource> resources) {
		selectedResources.clear();
		if (resources != null) {
			selectedResources.addAll(resources);
		}
		init();
	}

	public SpreadSheet getSpreadSheet() {
		return spreadSheet;
	}

	private void init() {
		if (project == null) {
			return;
		}
		if (spreadSheet == null) {
			spreadSheet = new TimesheetSpreadSheet();
			spreadSheet.setSpreadSheetCategory(spreadsheetCategory);
			spreadSheet.setCanModifyColumns(false);
			spreadSheet.setCanSelectFieldArray(false);
			spreadSheet.setActions(new String[] {});
			setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		}

		rebuildTimesheetAssignments();
		if (spreadSheet.getModel() == null) {
			SpreadSheetUtils.createCollectionSpreadSheet(
				spreadSheet,
				timesheetAssignments,
				"Text.Timesheet",
				spreadsheetCategory,
				TimesheetOption.getInstance().getTimesheetFieldArrayName(),
				false,
				(NodeModelDataFactory) project,
				0);
		} else {
			SpreadSheetUtils.updateCollectionSpreadSheet(
				spreadSheet,
				timesheetAssignments,
				(NodeModelDataFactory) project,
				0);
		}

		JViewport viewport = createViewport();
		viewport.setView(spreadSheet);
		setViewport(viewport);

		Dimension preferred = spreadSheet.getPreferredSize();
		Dimension enclosing = new Dimension();
		GraphicConfiguration config = GraphicConfiguration.getInstance();
		int rowHeaderWidth = config.getRowHeaderWidth() + spreadSheet.getRowMargin() * 2;
		enclosing.setSize(preferred.getWidth() + rowHeaderWidth, preferred.getHeight());
		viewport.setPreferredSize(enclosing);
		spreadSheet.setEnabled(!timesheetAssignments.isEmpty());
		if (spreadSheet.getRowHeader() != null) {
			spreadSheet.getRowHeader().setEnabled(!timesheetAssignments.isEmpty());
		}
	}

	private void rebuildTimesheetAssignments() {
		timesheetAssignments.clear();
		for (Assignment liveAssignment : resolveAssignments()) {
			Assignment timesheetSnapshot = prepareTimesheetSnapshot(liveAssignment);
			Resource resource = liveAssignment.getResource();
			TimesheetAssignment entry = new TimesheetAssignment(
				liveAssignment.getProjectName(),
				liveAssignment.getTask().getName(),
				liveAssignment.getOwningProject().getUniqueId(),
				liveAssignment.getTask().getUniqueId(),
				resource == null ? 0 : resource.getUniqueId(),
				timesheetSnapshot,
				null);
			timesheetAssignments.add(entry);
		}
		Collections.sort(timesheetAssignments, new Comparator<TimesheetAssignment>() {
			public int compare(TimesheetAssignment left, TimesheetAssignment right) {
				int byResource = safe(left.getResourceName()).compareToIgnoreCase(safe(right.getResourceName()));
				if (byResource != 0) {
					return byResource;
				}
				int byTask = safe(left.getTaskName()).compareToIgnoreCase(safe(right.getTaskName()));
				if (byTask != 0) {
					return byTask;
				}
				return Long.compare(left.getCachedStart(), right.getCachedStart());
			}
		});
	}

	private List<Assignment> resolveAssignments() {
		List<Assignment> assignments = new ArrayList<>();
		for (Resource resource : resolveResources()) {
			for (Association association : resource.getAssignments()) {
				if (association instanceof Assignment)
					assignments.add((Assignment) association);
			}
		}
		return assignments;
	}

	private static String safe(String value) {
		return value == null ? "" : value;
	}

	private Assignment prepareTimesheetSnapshot(Assignment liveAssignment) {
		Assignment timesheetSnapshot = liveAssignment.getBaselineAssignment(Snapshottable.TIMESHEET, true);
		timesheetSnapshot.setTimesheetAssignment(true);
		int status = timesheetSnapshot.getTimesheetStatus();
		if (status == TimesheetStatus.NO_DATA || status == TimesheetStatus.INTEGRATED) {
			Field.copyData(editableTimesheetFields, timesheetSnapshot, liveAssignment);
			timesheetSnapshot.setTimesheetStatus(status);
		}
		return timesheetSnapshot;
	}

	private Collection<Field> createEditableTimesheetFields() {
		List<Field> fields = new LinkedList<>();
		SpreadSheetFieldArray fieldArray = TimesheetOption.getInstance().getTimesheetFieldArray();
		for (Field field : asFields(fieldArray)) {
			if (!field.isReadOnly()) {
				fields.add(field);
			}
		}
		return fields;
	}

	private List<Resource> resolveResources() {
		LinkedHashSet<Resource> uniqueResources = new LinkedHashSet<>();
		if (!selectedResources.isEmpty()) {
			uniqueResources.addAll(selectedResources);
		} else {
			if (project.getResourcePool() != null) {
				for (Resource resource : project.getResourcePool().getResourceList()) {
					uniqueResources.add(resource);
				}
			}
		}
		List<Resource> result = new ArrayList<>();
		for (Resource resource : uniqueResources) {
			if (!resource.getAssignments().isEmpty()) {
				result.add(resource);
			}
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	private List<Field> asFields(SpreadSheetFieldArray fieldArray) {
		return (List<Field>) (List<?>) fieldArray;
	}

	public boolean hasRows() {
		return !timesheetAssignments.isEmpty();
	}

	public boolean applyTimesheet() {
		if (spreadSheet != null && spreadSheet.isEditing() && spreadSheet.getCellEditor() != null) {
			spreadSheet.getCellEditor().stopCellEditing();
		}
		normalizeEnteredTimesheets();
		boolean changed = project.applyTimesheet(editableTimesheetFields, System.currentTimeMillis());
		if (changed) {
			rebuildTimesheetAssignments();
			SpreadSheetUtils.updateCollectionSpreadSheet(
				spreadSheet,
				timesheetAssignments,
				(NodeModelDataFactory) project,
				0);
		}
		return changed;
	}

	private void normalizeEnteredTimesheets() {
		for (TimesheetAssignment entry : timesheetAssignments) {
			Assignment snapshot = entry.getAssignment();
			if (snapshot.getTimesheetStatus() == TimesheetStatus.ENTERED) {
				snapshot.setTimesheetStatus(TimesheetStatus.VALIDATED);
			}
		}
	}
}
