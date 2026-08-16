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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;

import javax.swing.AbstractCellEditor;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.TableCellEditor;

import com.microproject.pm.graphic.frames.GraphicManager;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetModel;
import com.microproject.field.Field;
import com.microproject.strings.Messages;
import com.microproject.util.Alert;
import com.microproject.util.DateFieldSupport;
import com.microproject.util.FlatUiSupport;

public class DateEditor extends AbstractCellEditor implements TableCellEditor {
	protected ExtDateField dateField;
	private JTable table;
	private int editingRow = -1;
	private int editingColumn = -1;
	private DateFormat editingFormat;
	private Date initialValue = null;
	private String initialFormattedText = null;
	public DateEditor() {
	}
	public static class ExtDateField extends com.microproject.dialog.util.ProjectLibreDateField implements KeyboardFocusable {
		public ExtDateField(DateFormat df) {
			super(df);
			addMouseListener();
		}
		public ExtDateField() {
			addMouseListener();
		}
		private void addMouseListener() {
			getTextField().addMouseListener(new MouseAdapter() {
				public void mousePressed(MouseEvent e) {
					if (e.getClickCount() == 2)
						GraphicManager.getInstance(ExtDateField.this).doInformationDialog(false);
				}
			});
			
		}
		public void requestFocus() { // override default needed otherwise key handling is wrong (backspace, arrows
			getTextField().requestFocus();
		}

		public JTextField getTextField() { // convenience method
			return (JTextField) getFormattedTextField();
		}
		
		public void selectAll(boolean keyboard) { // convenience method
			EditorSelectionSupport.selectAllWithOptionalRefocus(getTextField(), keyboard);
		}
		public String toString() {
			return getTextField().getText();
		}

	}
    public Component getTableCellEditorComponent(JTable table, Object value,
            boolean isSelected, int row, int col) {
		this.table = table;
		this.editingRow = row;
		this.editingColumn = col;
		Field field = ((SpreadSheetModel)table.getModel()).getFieldInViewColumn(col);
		DateFormat format = DateFieldSupport.dateFormatFor(field);
		this.editingFormat = format;

        dateField = new ExtDateField(format);
        dateField.setBorder(FlatUiSupport.tableEditorBorder());
        dateField.setBackground(FlatUiSupport.tableBackground());
        dateField.getTextField().setBackground(FlatUiSupport.tableBackground());
        if (value == null) {
        	value = DateFieldSupport.defaultDateFor(field);
        }
        dateField.setValue(value);
        dateField.getTextField().setSelectedTextColor(FlatUiSupport.tableSelectionForeground());
        dateField.getTextField().setSelectionColor(FlatUiSupport.tableSelectionBackground());
        initialValue = (Date)value;
        initialFormattedText = dateField.getFormattedTextField().getText();
        return dateField;
    }
	@Override
	public Object getCellEditorValue() {
		return dateField.getValue();
	}
	@Override
	public boolean stopCellEditing() {
		
		String text = dateField.getFormattedTextField().getText();
		Date date;
		if (text.equals("")) { // empty text means Zero time
			if (initialValue == null) {
				cancelCellEditing();
				return true;
			}
			dateField.setValue(null);
			return super.stopCellEditing();
		} else {
			try {
				date = DateFieldSupport.parseYearless(text, editingFormat, findReferenceDate());
			} catch (ParseException | IllegalArgumentException e) {
				cancelCellEditing();
				Alert.warn(Messages.getString("Message.invalidDate"),dateField);
				return true;
			}
		}
		if (matchesInitialValue(date)) {
			cancelCellEditing();
			return true;
		}
			
		dateField.setValue(date);
		return super.stopCellEditing();
	}

	private Date findReferenceDate() {
		return DateFieldSupport.referenceDateFromPreviousRows(table, editingRow, editingColumn);
	}

	private boolean matchesInitialValue(Date date) {
		if (date == null || initialValue == null) {
			return date == null && initialValue == null;
		}
		if (date.equals(initialValue)) {
			return true;
		}
		if (editingFormat == null) {
			return false;
		}
		String formattedDate = editingFormat.format(date);
		String formattedInitialValue = initialFormattedText != null ? initialFormattedText : editingFormat.format(initialValue);
		return formattedDate.equals(formattedInitialValue);
	}
}

