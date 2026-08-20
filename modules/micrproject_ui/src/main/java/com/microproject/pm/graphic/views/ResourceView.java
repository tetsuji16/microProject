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

import java.awt.Component;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.table.TableCellRenderer;

import com.microproject.help.HelpUtil;
import com.microproject.menu.MenuActionConstants;
import com.microproject.pm.graphic.model.cache.NodeModelCache;
import com.microproject.pm.graphic.model.cache.NodeModelCacheFactory;
import com.microproject.pm.graphic.model.cache.ReferenceNodeModelCache;
import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetModel;
import com.microproject.document.Document;
import com.microproject.field.FieldContext;
import com.microproject.graphic.configuration.CellStyle;
import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.model.NodeModel;
import com.microproject.pm.resource.Resource;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.strings.Messages;
import com.microproject.undo.UndoController;
import com.microproject.util.Alert;
import com.microproject.util.Environment;
import com.microproject.workspace.WorkspaceSetting;

/**
 * Resource view with spreadsheet
 */
public class ResourceView extends JScrollPane implements BaseView {
	/**
	 * 
	 */
	private static final long serialVersionUID = 591334548533168582L;
	private static String resourceWarning(String suffixKey, Resource resource, boolean includeMoveHint) {
		String pattern = Messages.getString("ResourceView.YouCannotDeleteTheResource") + "{0}"
				+ Messages.getString(suffixKey);
		if (includeMoveHint)
			pattern += "\n" + Messages.getString("ResourceView.ToMoveAProtectedResource");
		return java.text.MessageFormat.format(pattern, resource.getName());
	}
	public static final String spreadsheetCategory=resourceSpreadsheetCategory;
	protected SpreadSheet spreadSheet;
	protected NodeModel model;
	protected NodeModelCache cache;
	Document document;
	FieldContext fieldContext;
	CellStyle cellStyle;
	boolean readOnly;
	/**
	 * @param master 
	 * 
	 */
	public ResourceView(ReferenceNodeModelCache cache,NodeModel model, Document document, boolean readOnly, boolean master) {
		super();
		HelpUtil.addDocHelp(this,"Resource_View");
		this.model = model;
		this.document =document;
		this.cache=NodeModelCacheFactory.getInstance().createFilteredCache((ReferenceNodeModelCache)cache,getViewName(),null);
		fieldContext = new FieldContext();
		fieldContext.setLeftAssociation(false);
		/*cellStyle=new CellStyle(){
			CellFormat cellProperties=new CellFormat();
			public CellFormat getCellProperties(GraphicNode node){
				cellProperties.setBold(node.isSummary());
				cellProperties.setItalic(node.isAssignment());
				//cellProperties.setBackground((node.isAssignment())?"NORMAL_LIGHT_YELLOW":"NORMAL_YELLOW");
				cellProperties.setCompositeIcon(node.isComposite());
				return cellProperties;
			}
			
		};*/
		createSpreadsheet();
		JViewport viewport = createViewport();
		viewport.setView(spreadSheet);
		setViewport(viewport);
		
		cache.update(); //this is not required by certain views 
		if (!master && !Environment.isProjectLibre()) {
			SwingUtilities.invokeLater(new Runnable(){
				public void run() {
					Alert.warnWithOnceOption(Messages.getString("Info.resourceView"),"warnedResourceView");
				}});
		}
	}

	public void cleanUp() {
		SpreadsheetViewSupport.cleanup(spreadSheet);
		spreadSheet = null;
		model = null;
		cache = null;
		document = null;
		fieldContext = null;
		cellStyle = null;
	}
	public void createSpreadsheet(){
        spreadSheet = new SpreadSheet() {

    		private Object getEntryInRow(int row) {
    			Node node = ((SpreadSheetModel)getModel()).getNode(row).getNode();
    			if (node != null && !node.isVirtual()) 
    				return node.getImpl();
    			else
    				return null;
    		}

        
    		public Component prepareRenderer(TableCellRenderer renderer, int row,
    				int column) {
    			Component component =  super.prepareRenderer(renderer, row, column);
//    			Object r = getEntryInRow(row);
//    			if (r instanceof ResourceImpl) {
//    				if (((ResourceImpl)r).isUser()) // make user resources have a special color
//    					component.setBackground(Colors.PALE_GREEN);
//    			}
    			//Done in cellstyle
    			return component;
    		}
    		
    	    public boolean isNodeDeletable(Node node) {
    	    	if (node != null && node.getImpl() instanceof Resource) {
    	    		Resource r = (Resource)node.getImpl();
    	    		if (r.isUser()) {
						Alert.warn(resourceWarning("ResourceView.UsersCanOnlyBeRemoved", r, false)); //$NON-NLS-1$
    	    			return false;
    	    		}
    	    		if (r.isAssignedToSomeProject()) {
						Alert.warn(resourceWarning("ResourceView.ThisResourceCurrentlyHasAssignments", r, false)); //$NON-NLS-1$
    	    			return false;
    	    		}
	    		List<Node> children=node.getChildren();
	    		if (children!=null)
	    		for (Iterator<Node> i=children.listIterator();i.hasNext();){
	    			Node child=i.next();
	    			if (!isNodeDeletable(child)) return false;
	    		}
    	    	}
    	    	return true;
    	    }
    	    public boolean isNodeCuttable(Node node) {
    	    	if (node != null && node.getImpl() instanceof Resource) {
    	    		Resource r = (Resource)node.getImpl();
    	    		if (r.isUser()) {
						Alert.warn(resourceWarning("ResourceView.UsersCanOnlyBeRemoved", r, true)); //$NON-NLS-1$
    	    			return false;
    	    		}
    	    		if (r.isAssignedToSomeProject()) {
						Alert.warn(resourceWarning("ResourceView.ThisResourceCurrentlyHasAssignments", r, true)); //$NON-NLS-1$
    	    			return false;
    	    		}
	    		List<Node> children=node.getChildren();
	    		if (children!=null)
	    		for (Iterator<Node> i=children.listIterator();i.hasNext();){
	    			Node child=i.next();
	    			if (!isNodeDeletable(child)) return false;
	    		}
    	    	}
    	    	return true;
    	    }

        	
        };
		spreadSheet.setSpreadSheetCategory(spreadsheetCategory); // for columns - must do first
		
		com.microproject.graphic.configuration.SpreadSheetFieldArray fields = SpreadsheetViewSupport.getResourceFields();
		if (((ResourcePool)document).isMaster()){
			fields=(com.microproject.graphic.configuration.SpreadSheetFieldArray)fields.clone();
			fields.removeField("Field.userRole"); //$NON-NLS-1$
		}
		spreadSheet.setCache(cache,fields,fields.getCellStyle(),fields.getActionList());
		((SpreadSheetModel)spreadSheet.getModel()).setFieldContext(fieldContext);
		spreadSheet.setReadOnly(readOnly);
	}

	/**
	 * @return Returns the spreadSheet.
	 */
	public SpreadSheet getSpreadSheet() {
		return spreadSheet;
	}

	public UndoController getUndoController() {
		return ((ResourcePool)document).getUndoController();
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
		return MenuActionConstants.ACTION_RESOURCES;
	}
	public boolean showsTasks() {
		return false;
	}
	public boolean showsResources() {
		return true;
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
		private static final long serialVersionUID = -1251204386431239291L;
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
