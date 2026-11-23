/*******************************************************************************
* MIT License
*
* Copyright (c) 2026 microProject
*
* Permission is hereby granted, free of charge, to any person obtaining a copy
* of this software and associated documentation files (the "Software"), to deal
* in the Software without restriction, including without limitation the rights
* to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
* copies of the Software, and to permit persons to whom the Software is
* furnished to do so, subject to the following conditions:
*
* The above copyright notice and this permission notice shall be included in all
* copies or substantial portions of the Software.
*
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
* IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
* OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
* SOFTWARE.
*******************************************************************************/
package com.microproject.pm.graphic.views;

import javax.swing.JScrollPane;
import javax.swing.JViewport;

import com.microproject.help.HelpUtil;
import com.microproject.menu.MenuActionConstants;
import com.microproject.menu.MenuManager;
import com.microproject.pm.graphic.frames.DocumentFrame;
import com.microproject.pm.graphic.model.cache.NodeModelCache;
import com.microproject.pm.graphic.model.cache.NodeModelCacheFactory;
import com.microproject.pm.graphic.model.cache.ReferenceNodeModelCache;
import com.microproject.pm.graphic.pert.PertChart;
import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.configuration.Dictionary;
import com.microproject.graphic.configuration.BarStyles;
import com.microproject.grouping.core.model.NodeModel;
import com.microproject.pm.task.Project;
import com.microproject.undo.UndoController;
import com.microproject.workspace.WorkspaceSetting;
import com.microproject.pm.graphic.network.PertManualPositions;

/**
*
*/
public class PertView extends JScrollPane implements BaseView {
	private static final long serialVersionUID = 1493530627188782732L;
	protected PertChart pert;
	protected NodeModel model;
	protected Project project;
	DocumentFrame documentFrame;

	protected NodeModelCache cache;
	/**
	*
	*/
	public PertView(DocumentFrame documentFrame, MenuManager manager) {
		super();
		HelpUtil.addDocHelp(this,"Network_Diagram");
		this.documentFrame = documentFrame;
		this.project = documentFrame.getProject();
	}
	public void init(ReferenceNodeModelCache cache, NodeModel model){
		pert=new PertChart(project,"Network");
		this.cache=NodeModelCacheFactory.getInstance().createAntiAssignmentFilteredCache(cache,getViewName(),null);
		pert.setCache(this.cache);
		pert.setBarStyles((BarStyles) Dictionary.get(BarStyles.category,"pert"));


		JViewport viewport = createViewport();
		viewport.setView(pert);
		setViewport(viewport);
		if (project.isReadOnly())
			pert.setEnabled(false);
		cache.update(); //this is not required by certain views
	}

	public void cleanUp() {
		pert.cleanUp();
		pert = null;
		model= null;
		project= null;
		documentFrame= null;
	}
	public UndoController getUndoController() {
		return project.getUndoController();
	}

	public void zoomIn(){
		pert.zoomIn();
	}
	public void zoomOut(){
		pert.zoomOut();
	}
	public boolean canZoomIn() {
		return pert.canZoomIn();
	}
	public boolean canZoomOut() {
		return pert.canZoomOut();
	}
	public int getScale() {
		return pert.getZoom();
	}
	public SpreadSheet getSpreadSheet() {
		return null;
	}
	public boolean hasNormalMinWidth() {
		return true;
	}

	public String getViewName() {
		return MenuActionConstants.ACTION_NETWORK;
	}

	public boolean showsTasks() {
		return true;
	}
	public boolean showsResources() {
		return false;
	}
	public void onActivate(boolean activate) {
	}

	public boolean isPrintable() {
		return true;
	}
	public void restoreWorkspace(WorkspaceSetting w, int context) {
		Workspace ws = (Workspace) w;
		pert.restoreWorkspace(ws.network, context);
	}
	public WorkspaceSetting createWorkspace(int context) {
		Workspace ws = new Workspace();
		ws.network = pert.createWorkspace(context);
		return ws;
	}

	public static class Workspace implements WorkspaceSetting {
		private static final long serialVersionUID = 3364215160357571230L;
		WorkspaceSetting network;

		public WorkspaceSetting getNetwork() {
			return network;
		}

		public void setNetwork(WorkspaceSetting network) {
			this.network = network;
		}
	}

	@Override public boolean supportsSummaryTasksToggle() {
		return cache instanceof com.microproject.pm.graphic.model.cache.ViewNodeModelCache;
	}
	@Override public boolean isSummaryTasksVisible() {
		return !(cache instanceof com.microproject.pm.graphic.model.cache.ViewNodeModelCache viewCache) || !viewCache.getViewTransformer().isHideSummaryTasks();
	}
	/** Hides or shows the summary task boxes through the view transform (boxes only; tasks are untouched). */
	@Override public void setSummaryTasksVisible(boolean visible) {
		if (cache instanceof com.microproject.pm.graphic.model.cache.ViewNodeModelCache viewCache)
			viewCache.getViewTransformer().setHideSummaryTasks(!visible);
	}
	public com.microproject.pm.graphic.network.NetworkDisplayOptions getDisplayOptions() {
		return pert.getDisplayOptions();
	}
	/** The routing currently used for the links (orthogonal or straight). */
	public com.microproject.pm.graphic.graph.LinkRouting getLinkRouting() { return pert.getRouting(); }
	public void repaintDiagram() { pert.repaint(); }
	/** Layout dialog choices: link style, box spacing and whether summary task boxes show. */
	public void applyLayoutChoice(boolean straight, int spacing, boolean summaryTasks) {
		pert.setStraightLinks(straight);
		pert.getDisplayOptions().setBoxSpacing(spacing);
		if (supportsSummaryTasksToggle()) setSummaryTasksVisible(summaryTasks);
	}
	public void setProgressMarks(boolean value) { pert.getDisplayOptions().setProgressMarks(value); pert.repaint(); }
	public void setLinkLabels(boolean value) { pert.getDisplayOptions().setLinkLabels(value); pert.repaint(); }
	public void setStraightLinks(boolean value) { pert.setStraightLinks(value); }
	/** Collapsed boxes show the task ID only; the diagram is laid out again for the smaller boxes. */
	public void setCollapsedBoxes(boolean value) { pert.getDisplayOptions().setCollapsed(value); pert.relayout(); }
	public void setBoxSpacing(int value) { pert.getDisplayOptions().setBoxSpacing(value); pert.relayout(); }
	public void setBoxStyle(com.microproject.pm.graphic.network.NetworkDisplayOptions.BoxType type,
			com.microproject.pm.graphic.network.NetworkDisplayOptions.BoxStyle style) {
		pert.getDisplayOptions().setStyle(type, style);
		pert.repaint();
	}
	/** Recomputes every box position from the dependency levels (Layout Now). */
	public void layoutNow() {
		if (pert == null || pert.getNetworkLayout() == null) return;
		PertManualPositions pmp = project.findTransientDocumentState(PertManualPositions.class);
		if (pmp != null && !pmp.isEmpty()) {
			java.util.Map<Long, PertManualPositions.Position> previous = new java.util.HashMap<>(pmp.getPositions());
			pmp.clear();
			project.setDirty(true);
			UndoController undoController = project.getUndoController();
			if (undoController != null && undoController.getEditSupport() != null) {
				undoController.getEditSupport().postEdit(new com.microproject.pm.graphic.network.NetworkLayoutNowEdit(pert, previous));
			}
		}
		pert.relayout();
	}
	public boolean canScrollToTask() {
		return false;
	}
	public void scrollToTask() {
	}

	public NodeModelCache getCache(){
		return cache;
	}



}
