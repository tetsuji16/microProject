/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.frames;

import java.awt.Frame;

import com.microproject.document.ObjectEvent;
import com.microproject.dialog.ProjectInformationDialog;
import com.microproject.dialog.ResourceInformationDialog;
import com.microproject.dialog.TaskInformationDialog;
import com.microproject.pm.resource.Resource;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.Task;
import com.microproject.pm.graphic.spreadsheet.selection.event.SelectionNodeEvent;

/** Owns reusable project, task, and resource information dialog lifecycles. */
final class InformationDialogCoordinator {
	private ProjectInformationDialog projectDialog;
	private TaskInformationDialog taskDialog;
	private ResourceInformationDialog resourceDialog;

	void showProject(Frame owner, DocumentFrame documentFrame, Project project) {
		if (projectDialog == null) {
			projectDialog = ProjectInformationDialog.getInstance(owner, project);
			projectDialog.pack();
			projectDialog.setModal(false);
		} else {
			projectDialog.setObject(project);
		}
		projectDialog.setMoveProjectHandler(documentFrame::moveProject);
		projectDialog.setLocationRelativeTo(documentFrame);
		projectDialog.setVisible(true);
	}

	void showTask(Frame owner, DocumentFrame documentFrame, Task task, boolean notes,
			boolean resourcesTab) {
		if (taskDialog == null) {
			TaskInformationDialog dialog = TaskInformationDialog.getInstance(owner, task, notes);
			try {
				dialog.pack();
				dialog.setModal(false);
				taskDialog = dialog;
			} catch (RuntimeException | Error exception) {
				dialog.dispose();
				throw exception;
			}
		} else {
			taskDialog.setObject(task);
			taskDialog.updateAll();
		}
		taskDialog.setLocationRelativeTo(documentFrame);
		if (notes)
			taskDialog.showNotes();
		else if (resourcesTab)
			taskDialog.showResources();
		taskDialog.setVisible(true);
	}

	boolean isTaskDialogVisible() {
		return taskDialog != null && taskDialog.isVisible();
	}

	void showResource(Frame owner, DocumentFrame documentFrame, Resource resource, boolean notes) {
		if (resourceDialog == null) {
			resourceDialog = ResourceInformationDialog.getInstance(owner, resource);
			resourceDialog.pack();
			resourceDialog.setModal(false);
		} else {
			resourceDialog.setObject(resource);
			resourceDialog.updateAll();
		}
		resourceDialog.setLocationRelativeTo(documentFrame);
		if (notes)
			resourceDialog.showNotes();
		resourceDialog.setVisible(true);
	}

	boolean hasTaskDialog() {
		return taskDialog != null;
	}

	boolean hasResourceDialog() {
		return resourceDialog != null;
	}

	void hideTaskDialog() {
		if (taskDialog != null)
			taskDialog.setVisible(false);
	}

	void hideResourceDialog() {
		if (resourceDialog != null)
			resourceDialog.setVisible(false);
	}

	void documentSelected(DocumentSelectedEvent event) {
		if (projectDialog != null)
			projectDialog.documentSelected(event);
		if (taskDialog != null)
			taskDialog.documentSelected(event);
		if (resourceDialog != null)
			resourceDialog.documentSelected(event);
	}

	void selectionChanged(SelectionNodeEvent event) {
		if (taskDialog != null)
			taskDialog.selectionChanged(event);
		if (resourceDialog != null)
			resourceDialog.selectionChanged(event);
	}

	void objectChanged(ObjectEvent event) {
		if (projectDialog != null)
			projectDialog.objectChanged(event);
		if (taskDialog != null)
			taskDialog.objectChanged(event);
		if (resourceDialog != null)
			resourceDialog.objectChanged(event);
	}
}
