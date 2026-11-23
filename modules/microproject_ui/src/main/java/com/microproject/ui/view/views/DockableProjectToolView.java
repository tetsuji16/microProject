/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.ui.view.views;

import java.awt.BorderLayout;

import javax.swing.JComponent;
import javax.swing.JPanel;

import com.microproject.ui.dialog.ViewSettingsState;
import com.microproject.ui.view.model.cache.NodeModelCache;
import com.microproject.ui.view.spreadsheet.SpreadSheet;
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
	private final JComponent content;

	public DockableProjectToolView(Project project, String viewName, JComponent content) {
		this.project = project;
		this.viewName = viewName;
		this.content = content;
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
	@Override public WorkspaceSetting createWorkspace(int context) {
		Workspace workspace = new Workspace();
		if (content != null && content.getClientProperty(ViewSettingsState.PROPERTY) instanceof ViewSettingsState state)
			workspace.settings = state.saveSettings();
		return workspace;
	}
	@Override public void restoreWorkspace(WorkspaceSetting workspace, int context) {
		if (workspace instanceof Workspace saved && saved.settings != null && !saved.settings.isEmpty()
			&& content != null && content.getClientProperty(ViewSettingsState.PROPERTY) instanceof ViewSettingsState state) {
			state.restoreSettings(saved.settings);
			if (content.getClientProperty(ViewSettingsState.REFRESH_PROPERTY) instanceof Runnable refresh)
				refresh.run();
		}
	}

	private static final class Workspace implements WorkspaceSetting {
		private static final long serialVersionUID = 1L;
		/** The embedded view's display flags as text; null in workspaces saved before they were kept. */
		private String settings;
	}
}
