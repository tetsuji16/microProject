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
import com.microproject.pm.graphic.model.cache.ViewNodeModelCache;
import com.microproject.pm.graphic.spreadsheet.common.CommonSpreadSheetModel;
import com.microproject.datatype.Duration;
import com.microproject.field.Field;
import com.microproject.field.FieldParseException;
import com.microproject.graphic.configuration.ActionList;
import com.microproject.graphic.configuration.CellStyle;
import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.model.NodeModel;
import com.microproject.pm.dependency.Dependency;
import com.microproject.pm.dependency.DependencyType;
import com.microproject.util.ClassUtils;
import com.microproject.util.Environment;
import com.microproject.application.task.TaskCommandGateway;
import com.microproject.application.task.TaskCommandResult;
import com.microproject.application.task.TaskCommands.TaskFieldEditCommand;
import com.microproject.application.task.TaskCommands.TaskFieldBatchEditCommand;
import com.microproject.application.task.TaskCommands.TaskDependencyUpdateCommand;
import com.microproject.pm.task.ProjectTaskKey;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.Task;

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
	private transient TaskCellEditDraft taskCellEditDraft;
	private transient boolean editorCommitInProgress;

	private record TaskCellEditDraft(ProjectTaskKey taskKey, String fieldId, Object expectedValue) { }
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

	/** Captures the optimistic-concurrency token before an interactive editor starts. */
	public void beginTaskCellEdit(int row, int viewColumn) {
		taskCellEditDraft = null;
		if (row < 0 || row >= getRowCount() || viewColumn <= 0) return;
		Node node = getNodeInRow(row);
		Field field = getFieldInViewColumn(viewColumn);
		if (node == null || field == null || !(node.getImpl() instanceof Task task)) return;
		ProjectTaskKey key = ProjectTaskKey.from(task).orElse(null);
		if (key == null) return;
		int modelColumn = getModelColumnForViewColumn(viewColumn);
		taskCellEditDraft = new TaskCellEditDraft(key, field.getId(), getValueAt(row, modelColumn));
	}

	public void clearTaskCellEdit() {
		taskCellEditDraft = null;
	}
	public void beginTaskCellEditorCommit() { editorCommitInProgress = true; }
	public void endTaskCellEditorCommit() { editorCommitInProgress = false; }

	public record PasteCell(String value, int row, int modelColumn) { }

	/** Returns null when the target contains non-task rows and needs a different paste strategy. */
	public Boolean pasteTaskCellsAtomically(java.util.List<PasteCell> cells) {
		if (cells == null || cells.isEmpty()) return Boolean.FALSE;
		java.util.List<TaskFieldEditCommand> edits = new java.util.ArrayList<>(cells.size());
		Project commandProject = null;
		TaskCommandGateway gateway = null;
		for (PasteCell cell : cells) {
			if (cell.row() < 0 || cell.row() >= getRowCount() || cell.modelColumn() <= 0
					|| cell.modelColumn() >= getColumnCount()) return Boolean.FALSE;
			Node node = getNodeInRow(cell.row());
			Field field = getFieldInColumn(cell.modelColumn());
			if (node == null || field == null || !(node.getImpl() instanceof Task task)) return null;
			Project owner = task.getOwningProject();
			ProjectTaskKey key = ProjectTaskKey.from(task).orElse(null);
			if (owner == null || key == null || (commandProject != null && commandProject != owner)) return Boolean.FALSE;
			commandProject = owner;
			if (gateway == null) gateway = taskCommandGateway();
			edits.add(new TaskFieldEditCommand(key, field.getId(), getValueAt(cell.row(), cell.modelColumn()),
					cell.value(), fieldContext));
		}
		TaskCommandResult result = gateway.editFields(new TaskFieldBatchEditCommand(edits,
				commandProject.getDomainChangeJournal().revision()));
		return result.status() == TaskCommandResult.Status.COMMITTED
				|| result.status() == TaskCommandResult.Status.NO_OP;
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
			TaskCellEditDraft draft = taskCellEditDraft;
			if (editorCommitInProgress && draft != null && field.getId().equals(draft.fieldId())) {
				taskCellEditDraft = null;
				if (!(getCache().getModel().getDataFactory() instanceof Project commandProject))
					throw new IllegalStateException("Task edit has no project");
				TaskCommandGateway gateway = taskCommandGateway();
				TaskCommandResult result = gateway.editField(
						new TaskFieldEditCommand(draft.taskKey(), field.getId(), draft.expectedValue(), value, fieldContext));
				handleTaskCommandResult(result);
				return;
			}
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
				Task predecessor = dependency.getPredecessor() instanceof Task task ? task : null;
				Task successor = dependency.getSuccessor() instanceof Task task ? task : null;
				ProjectTaskKey predecessorKey = ProjectTaskKey.from(predecessor).orElse(null);
				ProjectTaskKey successorKey = ProjectTaskKey.from(successor).orElse(null);
				Project commandProject = predecessor == null ? null : predecessor.getOwningProject();
				if (predecessorKey == null || successorKey == null || commandProject == null)
					throw new IllegalStateException("Dependency has no command identity");
				TaskCommandGateway gateway = taskCommandGateway();
				TaskCommandResult result = gateway.updateDependency(new TaskDependencyUpdateCommand(
						predecessorKey, successorKey, dependency.getLag(), dependency.getDependencyType(),
						getDependencyLag(field, value, dependency), getDependencyType(field, value, dependency),
						commandProject.getDomainChangeJournal().revision()));
				handleTaskCommandResult(result);
			} else if (rowNode.getImpl() instanceof Task task) {
				ProjectTaskKey taskKey = ProjectTaskKey.from(task).orElse(null);
				Project commandProject = task.getOwningProject();
				if (taskKey == null || commandProject == null)
					throw new IllegalStateException("Task has no command identity");
				TaskCommandGateway gateway = taskCommandGateway();
				TaskCommandResult result = gateway.editField(
						new TaskFieldEditCommand(taskKey, field.getId(), oldValue, value, fieldContext));
				handleTaskCommandResult(result);
			} else {
				getCache().getModel().setFieldValue(field, rowNode, this, value, fieldContext, NodeModel.NORMAL);
			}
		} catch (FieldParseException e) {
			throw new RuntimeException(e); // exceptions will be treated by the spreadsheet, not the model, because there is a popup.  Because this method doesn't have an exception, a runtime exception will be caught by the spreadsheet
		}
	}

	private static void handleTaskCommandResult(TaskCommandResult result) {
		if (result.status() == TaskCommandResult.Status.COMMITTED
				|| result.status() == TaskCommandResult.Status.NO_OP) return;
		if (result.failure() instanceof RuntimeException runtime) throw runtime;
		throw new IllegalStateException("Task field command failed: " + result.status(), result.failure());
	}

	private TaskCommandGateway taskCommandGateway() {
		if (getCache() instanceof ViewNodeModelCache viewCache) return viewCache.getTaskCommandGateway();
		throw new IllegalStateException("task edits require a document view cache");
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
