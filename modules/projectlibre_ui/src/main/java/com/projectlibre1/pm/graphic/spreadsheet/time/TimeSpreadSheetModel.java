/*******************************************************************************
 * The contents of this file are subject to the Common Public Attribution License 
 * Version 1.0 (the "License"); you may not use this file except in compliance with 
 * the License. You may obtain a copy of the License at 
 * http://www.projectlibre.com/license . The License is based on the Mozilla Public 
 * License Version 1.1 but Sections 14 and 15 have been added to cover use of 
 * software over a computer network and provide for limited attribution for the 
 * Original Developer. In addition, Exhibit A has been modified to be consistent 
 * with Exhibit B. 
 *
 * Software distributed under the License is distributed on an "AS IS" basis, 
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for the 
 * specific language governing rights and limitations under the License. The 
 * Original Code is ProjectLibre. The Original Developer is the Initial Developer 
 * and is ProjectLibre Inc. All portions of the code written by ProjectLibre are 
 * Copyright (c) 2012-2019. All Rights Reserved. All portions of the code written by 
 * ProjectLibre are Copyright (c) 2012-2019. All Rights Reserved. Contributor 
 * ProjectLibre, Inc.
 *
 * Alternatively, the contents of this file may be used under the terms of the 
 * ProjectLibre End-User License Agreement (the ProjectLibre License) in which case 
 * the provisions of the ProjectLibre License are applicable instead of those above. 
 * If you wish to allow use of your version of this file only under the terms of the 
 * ProjectLibre License and not to allow others to use your version of this file 
 * under the CPAL, indicate your decision by deleting the provisions above and 
 * replace them with the notice and other provisions required by the ProjectLibre 
 * License. If you do not delete the provisions above, a recipient may use your 
 * version of this file under either the CPAL or the ProjectLibre Licenses. 
 *
 *
 * [NOTE: The text of this Exhibit A may differ slightly from the text of the notices 
 * in the Source Code files of the Original Code. You should use the text of this 
 * Exhibit A rather than the text found in the Original Code Source Code for Your 
 * Modifications.] 
 *
 * EXHIBIT B. Attribution Information for ProjectLibre required
 *
 * Attribution Copyright Notice: Copyright (c) 2012-2019, ProjectLibre, Inc.
 * Attribution Phrase (not exceeding 10 words): 
 * ProjectLibre, open source project management software.
 * Attribution URL: http://www.projectlibre.com
 * Graphic Image as provided in the Covered Code as file: projectlibre-logo.png with 
 * alternatives listed on http://www.projectlibre.com/logo 
 *
 * Display of Attribution Information is required in Larger Works which are defined 
 * in the CPAL as a work which combines Covered Code or portions thereof with code 
 * not governed by the terms of the CPAL. However, in addition to the other notice 
 * obligations, all copies of the Covered Code in Executable and Source Code form 
 * distributed must, as a form of attribution of the original author, include on 
 * each user interface screen the "ProjectLibre" logo visible to all users. 
 * The ProjectLibre logo should be located horizontally aligned with the menu bar 
 * The 
 * logo must be at least 144 x 31 pixels. When users click on the "ProjectLibre" 
 * logo it must direct them back to http://www.projectlibre.com. 
 *******************************************************************************/
package com.projectlibre1.pm.graphic.spreadsheet.time;
import java.util.ArrayList;
import java.util.EventListener;

import javax.swing.event.EventListenerList;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.AbstractLayoutCache;
import javax.swing.tree.TreePath;

import com.projectlibre1.pm.graphic.model.cache.GraphicNode;
import com.projectlibre1.pm.graphic.model.cache.NodeModelCache;
import com.projectlibre1.pm.graphic.spreadsheet.common.CommonSpreadSheetModel;
import com.projectlibre1.pm.graphic.spreadsheet.SpreadSheetUtils;
import com.projectlibre1.pm.graphic.timescale.CoordinatesConverter;
import com.projectlibre1.field.Field;
import com.projectlibre1.field.FieldContext;
import com.projectlibre1.field.FieldParseException;
import com.projectlibre1.graphic.configuration.ActionList;
import com.projectlibre1.graphic.configuration.CellStyle;
import com.projectlibre1.grouping.core.Node;
import com.projectlibre1.grouping.core.model.NodeModel;
import com.projectlibre1.pm.time.HasStartAndEnd;
import com.projectlibre1.timescale.TimeScaleEvent;
import com.projectlibre1.timescale.TimeScaleListener;
import com.projectlibre1.util.Alert;
import org.netbeans.swing.outline.DefaultOutlineModel;
import org.netbeans.swing.outline.OutlineModel;
import org.netbeans.swing.outline.RowModel;
import org.netbeans.swing.outline.TreePathSupport;
/**
 *  
 */
public class TimeSpreadSheetModel extends CommonSpreadSheetModel implements TimeScaleListener, OutlineModel, RowModel {
	protected ArrayList<Field> selectedFieldArray;
	//timescale
	protected CoordinatesConverter coord;
	protected ArrayList<HasStartAndEnd> timeIntervals; // cache: data filled by 
	FieldContext fieldContext = new FieldContext(); // this is re-used like a renderer
	//TimeSpreadSheetModel with coord.getProjectTimeIterator()
	ArrayList<Field> fieldArray;
	private transient OutlineModel outlineDelegate;
	public ArrayList<Field> getFieldArray() {
		return fieldArray;
	}


	public TimeSpreadSheetModel(NodeModelCache cache, ArrayList<Field> fieldArray, CellStyle cellStyle, ActionList actionList) {
		super(cache,null,cellStyle,actionList);
		this.fieldArray = fieldArray;
		selectedFieldArray=new ArrayList<Field>();
		timeIntervals=new ArrayList<HasStartAndEnd>();
		resetSelectedFieldArray();
		//initCellStyle();
		setFieldContext(fieldContext);
	}

	@Override
	public void setCache(NodeModelCache cache) {
		super.setCache(cache);
		rebuildOutlineDelegate();
	}

	private void rebuildOutlineDelegate() {
		var currentCache = getCache();
		if (currentCache == null) {
			outlineDelegate = null;
			return;
		}
		outlineDelegate = DefaultOutlineModel.createOutlineModel(currentCache, this, false, "");
	}
	
	
//    public void graphicNodesCompositeEvent(CompositeCacheEvent compositeEvent){
//    	fireTableStructureChanged();
////        for (Iterator i=compositeEvent.getNodeEvents().iterator();i.hasNext();){
////            final CacheEvent e=(CacheEvent)i.next();
////            e.forIntervals(new Closure() {
////                public void execute(Object obj) {
////                    CacheInterval i = (CacheInterval) obj;
////                     if (e.getType()==CacheEvent.NODES_CHANGED)
////                        fireTableRowsUpdated(i.getStart(), i.getEnd());
////                    else if (e.getType()==CacheEvent.NODES_INSERTED)
////                        fireTableRowsInserted(i.getStart(), i.getEnd());
////                    else if (e.getType()==CacheEvent.NODES_REMOVED)
////                        fireTableRowsDeleted(i.getStart(), i.getEnd());
////                }
////            });
////        }
//    }

	
	public void resetSelectedFieldArray(){
		int oldSize=selectedFieldArray.size();
		selectedFieldArray.clear();
		if (fieldArray.size()>0) selectedFieldArray.add(fieldArray.get(0));
		//only size is used
		if (oldSize!=selectedFieldArray.size()){
			fireFieldArrayChanged(this);
			fireUpdateAll();
		}
	}

	public void setFieldArray(ArrayList<Field> fieldArray) {
		this.fieldArray = fieldArray;
		resetSelectedFieldArray();
	}
	public ArrayList<Field> getSelectedFieldArray() {
		return selectedFieldArray;
	}
	public void setSelectedFieldArray(ArrayList<Field> selectedFieldArray) {
		int oldSize=this.selectedFieldArray.size();
		this.selectedFieldArray = selectedFieldArray;
		//only size is used
		if (oldSize!=selectedFieldArray.size()){
			fireFieldArrayChanged(this);
			fireUpdateAll();
		}
	}
	public void selectFieldArray(Field field){
		int oldSize=selectedFieldArray.size();
		if (selectedFieldArray.contains(field)){
				selectedFieldArray.remove(field);
				if (selectedFieldArray.size()==0) resetSelectedFieldArray();
		}else{
			selectedFieldArray.add(selectedFieldArray.size(),field);
		}
		//only size is used
		if (oldSize!=selectedFieldArray.size()){
			fireFieldArrayChanged(this);
			fireUpdateAll();
		}
	}
	
	public boolean isComposite(GraphicNode node){
		return node.isComposite();
	}
	
	public int getRowMultiple() {
		return selectedFieldArray.size();
	}
	

	void resetTimeIntervals(){
		timeIntervals.clear();
	}
	void addTimeInterval(HasStartAndEnd interval){
		timeIntervals.add(interval);
	}
	
	
	public Object getValueAt(int row, int col) {
		if (col==0) return getFieldInRow(row).toString();
//		Node node=getNodeInRow(row/getRowMultiple());
		Node node=getNodeInRow(row);
		if (node.isVoid()) return null;
		getFieldContext().setInterval((HasStartAndEnd)timeIntervals.get(col-1));

		 return getFieldInRow(row).getValue(node, getCache().getWalkersModel(), fieldContext);
	}
	public void setValueAt(Object value, int row, int col) {
		Object oldValue=getValueAt(row,col);
		if (oldValue==null&&(value==null||"".equals(value))) return;

		if (col == 0)
			return;
		try {
			Node rowNode = getNodeInRow(row);

			if (rowNode.isVirtual()) return;
			getFieldContext().setInterval((HasStartAndEnd)timeIntervals.get(col-1));

			getCache().getModel().setFieldValue(getFieldInRow(row),rowNode, this, value, fieldContext,NodeModel.NORMAL);
			fireTableCellUpdated(row, col);
		} catch (FieldParseException e) {
			Alert.error(e.getMessage());
		}

	}
	
	
	public Field getFieldInRow(int row){
		if (selectedFieldArray.size()==0) return null;
		return selectedFieldArray.get(row%getRowMultiple());
	}
	
	
	public Class getColumnClass(int col) { //for header only
		if (col==0) return String.class;
		return null;
	}
	public Class getRowClass(int row) {
		Field field=getFieldInRow(row);
		if (field==null) return String.class;
		else return field.getDisplayType(); 
	}
	
	
	protected int columnCount=1;
	void incrementColumnCount(){columnCount++;}
	void decrementColumnCount(){columnCount--;}
	public int getColumnCount() {
		return columnCount;//((coord==null)?0:coord.countProjectIntervals())+1;
	}

	@Override
	public Field getFieldInColumn(int col) {
		if (col == 0) {
			return null;
		}
		return SpreadSheetUtils.getFieldInColumn(col, colModel);
	}
	


	
	public boolean isCellEditable(int row, int col) {
		if (col==0) return false;
		Node node=getNodeInRow(row);
		if (node.isVoid()) return true;

		getFieldContext().setInterval((HasStartAndEnd)timeIntervals.get(col-1));

		return ! getFieldInRow(row).isReadOnly(node, getCache().getWalkersModel(),fieldContext);
	}

	@Override
	public Object getValueFor(Object node, int column) {
		if (!(node instanceof GraphicNode graphicNode)) {
			return null;
		}
		int row = cache.getRowAt(graphicNode);
		return row >= 0 ? getValueAt(row, column) : null;
	}

	@Override
	public void setValueFor(Object node, int column, Object value) {
		if (!(node instanceof GraphicNode graphicNode)) {
			return;
		}
		int row = cache.getRowAt(graphicNode);
		if (row >= 0) {
			setValueAt(value, row, column);
		}
	}

	@Override
	public boolean isCellEditable(Object node, int column) {
		if (!(node instanceof GraphicNode graphicNode)) {
			return false;
		}
		int row = cache.getRowAt(graphicNode);
		return row >= 0 && isCellEditable(row, column);
	}

	@Override
	public Object getRoot() {
		return getCache().getRoot();
	}

	@Override
	public Object getChild(Object parent, int index) {
		return getCache().getChild(parent, index);
	}

	@Override
	public int getChildCount(Object parent) {
		return getCache().getChildCount(parent);
	}

	@Override
	public boolean isLeaf(Object node) {
		return getCache().isLeaf(node);
	}

	@Override
	public int getIndexOfChild(Object parent, Object child) {
		return getCache().getIndexOfChild(parent, child);
	}

	@Override
	public void valueForPathChanged(TreePath path, Object newValue) {
		// Tree edits are handled through spreadsheet editing and the cache model.
	}

	@Override
	public void addTreeModelListener(TreeModelListener l) {
		getCache().addTreeModelListener(l);
	}

	@Override
	public void removeTreeModelListener(TreeModelListener l) {
		getCache().removeTreeModelListener(l);
	}

	@Override
	public TreePathSupport getTreePathSupport() {
		ensureOutlineDelegate();
		return outlineDelegate.getTreePathSupport();
	}

	@Override
	public boolean isLargeModel() {
		return false;
	}

	@Override
	public AbstractLayoutCache getLayout() {
		ensureOutlineDelegate();
		return outlineDelegate.getLayout();
	}

	private void ensureOutlineDelegate() {
		if (outlineDelegate == null && getCache() != null) {
			outlineDelegate = DefaultOutlineModel.createOutlineModel(getCache(), this, false, "");
		}
	}
	
	
	
	
//	timescale	
    public CoordinatesConverter getCoord() {
        return coord;
    }
    public void setCoord(CoordinatesConverter coord) {
        if (this.coord!=null) this.coord.removeTimeScaleListener(this);
        this.coord = coord;
		coord.addTimeScaleListener(this);
    }
	public void timeScaleChanged(TimeScaleEvent e) {
	}

	
	
	
 	//selectedFieldArray event handling
	
 	protected EventListenerList listenerList = new EventListenerList();

 	public void addFieldArrayListener(FieldArrayListener l) {
 		listenerList.add(FieldArrayListener.class, l);
 	}
 	public void removeFieldArrayListener(FieldArrayListener l) { 
 		listenerList.remove(FieldArrayListener.class, l);
 	}
 	public FieldArrayListener[] getFieldArrayListeners() {
 		return listenerList.getListeners(FieldArrayListener.class);
 	}
 	
 	protected void fireFieldArrayChanged(Object source) {
 		Object[] listeners = listenerList.getListenerList();
 		FieldArrayEvent e = null;
 		for (int i = listeners.length - 2; i >= 0; i -= 2) {
 			if (listeners[i] == FieldArrayListener.class) {
 				if (e == null) {
 					e = new FieldArrayEvent(source,getSelectedFieldArray());
 				}
 				((FieldArrayListener) listeners[i + 1]).fieldArrayChanged(e);
 			}
 		}
 	}
     public EventListener[] getFieldArrayListeners(Class listenerType) { 
     	return listenerList.getListeners(listenerType); 
      }

	
	

}

