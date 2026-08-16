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
package com.microproject.pm.graphic.spreadsheet.time;
import java.util.ArrayList;
import java.util.EventListener;

import javax.swing.event.EventListenerList;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.AbstractLayoutCache;
import javax.swing.tree.TreePath;

import com.microproject.pm.graphic.model.cache.GraphicNode;
import com.microproject.pm.graphic.model.cache.NodeModelCache;
import com.microproject.pm.graphic.spreadsheet.common.CommonSpreadSheetModel;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetUtils;
import com.microproject.pm.graphic.timescale.CoordinatesConverter;
import com.microproject.field.Field;
import com.microproject.field.FieldContext;
import com.microproject.field.FieldParseException;
import com.microproject.graphic.configuration.ActionList;
import com.microproject.graphic.configuration.CellStyle;
import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.model.NodeModel;
import com.microproject.pm.time.HasStartAndEnd;
import com.microproject.timescale.TimeScaleEvent;
import com.microproject.timescale.TimeScaleListener;
import com.microproject.util.Alert;
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
////            e.forIntervals(new Consumer<Object>() {
////                public void accept(Object obj) {
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

