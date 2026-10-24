/*******************************************************************************
 * MIT License
 *
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
import java.awt.Dimension;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JTable;

import com.microproject.field.BooleanText;
import com.microproject.field.Field;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetModel;
import com.microproject.pm.graphic.spreadsheet.renderer.YesNoRenderer;

/**
 * Edits a Yes/No field by choosing from a list, as Microsoft Project does
 * (issue #899): Yes then No, or for Task Mode Manually Scheduled then Auto
 * Scheduled with their icons. In a spreadsheet the committed value is the chosen
 * label, which {@code SpreadSheetModel.setValueAt} parses through the field, so
 * typing, paste and the list share one conversion and one Undo path. In a plain
 * table the committed value is the Boolean.
 */
public class YesNoComboBoxEditor extends SimpleComboBoxEditor {
	/** Client property on the combo box marking a Yes/No list, read by the sheet to open its popup. */
	public static final String LIST_CLIENT_PROPERTY = "microproject.yesNoList";

	private final String fieldId;
	private final List<String> choices;
	private boolean commitLabel = true;

	public YesNoComboBoxEditor(String fieldId) {
		super(new ListWideComboBox(new DefaultComboBoxModel<>(BooleanText.choices(fieldId).toArray())));
		this.fieldId = fieldId;
		this.choices = BooleanText.choices(fieldId);
		JComboBox<?> combo = (JComboBox<?>) getComponent();
		combo.putClientProperty(LIST_CLIENT_PROPERTY, Boolean.TRUE);
		@SuppressWarnings({"unchecked", "rawtypes"}) JComboBox raw = (JComboBox) combo;
		raw.setRenderer(new ChoiceRenderer());
	}

	public String getFieldId() {
		return fieldId;
	}

	/** The list items, in display order. */
	public List<String> getChoices() {
		return choices;
	}

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
		commitLabel = table == null || table.getModel() instanceof SpreadSheetModel;
		Boolean bool = value instanceof Boolean b ? b
			: value instanceof String text ? BooleanText.parse(fieldId, text) : null;
		Object label = bool == null ? choices.get(1) : choices.get(bool ? 0 : 1);
		return super.getTableCellEditorComponent(table, label, isSelected, row, column);
	}

	@Override
	public Object getCellEditorValue() {
		Object selected = super.getCellEditorValue();
		if (commitLabel)
			return selected;
		return Boolean.valueOf(choices.indexOf(selected) == 0);
	}

	@Override
	protected boolean isFreeTextAllowed(Field field) {
		return false; // the choices are fixed
	}

	/**
	 * A narrow Yes/No column must still show whole choices: the popup, which
	 * BasicComboPopup sizes from {@link #getSize()}, is as wide as the widest item.
	 */
	static final class ListWideComboBox extends JComboBox<Object> {
		private static final long serialVersionUID = 1L;
		private boolean layingOut;

		ListWideComboBox(DefaultComboBoxModel<Object> model) {
			super(model);
		}

		@Override
		public void doLayout() {
			layingOut = true;
			try {
				super.doLayout();
			} finally {
				layingOut = false;
			}
		}

		@Override
		public Dimension getSize() {
			Dimension size = super.getSize();
			if (!layingOut)
				size.width = Math.max(size.width, getPreferredSize().width);
			return size;
		}
	}

	private final class ChoiceRenderer extends DefaultListCellRenderer {
		private static final long serialVersionUID = 1L;

		@Override
		public Component getListCellRendererComponent(JList<?> list, Object item, int index, boolean selected,
				boolean focus) {
			super.getListCellRendererComponent(list, item, index, selected, focus);
			int position = choices.indexOf(item);
			setIcon(BooleanText.isTaskMode(fieldId) && position >= 0 ? YesNoRenderer.icon(position == 0) : null);
			return this;
		}
	}
}
