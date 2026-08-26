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
import java.awt.Component;
import java.awt.Container;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.InputMethodEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.im.InputContext;
import java.text.AttributedCharacterIterator;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.EventObject;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.CellEditor;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.InputMap;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.event.EventListenerList;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ChangeEvent;
import javax.swing.plaf.UIResource;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableModel;
import javax.swing.text.JTextComponent;
import javax.swing.text.BadLocationException;
import javax.swing.text.Utilities;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;

import com.microproject.pm.graphic.ChangeAwareComponent;
import com.microproject.pm.graphic.model.cache.GraphicNode;
import com.microproject.pm.graphic.model.cache.NodeModelCache;
import com.microproject.pm.graphic.model.event.CacheListener;
import com.microproject.pm.graphic.model.event.CompositeCacheEvent;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetColumnModel;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetModel;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetSearchContext;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetUtils;
import com.microproject.pm.graphic.spreadsheet.editor.DateEditor;
import com.microproject.pm.graphic.spreadsheet.editor.KeyboardFocusable;
import com.microproject.pm.graphic.spreadsheet.renderer.NameCellComponent;
import com.microproject.pm.graphic.spreadsheet.selection.SpreadSheetSelectionModel;
import com.microproject.pm.graphic.spreadsheet.selection.event.SelectionNodeEvent;
import com.microproject.pm.graphic.spreadsheet.selection.event.SelectionNodeListener;
import com.microproject.pm.graphic.timescale.ScaledScrollPane;
import com.microproject.pm.graphic.views.SearchContext;
import com.microproject.pm.graphic.views.Searchable;
import com.microproject.configuration.Dictionary;
import com.microproject.field.Field;
import com.microproject.field.FieldParseException;
import com.microproject.graphic.configuration.GraphicConfiguration;
import com.microproject.graphic.configuration.SpreadSheetFieldArray;
import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.NodeFactory;
import com.microproject.grouping.core.model.NodeModel;
import com.microproject.server.access.ErrorLogger;
import com.microproject.util.Alert;
import com.microproject.workspace.SavableToWorkspace;
import com.microproject.workspace.WorkspaceSetting;
import com.microproject.util.FlatUiSupport;
/**
 *
 */
@SuppressWarnings("unchecked")
public class CommonSpreadSheet extends CommonTable implements CacheListener, SavableToWorkspace, Searchable {
	private static final Logger logger = Logger.getLogger(CommonSpreadSheet.class.getName());
	/**
	 *
	 */
	private static final long serialVersionUID = 2541466281456673698L;
	public static final String RESOURCE_CATEGORY="resourceSpreadsheet";
	public static final String TASK_CATEGORY="taskSpreadsheet";
	private static final String COMMIT_AND_MOVE_DOWN_ACTION = "spreadsheet.commitAndMoveDown";
	private static final String COMPOSITION_PROPERTY = "projectlibre.input.composing";

	protected SpreadSheetSelectionModel selection;
	protected String spreadSheetCategory = null;
	protected SpreadSheetRowHeader rowHeader;
	protected SpreadSheetCorner corner;
	protected int lastEditingRow = -1;
	protected boolean canModifyColumns = true;
	protected boolean canSelectFieldArray = true;
	private PendingUndoSelection pendingUndoSelection;
	private boolean inputMethodEditingSessionActive;
	private final StringBuilder pendingReceivedText = new StringBuilder();
	private boolean headerColumnSelectionActive;
	private boolean rowHeaderSelectionActive;

	public CommonSpreadSheet() {
		super();
		setGridColor(FlatUiSupport.tableGridColor());
		FlatUiSupport.applyDataSurface(this);
		putClientProperty("JTable.autoStartsEdit", Boolean.FALSE);
		putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
		//setSurrendersFocusOnKeystroke(true); //has the side effect of selecting the first character of cell after ENTER keystroke
		setAutoCreateColumnsFromModel(false);
		enableInputMethods(true);
		rowHeader=new SpreadSheetRowHeader(this);
		rowHeader.setRowHeight(getRowHeight());

		setFocusCycleRoot(true);
	}
	public void cleanUp() {
		NodeModelCache currentCache = getCache();
		if (currentCache != null && getModel() instanceof CacheListener listener) {
			currentCache.removeNodeModelListener(listener);
		}
	}

//	public void setModel(CommonSpreadSheetModel spreadSheetModel, DefaultTableColumnModel spreadSheetColumnModel) {
//
//		setModel(spreadSheetModel);
//	    setColumnModel(spreadSheetColumnModel);
//
//	    selection = new SpreadSheetSelectionModel(this);
//		selection.setRowSelection(new SpreadSheetListSelectionModel(selection,
//				true));
//		selection.setColumnSelection(new SpreadSheetListSelectionModel(
//				selection, false));
//		setSelectionModel(selection.getRowSelection());
//		createDefaultColumnsFromModel();
//		getColumnModel().setSelectionModel(selection.getColumnSelection());
//
//		registerEditors();
//		initRowHeader(spreadSheetModel);
//		initModel();
//		initListeners();
//
//
//
//	}

	//helper
	public void setCache(NodeModelCache cache){
		((CommonSpreadSheetModel)getModel()).setCache(cache);
	}
	public NodeModelCache getCache(){
		var model = getCommonSpreadSheetModel();
		return model == null ? null : model.getCache();
	}

	public void setFieldArray(ArrayList<Field> fieldArray){
		clearHeaderColumnSelectionState();
		((SpreadSheetColumnModel)getColumnModel()).setFieldArray(fieldArray);
//
//		((CommonSpreadSheetModel)getModel()).setFieldArray(fieldArray);
	}
	public ArrayList<Field> getFieldArray() {
		var model = getCommonSpreadSheetModel();
		return model == null ? new ArrayList<>() : model.getFieldArray();
	}

	public final SpreadSheetFieldArray getFieldArrayWithWidths(ArrayList<Field> fieldArray) {
		if (fieldArray == null) {
			fieldArray = getFieldArray();
		}
		// the widths don't work now anyway, and someone had a crash due to code below
		var cols = (SpreadSheetColumnModel) getColumnModel();
		var colWidths = new ArrayList<Integer>(cols.getColumnCount());
		var manualWidths = new ArrayList<Boolean>(cols.getColumnCount() + 1);
		colWidths.add(-1); //id column ignored
		manualWidths.add(false);
		for (int i = 0; i < cols.getColumnCount(); i++) {
			colWidths.add(cols.getColumn(i).getWidth());
			manualWidths.add(cols.isWidthManuallyAdjusted(((Field) cols.getColumn(i).getIdentifier()).getId()));
		}
		((SpreadSheetFieldArray)fieldArray).setWidths(colWidths);
		((SpreadSheetFieldArray)fieldArray).setManualWidths(manualWidths);
		return (SpreadSheetFieldArray) fieldArray;
	}

	public final void setFieldArrayWithWidths(SpreadSheetFieldArray fieldArray) {
		setFieldArray(fieldArray);
		// the widths don't work now anyway, and someone had a crash due to code below
//		SpreadSheetColumnModel cols = (SpreadSheetColumnModel)getColumnModel();
//		for (int i=0; i < cols.getColumnCount(); i++)
//			cols.getColumn(i).setWidth(fieldArray.getWidth(i));
	}


	public void setRowHeight(int rowHeight) {
		super.setRowHeight(rowHeight);
		if (rowHeader!=null) rowHeader.setRowHeight(rowHeight);
	}
	protected void initRowHeader(CommonSpreadSheetModel spreadSheetModel){
		rowHeader.setModel(spreadSheetModel,new SpreadSheetRowHeaderColumnModel());
		rowHeader.createDefaultColumnsFromModel();
		if (selection != null) {
			rowHeader.setSelectionModel(selection.getRowSelection());
		}
	}

	protected void initModel(){


		GraphicConfiguration config=GraphicConfiguration.getInstance();
		setRowHeight(config.getRowHeight());
		setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		setCellSelectionEnabled(true);
		//setRowSelectionAllowed(true);
		//setColumnSelectionAllowed(true);
	}

	protected void initListeners(){
	}

	@Override
	public void columnSelectionChanged(ListSelectionEvent event) {
		super.columnSelectionChanged(event);
		if (getTableHeader() != null)
			getTableHeader().repaint();
	}

	@Override
	public void valueChanged(ListSelectionEvent event) {
		super.valueChanged(event);
		if (rowHeader != null)
			rowHeader.repaint();
	}

	/**
	 * @return Returns the selection.
	 */
	public SpreadSheetSelectionModel getSelection() {
		return selection;
	}
	public boolean isCellEditing(int row, int col) {
		return (!(isEditing() && getEditingRow() == row && getEditingColumn() == col));
	}

	// ---------------------------------------------------------------------
	// Editing entry points
	// ---------------------------------------------------------------------

	private void startEditingCurrentCell(boolean caretAtEnd) {
		startEditingCurrentCell(caretAtEnd, false);
	}

	private void startEditingCurrentCell(boolean caretAtEnd, boolean clearTextOnStart) {
		EditableCellTarget target = resolveEditableCellTarget();
		if (target == null)
			return;
		inputMethodEditingSessionActive = false;
		startEditingAtTarget(target, new StartEditEvent(this, caretAtEnd, null, clearTextOnStart, !caretAtEnd && !clearTextOnStart), text -> {
			if (clearTextOnStart) {
				text.setText("");
			}
			if (caretAtEnd) {
				positionEditorCaretToEnd(text);
			}
		});
	}

	private boolean startClearingCurrentCell() {
		EditableCellTarget target = resolveEditableCellTarget();
		if (target == null)
			return false;
		if (!isCellEditable(target.row, target.column))
			return false;
		startEditingCurrentCell(false, true);
		return true;
	}

	private void startEditingFromTypedKey(KeyEvent e) {
		EditableCellTarget target = resolveEditableCellTarget();
		if (target == null)
			return;
		inputMethodEditingSessionActive = false;
		final boolean clearTextOnStart = shouldClearFieldOnTypedDigit(target.row, target.column, e.getKeyChar());
		final char typedChar = e.getKeyChar();
		startEditingAtTarget(target, new StartEditEvent(this, false, Character.valueOf(typedChar), clearTextOnStart, false), text -> {
			applyTypedChar(text, typedChar, clearTextOnStart);
		});
	}

	private void insertReceivedText(String text) {
		if (text == null || text.isEmpty())
			return;
		EditableCellTarget target = resolveEditableCellTarget();
		if (target == null)
			return;
		final boolean startNewEdit = !isEditing();
		pendingReceivedText.append(text);
		if (startNewEdit) {
			inputMethodEditingSessionActive = false;
			startEditingAtTarget(target, new StartEditEvent(this, false, null, false, false), editorText -> {
				flushReceivedText(editorText, true);
			});
			return;
		}
		SwingUtilities.invokeLater(() -> {
			requestEditorFocus();
			JTextComponent editorText = getEditorTextComponent();
			if (editorText == null)
				return;
			flushReceivedText(editorText, false);
		});
	}

	private void startEditingFromInputMethod(InputMethodEvent e) {
		EditableCellTarget target = resolveEditableCellTarget();
		if (target == null)
			return;
		inputMethodEditingSessionActive = true;
		startEditingAtTarget(target, new StartEditEvent(this, false, null, false, false), text -> {
			dispatchInputMethodEvent(text, e);
		});
	}

	private void startEditingForReconversion(KeyEvent e) {
		EditableCellTarget target = resolveEditableCellTarget();
		if (target == null)
			return;
		startEditingAtTarget(target, new StartEditEvent(this, false, null, false, false), text -> {
			text.selectAll();
			requestReconversion(text);
		});
	}

	private void startEditingAtTarget(EditableCellTarget target, StartEditEvent startEvent, EditorStartAction action) {
		if (target == null)
			return;
		if (!editCellAt(target.row, target.column, startEvent))
			return;
		SwingUtilities.invokeLater(() -> {
			requestEditorFocus();
			JTextComponent text = getEditorTextComponent();
			if (text == null)
				return;
			if (action != null) {
				action.apply(text);
			}
		});
	}

	// ---------------------------------------------------------------------
	// Input dispatch
	// ---------------------------------------------------------------------

	@Override
	protected void processKeyEvent(KeyEvent e) {
		if (e != null && e.getID() == KeyEvent.KEY_TYPED && shouldTreatAsReceivedText(e) && !shouldSuppressReceivedText(e)) {
			insertReceivedText(String.valueOf(e.getKeyChar()));
			e.consume();
			return;
		}
		if (e != null && !isEditing()) {
			if (e.getID() == KeyEvent.KEY_PRESSED && isClearCellKey(e)) {
				if (handleClearCellKey(e)) {
					e.consume();
					return;
				}
				if (startClearingCurrentCell()) {
					e.consume();
					return;
				}
			}
			if (e.getID() == KeyEvent.KEY_PRESSED && isReconversionKey(e)) {
				startEditingForReconversion(e);
				e.consume();
				return;
			}
			if (e.getID() == KeyEvent.KEY_TYPED && shouldStartTypingEdit(e)) {
				startEditingFromTypedKey(e);
				e.consume();
				return;
			}
		}
		super.processKeyEvent(e);
	}

	@Override
	protected void processInputMethodEvent(InputMethodEvent e) {
		if (e != null && shouldStartInputMethodEdit(e)) {
			inputMethodEditingSessionActive = true;
		}
		if (e != null && !isEditing() && shouldStartInputMethodEdit(e)) {
			startEditingFromInputMethod(e);
			e.consume();
			return;
		}
		if (e != null && isEditing() && dispatchInputMethodEvent(getEditorTextComponent(), e)) {
			e.consume();
			return;
		}
		super.processInputMethodEvent(e);
	}

	// ---------------------------------------------------------------------
	// Input classification
	// ---------------------------------------------------------------------

	private boolean shouldStartTypingEdit(KeyEvent e) {
		if (e == null)
			return false;
		if (e.isControlDown() || e.isAltDown() || e.isMetaDown())
			return false;
		char c = e.getKeyChar();
		return c != KeyEvent.CHAR_UNDEFINED && !Character.isISOControl(c);
	}

	private boolean shouldTreatAsReceivedText(KeyEvent e) {
		if (e == null || e.getID() != KeyEvent.KEY_TYPED)
			return false;
		if (e.isControlDown() || e.isAltDown() || e.isMetaDown())
			return false;
		char c = e.getKeyChar();
		return c != KeyEvent.CHAR_UNDEFINED && !Character.isISOControl(c) && c > 0x7f;
	}

	private boolean shouldSuppressReceivedText(KeyEvent e) {
		if (e == null)
			return false;
		return inputMethodEditingSessionActive || isEditorCompositionActive();
	}

	private boolean shouldStartInputMethodEdit(InputMethodEvent e) {
		return e.getID() == InputMethodEvent.INPUT_METHOD_TEXT_CHANGED && e.getText() != null;
	}

	private boolean isReconversionKey(KeyEvent e) {
		return e.getKeyCode() == KeyEvent.VK_CONVERT && !e.isControlDown() && !e.isAltDown() && !e.isMetaDown();
	}

	private boolean isClearCellKey(KeyEvent e) {
		if (e.getID() == KeyEvent.KEY_PRESSED) {
			return e.getKeyCode() == KeyEvent.VK_BACK_SPACE && !e.isControlDown() && !e.isAltDown() && !e.isMetaDown();
		}
		return e.getID() == KeyEvent.KEY_TYPED && e.getKeyChar() == '\b' && !e.isControlDown() && !e.isAltDown() && !e.isMetaDown();
	}

	// ---------------------------------------------------------------------
	// Input method helpers
	// ---------------------------------------------------------------------

	// ---------------------------------------------------------------------
	// Text initialization helpers
	// ---------------------------------------------------------------------

	private void requestReconversion(JTextComponent text) {
		try {
			InputContext inputContext = text.getInputContext();
			if (inputContext != null)
				inputContext.reconvert();
		} catch (RuntimeException ignored) {
			// Some input methods do not expose reconversion; selection remains active for the user's next IME action.
		}
	}

	private boolean shouldClearFieldOnTypedDigit(int row, int column, char typedChar) {
		if (!Character.isDigit(typedChar))
			return false;
		SpreadSheetModel model = getSpreadSheetModel();
		if (model == null)
			return false;
		Field field = model.getFieldInViewColumn(column);
		return field != null && field.isDate() && (field.isStartValue() || field.isEndValue());
	}

	/**
	 * Gives specialised spreadsheets a chance to handle Backspace before it is
	 * interpreted as clearing the lead cell.  In particular, a full-column
	 * selection is not a cell selection.
	 */
	protected boolean handleClearCellKey(KeyEvent e) {
		return false;
	}

	private void positionEditorCaretToEnd() {
		positionEditorCaretToEnd(getEditorTextComponent());
	}

	private void requestEditorFocus() {
		JTextComponent text = getEditorTextComponent();
		if (text != null) {
			text.requestFocusInWindow();
			return;
		}
		if (editorComp instanceof Component) {
			((Component)editorComp).requestFocusInWindow();
		}
	}

	private JTextComponent getEditorTextComponent() {
		if (!(editorComp instanceof Component component)) {
			return null;
		}
		if (component instanceof JTextComponent textComponent) {
			return textComponent;
		}
		if (component instanceof Container container) {
			for (Component child : container.getComponents()) {
				JTextComponent text = findTextComponent(child);
				if (text != null)
					return text;
			}
		}
		return null;
	}

	private JTextComponent findTextComponent(Component component) {
		if (component instanceof JTextComponent text)
			return text;
		if (component instanceof Container container) {
			for (Component child : container.getComponents()) {
				JTextComponent text = findTextComponent(child);
				if (text != null)
					return text;
			}
		}
		return null;
	}

	private EditableCellTarget resolveEditableCellTarget() {
		int row = hasSelectionModel() ? getSelection().getActiveRow() : -1;
		int column = hasSelectionModel() ? getSelection().getActiveColumn() : -1;
		if (row < 0)
			row = getCurrentRow();
		if (column < 0)
			column = getSelectedColumn();
		if (row < 0 && getRowCount() > 0) {
			row = 0;
		}
		if (column < 0 && getColumnCount() > 0) {
			column = 0;
		}
		if (row < 0 || column < 0) {
			return null;
		}
		return new EditableCellTarget(row, column);
	}

	private void resetEditorHorizontalOffset(JTextComponent text) {
		if (text instanceof JTextField textField) {
			textField.setScrollOffset(0);
		}
	}

	private void positionEditorCaretToEnd(JTextComponent text) {
		if (text == null)
			return;
		resetEditorHorizontalOffset(text);
		text.setCaretPosition(text.getDocument().getLength());
	}

	private CommonSpreadSheetModel getCommonSpreadSheetModel() {
		var model = getModel();
		return model instanceof CommonSpreadSheetModel commonModel ? commonModel : null;
	}

	private SpreadSheetModel getSpreadSheetModel() {
		var model = getModel();
		return model instanceof SpreadSheetModel spreadSheetModel ? spreadSheetModel : null;
	}

	private boolean hasSelectionModel() {
		return getSelection() != null;
	}

	private boolean hasRowHeaderSelection() {
		return rowHeaderSelectionActive;
	}

	private void selectRows(SpreadSheetSelectionModel selection, int startRow, int endRow) {
		if (getRowCount() > 0) {
			selection.getRowSelection().setSelectionInterval(startRow, endRow);
		}
	}

	private void selectColumns(SpreadSheetSelectionModel selection, int startColumn, int endColumn) {
		if (getColumnCount() > 0) {
			selection.getColumnSelection().setSelectionInterval(startColumn, endColumn);
		}
	}

	private ArrayList<Node> collectSelectedNodes(SpreadSheetModel model) {
		var rows = getSelectedRows();
		var nodes = new ArrayList<Node>(rows.length);
		for (int row : rows){
			GraphicNode graphicNode = model.getNode(row);
			if (graphicNode != null && graphicNode.getNode() != null) {
				nodes.add(graphicNode.getNode());
			}
		}
		return nodes;
	}

	private ArrayList<Object> collectSelectedNodeImpls(SpreadSheetModel model) {
		var rows = getSelectedRows();
		var nodes = new ArrayList<Object>(rows.length);
		for (int row : rows){
			GraphicNode graphicNode = model.getNode(row);
			if (graphicNode != null && graphicNode.getNode() != null) {
				nodes.add(graphicNode.getNode().getImpl());
			}
		}
		return nodes;
	}

	private ArrayList<Field> collectSelectedFields() {
		var columns = getSelectedColumns();
		var fields = new ArrayList<Field>(columns.length);
		SpreadSheetModel model = getSpreadSheetModel();
		if (model == null)
			return fields;
		for (int column : columns) {
			Field field = model.getFieldInViewColumn(column);
			if (field != null)
				fields.add(field);
		}
		return fields;
	}

	private ArrayList<Field> collectSelectableFields() {
		var fieldArray = getFieldArray();
		var fields = new ArrayList<Field>(fieldArray.size());
		fields.addAll(fieldArray);
		if (fields.size()>0) fields.remove(0); //ID not selectable
		return fields;
	}

	private Object getCurrentRowImpl(SpreadSheetModel model) {
		int row = getSelectedRow();
		return row < 0 ? null : model.getObjectInRow(row);
	}

	private Node getCurrentRowNode(SpreadSheetModel model) {
		var row = getCurrentRow();
		if (row < 0 || row >= getRowCount()) {
        	return null;
        }
        return model.getNodeInRow(row);
	}

	private void applyTypedChar(JTextComponent text, char typedChar, boolean clearTextOnStart) {
		if (text == null)
			return;
		if (clearTextOnStart) {
			text.setText("");
		}
		text.setText(String.valueOf(typedChar));
		positionEditorCaretToEnd(text);
		stabilizeDateEditorSelection(text);
	}

	private void applyReceivedText(JTextComponent text, String receivedText, boolean startNewEdit) {
		if (text == null)
			return;
		if (startNewEdit) {
			text.setText(receivedText);
		} else {
			text.replaceSelection(receivedText);
		}
		positionEditorCaretToEnd(text);
		stabilizeDateEditorSelection(text);
	}

	/** Applies every received character that arrived before the editor was ready. */
	private void flushReceivedText(JTextComponent text, boolean startNewEdit) {
		if (text == null || pendingReceivedText.isEmpty())
			return;
		String receivedText = pendingReceivedText.toString();
		pendingReceivedText.setLength(0);
		applyReceivedText(text, receivedText, startNewEdit);
	}

	private void stabilizeDateEditorSelection(JTextComponent text) {
		if (!(editorComp instanceof DateEditor.ExtDateField)) {
			return;
		}
		SwingUtilities.invokeLater(() -> {
			JTextComponent currentText = getEditorTextComponent();
			if (currentText == null || currentText != text) {
				return;
			}
			currentText.setSelectionStart(currentText.getDocument().getLength());
			currentText.setSelectionEnd(currentText.getDocument().getLength());
			currentText.setCaretPosition(currentText.getDocument().getLength());
		});
	}

	private boolean dispatchInputMethodEvent(JTextComponent text, InputMethodEvent sourceEvent) {
		if (text == null || sourceEvent == null)
			return false;
		InputMethodEvent editorEvent = copyInputMethodEvent(text, sourceEvent);
		if (editorEvent == null)
			return false;
		requestEditorFocus();
		text.dispatchEvent(editorEvent);
		return true;
	}

	private InputMethodEvent copyInputMethodEvent(Component target, InputMethodEvent sourceEvent) {
		if (target == null || sourceEvent == null)
			return null;
		AttributedCharacterIterator text = sourceEvent.getText();
		return new InputMethodEvent(target, sourceEvent.getID(), sourceEvent.getWhen(), text,
			sourceEvent.getCommittedCharacterCount(), sourceEvent.getCaret(), sourceEvent.getVisiblePosition());
	}

	private boolean isEditorCompositionActive() {
		JTextComponent text = getEditorTextComponent();
		if (!(text instanceof JComponent component)) {
			return false;
		}
		return Boolean.TRUE.equals(component.getClientProperty(COMPOSITION_PROPERTY));
	}

	private static final class EditableCellTarget {
		private final int row;
		private final int column;

		private EditableCellTarget(int row, int column) {
			this.row = row;
			this.column = column;
		}
	}

	private interface EditorStartAction {
		void apply(JTextComponent text);
	}

	private void installCommitAndMoveDownAction(JComponent component, final int row, final int column) {
		if (component == null)
			return;
		InputMap inputMap = component.getInputMap(JComponent.WHEN_FOCUSED);
		ActionMap actionMap = component.getActionMap();
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), COMMIT_AND_MOVE_DOWN_ACTION);
		actionMap.put(COMMIT_AND_MOVE_DOWN_ACTION, new AbstractAction() {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				CellEditor editor = getCellEditor();
				if (editor == null)
					return;
				boolean stopped = editor.stopCellEditing();
				if (stopped) {
					moveSelectionDownAfterCommit(row, column);
				}
			}
		});
	}

	private void moveSelectionDownAfterCommit(final int row, final int column) {
		rememberPendingUndoSelection(row, column);
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				if (getRowCount() <= 0 || getColumnCount() <= 0)
					return;
				int targetRow = Math.min(Math.max(row + 1, 0), getRowCount() - 1);
				int targetColumn = Math.min(Math.max(column, 0), getColumnCount() - 1);
				requestFocusInWindow();
				changeSelection(targetRow, targetColumn, false, false);
				scrollRectToVisible(getCellRect(targetRow, targetColumn, true));
			}
		});
	}

	private void rememberPendingUndoSelection(int row, int column) {
		Node node = null;
		Object impl = null;
		SpreadSheetModel model = getSpreadSheetModel();
		if (model != null && row >= 0 && row < getRowCount()) {
			node = model.getNodeInRow(row);
			impl = (node == null) ? null : node.getImpl();
		}
		int followRow = Math.min(Math.max(row + 1, 0), Math.max(getRowCount() - 1, 0));
		int followColumn = Math.min(Math.max(column, 0), Math.max(getColumnCount() - 1, 0));
		pendingUndoSelection = new PendingUndoSelection(node, impl, row, column, followRow, followColumn);
	}

	public PendingUndoSelection consumePendingUndoSelection(int currentRow, int currentColumn) {
		PendingUndoSelection selection = pendingUndoSelection;
		pendingUndoSelection = null;
		if (selection == null)
			return null;
		if (selection.followRow() != currentRow || selection.followColumn() != currentColumn)
			return null;
		return selection;
	}

	public static record PendingUndoSelection(Node node, Object impl, int row, int column, int followRow, int followColumn) {}

	private static final class StartEditEvent extends EventObject {
		private static final long serialVersionUID = 1L;
		private final boolean caretAtEnd;
		private final Character typedChar;
		private final boolean clearTextOnStart;
		private final boolean selectAllOnStart;

		private StartEditEvent(Object source, boolean caretAtEnd, Character typedChar, boolean clearTextOnStart, boolean selectAllOnStart) {
			super(source);
			this.caretAtEnd = caretAtEnd;
			this.typedChar = typedChar;
			this.clearTextOnStart = clearTextOnStart;
			this.selectAllOnStart = selectAllOnStart;
		}
	}

	//editing for example
	public int[] finishCurrentOperations(){
		int[] rows=null;
		if (isEditing()){
			lastEditingRow = getEditingRow();
			CellEditor editor=getCellEditor();
			if (editor!=null){
				rows=getSelectedRows();
				editor.stopCellEditing();//editor.cancelCellEditing();

			}
		}
		inputMethodEditingSessionActive = false;
		pendingReceivedText.setLength(0);
		//System.out.println("finishCurrentOperations()="+rows);
		return rows;
	}




	//node selection
	protected EventListenerList selectionNodeListenerList = new EventListenerList();

	public void addSelectionNodeListener(SelectionNodeListener l) {
	    selectionNodeListenerList.add(SelectionNodeListener.class, l);
	}
	public void removeSelectionNodeListener(SelectionNodeListener l) {
	    selectionNodeListenerList.remove(SelectionNodeListener.class, l);
	}
	public SelectionNodeListener[] getSelectionNodeListeners() {
		return (SelectionNodeListener[]) selectionNodeListenerList.getListeners(SelectionNodeListener.class);
	}
	public void fireContentsChanged(Object source, List<?> nodes, Node currentNode) {
		Object[] listeners = selectionNodeListenerList.getListenerList();
		SelectionNodeEvent e = null;
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == SelectionNodeListener.class) {
				if (e == null) {
					e = new SelectionNodeEvent(source,
							SelectionNodeEvent.SELECTION_CHANGED, nodes, currentNode,getSpreadSheetCategory());
				}
				((SelectionNodeListener) listeners[i + 1]).selectionChanged(e);
			}
		}
	}


    public EventListener[] getSelectionNodeListeners(Class listenerType) {
    	return selectionNodeListenerList.getListeners(listenerType);
    }

    public boolean isNodeDeletable(Node node) {
    	return true;
    }
    public boolean isNodeCuttable(Node node) {
    	return true;
    }
    public List<Node> getSelectedDeletableRows() {
    	var list = getSelectedNodes();
    	CollectionUtils.filter(list, new Predicate() {
			public boolean evaluate(Object arg0) {
				return isNodeDeletable((Node)arg0);
			}});
    	return list;

    }
    public List<Node> getSelectedCuttableRows(List<Node> nodes) {
    	CollectionUtils.filter(nodes, new Predicate() {
			public boolean evaluate(Object arg0) {
				return isNodeCuttable((Node)arg0);
			}});
    	return nodes;

    }
    public ArrayList<Node> getSelectedNodes(){
        var model = getSpreadSheetModel();
        if (model == null) {
        	return new ArrayList<Node>();
        }
		return collectSelectedNodes(model);
    }
    public ArrayList<Object> getSelectedNodesImpl(){
        var model = getSpreadSheetModel();
        if (model == null) {
        	return new ArrayList<Object>();
        }
		return collectSelectedNodeImpls(model);
    }
    public ArrayList<Field> getSelectedFields(){
    	if (hasRowHeaderSelection()) return null;
		return collectSelectedFields();
    }
    public ArrayList<Field> getSelectableFields(){
    	return collectSelectableFields();
    }

    public Object getCurrentRowImpl() {
        var model = getSpreadSheetModel();
        if (model == null) {
        	return null;
        }
        return getCurrentRowImpl(model);
    }
    public Node getCurrentRowNode() {
        var model = getSpreadSheetModel();
        if (model == null) {
        	return null;
        }
		return getCurrentRowNode(model);
    }
    public int getCurrentRow() {
        int row = getSelectedRow();
        if (row == -1)
        	row = getEditingRow();
        if (row == -1)
        	row = lastEditingRow;
        return row;

    }

    protected boolean cellEditable=true;

	public boolean isCellEditable(int row,int col) {
		return (cellEditable)?super.isCellEditable(row,col):false;
	}
	public void setCellEditable(boolean cellEditable) {
		this.cellEditable = cellEditable;
	}
    // edit triggered by click
	public boolean editCellAt(int row, int column, EventObject e){
		if (row < 0 || column < 0 || row >= getRowCount() || column >= getColumnCount()) {
			return false;
		}
		if (e == null)
			e = new StartEditEvent(this, true, null, false, false);
		if (e instanceof MouseEvent me && me.getClickCount() < 2) {
			return false;
		}
		SpreadSheetModel model = getSpreadSheetModel();
		if (column > 0 && model != null) {
			model.beginTaskCellEdit(row, column);
		}
		var editingStarted = super.editCellAt(row, column, e);
		if (!editingStarted && model != null)
			model.clearTaskCellEdit();
    	if (editingStarted && editorComp != null) {
//    		System.out.println("editing cell at " + row + " " + column);
    		configureEditorComponentAfterStart(row, column, e);
    	}
    	return editingStarted;
    }

	@Override
	public void removeEditor() {
		super.removeEditor();
		SpreadSheetModel model = getSpreadSheetModel();
		if (model != null) model.clearTaskCellEdit();
	}

	@Override
	public void editingStopped(ChangeEvent event) {
		SpreadSheetModel model = getSpreadSheetModel();
		if (model != null) model.beginTaskCellEditorCommit();
		try {
			super.editingStopped(event);
		} finally {
			if (model != null) model.endTaskCellEditorCommit();
		}
	}

	private void configureEditorComponentAfterStart(int row, int column, EventObject e) {
		Component component;
		var nameCell = false;
		if (editorComp instanceof NameCellComponent nameCellComponent) {
			nameCell = true;
			component = nameCellComponent.getTextComponent();
			if (component instanceof JComponent jComponent) {
				installCommitAndMoveDownAction(jComponent, row, column);
			}
		} else {
			component = editorComp;
		}
		if (editorComp instanceof JComponent jComponent) {
			installCommitAndMoveDownAction(jComponent, row, column);
		}

		var selectAll = true;
		var caretAtEnd = false;
		Character typedChar = null;
		var clearTextOnStart = false;
		if (e instanceof StartEditEvent startEditEvent) {
			selectAll = false;
			caretAtEnd = startEditEvent.caretAtEnd;
			typedChar = startEditEvent.typedChar;
			clearTextOnStart = startEditEvent.clearTextOnStart;
			selectAll = startEditEvent.selectAllOnStart;
		} else if (e == null) {
			selectAll = false;
			caretAtEnd = true;
		}

		var text = component instanceof JTextComponent textComponent ? textComponent : getEditorTextComponent();
		if (text != null) {
			installCommitAndMoveDownAction(text, row, column);
		}
		var shouldUseKeyboardFocusableSelectAll = !(caretAtEnd || typedChar != null);
		if (component instanceof KeyboardFocusable keyboardFocusable && shouldUseKeyboardFocusableSelectAll) {
			keyboardFocusable.selectAll(selectAll);
		}
		if (text == null) {
			return;
		}

		var mouseEditingNameCell = nameCell && e instanceof MouseEvent;
		if (nameCell && !mouseEditingNameCell) {
			resetEditorHorizontalOffset(text);
		}
		if (clearTextOnStart) {
			text.setText(typedChar == null ? "" : String.valueOf(typedChar));
			if (nameCell) {
				resetEditorHorizontalOffset(text);
			}
			text.setCaretPosition(text.getDocument().getLength());
			selectAll = false;
		} else if (typedChar != null) {
			text.setText(String.valueOf(typedChar));
			if (nameCell) {
				resetEditorHorizontalOffset(text);
			}
			text.setCaretPosition(text.getDocument().getLength());
			selectAll = false;
		}
		if (caretAtEnd) {
			if (nameCell) {
				resetEditorHorizontalOffset(text);
			}
			text.setCaretPosition(text.getDocument().getLength());
		}
		if (e instanceof MouseEvent me && nameCell) {
			handleNameCellMouseEdit(row, column, text, me);
			selectAll = false;
		}
		if (selectAll) {
			text.selectAll();
		}
		if (nameCell && !mouseEditingNameCell) {
			resetEditorHorizontalOffset(text);
		}
		if (text instanceof ChangeAwareComponent changeAwareComponent) {
			changeAwareComponent.resetChange();
			// Clearing via Backspace changes the value during editor setup.  The
			// reset above is still needed for ordinary editor initialization, but
			// must not turn this explicit clear into a no-op on commit.
			if (clearTextOnStart) {
				changeAwareComponent.markChanged();
			}
		}
    }

	private void handleNameCellMouseEdit(int row, int column, JTextComponent text, MouseEvent me) {
		var bounds = text.getBounds();
		var cell = getCellRect(row, column, false);
		bounds.setFrame(cell.getX() + bounds.getX(), cell.getY() + bounds.getY(), bounds.getWidth(), bounds.getHeight());
		if (!bounds.contains(me.getPoint())) {
			return;
		}
		positionCaretAtMousePoint(text, me, bounds);
	}

    private void positionCaretAtMousePoint(final JTextComponent text, final MouseEvent me, final Rectangle cellBounds) {
    	if (text == null || me == null || cellBounds == null)
    		return;
    	final Rectangle visibleBefore = text.getVisibleRect();
    	final int scrollOffsetBefore = (text instanceof JTextField) ? ((JTextField)text).getScrollOffset() : -1;
    	SwingUtilities.invokeLater(new Runnable() {
    		public void run() {
    			try {
    				Point localPoint = SwingUtilities.convertPoint(CommonSpreadSheet.this, me.getPoint(), text);
    				int pos = text.viewToModel2D(localPoint);
    				if (pos >= 0) {
    					int caret = pos;
    					try {
    						caret = getWordStartPosition(text, pos);
    					} catch (BadLocationException ex) {
    						caret = pos;
    					}
    					text.setCaretPosition(caret);
     					if (visibleBefore != null) {
     						text.scrollRectToVisible(visibleBefore);
     					}
     					restoreEditorHorizontalPosition(text, visibleBefore, scrollOffsetBefore);
    				}
    			} catch (RuntimeException ex) {
    				// If mapping fails, leave the default caret position in place.
    			}
    		}
    	});
    }

    private int getWordStartPosition(JTextComponent text, int position) throws BadLocationException {
    	if (text == null)
    		return position;
    	String content = text.getText();
    	if (content == null || content.length() == 0)
    		return 0;
    	int bounded = Math.max(0, Math.min(position, content.length()));
    	if (bounded == content.length() && bounded > 0)
    		bounded--;
    	if (bounded < 0 || bounded >= content.length())
    		return Math.max(0, Math.min(position, content.length()));
    	char current = content.charAt(bounded);
    	if (Character.isWhitespace(current))
    		return bounded;
    	return Utilities.getWordStart(text, bounded);
    }

	protected boolean editOnSelect=false;

	/**
	 * @return Returns the editOnSelect.
	 */
	public boolean isEditOnSelect() {
		return editOnSelect;
	}
	/**
	 * @param editOnSelect The editOnSelect to set.
	 */
	public void setEditOnSelect(boolean editOnSelect) {
		this.editOnSelect = editOnSelect;
	}

	public void changeSelection(int rowIndex, int columnIndex, boolean toggle,
			boolean extend) {
		changeSelection(rowIndex,columnIndex,toggle,extend,true);
	}
	public void changeSelection(int rowIndex, int columnIndex, boolean toggle,
			boolean extend,boolean forwards) {
		headerColumnSelectionActive = false;
		rowHeaderSelectionActive = false;
		super.changeSelection(rowIndex,columnIndex,toggle,extend);
		if (hasSelectionModel())
			getSelection().setActiveCell(rowIndex, columnIndex);
	}

	/** Starts editing the active task-table cell, preserving a cell click inside a whole-row selection. */
	public boolean editActiveCell() {
		EditableCellTarget target = resolveEditableCellTarget();
		return target != null && editCellAt(target.row, target.column,
				new StartEditEvent(this, true, null, false, false));
	}



	public void clearSelection() {
		headerColumnSelectionActive = false;
		rowHeaderSelectionActive = false;
		super.clearSelection();
		if (hasSelectionModel())
			getSelection().clearActiveCell();
	}

	public void selectRowAndAllColumns(int row) {
		headerColumnSelectionActive = false;
		rowHeaderSelectionActive = false;
		if (row < 0 || row >= getRowCount())
			return;
		if (!hasSelectionModel())
			return;
		SpreadSheetSelectionModel selection = getSelection();
		selectRows(selection, row, row);
		selectColumns(selection, 0, getColumnCount() - 1);
		selection.clearActiveCell();
		rowHeaderSelectionActive = true;
	}

	public void selectColumnAndAllRows(int column) {
		headerColumnSelectionActive = false;
		rowHeaderSelectionActive = false;
		if (column < 0 || column >= getColumnCount())
			return;
		if (!hasSelectionModel())
			return;
		SpreadSheetSelectionModel selection = getSelection();
		selectColumns(selection, column, column);
		selectRows(selection, 0, getRowCount() - 1);
		selection.clearActiveCell();
		headerColumnSelectionActive = true;
	}

	public void selectEntireSpreadsheet() {
		headerColumnSelectionActive = false;
		rowHeaderSelectionActive = true;
		if (!hasSelectionModel())
			return;
		SpreadSheetSelectionModel selection = getSelection();
		selectRows(selection, 0, getRowCount() - 1);
		selectColumns(selection, 0, getColumnCount() - 1);
		selection.clearActiveCell();
	}

	public boolean isRowFullySelected(int row) {
		if (row < 0 || row >= getRowCount())
			return false;
		return getSelectionModel().isSelectedIndex(row)
			&& getSelectedRowCount() == 1
			&& getSelectedRow() == row
			&& getSelectedColumnCount() == getColumnCount();
	}

	public boolean isColumnFullySelected(int column) {
		if (column < 0 || column >= getColumnCount())
			return false;
		return headerColumnSelectionActive
			&& getColumnModel().getSelectionModel().isSelectedIndex(column)
			&& getSelectedColumnCount() == 1
			&& getSelectedRowCount() == getRowCount();
	}

	public boolean isHeaderColumnSelectionActive() {
		return headerColumnSelectionActive;
	}

	public boolean isRowHeaderSelectionActive() {
		return rowHeaderSelectionActive;
	}

	public void setRowHeaderSelectionActive(boolean active) {
		rowHeaderSelectionActive = active;
		if (active) {
			headerColumnSelectionActive = false;
		}
	}

	protected void clearHeaderColumnSelectionState() {
		headerColumnSelectionActive = false;
	}


	/**
	 * @return Returns the spreadSheetCategory.
	 */
	public String getSpreadSheetCategory() {
		return spreadSheetCategory;
	}
	/**
	 * @param spreadSheetCategory The spreadSheetCategory to set.
	 */
	public void setSpreadSheetCategory(String spreadSheetCategory) {
		this.spreadSheetCategory = spreadSheetCategory;
	}

	@SuppressWarnings("unchecked")
	public List<Field> getAvailableFields() {
		return (List<Field>) SpreadSheetUtils.getFieldsForCategory(getSpreadSheetCategory());
	}

	protected void configureScrollPaneHeaders(JScrollPane scrollPane){
        if (scrollPane instanceof ScaledScrollPane)
        	scrollPane.setColumnHeaderView(((ScaledScrollPane)scrollPane).getTimeScaleComponent());
        else scrollPane.setColumnHeaderView(getTableHeader());
        JViewport vp=new JViewport();
        vp.setView(rowHeader);
        vp.setPreferredSize(rowHeader.getPreferredSize());
        scrollPane.setRowHeader(vp);
        corner=new SpreadSheetCorner(this);
        scrollPane.setCorner(JScrollPane.UPPER_LEFT_CORNER,corner);
        //scrollPane.setCorner(JScrollPane.LOWER_LEFT_CORNER,new GradientCorner());
	}

    protected void configureEnclosingScrollPane() {
    	super.configureEnclosingScrollPane();
     	Container p = getParent();
     	if (p instanceof JViewport) {
     		Container gp = p.getParent();
     		if (gp instanceof JScrollPane) {
     			JScrollPane scrollPane = (JScrollPane)gp;
     			JViewport viewport = scrollPane.getViewport();
     			if (viewport == null || viewport.getView() != this) return;
				FlatUiSupport.applyViewportSurface(viewport);

				 //fix the mouse wheel scroll but introduces a middle useless vertical scrollbar
//				scrollPane.setAutoscrolls(true);
//				scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);


     			configureScrollPaneHeaders(scrollPane);

     			Border border = scrollPane.getBorder();
     			if (border == null || border instanceof UIResource) {
     				scrollPane.setBorder(UIManager.getBorder("Table.scrollPaneBorder"));
     			}
     		}
     	}
     }

	public Node addNodeForImpl(Object impl) {
		return addNodeForImpl(impl,NodeModel.NORMAL);
	}
	public Node addNodeForImpl(Object impl,int eventType) {
		finishCurrentOperations();
        int row = getCurrentRow();
        if (row == -1)  { // fix for bug when inserting subproject and no selection
        	row = 0; // use 0th row if no selection
        	addRowSelectionInterval(0, 0);
        }
		Node current = getCurrentRowNode();
		Node newNode = NodeFactory.getInstance().createNode(impl);
        SpreadSheetModel model = getSpreadSheetModel();
        NodeModel nodeModel = model.getCache().getModel();
		nodeModel.addBefore(current,newNode,eventType);
        return newNode;
	}

    public void graphicNodesCompositeEvent(CompositeCacheEvent compositeEvent){
    	//System.out.println("cache event -> editCellAt");
    	if (isEditing()){
    		int row=getEditingRow();
    		int col=getEditingColumn();
    		TableCellEditor editor=getCellEditor();
    		editor.cancelCellEditing();
    		editCellAt(row,col,new StartEditEvent(this, true, null, false, false));
    	}
    }

    private void restoreEditorHorizontalPosition(final JTextComponent text, final Rectangle visibleBefore, final int scrollOffsetBefore) {
    	if (text == null)
    		return;
    	if (visibleBefore != null)
    		text.scrollRectToVisible(visibleBefore);
    	if (text instanceof JTextField && scrollOffsetBefore >= 0)
    		((JTextField)text).setScrollOffset(scrollOffsetBefore);
    	SwingUtilities.invokeLater(new Runnable() {
    		public void run() {
    			if (visibleBefore != null)
    				text.scrollRectToVisible(visibleBefore);
    			if (text instanceof JTextField && scrollOffsetBefore >= 0)
    				((JTextField)text).setScrollOffset(scrollOffsetBefore);
    		}
    	});
    }




	public SpreadSheetRowHeader getRowHeader() {
		return rowHeader;
	}


	public SpreadSheetCorner getCorner() {
		return corner;
	}


/**
 * For minor spreadsheets that have fixed columns, make sure they are not modifiable
 * @return
 */
    public final boolean isCanModifyColumns() {
		return canModifyColumns && SpreadSheetUtils.getFieldsForCategory(getSpreadSheetCategory()) != null;
	}



	public final void setCanModifyColumns(boolean canModifyColumns) {
		this.canModifyColumns = canModifyColumns;
	}

	public boolean isHasColumnHeaderPopup() {
		return isCanModifyColumns();
	}

	public final boolean isCanSelectFieldArray() {
		return canSelectFieldArray;
	}


	public final void setCanSelectFieldArray(boolean canSelectFieldArray) {
		this.canSelectFieldArray = canSelectFieldArray;
	}

	public void resizeAndRepaintHeader() { // this is really abstract
	}

	protected Exception lastException;

	public final Exception getLastException() {
		return lastException;
	}

	protected void doPostExceptionTreatment() {

	}
	public void setValueAt(Object arg0, int arg1, int arg2) {
		lastException = null; // initialize. will get set if a throw
		try {
			super.setValueAt(arg0, arg1, arg2);
		} catch (Exception e) { // because setValue has no exceptions, I package it in a runtime one
			lastException = (Exception) e.getCause(); // editors will use this value to see if exception
			if (lastException==null) lastException=e;
			Alert.error(lastException.getMessage(),this);

			doPostExceptionTreatment();
		}
	}

	public SearchContext createSearchContext() {
		SpreadSheetSearchContext ctx = new SpreadSheetSearchContext();
		return ctx;

	}

/**
 * Used by find dialog
 */
	public boolean findNext(SearchContext context) {
		SpreadSheetSearchContext ctx = (SpreadSheetSearchContext)context;

		int row = this.getCurrentRow();
		NodeModelCache currentCache = getCache();
		if (currentCache == null || currentCache.getSize() <= 0) {
			return false;
		}
		// make sure in bounds
		if (row < 0)
			row =0;
		if (row >= currentCache.getSize())
			row = currentCache.getSize() -1;

		ListIterator i = currentCache.getIterator(row);
		if (ctx.getRow() != -1) { // after the first search, need to move ahead or back
			if (ctx.isForward())
				if (i.hasNext())
					i.next();
			else
				if (i.hasPrevious())
					i.previous();
		}

		boolean found = false;
		GraphicNode gnode = null;
		Object obj;
		Node node;
		while (ctx.isForward() ? i.hasNext() : i.hasPrevious()) {
			gnode=(GraphicNode)(ctx.isForward() ? i.next() : i.previous());
			if (gnode.isVoid())
				continue;
			node = gnode.getNode();
			obj = node.getImpl();
			if (ctx.matches(obj)) {
				found = true;
				break;
			}
		}
		if (found) {
			int r = currentCache.getRowAt(gnode);
			int col = getFieldArray().indexOf(ctx.getField())-1;
			this.changeSelection(r, col, false, false);
			ctx.setRow(r);
		}
		return found;
	}

	public void selectObject(Object object) {
		int row = ((CommonSpreadSheetModel)getModel()).findObjectRow(object);
		if (row != -1) {
			finishCurrentOperations();
			changeSelection(row, getSelectedColumn(), false, false);
		}

	}

	public void restoreWorkspace(WorkspaceSetting w, int context) {
		// this checks for invalid conditions and continues

		Workspace ws = (Workspace) w;
		boolean legacyPositionState = ws.schemaVersion < 2;
		if (legacyPositionState && getRowCount() > ws.editingRow)
			setEditingRow(ws.editingRow);
		if (legacyPositionState && getColumnCount() > ws.editingColumn)
			setEditingColumn(ws.editingColumn);
		if (legacyPositionState && getRowCount() > ws.lastEditingRow)
			lastEditingRow = ws.lastEditingRow;
		if (legacyPositionState && ws.selectedRows != null) {
			for (int i=0; i < ws.selectedRows.length; i++) {
				try {
					addRowSelectionInterval(ws.selectedRows[i], ws.selectedRows[i]);
					// this isn't quite right.
					rowHeader.addRowSelectionInterval(ws.selectedRows[i], ws.selectedRows[i]);

				} catch (RuntimeException e) {
					// in case out of bounds
				}
			}
		}
		if (legacyPositionState && ws.selectedColumns != null) {
			for (int i=0; i < ws.selectedColumns.length; i++) {
				try {
					addColumnSelectionInterval(ws.selectedColumns[i], ws.selectedColumns[i]);
				} catch (RuntimeException e) {
					// in case out of bounds
				}
			}
		}
		SpreadSheetFieldArray s = (SpreadSheetFieldArray) Dictionary.get(getSpreadSheetCategory(),ws.fieldArrayName);
		if (s != null)
			setFieldArray(s);
     	Container p = getParent();
		if (legacyPositionState && p instanceof JViewport && ws.viewPosition != null) {
     		try {
     		((JViewport)p).setViewPosition(ws.viewPosition);
     		} catch (RuntimeException e) {
     			logger.log(Level.FINE, "problem restoring viewport to point {0}", ws.viewPosition);
     		}
     	}
	}



	public WorkspaceSetting createWorkspace(int context) {
		Workspace ws = new Workspace();
		ws.editingRow = getEditingRow();
		ws.editingColumn = getEditingColumn();
		ws.lastEditingRow = lastEditingRow;
		ws.selectedRows = getSelectedRows();
		ws.selectedColumns = getSelectedColumns();
		ws.fieldArrayName = getFieldArray().toString();
     	Container p = getParent();
     	if (p instanceof JViewport) {
     		ws.viewPosition = ((JViewport)p).getViewPosition();
     	}
		return ws;
	}

	public static class Workspace implements WorkspaceSetting {
		private static final long serialVersionUID = -847570793053006783L;
		int editingRow;
		int editingColumn;
		int lastEditingRow;
		int[] selectedRows=null;
		int[] selectedColumns=null;
		String fieldArrayName;
		Point viewPosition = null;
		int schemaVersion;
		String[] selectedEntityKeys;
		String[] selectedFieldIds;
		String activeEntityKey;
		String activeFieldId;
		String topVisibleEntityKey;
		String nextVisibleEntityKey;
		String previousVisibleEntityKey;
		int topVisibleRowOffset;

		public final int getEditingColumn() {
			return editingColumn;
		}

		public final void setEditingColumn(int editingColumn) {
			this.editingColumn = editingColumn;
		}

		public final int getEditingRow() {
			return editingRow;
		}

		public final void setEditingRow(int editingRow) {
			this.editingRow = editingRow;
		}

		public final String getFieldArrayName() {
			return fieldArrayName;
		}

		public final void setFieldArrayName(String fieldArrayName) {
			this.fieldArrayName = fieldArrayName;
		}

		public final int getLastEditingRow() {
			return lastEditingRow;
		}

		public final void setLastEditingRow(int lastEditingRow) {
			this.lastEditingRow = lastEditingRow;
		}

		public final int[] getSelectedColumns() {
			return selectedColumns;
		}

		public final void setSelectedColumns(int[] selectedColumns) {
			this.selectedColumns = selectedColumns;
		}

		public final int[] getSelectedRows() {
			return selectedRows;
		}

		public final void setSelectedRows(int[] selectedRows) {
			this.selectedRows = selectedRows;
		}

		public Point getViewPosition() {
			return viewPosition;
		}

		public void setViewPosition(Point viewPosition) {
			this.viewPosition = viewPosition;
		}

		public int getSchemaVersion() {
			return schemaVersion;
		}

		public void setSchemaVersion(int schemaVersion) {
			this.schemaVersion = schemaVersion;
		}

		public String[] getSelectedEntityKeys() {
			return selectedEntityKeys;
		}

		public void setSelectedEntityKeys(String[] selectedEntityKeys) {
			this.selectedEntityKeys = selectedEntityKeys;
		}

		public String[] getSelectedFieldIds() {
			return selectedFieldIds;
		}

		public void setSelectedFieldIds(String[] selectedFieldIds) {
			this.selectedFieldIds = selectedFieldIds;
		}

		public String getActiveEntityKey() {
			return activeEntityKey;
		}

		public void setActiveEntityKey(String activeEntityKey) {
			this.activeEntityKey = activeEntityKey;
		}

		public String getActiveFieldId() {
			return activeFieldId;
		}

		public void setActiveFieldId(String activeFieldId) {
			this.activeFieldId = activeFieldId;
		}

		public String getTopVisibleEntityKey() {
			return topVisibleEntityKey;
		}

		public void setTopVisibleEntityKey(String topVisibleEntityKey) {
			this.topVisibleEntityKey = topVisibleEntityKey;
		}

		public String getNextVisibleEntityKey() {
			return nextVisibleEntityKey;
		}

		public void setNextVisibleEntityKey(String nextVisibleEntityKey) {
			this.nextVisibleEntityKey = nextVisibleEntityKey;
		}

		public String getPreviousVisibleEntityKey() {
			return previousVisibleEntityKey;
		}

		public void setPreviousVisibleEntityKey(String previousVisibleEntityKey) {
			this.previousVisibleEntityKey = previousVisibleEntityKey;
		}

		public int getTopVisibleRowOffset() {
			return topVisibleRowOffset;
		}

		public void setTopVisibleRowOffset(int topVisibleRowOffset) {
			this.topVisibleRowOffset = topVisibleRowOffset;
		}
	}




}
