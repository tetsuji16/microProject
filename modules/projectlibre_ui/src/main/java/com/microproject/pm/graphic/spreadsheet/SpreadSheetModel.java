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
package com.microproject.pm.graphic.spreadsheet;

import java.util.LinkedList;

import com.microproject.pm.graphic.model.cache.NodeModelCache;
import com.microproject.pm.graphic.model.cache.GraphicNode;
import com.microproject.pm.graphic.spreadsheet.common.CommonSpreadSheetModel;
import com.microproject.association.InvalidAssociationException;
import com.microproject.datatype.Duration;
import com.microproject.field.Field;
import com.microproject.field.FieldParseException;
import com.microproject.graphic.configuration.ActionList;
import com.microproject.graphic.configuration.CellStyle;
import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.model.NodeModel;
import com.microproject.pm.dependency.Dependency;
import com.microproject.pm.dependency.DependencyService;
import com.microproject.pm.dependency.DependencyType;
import com.microproject.util.ClassUtils;
import com.microproject.util.Environment;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.AbstractLayoutCache;
import javax.swing.tree.TreePath;

import org.netbeans.swing.outline.DefaultOutlineModel;
import org.netbeans.swing.outline.OutlineModel;
import org.netbeans.swing.outline.RowModel;
import org.netbeans.swing.outline.TreePathSupport;

/**
 * 
 */
public class SpreadSheetModel extends CommonSpreadSheetModel implements OutlineModel, RowModel {
	protected boolean readOnly;
	private transient OutlineModel outlineDelegate;
	private static final String DEPENDENCY_TYPE_FIELD_ID = "Field.dependencyType";
	private static final String DEPENDENCY_LAG_FIELD_ID = "Field.lag";
	/**
	 * 
	 */
	public SpreadSheetModel(NodeModelCache cache, SpreadSheetColumnModel colModel, CellStyle cellStyle, ActionList actionList) {
		super(cache, colModel, cellStyle, actionList);
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

	public int getColumnCount() {
		return colModel.getFieldColumnCount();
	}

	public Field getFieldInColumn(int col) {
		return SpreadSheetUtils.getFieldInColumn(col,colModel);
		//return colModel.getFieldInColumn(col);
	}

	public Field getFieldInNonTranslatedColumn(int col) {
		return colModel.getFieldInNonTranslatedColumn(col);
	}

	public Field getFieldInViewColumn(int viewColumn) {
		return colModel.getFieldInViewColumn(viewColumn);
	}

	public int getModelColumnForViewColumn(int viewColumn) {
		return colModel.getModelColumnForViewColumn(viewColumn);
	}

	@Override
	public String getColumnName(int col) {
		if (col == 0) {
			return "";
		}
		return getFieldInColumn(col).getName();
	}

	@Override
	public Class getColumnClass(int col) {
		return getFieldInColumn(col).getDisplayType();
	}

	public Object getValueAt(int row, int col) {
		return SpreadSheetUtils.getValueAt(row,col,getRowMultiple(),cache,colModel,fieldContext);
	}

	public void setValueAt(Object value, int row, int col) {
		if (isReadOnly()) return;
		if (col == 0)
			return;
		Field field=getFieldInColumn(col);
		boolean roleField="Field.userRole".equals(field.getId()); //an exception for roles
		NodeModel nodeModel=getCache().getModel();
		if (!nodeModel.isLocal()&&!nodeModel.isMaster()&&!Environment.getStandAlone()&&!roleField) return;
		
		
		// System.out.println("Field " + getFieldInColumn(col) +
		// "setValueAt("+value+","+row+","+col+")");

		Object oldValue = getValueAt(row, col);
		// if (oldValue==null&&(value==null||"".equals(value))) return;
		if (oldValue == null && ("".equals(value)))
			return;

		Node rowNode = getNodeInRow(row);
		//Field field = getFieldInColumn(col);

		try {
			if (rowNode.isVoid()) {
				if (value == null) { // null means parse error, so generate error here
					getCache().getModel().setFieldValue(field, rowNode, this, value, fieldContext, NodeModel.NORMAL);
				} else{
					//boolean previousIsParent=false;
					LinkedList previousNodes=getPreviousVisibleNodesFromRow(row);
					if (previousNodes!=null){
						Node nextSibling=getNextNonVoidSiblingFromRow(row);
						if(nextSibling!=null&&nextSibling.getParent()==previousNodes.getFirst()) previousNodes=null;
					}
					getCache().getModel()
							.replaceImplAndSetFieldValue(rowNode, previousNodes, getFieldInColumn(col), this, value, fieldContext, NodeModel.NORMAL);
			
				}
			} else if (rowNode.getImpl() instanceof Dependency) { // dependencies
																	// need
																	// specific
																	// handling
																	// at least
																	// for undo
				Dependency dependency = (Dependency) rowNode.getImpl();
				DependencyService dependencyService = DependencyService.getInstance();
				try {
					long lag = getDependencyLag(field, value, dependency);
					int type = getDependencyType(field, value, dependency);
					dependencyService.setFields(dependency, lag, type, this);
					dependencyService.update(dependency, this);
				} catch (InvalidAssociationException e1) {
					throw new RuntimeException(e1);
				}
			} else {
				getCache().getModel().setFieldValue(field, rowNode, this, value, fieldContext, NodeModel.NORMAL);
			}
		} catch (FieldParseException e) {
			throw new RuntimeException(e); // exceptions will be treated by the spreadsheet, not the model, because there is a popup.  Because this method doesn't have an exception, a runtime exception will be caught by the spreadsheet
		}
	}

	static long getDependencyLag(Field editedField, Object value, Dependency dependency) {
		if (editedField != null && DEPENDENCY_LAG_FIELD_ID.equals(editedField.getId())) {
			return ((Duration) value).getEncodedMillis();
		}
		return dependency.getLag();
	}

	static int getDependencyType(Field editedField, Object value, Dependency dependency) {
		if (editedField != null && DEPENDENCY_TYPE_FIELD_ID.equals(editedField.getId())) {
			Integer parsedType = DependencyType.mapStringToValue((String) value);
			if (parsedType == null) {
				throw new IllegalArgumentException("Invalid dependency type: " + value);
			}
			return parsedType.intValue();
		}
		return dependency.getDependencyType();
	}

	public boolean isRowEditable(int row) {
		if (isReadOnly()) return false;
		NodeModel nodeModel=getCache().getModel();
		//if (!nodeModel.isLocal()&&!nodeModel.isMaster()&&!Environment.getStandAlone()) return false;
		Node node = getNodeInRow(row);
		if (node.isVoid())
			return true;
		return !ClassUtils.isObjectReadOnly(node.getImpl());
	}
	
	public boolean isCellEditable(int row, int col) {
		if (isReadOnly()) return false;
		if (col == 0)
			return false;
		Field field=getFieldInColumn(col);
		if (field.getLookupTypes() != null)
			return false;
		Node node = getNodeInRow(row);
		NodeModel nodeModel=getCache().getModel();
// 		if (!nodeModel.isLocal()&&!nodeModel.isMaster()&&!Environment.getStandAlone()) return false;
		
		if (node.isVoid()&&!(nodeModel.isLocal()||nodeModel.isMaster())&&"Field.userRole".equals(field.getId()))
			return false;

		if (node.isVoid())
			return true;
		return !field.isReadOnly(node, getCache().getWalkersModel(), fieldContext);
	}

	private int findFieldColumn(Field field) {
		return colModel.findFieldColumn(field);
	}

	public boolean isReadOnly() {
		return readOnly;
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
		if (!(node instanceof com.microproject.pm.graphic.model.cache.GraphicNode graphicNode)) {
			return false;
		}
		int row = cache.getRowAt(graphicNode);
		return row >= 0 && isCellEditable(row, column);
	}

	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
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
		// Tree edits are driven through the spreadsheet actions and node model.
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
	
	


}

