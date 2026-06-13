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
 * and left justified on the top left of the screen adjacent to the File menu. The 
 * logo must be at least 144 x 31 pixels. When users click on the "ProjectLibre" 
 * logo it must direct them back to http://www.projectlibre.com. 
 *******************************************************************************/
package com.projectlibre1.pm.graphic.spreadsheet.editor;

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;

import javax.swing.JTable;
import javax.swing.JTextField;
import net.sf.nachocalendar.components.DateField;
import net.sf.nachocalendar.table.DateFieldTableEditor;

import com.projectlibre1.pm.graphic.frames.GraphicManager;
import com.projectlibre1.pm.graphic.spreadsheet.SpreadSheetModel;
import com.projectlibre1.field.Field;
import com.projectlibre1.options.CalendarOption;
import com.projectlibre1.options.EditOption;
import com.projectlibre1.strings.Messages;
import com.projectlibre1.util.Alert;
import com.projectlibre1.util.DateTime;
import com.projectlibre1.util.FlatUiSupport;
import com.projectlibre1.util.YearlessDateInputParser;

public class DateEditor extends DateFieldTableEditor {
	protected ExtDateField dateField;
	private JTable table;
	private int editingRow = -1;
	private int editingColumn = -1;
	private Field editingField;
	private DateFormat editingFormat;
	private Date initialValue = null;
	public DateEditor() {
	}
	public static class ExtDateField extends DateField implements KeyboardFocusable {
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
		Field field = ((SpreadSheetModel)table.getModel()).getFieldInColumn(col+1);
		this.editingField = field;
		DateFormat format;
		if (field.isDateOnly())
			format = EditOption.getInstance().getShortDateFormat();
		else
			format = EditOption.getInstance().getDateFormat();
		this.editingFormat = format;

        dateField = new ExtDateField(format);
        dateField.setBorder(FlatUiSupport.tableEditorBorder());
        dateField.setBackground(FlatUiSupport.tableBackground());
        dateField.getTextField().setBackground(FlatUiSupport.tableBackground());
        if (value == null) {
        	long date = DateTime.midnightToday();
    		if (field.isStartValue())
				date = CalendarOption.getInstance().makeValidStart(date, true);
    		else if (field.isEndValue())
				date = CalendarOption.getInstance().makeValidEnd(date, true);
    		value = new Date(date);
        }
        dateField.setValue(value);
        dateField.getTextField().setSelectedTextColor(FlatUiSupport.tableSelectionForeground());
        dateField.getTextField().setSelectionColor(FlatUiSupport.tableSelectionBackground());
//        Date d = DateTime.gmtDate(new Date(DateTime.midnightToday()));
//        initialValue = (value == null) ? d : (Date) value;
        initialValue = (Date)value;
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
			return super.stopCellEditing();
		} else {
			try {
				date = YearlessDateInputParser.parse(text, editingFormat, findReferenceDate());
			} catch (ParseException | IllegalArgumentException e) {
				cancelCellEditing();
				Alert.warn(Messages.getString("Message.invalidDate"),dateField);
				return true;
			}
		}
		if (date.equals(initialValue)) {
			cancelCellEditing();
			return true;
		}
			
		dateField.setValue(date);
		return super.stopCellEditing();
	}

	private Date findReferenceDate() {
		if (table == null || editingRow <= 0 || editingField == null) {
			return null;
		}
		for (int row = editingRow - 1; row >= 0; row--) {
			Object candidate = table.getValueAt(row, editingColumn);
			if (candidate instanceof Date date && !DateTime.getZeroDate().equals(date)) {
				return date;
			}
		}
		return null;
	}
}
