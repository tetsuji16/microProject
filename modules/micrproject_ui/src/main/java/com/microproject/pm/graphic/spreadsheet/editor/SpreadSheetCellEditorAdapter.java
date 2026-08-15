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
package com.microproject.pm.graphic.spreadsheet.editor;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.InputMethodEvent;
import java.awt.event.InputMethodListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.im.InputContext;
import java.util.EventObject;
import java.text.AttributedCharacterIterator;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.JComponent;
import javax.swing.InputMap;
import javax.swing.KeyStroke;
import javax.swing.JTable;
import javax.swing.event.CellEditorListener;
import javax.swing.table.TableCellEditor;

import com.microproject.menu.MenuActionConstants;
import com.microproject.pm.graphic.ChangeAwareTextField;
import com.microproject.pm.graphic.frames.GraphicManager;
import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetModel;
import com.microproject.pm.graphic.spreadsheet.renderer.CellUtility;
/**
 * Adapter to modify defaults editors behaviour
 */
public class SpreadSheetCellEditorAdapter implements TableCellEditor {
	protected static JTable lastTable;
	private static final String COMPOSITION_PROPERTY = "projectlibre.input.composing";
	private static final String NAME_TAB_INSTALL_PROPERTY = "projectlibre.nameTabActionsInstalled";
	private static final String NAME_COLLAPSE_ACTION = "spreadsheet.nameColumnCollapse";
	private static final String NAME_EXPAND_ACTION = "spreadsheet.nameColumnExpand";
	private static final String NAME_PREVIOUS_ACTION = "spreadsheet.nameColumnPrevious";
	private static final String NAME_NEXT_ACTION = "spreadsheet.nameColumnNext";
	private static final String NAME_UNDO_ACTION = "spreadsheet.nameColumnUndo";
	private static final String NAME_REDO_ACTION = "spreadsheet.nameColumnRedo";
	private static final String RECONVERT_ACTION = "spreadsheet.imeReconvert";
	protected TableCellEditor editor;
	private JComponent activeEditorComponent;
	public SpreadSheetCellEditorAdapter(TableCellEditor editor) {
		this.editor=editor;
		
	}
	public static void clearLastTable() {
		lastTable = null;
	}

	protected void prepareEditorComponent(JComponent edit) {
		activeEditorComponent = edit;
		edit.enableInputMethods(true);
		installReconversionAction(edit);
		installCompositionTracking(edit);
	}

	/**
	 * Swing's table editing path can consume VK_CONVERT before the active text
	 * component reaches the input method.  Route it explicitly to the editor's
	 * input context so a selected, already committed string can be reconverted
	 * by Microsoft IME.
	 */
	private void installReconversionAction(final JComponent edit) {
		InputMap inputMap = edit.getInputMap(JComponent.WHEN_FOCUSED);
		ActionMap actionMap = edit.getActionMap();
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_CONVERT, 0), RECONVERT_ACTION);
		actionMap.put(RECONVERT_ACTION, new AbstractAction() {
			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent e) {
				try {
					InputContext inputContext = edit.getInputContext();
					if (inputContext != null) {
						inputContext.reconvert();
					}
				} catch (RuntimeException ignored) {
					// Reconversion is optional for an input method; keep the selection intact.
				}
			}
		});
	}

	protected void clearActiveEditorComponent() {
		if (activeEditorComponent != null) {
			activeEditorComponent.putClientProperty(COMPOSITION_PROPERTY, Boolean.FALSE);
		}
		activeEditorComponent = null;
	}
	
	
	
	/**
	 * @see javax.swing.table.TableCellEditor#getTableCellEditorComponent(javax.swing.JTable, java.lang.Object, boolean, int, int)
	 */
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
		if (table!=null&&lastTable!=null&&table!=lastTable){
			if(lastTable.isEditing()){
				TableCellEditor lastEditor=lastTable.getCellEditor();
				if (lastEditor!=null) lastEditor.stopCellEditing();
			}
			lastTable.clearSelection();
		}
		lastTable=table;
		
		JComponent component=(JComponent)editor.getTableCellEditorComponent(table,value,isSelected,row,column);
		CellUtility.setAppearance(table,value,isSelected,true,row,column,component);
		
		if (table instanceof SpreadSheet){
			final SpreadSheet spreadSheet=(SpreadSheet)table;
			JComponent edit = (component instanceof DateEditor.ExtDateField) ? ((DateEditor.ExtDateField)component).getTextField() : component;
			prepareEditorComponent(edit);
			installClipboardActions(spreadSheet, edit);
			if (table.getModel() instanceof SpreadSheetModel && spreadSheet.isNameFieldColumn(column)) {
				installNameFieldTabActions(spreadSheet, edit);
			} else {
				resetNameFieldTabActions(edit);
			}
		} else {
			prepareEditorComponent(component);
		}
		
		return component;
	}

	protected void installClipboardActions(final SpreadSheet spreadSheet, JComponent edit) {
		edit.getActionMap().put("cut",new AbstractAction(){
			public void actionPerformed(java.awt.event.ActionEvent e) {
				spreadSheet.prepareAction(MenuActionConstants.ACTION_CUT).actionPerformed(new ActionEvent(spreadSheet,e.getID(),e.getActionCommand()));
			}
		});
		edit.getActionMap().put("copy",new AbstractAction(){
			public void actionPerformed(java.awt.event.ActionEvent e) {
				spreadSheet.prepareAction(MenuActionConstants.ACTION_COPY).actionPerformed(new ActionEvent(spreadSheet,e.getID(),e.getActionCommand()));
			}
		});
		edit.getActionMap().put("paste",new AbstractAction(){
			public void actionPerformed(java.awt.event.ActionEvent e) {
				spreadSheet.prepareAction(MenuActionConstants.ACTION_PASTE).actionPerformed(new ActionEvent(spreadSheet,e.getID(),e.getActionCommand()));
			}
		});
		edit.getActionMap().put("pasteInsert", new AbstractAction() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				spreadSheet.prepareAction(MenuActionConstants.ACTION_PASTE_INSERT).actionPerformed(new ActionEvent(spreadSheet,e.getID(),e.getActionCommand()));
			}
		});
		InputMap inputMap = edit.getInputMap(JComponent.WHEN_FOCUSED);
		inputMap.put(KeyStroke.getKeyStroke("shift ctrl V"), "pasteInsert");
	}

	protected void installNameFieldTabActions(final SpreadSheet spreadSheet, final JComponent edit) {
		edit.setFocusTraversalKeysEnabled(false);
		InputMap inputMap = edit.getInputMap(JComponent.WHEN_FOCUSED);
		ActionMap actionMap = edit.getActionMap();
		inputMap.put(KeyStroke.getKeyStroke("TAB"), SpreadSheet.NAME_COLUMN_INDENT_ACTION);
		inputMap.put(KeyStroke.getKeyStroke("shift TAB"), SpreadSheet.NAME_COLUMN_OUTDENT_ACTION);
		actionMap.put(SpreadSheet.NAME_COLUMN_INDENT_ACTION, new AbstractAction() {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				spreadSheet.executeNameCellTabAction(false);
			}
		});
		actionMap.put(SpreadSheet.NAME_COLUMN_OUTDENT_ACTION, new AbstractAction() {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				spreadSheet.executeNameCellTabAction(true);
			}
		});
		actionMap.put(NAME_COLLAPSE_ACTION, new AbstractAction() {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				spreadSheet.executeNameCellCollapseExpand(false);
			}
		});
		actionMap.put(NAME_EXPAND_ACTION, new AbstractAction() {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				spreadSheet.executeNameCellCollapseExpand(true);
			}
		});
		actionMap.put(NAME_PREVIOUS_ACTION, new AbstractAction() {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				spreadSheet.executeNameCellHierarchyJump(false);
			}
		});
		actionMap.put(NAME_NEXT_ACTION, new AbstractAction() {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				spreadSheet.executeNameCellHierarchyJump(true);
			}
		});
		inputMap.put(KeyStroke.getKeyStroke("ctrl Z"), NAME_UNDO_ACTION);
		inputMap.put(KeyStroke.getKeyStroke("ctrl Y"), NAME_REDO_ACTION);
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, KeyEvent.CTRL_DOWN_MASK), NAME_PREVIOUS_ACTION);
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, KeyEvent.CTRL_DOWN_MASK), NAME_NEXT_ACTION);
		actionMap.put(NAME_UNDO_ACTION, new AbstractAction() {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				spreadSheet.finishCurrentOperations();
				if (GraphicManager.getDocumentFrameInstance() != null)
					GraphicManager.getDocumentFrameInstance().doUndoRedo(true);
			}
		});
		actionMap.put(NAME_REDO_ACTION, new AbstractAction() {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				spreadSheet.finishCurrentOperations();
				if (GraphicManager.getDocumentFrameInstance() != null)
					GraphicManager.getDocumentFrameInstance().doUndoRedo(false);
			}
		});
		edit.putClientProperty(ChangeAwareTextField.NAME_HIERARCHY_COLLAPSE_ACTION_PROPERTY, actionMap.get(NAME_COLLAPSE_ACTION));
		edit.putClientProperty(ChangeAwareTextField.NAME_HIERARCHY_EXPAND_ACTION_PROPERTY, actionMap.get(NAME_EXPAND_ACTION));
		edit.putClientProperty(ChangeAwareTextField.NAME_HIERARCHY_PREVIOUS_ACTION_PROPERTY, actionMap.get(NAME_PREVIOUS_ACTION));
		edit.putClientProperty(ChangeAwareTextField.NAME_HIERARCHY_NEXT_ACTION_PROPERTY, actionMap.get(NAME_NEXT_ACTION));
	}

	protected void resetNameFieldTabActions(JComponent edit) {
		edit.setFocusTraversalKeysEnabled(true);
		InputMap inputMap = edit.getInputMap(JComponent.WHEN_FOCUSED);
		ActionMap actionMap = edit.getActionMap();
		inputMap.remove(KeyStroke.getKeyStroke("TAB"));
		inputMap.remove(KeyStroke.getKeyStroke("shift TAB"));
		inputMap.remove(KeyStroke.getKeyStroke("ctrl Z"));
		inputMap.remove(KeyStroke.getKeyStroke("ctrl Y"));
		inputMap.remove(KeyStroke.getKeyStroke(KeyEvent.VK_UP, KeyEvent.CTRL_DOWN_MASK));
		inputMap.remove(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, KeyEvent.CTRL_DOWN_MASK));
		actionMap.remove(SpreadSheet.NAME_COLUMN_INDENT_ACTION);
		actionMap.remove(SpreadSheet.NAME_COLUMN_OUTDENT_ACTION);
		actionMap.remove(NAME_COLLAPSE_ACTION);
		actionMap.remove(NAME_EXPAND_ACTION);
		actionMap.remove(NAME_PREVIOUS_ACTION);
		actionMap.remove(NAME_NEXT_ACTION);
		actionMap.remove(NAME_UNDO_ACTION);
		actionMap.remove(NAME_REDO_ACTION);
		edit.putClientProperty(ChangeAwareTextField.NAME_HIERARCHY_COLLAPSE_ACTION_PROPERTY, null);
		edit.putClientProperty(ChangeAwareTextField.NAME_HIERARCHY_EXPAND_ACTION_PROPERTY, null);
		edit.putClientProperty(ChangeAwareTextField.NAME_HIERARCHY_PREVIOUS_ACTION_PROPERTY, null);
		edit.putClientProperty(ChangeAwareTextField.NAME_HIERARCHY_NEXT_ACTION_PROPERTY, null);
	}

	protected void installCompositionTracking(final JComponent edit) {
		if (Boolean.TRUE.equals(edit.getClientProperty(NAME_TAB_INSTALL_PROPERTY))) {
			return;
		}
		edit.putClientProperty(NAME_TAB_INSTALL_PROPERTY, Boolean.TRUE);
		edit.putClientProperty(COMPOSITION_PROPERTY, Boolean.FALSE);
		edit.addInputMethodListener(new InputMethodListener() {
			public void inputMethodTextChanged(InputMethodEvent event) {
				edit.putClientProperty(COMPOSITION_PROPERTY, Boolean.valueOf(isComposing(event)));
			}
			public void caretPositionChanged(InputMethodEvent event) {
			}
		});
	}

	protected boolean isComposing(InputMethodEvent event) {
		AttributedCharacterIterator text = event.getText();
		if (text == null) {
			return false;
		}
		int committed = event.getCommittedCharacterCount();
		int length = text.getEndIndex() - text.getBeginIndex();
		return committed < length;
	}

	protected boolean isCompositionActive(JComponent edit) {
		return Boolean.TRUE.equals(edit.getClientProperty(COMPOSITION_PROPERTY));
	}

	/**
	 * @see javax.swing.CellEditor#addCellEditorListener(javax.swing.event.CellEditorListener)
	 */
	public void addCellEditorListener(CellEditorListener l) {
		editor.addCellEditorListener(l);
	}
	/**
	 * @see javax.swing.CellEditor#cancelCellEditing()
	 */
	public void cancelCellEditing() {
		clearActiveEditorComponent();
		editor.cancelCellEditing();
	}
	/**
	 * @see javax.swing.CellEditor#getCellEditorValue()
	 */
	public Object getCellEditorValue() {
		Object value=editor.getCellEditorValue();
		return value;
	}
	/**
	 * Special behavior here to be able to control edition and trigger it by JTable.editCellAt
	 * 
	 * @see javax.swing.CellEditor#isCellEditable(java.util.EventObject)
	 */
	public boolean isCellEditable(EventObject event) {
		if (event == null)
			return true;
		if (event instanceof MouseEvent)
			return ((MouseEvent)event).getClickCount() >= 2;
		if (event instanceof InputMethodEvent)
			return true;
		if (event instanceof KeyEvent) {
			KeyEvent keyEvent = (KeyEvent)event;
			return keyEvent.getID() == KeyEvent.KEY_TYPED
				|| (keyEvent.getID() == KeyEvent.KEY_PRESSED && keyEvent.getKeyCode() == KeyEvent.VK_CONVERT);
		}
		return true;
	}
	/**
	 * @see javax.swing.CellEditor#removeCellEditorListener(javax.swing.event.CellEditorListener)
	 */
	public void removeCellEditorListener(CellEditorListener l) {
		editor.removeCellEditorListener(l);
	}
	/**
	 * @see javax.swing.CellEditor#shouldSelectCell(java.util.EventObject)
	 */
	public boolean shouldSelectCell(EventObject event){
		return editor.shouldSelectCell(event);
	}
	/**
	 * @see javax.swing.CellEditor#stopCellEditing()
	 */
	public boolean stopCellEditing() {
		if (activeEditorComponent != null && isCompositionActive(activeEditorComponent))
			return false;
		clearActiveEditorComponent();
		return editor.stopCellEditing();
	}
	
}

