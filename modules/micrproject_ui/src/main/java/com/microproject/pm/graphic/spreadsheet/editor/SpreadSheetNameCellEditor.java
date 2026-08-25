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
package com.microproject.pm.graphic.spreadsheet.editor;

import java.awt.Component;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.TableCellEditor;

import com.microproject.pm.graphic.model.cache.GraphicNode;
import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetModel;
import com.microproject.pm.graphic.spreadsheet.renderer.CellUtility;
import com.microproject.pm.graphic.spreadsheet.renderer.FontManager;
import com.microproject.pm.graphic.spreadsheet.renderer.NameCellComponent;


/**
 * Adapter to modify defaults editors behaviour
 */
public class SpreadSheetNameCellEditor extends SpreadSheetCellEditorAdapter{
	protected static JTable lastTable;
	protected NameCellComponent component=null;
	
	public SpreadSheetNameCellEditor(TableCellEditor editor) {
		super(editor);
	}
	public static void clearLastTable() {
		lastTable = null;
	}

	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
		if (table!=null&&lastTable!=null&&table!=lastTable){
			if(lastTable.isEditing()){
				TableCellEditor lastEditor=lastTable.getCellEditor();
				if (lastEditor!=null) lastEditor.stopCellEditing();
			}
			lastTable.clearSelection();
		}
		lastTable=table;

		JComponent textComponent = (JComponent)editor.getTableCellEditorComponent(table,value,isSelected,row,column);
		if (textComponent instanceof JTextField) {
			((JTextField)textComponent).setScrollOffset(0);
		}
		prepareEditorComponent(textComponent);
		component = new NameCellComponent(textComponent);
		component.init();
		CellUtility.setAppearance(table, value, isSelected, true, row, column, component);
		if (table.getModel() instanceof SpreadSheetModel) {
			SpreadSheetModel model = (SpreadSheetModel)table.getModel();
			GraphicNode node = model.getNode(row);
			component.setText(value == null ? "" : value.toString());
			int level = model.getCache().getLevel(node);
			component.setLevel((node.isVoid()) ? (level + 1) : level);
			component.setLazy(node.isLazyParent());
			component.setFetched(node.isFetched());
			if (model.getCellProperties(node).isCompositeIcon()) {
				component.setCollapsed(model.getCache().isCollapsed(node));
			} else {
				component.setLeaf(node.isVoid());
			}
			FontManager.setComponentFont(model.getCellProperties(node), component);
			component.doLayout();
		}
		if (table instanceof SpreadSheet) {
			SpreadSheet spreadSheet = (SpreadSheet)table;
			installNameFieldTabActions(spreadSheet, textComponent);
		}
		return component;
	}
	
	
	/**
	 * @see javax.swing.CellEditor#getCellEditorValue()
	 */
	public Object getCellEditorValue() {
		return editor.getCellEditorValue();
	}
}
