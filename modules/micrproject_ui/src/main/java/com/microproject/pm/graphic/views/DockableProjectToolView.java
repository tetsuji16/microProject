/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.pm.graphic.views;

import java.awt.BorderLayout;

import javax.swing.JComponent;
import javax.swing.JPanel;

import com.microproject.pm.graphic.model.cache.NodeModelCache;
import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.pm.task.Project;
import com.microproject.undo.UndoController;
import com.microproject.workspace.WorkspaceSetting;

/**
 * Adapter for project tools which historically lived in a separate dialog.
 * The wrapped component stays owned by the tool while this class gives it the
 * same lifecycle as the document's regular top views.
 */
public final class DockableProjectToolView extends JPanel implements BaseView {
	private static final long serialVersionUID = 1L;
	private final Project project;
	private final String viewName;

	public DockableProjectToolView(Project project, String viewName, JComponent content) {
		this.project = project;
		this.viewName = viewName;
		setLayout(new BorderLayout());
		if (content != null)
			add(content, BorderLayout.CENTER);
	}

	@Override public UndoController getUndoController() { return project.getUndoController(); }
	@Override public void zoomIn() { }
	@Override public void zoomOut() { }
	@Override public void scrollToTask() { }
	@Override public boolean canZoomIn() { return false; }
	@Override public boolean canZoomOut() { return false; }
	@Override public boolean canScrollToTask() { return false; }
	@Override public int getScale() { return -1; }
	@Override public SpreadSheet getSpreadSheet() { return null; }
	@Override public boolean hasNormalMinWidth() { return true; }
	@Override public String getViewName() { return viewName; }
	@Override public boolean showsTasks() { return true; }
	@Override public boolean showsResources() { return true; }
	@Override public void onActivate(boolean activate) { }
	@Override public boolean isPrintable() { return false; }
	@Override public void cleanUp() { removeAll(); }
	@Override public NodeModelCache getCache() { return null; }
	@Override public WorkspaceSetting createWorkspace(int context) { return new Workspace(); }
	@Override public void restoreWorkspace(WorkspaceSetting workspace, int context) { }

	private static final class Workspace implements WorkspaceSetting {
		private static final long serialVersionUID = 1L;
	}
}
