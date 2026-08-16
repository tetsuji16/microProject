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
package com.microproject.dialog.assignment;

import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.Iterator;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import org.apache.commons.collections.Transformer;

import com.microproject.pm.graphic.model.cache.NodeModelCache;
import com.microproject.pm.graphic.model.cache.NodeModelCacheFactory;
import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetModel;
import com.microproject.pm.graphic.spreadsheet.editor.RateEditor;
import com.microproject.pm.graphic.spreadsheet.renderer.RateRenderer;
import com.microproject.configuration.Dictionary;
import com.microproject.datatype.Rate;
import com.microproject.field.Field;
import com.microproject.graphic.configuration.SpreadSheetFieldArray;
import com.microproject.graphic.configuration.shape.Colors;
import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.NodeList;
import com.microproject.grouping.core.event.HierarchyEvent;
import com.microproject.grouping.core.event.HierarchyListener;
import com.microproject.grouping.core.model.NodeModel;
import com.microproject.grouping.core.model.NodeModelFactory;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.assignment.AssignmentEntry;
import com.microproject.pm.assignment.HasAssignments;
import com.microproject.pm.resource.Resource;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.strings.Messages;

/**
 *
 */
@SuppressWarnings("unchecked")
public class AssignmentEntryPane extends JScrollPane implements HierarchyListener {
	private static final long serialVersionUID = 1L;
	CommonAssignmentDialog dialog;
	AssignmentSpreadSheet spreadSheet;
	NodeModel assignmentModel;
	Project project;
	ResourceAssigner resourceAssigner;
	List<NormalTask> taskList = new ArrayList<>(); // empty selection to start
	public static final String spreadsheetCategory="assignmentEntrySpreadsheet";
	protected NodeModelCache cache;
	private boolean replace;
	
	private static final int REQUEST_DEMAND_TYPE_COLUMN = 0; // hidden now
	private static final int UNITS_COLUMN = REQUEST_DEMAND_TYPE_COLUMN+1;
	
	class AssignmentSpreadSheet extends SpreadSheet {
		private static final long serialVersionUID = 1L;
		ResourceAssigner resourceAssigner;
		/**
		 * @param resourceAssigner
		 */
		public AssignmentSpreadSheet(ResourceAssigner resourceAssigner) {
			this.resourceAssigner = resourceAssigner;
	    	setCanModifyColumns(false);
	    	setCanSelectFieldArray(false);

		}

		private AssignmentEntry getEntryInRow(int row) {
			Node node = ((SpreadSheetModel)getModel()).getNode(row).getNode();
			if (node != null && !node.isVirtual()) 
				return (AssignmentEntry)node.getImpl();
			else
				return null;
		}
			
		public void setValueAt(Object aValue, int row, int column) {
			AssignmentEntry entry = getEntryInRow(row);
			if (entry == null)
				return;
			
			if (!entry.isAssigned()) { // assign it first, then set value
				if (resourceAssigner != null) {
					Field field = ((SpreadSheetModel)getModel()).getFieldInColumn(column+1);
					double units = 1.0;
					if (field == AssignmentEntry.getRateField()) {
						units = ((Rate)aValue).getValue();
					}
					resourceAssigner.assign((Resource)entry.getResource(), units);
				}
			}
			super.setValueAt(aValue,row,column);
		}
        
/**
 * Gets selected resources on spreadsheet.
 * @param assignedOnly - if true, only selected resources are returned
 * @return
 */	 	List<Resource> getSelectedResources(boolean assignedOnly){
	 		List<?> list = NodeList.nodeListToImplList(getSelectedNodes());
	 		ArrayList<Resource> resourceList = new ArrayList<>();
			Iterator<?> i = list.iterator();
			AssignmentEntry entry;
			while (i.hasNext()) {
				entry = (AssignmentEntry)i.next();
				if (!assignedOnly || entry.isAssigned()) // see if should add.
					resourceList.add((Resource) entry.getResource());
			}
			return resourceList;
	 	}
 		public TableCellEditor getCellEditor(int row, int column) {
 			TableCellEditor editor = null;
			AssignmentEntry entry = getEntryInRow(row);
			
			if (entry != null) {
				Field field = ((SpreadSheetModel)getModel()).getFieldInColumn(column+1);
				if (field == AssignmentEntry.getRateField()) {
					if (entry.getTimeUnitLabel() != null) {
						boolean labor = ((AssignmentEntry)entry).getResource().isLabor();
						editor = new RateEditor(entry.getTimeUnitLabel(),field.isMoney(),labor && field.isPercent(),labor);
					}
				}
			}
			if (editor == null)
				editor =  super.getCellEditor(row, column);
			return editor;
		}
 		
		public TableCellRenderer getCellRenderer(int row, int column) {
			TableCellRenderer renderer = null;
			AssignmentEntry entry = getEntryInRow(row);
			
			if (entry != null) {
				Field field = ((SpreadSheetModel)getModel()).getFieldInColumn(column+1);
				if (field == AssignmentEntry.getRateField()) {
					if (entry.getTimeUnitLabel() != null) {
						renderer = new RateRenderer();
					}
				}
			}
			if (renderer == null)
				renderer =  super.getCellRenderer(row, column);
			return renderer;
		}

 	
	 	int getSelectedCount() {
	 		return NodeList.nodeListToImplList(getSelectedNodes()).size(); // doesn't count void nodes
	 	}
		
		public Component prepareRenderer(TableCellRenderer renderer, int row,
				int column) {
			Component component =  super.prepareRenderer(renderer, row, column);
			AssignmentEntry entry = getEntryInRow(row);
			component.setForeground(Colors.BLACK);
			
			if (entry != null) {
				if (entry.isAssigned()) {
					if (taskList.size() == entry.getAssignmentCount()) { // if all selected tasks are assigned to this resource, show it green
							component.setBackground(Colors.PALE_GREEN);						
					} else {
							component.setBackground(Colors.PALE_YELLOW);
						if (column!=0)  {
							Field field = ((SpreadSheetModel)getModel()).getFieldInColumn(column);
							if (field == Assignment.getRequestDemandTypeField() || field == AssignmentEntry.getRateField()) {
								((JLabel)component).setText(Field.MULTIPLE_VALUES);
							}
						}
					}	
				}
			}
			return component;
		}
		
		
        public Component prepareEditor(TableCellEditor editor, int row,
                int column) {
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
        
        public void doDoubleClick(int row, int col) {
        	if (dialog instanceof AssignmentDialog) {
        		((AssignmentDialog)dialog).assign();
        		((AssignmentDialog)dialog).setVisible(false);
        	} else if (dialog instanceof ReplaceAssignmentDialog)
        		((ReplaceAssignmentDialog)dialog).onOk();
        }
}
	public class NodeFactoryTransformer implements Transformer{
		public Object transform(Object impl) {
		    if (impl instanceof HasAssignments){
		        HasAssignments hasAssignments = (HasAssignments) impl;
		        return new AssignmentEntry(hasAssignments, null,project);
		    }
		    return null;
		}
    }

	protected Consumer<Object> transformerClosure;
	
	public AssignmentEntryPane(CommonAssignmentDialog dialog, Project project, ResourceAssigner resourceAssigner, boolean replace,Consumer<Object> transformerClosure) {
		super();
		this.replace = replace;
		this.resourceAssigner = resourceAssigner;
		this.dialog=dialog;
		this.transformerClosure=transformerClosure;
	}
	
	private SpreadSheetFieldArray getFields() {
		return (SpreadSheetFieldArray) Dictionary.get(spreadsheetCategory,Messages.getString(
				replace ? "Spreadsheet.AssignmentEntry.replaceResources"
						: "Spreadsheet.AssignmentEntry.assignResources"));
	}
	public void init() {
		if (project == null)
			return;
		ResourcePool pool = project.getResourcePool();
		
			pool.getResourceOutline().getHierarchy().removeHierarchyListener(this);
			pool.getResourceOutline().getHierarchy().addHierarchyListener(this);
	
			assignmentModel = NodeModelFactory.getInstance().replicate(pool.getResourceOutline(),new NodeFactoryTransformer());
			assignmentModel.getHierarchy().setNbEndVoidNodes(0); // don't allow blank lines
		if (spreadSheet==null){
			spreadSheet = new AssignmentSpreadSheet(resourceAssigner);
			spreadSheet.setSpreadSheetCategory(spreadsheetCategory);  // for columns.  Must do first
			spreadSheet.setActions(new String[]{});

		}
		
		
		cache=NodeModelCacheFactory.getInstance().createDefaultCache(assignmentModel,pool,NodeModelCache.ASSIGNMENT_TYPE,"AssignmentEntry",transformerClosure);
		SpreadSheetFieldArray fields=getFields();
		spreadSheet.setCache(cache,fields,fields.getCellStyle(),fields.getActionList());
		
		if (!replace) { // Keep this width aligned with the spreadsheet definition until it becomes data-driven.
			spreadSheet.getColumnModel().getColumn(UNITS_COLUMN).setPreferredWidth(50);
		}

		JViewport viewport = createViewport();
		viewport.setView(spreadSheet);
		setViewport(viewport);
		
		setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		
		Dimension d=spreadSheet.getPreferredSize();
		Dimension enclosing=new Dimension();

		enclosing.setSize(d.getWidth()/*+rowHeaderWidth*/,d.getHeight());
		viewport.setPreferredSize(enclosing);
		
		updateTable();
	}
	
	/**
	 * @param project The project to set.
	 */
	public void setProject(Project project) {
		this.project = project;
		init();
	}
	
	/**
	 * Functor to call setAssignmentsFromTaskList
 */	private class AssignmentEntrySetter implements Consumer<Object> {
		List<NormalTask> taskList;
		AssignmentEntrySetter(List<NormalTask> taskList) {
			this.taskList = taskList;
		}
		

		public void accept(Object arg0) {
			AssignmentEntry entry = (AssignmentEntry)(((Node)arg0).getImpl());
			entry.setAssignmentsFromTaskList(taskList);
		}
		
	}

	void setSelectedTasks(List<NormalTask> taskList) {
  		this.taskList = taskList;
		updateTable();
		boolean enabled = !taskList.isEmpty();
		spreadSheet.setEnabled(enabled);
		spreadSheet.getRowHeader().setEnabled(enabled);

	}
 	List<Resource> getSelectedResources(boolean assignedOnly){
  		return spreadSheet.getSelectedResources(assignedOnly);
  	}

 	
 	int getSelectedCount(){
 		return spreadSheet.getSelectedCount();
 	}

 	void updateTable() {
 		assignmentModel.getHierarchy().visitAll(new AssignmentEntrySetter(taskList));
 		((SpreadSheetModel)spreadSheet.getModel()).getCache().update();
 		((SpreadSheetModel)spreadSheet.getModel()).fireTableDataChanged(); // redraw it
 	}
 	
 	
 	
 	
    public AssignmentSpreadSheet getSpreadSheet() {
        return spreadSheet;
    }

    

	public void nodesChanged(HierarchyEvent e) {
	}

	public void nodesInserted(HierarchyEvent e) {
		init();
	}

	public void nodesRemoved(HierarchyEvent e) {
		init();
	}

	public void structureChanged(HierarchyEvent e) {
	}
}

