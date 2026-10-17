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
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.table.TableCellRenderer;

import com.microproject.help.HelpUtil;
import com.microproject.menu.MenuActionConstants;
import com.microproject.pm.graphic.model.cache.NodeModelCache;
import com.microproject.pm.graphic.model.cache.NodeModelCacheFactory;
import com.microproject.pm.graphic.model.cache.ReferenceNodeModelCache;
import com.microproject.pm.graphic.model.cache.GraphicNode;
import com.microproject.pm.graphic.model.transform.NodeCacheTransformer;
import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetModel;
import com.microproject.document.Document;
import com.microproject.field.Field;
import com.microproject.field.FieldContext;
import com.microproject.graphic.configuration.CellStyle;
import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.model.NodeModel;
import com.microproject.grouping.core.transform.sorting.NodeSorter;
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
		String message = Messages.format("Format.threeParts", Messages.getString("ResourceView.YouCannotDeleteTheResource"),
				resource.getName(), Messages.getString(suffixKey));
		if (includeMoveHint)
			message += "\n" + Messages.getString("ResourceView.ToMoveAProtectedResource");
		return message;
	}
	static boolean areChildrenAllowed(Node node, Predicate<Node> allowed) {
		List<Node> children = node.getChildren();
		if (children == null) {
			return true;
		}
		for (Node child : children) {
			if (!allowed.test(child)) {
				return false;
			}
		}
		return true;
	}
	public static final String spreadsheetCategory=resourceSpreadsheetCategory;
	protected SpreadSheet spreadSheet;
	protected NodeModel model;
	protected NodeModelCache cache;
	Document document;
	FieldContext fieldContext;
	CellStyle cellStyle;
	boolean readOnly;
	/** Microsoft Application.Sort allows Key1-Key3; the first entry is the primary key. */
	public static final int MAX_SORT_KEYS = 3;
	/** One view-only resource sort key. */
	public record SortKey(String fieldId, boolean ascending) {}
	private List<SortKey> sortKeys = List.of();
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
		setColumnHeaderView(spreadSheet.getTableHeader());
		
		cache.update(); //this is not required by certain views 
		if (!master && !Environment.isProjectLibre()) {
			SwingUtilities.invokeLater(() ->
					Alert.warnWithOnceOption(Messages.getString("Info.resourceView"), "warnedResourceView"));
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
				if (!areChildrenAllowed(node, this::isNodeDeletable)) return false;
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
				if (!areChildrenAllowed(node, this::isNodeCuttable)) return false;
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
		spreadSheet.setHeaderClickHandler(this::sortByViewColumn);
		((SpreadSheetModel)spreadSheet.getModel()).setFieldContext(fieldContext);
		spreadSheet.setReadOnly(readOnly);
	}

	private void sortByViewColumn(int viewColumn) {
		int modelColumn = spreadSheet.convertColumnIndexToModel(viewColumn);
		List<Field> fields = spreadSheet.getFieldArray();
		if (modelColumn < 0 || modelColumn >= fields.size()) return;
		Field field = fields.get(modelColumn);
		boolean ascending = !field.getId().equals(getSortedFieldId()) || !isSortAscending();
		applySort(List.of(new SortKey(field.getId(), ascending)));
	}

	/** Resolves keys to fields, dropping unknown and duplicate fields, capped at {@link #MAX_SORT_KEYS}. */
	private List<SortKey> resolveKeys(List<SortKey> requested) {
		List<SortKey> resolved = new ArrayList<>();
		for (SortKey key : requested) {
			if (key == null || key.fieldId() == null || resolved.size() >= MAX_SORT_KEYS) continue;
			boolean duplicate = resolved.stream().anyMatch(existing -> existing.fieldId().equals(key.fieldId()));
			if (!duplicate && findField(key.fieldId()) != null) resolved.add(key);
		}
		return List.copyOf(resolved);
	}

	private Field findField(String fieldId) {
		for (Field field : spreadSheet.getFieldArray()) if (field.getId().equals(fieldId)) return field;
		return null;
	}

	private void applySort(List<SortKey> requested) {
		List<SortKey> keys = resolveKeys(requested);
		if (keys.isEmpty()) return;
		List<GraphicNode> selected = spreadSheet.getSelectedGraphicNodes();
		// Clear row indexes before the projection emits remove/insert events. JTable
		// otherwise shifts the old selection during those events and can briefly
		// address a row beyond the new projection, before we restore by node identity.
		spreadSheet.clearSelection();
		sortKeys = keys;
		List<ResourceFieldSorter.Key> comparators = new ArrayList<>();
		for (SortKey key : keys) comparators.add(new ResourceFieldSorter.Key(findField(key.fieldId()), key.ascending()));
		NodeSorter sorter = new ResourceFieldSorter(comparators);
		((NodeCacheTransformer) cache.getVisibleNodes().getTransformer()).getTransformer().setUserSorter(sorter);
		for (GraphicNode selectedNode : selected) {
			Object visible = cache.getGraphicNode(selectedNode.getNode());
			int row = cache.getRowAt(visible);
			if (row >= 0 && row < spreadSheet.getRowCount()) spreadSheet.addRowSelectionInterval(row, row);
		}
	}

	/** Applies the same stable, view-only single-key sort used by header clicks. */
	public boolean applySortOrder(String fieldId, boolean ascending) {
		return applySortOrder(List.of(new SortKey(fieldId, ascending)));
	}

	/** Applies up to three keys (Microsoft Key1-Key3); false when the first key is not a visible field. */
	public boolean applySortOrder(List<SortKey> requested) {
		if (requested == null || requested.isEmpty() || requested.get(0) == null
				|| requested.get(0).fieldId() == null || findField(requested.get(0).fieldId()) == null) return false;
		applySort(requested);
		return true;
	}

	public List<Field> getResourceSortFields() {
		return List.copyOf(spreadSheet.getFieldArray());
	}

	public List<SortKey> getSortKeys() { return sortKeys; }
	public String getSortedFieldId() { return sortKeys.isEmpty() ? null : sortKeys.get(0).fieldId(); }
	public boolean isSortAscending() { return sortKeys.isEmpty() || sortKeys.get(0).ascending(); }
	@Override public String getProjectionStateSignature() {
		if (sortKeys.size() <= 1) return "sortField=" + getSortedFieldId() + ",ascending=" + isSortAscending();
		StringBuilder signature = new StringBuilder();
		for (SortKey key : sortKeys) signature.append(signature.length() == 0 ? "sortKeys=" : ";")
			.append(key.fieldId()).append(key.ascending() ? "+" : "-");
		return signature.toString();
	}

	/** Compares by each key in order; the stable list sort keeps equal rows in model order. */
	static final class ResourceFieldSorter extends NodeSorter {
		record Key(java.util.Comparator<Object> comparator, boolean ascending) {}
		private final List<Key> keys;

		ResourceFieldSorter(java.util.Comparator<Object> field, boolean ascending) {
			this(List.of(new Key(field, ascending)));
		}

		ResourceFieldSorter(List<Key> keys) {
			this.keys = List.copyOf(keys);
			setShowAssignments(false);
			setPreserveHierarchy(true);
		}

		@Override
		public int compare(Object left, Object right) {
			Object leftResource = left instanceof Node node ? node.getImpl() : left;
			Object rightResource = right instanceof Node node ? node.getImpl() : right;
			for (Key key : keys) {
				int result = Integer.signum(key.comparator().compare(leftResource, rightResource));
				if (result != 0) return key.ascending() ? result : -result;
			}
			return 0;
		}
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
		if (ws.sortedFieldId != null) {
			List<SortKey> keys = new ArrayList<>();
			keys.add(new SortKey(ws.sortedFieldId, ws.sortAscending));
			if (ws.additionalSortKeys != null) {
				for (String encoded : ws.additionalSortKeys) {
					if (encoded == null || encoded.length() < 2) continue;
					char direction = encoded.charAt(0);
					if (direction == '+' || direction == '-') keys.add(new SortKey(encoded.substring(1), direction == '+'));
				}
			}
			applySortOrder(keys);
		}
	}
	public WorkspaceSetting createWorkspace(int context) {
		Workspace ws = new Workspace();
		ws.spreadSheet = spreadSheet.createWorkspace(context);
		ws.sortedFieldId = getSortedFieldId();
		ws.sortAscending = isSortAscending();
		// Secondary/tertiary keys use a separate field so the primary-key contract stays readable by older files.
		ws.additionalSortKeys = sortKeys.stream().skip(1)
			.map(key -> (key.ascending() ? "+" : "-") + key.fieldId()).toArray(String[]::new);
		return ws;
	}

	public static class Workspace implements WorkspaceSetting { 
		private static final long serialVersionUID = -1251204386431239291L;
		WorkspaceSetting spreadSheet;
		private String sortedFieldId;
		private boolean sortAscending = true;
		private String[] additionalSortKeys = new String[0];
		public WorkspaceSetting getSpreadSheet() {
			return spreadSheet;
		}
		public void setSpreadSheet(WorkspaceSetting spreadSheet) {
			this.spreadSheet = spreadSheet;
		}
		public String getSortedFieldId() { return sortedFieldId; }
		public void setSortedFieldId(String sortedFieldId) { this.sortedFieldId = sortedFieldId; }
		public boolean isSortAscending() { return sortAscending; }
		public void setSortAscending(boolean sortAscending) { this.sortAscending = sortAscending; }
		public String[] getAdditionalSortKeys() { return additionalSortKeys; }
		public void setAdditionalSortKeys(String[] additionalSortKeys) { this.additionalSortKeys = additionalSortKeys; }
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
