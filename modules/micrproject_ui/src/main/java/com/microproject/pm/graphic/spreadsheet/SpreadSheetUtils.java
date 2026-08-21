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
package com.microproject.pm.graphic.spreadsheet;

import java.awt.Dimension;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;

import javax.swing.JScrollPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.microproject.pm.graphic.frames.DocumentFrame;
import com.microproject.pm.graphic.model.cache.GraphicNode;
import com.microproject.pm.graphic.model.cache.NodeModelCache;
import com.microproject.pm.graphic.model.cache.NodeModelCacheFactory;
import com.microproject.pm.graphic.model.cache.ReferenceNodeModelCache;
import com.microproject.pm.graphic.model.transform.NodeCacheTransformer;
import com.microproject.configuration.Dictionary;
import com.microproject.configuration.FieldDictionary;
import com.microproject.field.Field;
import com.microproject.field.FieldContext;
import com.microproject.graphic.configuration.SpreadSheetCategories;
import com.microproject.graphic.configuration.SpreadSheetFieldArray;
import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.model.NodeModel;
import com.microproject.grouping.core.model.NodeModelDataFactory;
import com.microproject.grouping.core.model.NodeModelFactory;
import com.microproject.grouping.core.transform.ViewTransformer;
import com.microproject.grouping.core.transform.filtering.BelongsToCollectionFilter;
import com.microproject.grouping.core.transform.filtering.NodeFilter;
import com.microproject.strings.Messages;

/**
 * Helper methods for working with spreadsheets
 */



public class SpreadSheetUtils {

	public static void setFieldsAndContext(SpreadSheet ss
			,NodeModelCache cache
			,String spreadSheetCategory
			,String spreadSheetId
			,boolean leftAssociation) {
		SpreadSheetFieldArray fields = (SpreadSheetFieldArray) Dictionary.get(spreadSheetCategory, Messages.getString(spreadSheetId));
		ss.setCache(cache, fields, fields.getCellStyle(), fields.getActionList());
		FieldContext fieldContext = new FieldContext();
		fieldContext.setLeftAssociation(leftAssociation);
		fieldContext.setTaskSheetUpdate(true);
		((SpreadSheetModel) ss.getModel()).setFieldContext(fieldContext);
		((SpreadSheetModel) ss.getModel()).getCache().update();
	}
	/** Refresh the contents of a collection based spreadsheet
	 * @param ss
	 * @param collection
	 * @param document
	 * @param viewId
	 * @param spreadSheetCategory
	 * @param spreadSheetId
	 * @param leftAssociation
	 * @param nbVoidNodes number of trailing void rows to reserve
	 */
	public static void createCollectionSpreadSheet(	SpreadSheet ss
													,Collection collection
													//,Document document
													,String viewId
													,String spreadSheetCategory
													,String spreadSheetId
													,boolean leftAssociation
													,NodeModelDataFactory dataFactory,
													int nbVoidNodes
//													,boolean local
//													,boolean master
													) {
		NodeModel nodeModel = NodeModelFactory.getInstance().createNodeModelFromCollection(collection,dataFactory);
//    	nodeModel.setLocal(local);
//    	nodeModel.setMaster(master);
		nodeModel.getHierarchy().setNbEndVoidNodes(nbVoidNodes);
		ReferenceNodeModelCache refCache = NodeModelCacheFactory.getInstance().createReferenceCache(nodeModel, /*document*/null,((leftAssociation)?NodeModelCache.TASK_TYPE:NodeModelCache.RESOURCE_TYPE)|NodeModelCache.ASSIGNMENT_TYPE);
		NodeModelCache cache = NodeModelCacheFactory.getInstance().createFilteredCache(refCache, Messages.getString(viewId),null);
		setFieldsAndContext(ss,cache,spreadSheetCategory,spreadSheetId,leftAssociation);

	}
	public static void updateCollectionSpreadSheet(	SpreadSheet ss
			,Collection collection
			,NodeModelDataFactory dataFactory
			,int nbVoidNodes) {

		ss.clearActions();
		NodeModel nodeModel = ss.getCache().getModel();
		NodeModelFactory.getInstance().updateNodeModelFromCollection(nodeModel,collection,dataFactory,nbVoidNodes);
	}

	/**
     * This one doesn't recreate the cache and all its associated objects since they allready exist in the main referenceNodeModelCache.
     * It just applies a filter. (a simplified version of SelectionFilter used by UsageDetail)
	 * @param nbVoidNodes number of trailing void rows to reserve
	 * @param popupActions popup actions to attach to the spreadsheet
     */
    public static SpreadSheet createFilteredSpreadsheet(DocumentFrame df
    													,boolean task // if task based
														,String viewId
														,String spreadSheetCategory
														,String spreadSheetId
														,boolean leftAssociation
														//,int nbVoidNodes
														,String[] actionList) {

        NodeModelCache cache = df.createCache(task,Messages.getString(viewId));
        cache.update();
        return createFilteredSpreadsheet(cache,spreadSheetCategory,spreadSheetId,leftAssociation,/*nbVoidNodes,*/actionList);
    }
    public static SpreadSheet createFilteredSpreadsheet(NodeModelCache cache
			,String spreadSheetCategory
			,String spreadSheetId
			,boolean leftAssociation
			//,int nbVoidNodes
			,String[] actionList) {
		SpreadSheet ss = new SpreadSheet();
		// The category owns the available columns, saved layout, and column popup.
		// It must therefore match the field array requested by the caller.  The
		// association direction only describes how assignment fields are resolved;
		// it is not a substitute for the spreadsheet category.  In particular,
		// the Resource Information "Tasks" pane has task-assignment columns while
		// resolving assignments from a resource, and vice versa for Task Information.
		ss.setSpreadSheetCategory(spreadSheetCategory);
		//cache.getModel().getHierarchy().setNbEndVoidNodes(nbVoidNodes);
		setFieldsAndContext(ss,cache,spreadSheetCategory,spreadSheetId,leftAssociation);
		ss.setActions(actionList);
		return ss;
    }



    /**
     * changes filter's collection
     */
    public static void updateFilteredSpreadsheet(SpreadSheet ss, Collection collection) {
	    ViewTransformer transformer=((NodeCacheTransformer)ss.getCache().getVisibleNodes().getTransformer()).getTransformer();
	    NodeFilter filter=transformer.getHiddenFilter();
	    if (filter instanceof BelongsToCollectionFilter)
	        ((BelongsToCollectionFilter)filter).setSelectedNodesImpl(collection,true);
    }

	/** put a spreadsheet in a scroll pane and fix problems with scrolling header
	 *
	 * @param spreadSheet
	 * @return
	 */
	public static JScrollPane makeSpreadsheetScrollPane(SpreadSheet spreadSheet) {
		final JScrollPane spreadSheetScrollPane=new JScrollPane(spreadSheet);
		//a fix to resize column header when viewport size changes
		spreadSheetScrollPane.getViewport().addChangeListener(new ChangeListener(){
			private Dimension olddmain=null;
			public void stateChanged(ChangeEvent e){
//				Dimension dmain=spreadSheetScrollPane.getViewport().getViewSize();
//				if (dmain.equals(olddmain)) return;
//				olddmain=dmain;
//				System.out.println("pref size #1="+spreadSheetScrollPane.getColumnHeader().getPreferredSize());
//				spreadSheetScrollPane.getColumnHeader().setPreferredSize(new Dimension(dmain.width,spreadSheetScrollPane.getColumnHeader().getPreferredSize().height));
//				System.out.println("pref size #2="+spreadSheetScrollPane.getColumnHeader().getPreferredSize());
//				spreadSheetScrollPane.getColumnHeader().revalidate();
//				System.out.println("pref size #3="+spreadSheetScrollPane.getColumnHeader().getPreferredSize());
//

//				Dimension d=spreadSheetScrollPane.getColumnHeader().getPreferredSize();
//				d.setSize(dmain.getWidth(),d.getHeight());
//				spreadSheetScrollPane.getColumnHeader().revalidate();
			}
		});
		return spreadSheetScrollPane;
	}

	public static List getFieldsForCategory(String category) {
		if (category == null)
			return null;
		if (category.equals(SpreadSheetCategories.projectSpreadsheetCategory)) {
			return FieldDictionary.getInstance().getProjectFields();
		} else if (category.equals(SpreadSheetCategories.taskSpreadsheetCategory)) {
			return FieldDictionary.getInstance().getTaskFields();
		} else if (category.equals(SpreadSheetCategories.resourceSpreadsheetCategory)) {
			return FieldDictionary.getInstance().getResourceFields();
		} else if (category.equals(SpreadSheetCategories.taskAssignmentSpreadsheetCategory)) {
			return FieldDictionary.getInstance().getTaskAndAssignmentFields();
		} else if (category.equals(SpreadSheetCategories.resourceAssignmentSpreadsheetCategory)) {
			return FieldDictionary.getInstance().getResourceAndAssignmentFields();
		} else if (category.equals(SpreadSheetCategories.timesheetSpreadsheetCategory)) {
			return FieldDictionary.getInstance().getAssignmentFields();
		} else if (category.equals("assignmentEntrySpreadsheet")) {
			return FieldDictionary.getInstance().getAssignmentFields();
		} else if (category.equals(SpreadSheetCategories.dependencySpreadsheetCategory)) {
			return FieldDictionary.getInstance().getDependencyFields();
		}
		return null;

	}



	public static GraphicNode getNodeFromCacheRow(int row,int rowMultiple,NodeModelCache cache) {
		return (GraphicNode) cache.getElementAt(row/rowMultiple);
	}
	public static Node getNodeInRow(int row,int rowMultiple,NodeModelCache cache) {
		GraphicNode gnode = getNodeFromCacheRow(row,rowMultiple,cache);
		if (gnode == null)
			return null;
		return gnode.getNode();

	}
	public static Field getFieldInColumn(int col,SpreadSheetColumnModel colModel) {
		return colModel.getFieldInColumn(col);
	}
	public static Object getValueAt(int row, int col,int rowMultiple,NodeModelCache cache,SpreadSheetColumnModel colModel,FieldContext context) {
		Node node = getNodeInRow(row,rowMultiple,cache);
		return getValueAt(node, col, cache, colModel, context);
	}
	public static Object getValueAt(Node node,int col,NodeModelCache cache,SpreadSheetColumnModel colModel,FieldContext context) {
		if (node.isVoid())
			return (col == 0) ? "" : null;
		return getFieldInColumn(col,colModel).getValue(node, cache.getWalkersModel(), context);
	}



}
