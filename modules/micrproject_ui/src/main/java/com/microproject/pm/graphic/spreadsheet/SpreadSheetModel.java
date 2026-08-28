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

import org.netbeans.swing.outline.OutlineModel;
import org.netbeans.swing.outline.RowModel;

/**
 * 
 */
public class SpreadSheetModel extends CommonSpreadSheetModel implements OutlineModel, RowModel {
	protected boolean readOnly;
	private static final String DEPENDENCY_TYPE_FIELD_ID = "Field.dependencyType";
	private static final String DEPENDENCY_LAG_FIELD_ID = "Field.lag";
	/**
	 * 
	 */
	public SpreadSheetModel(NodeModelCache cache, SpreadSheetColumnModel colModel, CellStyle cellStyle, ActionList actionList) {
		super(cache, colModel, cellStyle, actionList);
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

	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}

}

