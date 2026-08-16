/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2012-2019 ProjectLibre, Inc.  (Previous Copyright Holder)
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

import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.List;

import javax.swing.JScrollPane;
import javax.swing.JViewport;


import com.microproject.help.HelpUtil;
import com.microproject.menu.MenuActionConstants;
import com.microproject.menu.MenuManager;
import com.microproject.pm.graphic.frames.DocumentFrame;
import com.microproject.pm.graphic.graph.GraphInteractor;
import com.microproject.pm.graphic.model.cache.NodeModelCache;
import com.microproject.pm.graphic.model.cache.NodeModelCacheFactory;
import com.microproject.pm.graphic.model.cache.GraphicNode;
import com.microproject.pm.graphic.model.cache.ReferenceNodeModelCache;
import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.pm.graphic.xbs.Xbs;
import com.microproject.configuration.Dictionary;
import com.microproject.graphic.configuration.BarStyles;
import com.microproject.grouping.core.model.NodeModel;
import com.microproject.pm.task.Project;
import com.microproject.undo.UndoController;
import com.microproject.workspace.WorkspaceSetting;

/**
 *
 */
public class TreeView extends JScrollPane implements BaseView {
	private static final long serialVersionUID = 2390048109591199408L;

	protected Xbs tree;
	protected NodeModel model;
	protected Project project;
	DocumentFrame documentFrame;
	String viewName = null;
	protected NodeModelCache cache;
	/**
	 * 
	 */
	public TreeView(DocumentFrame documentFrame, MenuManager manager) {
		super();
		this.documentFrame = documentFrame;
		this.project = documentFrame.getProject();
	}
	public void init(ReferenceNodeModelCache cache, NodeModel model,String viewName,Consumer<Object> transformerClosure){
		tree=new Xbs(project,viewName);
		this.viewName = viewName;
		this.cache=NodeModelCacheFactory.getInstance().createAntiAssignmentFilteredCache((ReferenceNodeModelCache)cache,viewName,transformerClosure);
		tree.setCache(this.cache);
		tree.setBarStyles((BarStyles) Dictionary.get(BarStyles.category, viewName));
			
		
		JViewport viewport = createViewport();
		viewport.setView(tree);
		setViewport(viewport);
		cache.update(); //this is not required by certain views 
		HelpUtil.addDocHelp(this,viewName == MenuActionConstants.ACTION_RBS ? "RBS_Chart" : "WBS_Chart");
	//tree.insertCacheData();
	}
	public void cleanUp() {
		tree.cleanUp();
		tree = null;
		model = null;
		project = null;
		documentFrame = null;
	}

	
	public void zoomIn(){
		tree.zoomIn();
	}
	public void zoomOut(){
		tree.zoomOut();
	}
	public boolean canZoomIn() {
		return tree.canZoomIn();
	}
	public boolean canZoomOut() {
		return tree.canZoomOut();
	}
	public int getScale() {
		return tree.getZoom();
	}
	
	public UndoController getUndoController() {
		if (showsTasks())
			return project.getUndoController();
		else 
			return project.getResourcePool().getUndoController();
	}
	
	public SpreadSheet getSpreadSheet() {
		return null;
	}
	public boolean hasNormalMinWidth() {
		return true;
	}
	public String getViewName() {
		return viewName;
	}
	public boolean showsTasks() {
		return viewName == MenuActionConstants.ACTION_WBS;
	}
	public boolean showsResources() {
		return viewName == MenuActionConstants.ACTION_RBS;
	}
	public void onActivate(boolean activate) {
	}
	public boolean isPrintable() {
		return true;
	}
	public void restoreWorkspace(WorkspaceSetting w, int context) {
		Workspace ws = (Workspace) w;
		tree.restoreWorkspace(ws.network, context);
	}
	public WorkspaceSetting createWorkspace(int context) {
		Workspace ws = new Workspace();
		ws.network = tree.createWorkspace(context);
		return ws;
	}

	public static class Workspace implements WorkspaceSetting { 
		private static final long serialVersionUID = 7828075902711289247L;
		WorkspaceSetting network;

		public WorkspaceSetting getNetwork() {
			return network;
		}

		public void setNetwork(WorkspaceSetting network) {
			this.network = network;
		}
	}

	public boolean canScrollToTask() {
		return false;
	}
	public void scrollToTask() {
	}
	
	public NodeModelCache getCache(){
		return cache;
	}

	private GraphicNode getSelectedGraphicNode() {
		if (tree == null || tree.getUI() == null) {
			return null;
		}
		GraphInteractor interactor = tree.getUI().getInteractor();
		if (interactor == null) {
			return null;
		}
		Object selected = interactor.getSelectedObject();
		return (selected instanceof GraphicNode) ? (GraphicNode) selected : null;
	}

	public List getSelectedNodes() {
		GraphicNode selectedNode = getSelectedGraphicNode();
		if (selectedNode == null || selectedNode.getNode() == null) {
			return null;
		}
		ArrayList nodes = new ArrayList(1);
		nodes.add(selectedNode.getNode());
		return nodes;
	}

	public Object getSelectedImpl() {
		GraphicNode selectedNode = getSelectedGraphicNode();
		return (selectedNode == null || selectedNode.getNode() == null) ? null : selectedNode.getNode().getImpl();
	}
	

}

