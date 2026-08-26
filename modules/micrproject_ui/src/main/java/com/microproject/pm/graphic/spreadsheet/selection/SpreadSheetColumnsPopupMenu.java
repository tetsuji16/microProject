/*
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
 * furnished to do so subject to the following conditions:
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
 */
package com.microproject.pm.graphic.spreadsheet.selection;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;

import com.microproject.configuration.Dictionary;
import com.microproject.configuration.NamedItem;
import com.microproject.dialog.RenameDialog;
import com.microproject.field.Field;
import com.microproject.pm.graphic.spreadsheet.common.CommonSpreadSheet;
import com.microproject.strings.Messages;

/**
 * Column-layout picker shown from the spreadsheet corner: lists the named field
 * arrays registered under {@code type} in the {@link Dictionary} as radio
 * buttons. The default implementation treats each Dictionary entry as a full
 * field-array layout (task/resource spreadsheets); time-based spreadsheets
 * supply {@link TimeSpreadSheetColumnsPopupMenu} instead.
 */
public class SpreadSheetColumnsPopupMenu extends JPopupMenu {
	protected final CommonSpreadSheet spreadSheet;
	protected final String type;

	public SpreadSheetColumnsPopupMenu(CommonSpreadSheet spreadSheet, String type) {
		super();
		this.spreadSheet = spreadSheet;
		this.type = type;
		setContents();
	}

	/** True when {@code item} (a field-array layout) is the current column set. */
	protected boolean isSelected(Object item) {
		return spreadSheet.getFieldArray() == item;
	}

	/** Applies {@code item} (a field-array layout) as the column set. */
	protected void applySelection(Object item) {
		@SuppressWarnings("unchecked")
		ArrayList<Field> fields = (ArrayList<Field>) item;
		spreadSheet.setFieldArray(fields);
		if (item instanceof NamedItem)
			RenameDialog.doRename(spreadSheet, (NamedItem) item);
	}

	private void setContents() {
		for (Object item : getColumnDefinitions()) {
			add(new MenuAction(item.toString(), item, isSelected(item)));
		}
	}

	/** Column-definition items listed in the menu. Defaults to every entry under
	 *  {@code type} in the Dictionary; subclasses may narrow or reshape it. */
	protected Object[] getColumnDefinitions() {
		return Dictionary.getAll(type);
	}

	private final class MenuAction extends JRadioButtonMenuItem implements ActionListener {
		private final Object item;
		private final boolean current;
		private MenuAction(String text, Object item, boolean selected) {
			super(text);
			this.item = item;
			this.current = selected;
			if (selected)
				setText(Messages.format("Format.htmlEmphasizedWords",
						text, Messages.getString("Text.clickToRename")));
			this.setSelected(selected);
			this.addActionListener(this);
		}
		@Override
		public void actionPerformed(ActionEvent arg0) {
			spreadSheet.finishCurrentOperations();
			applySelection(item);
		}
	}
}
