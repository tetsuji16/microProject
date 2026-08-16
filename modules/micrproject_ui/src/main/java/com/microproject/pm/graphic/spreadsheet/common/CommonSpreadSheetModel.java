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
package com.microproject.pm.graphic.spreadsheet.common;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.table.AbstractTableModel;


import com.microproject.pm.graphic.model.cache.CacheInterval;
import com.microproject.pm.graphic.model.cache.GraphicNode;
import com.microproject.pm.graphic.model.cache.NodeModelCache;
import com.microproject.pm.graphic.model.event.CacheEvent;
import com.microproject.pm.graphic.model.event.CacheListener;
import com.microproject.pm.graphic.model.event.CompositeCacheEvent;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetColumnModel;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetUtils;
import com.microproject.field.Field;
import com.microproject.field.FieldContext;
import com.microproject.graphic.configuration.ActionList;
import com.microproject.graphic.configuration.CellFormat;
import com.microproject.graphic.configuration.CellStyle;
import com.microproject.grouping.core.Node;
import com.microproject.pm.assignment.Assignment;
/**
 *  
 */
@SuppressWarnings("unchecked")
public abstract class CommonSpreadSheetModel extends AbstractTableModel implements CacheListener/*implements ObjectEvent.Listener*/ {
	protected NodeModelCache cache = null;
	protected FieldContext fieldContext = null; // only used if a field context is set
	protected CellStyle cellStyle;
	protected ActionList actionList;
	protected SpreadSheetColumnModel colModel;

	
	/**
	 *  
	 */
	public CommonSpreadSheetModel(NodeModelCache cache,SpreadSheetColumnModel colModel, CellStyle cellStyle, ActionList actionList) {
		super();
//		this.fieldArray = fieldArray;
		this.colModel = colModel;
		this.cellStyle=cellStyle;
		this.actionList=actionList;
		setCache(cache);
	}
	
	protected CacheListener cacheListener=null;
	public void setCache(NodeModelCache cache){
		if (cache==this.cache) return;
		if (this.cache!=null) 
			this.cache.removeNodeModelListener(this);
		this.cache = cache;
		cache.addNodeModelListener(this);
		fireTableDataChanged();

	}
    public void graphicNodesCompositeEvent(CompositeCacheEvent compositeEvent){
        for (Iterator<?> iterator = compositeEvent.getNodeEvents().iterator(); iterator.hasNext();){
            CacheEvent event = (CacheEvent) iterator.next();
            ArrayList<CacheInterval> intervals = new ArrayList<>();
            event.forIntervals(obj -> intervals.add((CacheInterval) obj));
            fireIntervalEvents(event.getType(), intervals);
        }
    }

    private void fireIntervalEvents(int type, List<CacheInterval> intervals) {
        if (intervals == null || intervals.isEmpty()) {
            return;
        }
        intervals.sort(Comparator.comparingInt(CacheInterval::getStart).thenComparingInt(CacheInterval::getEnd));

        int start = -1;
        int end = -1;
        for (CacheInterval interval : intervals) {
            if (start == -1) {
                start = interval.getStart();
                end = interval.getEnd();
                continue;
            }
            if (interval.getStart() <= end + 1) {
                end = Math.max(end, interval.getEnd());
                continue;
            }
            fireIntervalEvent(type, start, end);
            start = interval.getStart();
            end = interval.getEnd();
        }
        fireIntervalEvent(type, start, end);
    }

    private void fireIntervalEvent(int type, int start, int end) {
        switch (type) {
            case CacheEvent.NODES_CHANGED -> fireTableRowsUpdated(start, end);
            case CacheEvent.NODES_INSERTED -> fireTableRowsInserted(start, end);
            case CacheEvent.NODES_REMOVED -> fireTableRowsDeleted(start, end);
            default -> {
            }
        }
    }
	
//	public CommonSpreadSheetModel(NodeModel model, ArrayList fieldArray,CellStyle cellStyle,String viewName) {
//		this(NodeModelCacheFactory.getInstance().createDefaultCache(model,viewName),fieldArray,cellStyle);
//	}
	public ArrayList<Field> getFieldArray() {
		return colModel.getFieldArray();
	}
	public void setFieldArray(ArrayList<Field> fieldArray) {
		colModel.setFieldArray(fieldArray);
	}

	public NodeModelCache getCache() {
		return cache;
	}

	/*public CellStyle getCellStyle() {
		return cellStyle;
	}
	public void setCellStyle(CellStyle cellStyle) {
		this.cellStyle = cellStyle;
	}*/
	
	public abstract Field getFieldInColumn(int col);

	public String getColumnName(int col) {
		return ""+col;
	}

	
	public int getRowMultiple(){ //for TimeSpreadSheet
		return 1;
	}
	
	//real rows
	public int getRowCount() {
		return getCache().getSize()*getRowMultiple();
	}
	
	public GraphicNode getNode(int row) {
		return getNodeFromCacheRow(row);
	}

	public void changeCollapsedState(int row) {
		getCache().changeCollapsedState((GraphicNode)getCache().getElementAt(row));
	}
	
	
	
	public CellFormat getCellProperties(GraphicNode node){
		return cellStyle.getCellFormat(node);
	}
	
	private String[] actions=null;
	public String[] getActionList(){
		if (actions==null){
			actions=convertActions(actionList.getList(getCache().getModel()));
		}
		return actions;
	}
	public void clearActions(){
		actions=null;
	}
	public static String[] convertActions(String actionList){
		if (actionList==null) return null;
		StringTokenizer st=new StringTokenizer(actionList,",;:|");
		String[] actions=new String[st.countTokens()];
		for (int i=0;i<actions.length;i++) actions[i]=st.nextToken();
		return actions;
	}
	
	
	protected Node getNodeInRow(int row) {
		return SpreadSheetUtils.getNodeInRow(row,getRowMultiple(),cache);
//		GraphicNode gnode = getNodeFromCacheRow(row);
//		if (gnode == null)
//			return null;
//		return gnode.getNode();
		
	}
	public LinkedList getPreviousVisibleNodesFromRow(int row) {
		LinkedList siblings=null;
		for (int r=row-1;r>=0;r--){
			Node node=getNodeInRow(r);
			if (node.getImpl() instanceof Assignment) continue;
			if (siblings==null) siblings=new LinkedList();
			siblings.addFirst(node);
			if (!node.isVoid()) return siblings;
		}
		return null; //no need to move nodes in this case since they are children of root
	}
	protected Node getNextNonVoidSiblingFromRow(int row) {
		int rowCount=getRowCount();
		Node ref=getNodeInRow(row);
		Object parent=ref.getParent();
		for (int r=row+1;r<rowCount;r++){
			Node node=getNodeInRow(r);
			if (node.getImpl() instanceof Assignment) continue;
			if (node.getParent()!=parent) break;
			if (!node.isVoid()) return node;
		}
		return null;
	}

	private GraphicNode getNodeFromCacheRow(int row) {
		return SpreadSheetUtils.getNodeFromCacheRow(row,getRowMultiple(),cache);
		//return (GraphicNode) getCache().getElementAt(row/getRowMultiple());
	}
	

	protected int findNodeRow(Node node, int searchEnd) { // limit endpoint because parents are always above 
		for (int i = 0; i < searchEnd; i++) {
			if (getNodeInRow(i) == node)
				return i;
		}
		return -1;
	}

	public int findGraphicNodeRow(Object node) {
		int row=getCache().getRowAt(node);
		if (row==-1) return -1;
		return row*getRowMultiple();
	}
	
	public Object getObjectInRow(int row) {
		if (row == -1)
			return null;
		GraphicNode gnode = getNodeFromCacheRow(row);
		if (gnode == null)
			return null;
		return gnode.getNode().getImpl();
	
	}
	int findObjectRow(Object object) {
		for (int i = 0; i < getRowCount(); i++) {
			if (getObjectInRow(i) == object)
				return i;
		}
		return -1;
	}

	
	
	
	
	
	public abstract int getColumnCount();
	public abstract Object getValueAt(int rowIndex, int columnIndex);
	
	/**
	 * @see javax.swing.table.TableModel#getColumnClass(int)
	 */
	public Class getColumnClass(int col) {
		//if (col==0) return String.class;
		return getFieldInColumn(col).getDisplayType(); 
	}
	
	
	public void fireUpdateAll(){
		fireTableDataChanged();
//		fireUpdate(NULL_ROW,NULL_COL);
	}
//	
//	private static final int NULL_ROW = -1;
//	private static final int NULL_COL = 0;
//	
//	/**
//	 * Update a single cell, a column, a row, or everything
//	 * @param row
//	 * @param col
//	 */
//	private void fireUpdate(int row, int col) { //cache row
//		if (row == NULL_ROW) {
//			if (col == NULL_COL) {
//				fireTableDataChanged(); // everything changed
//			}
//		}
//	}
//
//
//
//	public void objectChanged(ObjectEvent objectEvent) {
//		Object object= objectEvent.getObject();
//		if (objectEvent.getSource() != this) { // if this was the source, then the event has alrady been fired for the cell
//			if (object == null) {
//				fireUpdate(NULL_ROW,NULL_COL) ;
//				return;
//			}
//			int row = findObjectRow(objectEvent.getObject());
//			fireUpdate(row,NULL_COL);
//		}
//		Node node = objectToNode(objectEvent.getObject()); // find the node if any
//		nodeChanged(node,NULL_COL,getRowCount());// do node parents recursively
//	}	
//	
//			
///**
// * Recursively update parent nodes
// * @param node starting node
// * @param col column to update
// * @param searchEnd end row number to use when searching.  Because parents are always above children there is no need to search
// * for a parent node past its child
// */	private void nodeChanged(Node node, int col, int searchEnd) {
// 		node = getCache().getWalkersModel().getParent(node); // initial child will have already been done. Note also using model and not cache.
//		if (node == null)
//			return;
//		int row = findNodeRow(node,searchEnd);
//		if (row != NULL_ROW) {
//			fireUpdate(row,col);
//			searchEnd = row;
//		}
//		nodeChanged(node,col,searchEnd);
//	}
	
/**
 * Finds the node for this object
 * @param object
 * @return Node found, null if not found
 */	private Node objectToNode(Object object) {
 		return getCache().getWalkersModel().search(object);
	}
 

	/**
	 * @param fieldContext The fieldContext to set.
	 */
	public void setFieldContext(FieldContext fieldContext) {
		this.fieldContext = fieldContext;
	}
	public FieldContext getFieldContext() {
		return fieldContext;
	}
	public boolean isRowEditable(int row) {
		return true;
	}

}

