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
 *******************************************************************************/
package com.projectlibre1.pm.graphic.spreadsheet.editor;

import java.awt.Component;
import java.text.NumberFormat;
import java.text.ParseException;

import javax.swing.JTable;
import javax.swing.JTextField;

import com.projectlibre1.datatype.PercentFormat;

public class PercentEditor extends SimpleEditor {
	private static final NumberFormat NUMBER_FORMAT = NumberFormat.getNumberInstance();

	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
		JTextField result = (JTextField)super.getTableCellEditorComponent(table, value, isSelected, row, column);
		result.setText(value == null ? "" : PercentFormat.getInstance().format(value));
		result.setHorizontalAlignment(JTextField.RIGHT);
		result.selectAll();
		return result;
	}

	public Object getCellEditorValue() {
		String text = component.getText();
		if (text == null)
			return null;
		text = text.trim();
		if (text.length() == 0)
			return null;
		try {
			Number parsed;
			if (text.indexOf('%') >= 0) {
				parsed = (Number)PercentFormat.getInstance().parseObject(text);
			} else {
				parsed = NUMBER_FORMAT.parse(text);
				double value = parsed.doubleValue();
				parsed = Double.valueOf(Math.abs(value) <= 1.0D ? value : value / 100.0D);
			}
			double value = parsed.doubleValue();
			if (value < 0.0D)
				value = 0.0D;
			if (value > 1.0D)
				value = 1.0D;
			return Double.valueOf(value);
		} catch (ParseException e) {
			return null;
		}
	}
}
