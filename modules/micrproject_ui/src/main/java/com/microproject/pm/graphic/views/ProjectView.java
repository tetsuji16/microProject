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

import javax.swing.JScrollPane;
import javax.swing.JViewport;

import com.microproject.help.HelpUtil;
import com.microproject.menu.MenuActionConstants;
import com.microproject.pm.graphic.frames.GraphicManager;
import com.microproject.pm.graphic.model.cache.NodeModelCache;
import com.microproject.pm.graphic.model.cache.NodeModelCacheFactory;
import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.document.Document;
import com.microproject.document.ObjectEvent;
import com.microproject.grouping.core.model.NodeModel;
import com.microproject.pm.task.Project;
import com.microproject.undo.UndoController;
import com.microproject.workspace.WorkspaceSetting;
/**
 * Resource view with spreadsheet
 */
public class ProjectView extends JScrollPane implements BaseView, ObjectEvent.Listener {
	private static final long serialVersionUID = -4440711646626221865L;
	private static final String spreadsheetCategory=projectSpreadsheetCategory;
	protected SpreadSheet spreadSheet;
	protected NodeModel model;
	protected NodeModelCache cache;
	Document document;
	/**
	 * 
	 */
	public ProjectView(NodeModel model, Document document) {
		super();
		this.model = model;
		this.document =document;
		HelpUtil.addDocHelp(this,"Projects_View");
		createSpreadsheet(model);
		GraphicManager.getInstance(this).getProjectFactory().getPortfolio().addObjectListener(this);		
	}
	
	public void cleanUp() {
		GraphicManager.getInstance(this).getProjectFactory().getPortfolio().removeObjectListener(this);		
		SpreadsheetViewSupport.cleanup(spreadSheet);
		spreadSheet = null;
		model = null;
		cache = null;
		document = null;
		
	}

	public void createSpreadsheet(NodeModel model){
        spreadSheet = new SpreadSheet();
		spreadSheet.setSpreadSheetCategory(spreadsheetCategory); // for columns - must do first
		
		cache=NodeModelCacheFactory.getInstance().createDefaultCache(model,document,NodeModelCache.PROJECT_TYPE,getViewName(),null);
		com.microproject.graphic.configuration.SpreadSheetFieldArray fields = SpreadsheetViewSupport.getProjectFields();
		spreadSheet.setCache(cache,fields,fields.getCellStyle(),fields.getActionList());

		JViewport viewport = createViewport();
		viewport.setView(spreadSheet);
		setViewport(viewport);
		
		cache.update(); //this is not required by certain views 

	}

	/**
	 * @return Returns the spreadSheet.
	 */
	public SpreadSheet getSpreadSheet() {
		return spreadSheet;
	}

	public void objectChanged(ObjectEvent objectEvent) {
		if (objectEvent.getObject() instanceof Project) {
			if (objectEvent.isCreate() /*|| objectEvent.isDelete()*/) {
				if (model == null) {
					return; // in the process of being cleaned up
				} else {
					cache.update();
					spreadSheet.invalidate();
				}
			}
		}
	}

	public UndoController getUndoController() {
		return null;
	}

	public void zoomIn() {
	}

	public void zoomOut() {
	}
	public boolean canZoomIn() {
		return false;
	}
	public boolean canZoomOut() {
		return false;
	}
	public int getScale() {
		return -1;
	}
	public boolean hasNormalMinWidth() {
		return true;
	}
	public String getViewName() {
		return MenuActionConstants.ACTION_PROJECTS;
	}
	public boolean showsTasks() {
		return false;
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
		spreadSheet.restoreWorkspace(ws.spreadSheet, context);
	}
	public WorkspaceSetting createWorkspace(int context) {
		Workspace ws = new Workspace();
		ws.spreadSheet = spreadSheet.createWorkspace(context);
		return ws;
	}

	public static class Workspace implements WorkspaceSetting { 
		private static final long serialVersionUID = -1801198970620970719L;
		WorkspaceSetting spreadSheet;
		public WorkspaceSetting getSpreadSheet() {
			return spreadSheet;
		}
		public void setSpreadSheet(WorkspaceSetting spreadSheet) {
			this.spreadSheet = spreadSheet;
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
	

}

